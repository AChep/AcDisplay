/*
 * Copyright (C) 2014 AChep@xda <artemchep@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package com.achep.acdisplay.services.activemode.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.services.activemode.ActiveModeSensor;
import com.achep.base.content.ConfigBase;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static com.achep.base.Build.DEBUG;

/**
 * Basing on results of proximity sensor it notifies when
 * {@link com.achep.acdisplay.ui.activities.AcDisplayActivity AcDisplay}
 * should be shown.
 *
 * @author Artem Chepurnoy
 */
public final class ProximitySensor extends ActiveModeSensor implements
        SensorEventListener, ConfigBase.OnConfigChangedListener {

    private static final String TAG = "ProximitySensor";

    private static final int LAST_EVENT_MAX_TIME = 1000; // ms.

    // pocket program
    private static final int POCKET_START_DELAY = 4000; // ms.

    private static WeakReference<ProximitySensor> sProximitySensorWeak;
    private static long sLastEventTime;
    private static boolean sAttached;
    private static boolean sNear;

    private float mMaximumRange;
    private boolean mFirstChange;

    @NonNull
    private final Object monitor = new Object();

    private final ArrayList<Program> mPrograms;
    private final ArrayList<Event> mHistory;
    private final Handler mHandler;
    private int mHistoryMaximumSize;

    private final Program mPocketProgram;
    private final Program mWave2WakeProgram;

    private static class Program {

        @NonNull
        public final Data[] dataArray;

        private static class Data {
            public final boolean isNear;
            public int timeMin;
            public final long timeMax;

            public Data(boolean isNear, int timeMin, long timeMax) {
                this.isNear = isNear;
                this.timeMin = timeMin;
                this.timeMax = timeMax;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public int hashCode() {
                return new HashCodeBuilder(31, 3615)
                        .append(isNear)
                        .append(timeMin)
                        .append(timeMax)
                        .toHashCode();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean equals(Object o) {
                if (o == this)
                    return true;
                if (!(o instanceof Data))
                    return false;

                Data data = (Data) o;
                return new EqualsBuilder()
                        .append(isNear, data.isNear)
                        .append(timeMin, data.timeMin)
                        .append(timeMax, data.timeMax)
                        .isEquals();
            }
        }

        public Program(@NonNull Data[] dataArray) {
            this.dataArray = dataArray;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return new HashCodeBuilder(2369, 31)
                    .append(dataArray)
                    .toHashCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof Program))
                return false;

            Program program = (Program) o;
            return new EqualsBuilder()
                    .append(dataArray, program.dataArray)
                    .isEquals();
        }

        public static int fits(@NonNull Program program, @NonNull ArrayList<Event> history) {
            Data[] dataArray = program.dataArray;

            int historySize = history.size();
            int programSize = dataArray.length;
            if (historySize < programSize) {
                // Program needs slightly longer history.
                return -1;
            }

            int historyOffset = historySize - programSize;
            Event eventPrevious = history.get(historyOffset);

            for (int i = 1; i < programSize; i++) {
                Data data = dataArray[i - 1];
                Event eventFuture = history.get(historyOffset + i);

                final long delta = eventFuture.time - eventPrevious.time;

                if (eventPrevious.isNear != data.isNear
                        || delta <= data.timeMin
                        || delta >= data.timeMax) {
                    return -1;
                }

                eventPrevious = eventFuture;
            }

            Data data = dataArray[programSize - 1];
            if (eventPrevious.isNear == data.isNear) {
                return data.timeMin;
            }

            return -1;
        }

        public static class Builder {

            private final ArrayList<Data> mProgram = new ArrayList<>(10);
            private boolean mLastNear;

            @NonNull
            public Builder begin(boolean isNear, int timeMin) {
                return add(isNear, timeMin, Long.MAX_VALUE);
            }

            @NonNull
            public Builder add(int timeMin, long timeMax) {
                return add(!mLastNear, timeMin, timeMax);
            }

            @NonNull
            public Builder end(int timeMin) {
                return add(timeMin, 0);
            }

            @NonNull
            private Builder add(boolean isNear, int timeMin, long timeMax) {
                Data data = new Data(isNear, timeMin, timeMax);
                mProgram.add(data);
                mLastNear = isNear;
                return this;
            }

            @NonNull
            public Program build() {
                return new Program(mProgram.toArray(new Data[mProgram.size()]));
            }

        }

    }

    /**
     * Proximity event.
     */
    private static class Event {
        final boolean isNear;
        final long time;

        public Event(boolean isNear, long time) {
            this.isNear = isNear;
            this.time = time;
        }

    }

    private ProximitySensor() {
        super();
        mPocketProgram = new Program.Builder()
                .begin(true, POCKET_START_DELAY) /* is near at least for some seconds */
                .end(0) /* and after: is far  at least for 0 seconds */
                .build();
        mWave2WakeProgram = new Program.Builder()
                .begin(true, 200) /*        is near at least for 200 millis */
                .add(0, 1500) /* and after: is far  not more than 1 second  */
                .add(0, 1500) /* and after: is near not more than 1 second  */
                .end(0)       /* and after: is far  at least for  0 second  */
                .build();

        mPrograms = new ArrayList<>();
        mPrograms.add(mPocketProgram);
        mPrograms.add(mWave2WakeProgram); // needed to include in history size calculation

        for (Program program : mPrograms) {
            int size = program.dataArray.length;
            if (size > mHistoryMaximumSize) {
                mHistoryMaximumSize = size;
            }
        }

        mHistory = new ArrayList<>(mHistoryMaximumSize);
        mHandler = new Handler();
    }

    @NonNull
    public static ProximitySensor getInstance() {
        ProximitySensor sensor = sProximitySensorWeak != null
                ? sProximitySensorWeak.get() : null;
        if (sensor == null) {
            sensor = new ProximitySensor();
            sProximitySensorWeak = new WeakReference<>(sensor);
        }
        return sensor;
    }

    /**
     * @return {@code true} if sensor is currently in "near" state, and {@code false} otherwise.
     */
    public static boolean isNear() {
        return (getTimeNow() - sLastEventTime < LAST_EVENT_MAX_TIME || sAttached) && sNear;
    }

    @Override
    public int getType() {
        return Sensor.TYPE_PROXIMITY;
    }

    @Override
    public void onStart(@NonNull SensorManager sensorManager) {
        if (DEBUG) Log.d(TAG, "Starting proximity sensor...");

        mHistory.clear();
        mHistory.add(new Event(false, getTimeNow()));

        Config.getInstance().registerListener(this);
        updateWave2WakeProgram();

        // Ignore pocket program's start delay,
        // so app can act just after it has started.
        mFirstChange = true;
        mPocketProgram.dataArray[0].timeMin = 0;

        Sensor proximitySensor = sensorManager.getDefaultSensor(getType());
        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);

        mMaximumRange = proximitySensor.getMaximumRange();
        sAttached = true;
    }

    @Override
    public void onStop() {
        if (DEBUG) Log.d(TAG, "Stopping proximity sensor...");

        SensorManager sensorManager = getSensorManager();
        sensorManager.unregisterListener(this);
        mHandler.removeCallbacksAndMessages(null);
        mHistory.clear();

        Config.getInstance().unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        final float distance = event.values[0];
        final boolean isNear = distance < mMaximumRange || distance < 1.0f;
        final boolean changed = sNear != (sNear = isNear) || mFirstChange;

        synchronized (monitor) {
            long now = getTimeNow();
            if (DEBUG) {
                int historySize = mHistory.size();
                String delta = (historySize > 0
                        ? " delta=" + (now - mHistory.get(historySize - 1).time)
                        : " first_event");
                Log.d(TAG + ":Event", "distance=" + distance
                        + " is_near=" + isNear
                        + " changed=" + changed
                        + delta);
            }

            if (!changed) {
                // Well just in cause if proximity sensor is NOT always eventual.
                // This should not happen, but who knows... I found maximum
                // range buggy enough.
                return;
            }

            while (mHistory.size() >= mHistoryMaximumSize)
                mHistory.remove(0);

            mHandler.removeCallbacksAndMessages(null);
            mHistory.add(new Event(isNear, now));
            for (Program program : mPrograms) {
                int delay = Program.fits(program, mHistory);
                if (delay >= 0) {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (monitor) {
                                mHandler.removeCallbacksAndMessages(null);
                                mHistory.clear();
                                requestWakeUp();
                            }
                        }
                    }, delay);
                }
            }

            if (mFirstChange) {
                // Change pocket program back to defaults.
                mPocketProgram.dataArray[0].timeMin = POCKET_START_DELAY;
            }

            sLastEventTime = now;
            mFirstChange = false;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { /* unused */ }

    @Override
    public void onConfigChanged(@NonNull ConfigBase config,
                                @NonNull String key,
                                @NonNull Object value) {
        switch (key) {
            case Config.KEY_ACTIVE_MODE_WAVE_TO_WAKE:
                updateWave2WakeProgram();
                break;
        }
    }

    private void updateWave2WakeProgram() {
        synchronized (monitor) {
            boolean enabled = Config.getInstance().isActiveModeWaveToWakeEnabled();
            if (enabled) {
                if (!mPrograms.contains(mWave2WakeProgram)) {
                    if (DEBUG) Log.d(TAG, "Added the \"Wave to wake\" program");
                    mPrograms.add(mWave2WakeProgram);
                }
            } else {
                if (DEBUG) Log.d(TAG, "Removed the \"Wave to wake\" program");
                mPrograms.remove(mWave2WakeProgram);
            }
        }
    }

}
