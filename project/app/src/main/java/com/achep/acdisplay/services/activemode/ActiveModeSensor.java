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
package com.achep.acdisplay.services.activemode;

import android.content.Context;
import android.hardware.SensorManager;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.achep.acdisplay.services.activemode.sensors.ProximitySensor;
import com.achep.base.async.WeakHandler;

import java.util.ArrayList;

/**
 * Provides a callback when {@link com.achep.acdisplay.ui.activities.AcDisplayActivity}
 * should be started and stopped.
 *
 * @author Artem Chepurnoy
 */
public abstract class ActiveModeSensor {

    int mAttachedNumber;
    private Context mContext;
    private SensorManager mSensorManager;

    private final ArrayList<Callback> mCallbacks;

    /**
     * Provides a callback when AcDisplay should be shown / hidden.
     *
     * @author Artem Chepurnoy
     */
    public interface Callback {

        /**
         * Requests to show the AcDisplay.
         */
        void onWakeRequested(@NonNull ActiveModeSensor sensor);

    }

    protected ActiveModeSensor() {
        mCallbacks = new ArrayList<>(4);
    }

    /**
     * Registers given callback to listen to this sensor.
     * You must call {@link #unregisterCallback(ActiveModeSensor.Callback)}
     * later.
     *
     * @see #unregisterCallback(ActiveModeSensor.Callback)
     */
    public void registerCallback(@NonNull Callback callback) {
        mCallbacks.add(callback);
    }

    /**
     * Unregisters given callback from this sensor.
     *
     * @see #registerCallback(ActiveModeSensor.Callback)
     */
    public void unregisterCallback(@NonNull Callback callback) {
        mCallbacks.remove(callback);
    }

    protected void requestWakeUp/*, Neo!*/() {
        for (Callback callback : mCallbacks) {
            callback.onWakeRequested(this);
        }
    }

    /**
     * Checks if this sensor is supported by device.
     * By default it does the following code:
     * {@code sensorManager.getSensorList(getType()).size() > 0}
     *
     * @return {@code true} if the sensor is supported by device, {@code false} otherwise.
     */
    public boolean isSupported(@NonNull SensorManager sensorManager) {
        return sensorManager.getSensorList(getType()).size() > 0;
    }

    /**
     * @return The type of used sensor.
     * @see android.hardware.Sensor#TYPE_ACCELEROMETER
     * @see android.hardware.Sensor#TYPE_PROXIMITY
     */
    public abstract int getType();

    public abstract void onStart(@NonNull SensorManager sensorManager);

    public abstract void onStop();

    /**
     * Called when the sensor is attached to main class.
     */
    public void onAttached(@NonNull SensorManager sensorManager, @NonNull Context context) {
        // Register sensors only once.
        if (mAttachedNumber++ > 0) {
            return;
        }

        setup(sensorManager, context);
        onStart(sensorManager);
    }

    /**
     * Called when the sensor is detached from main class.
     */
    public void onDetached() {
        if (--mAttachedNumber > 0) {
            return;
        }

        onStop();
        setup(null, null);
    }

    void setup(@Nullable SensorManager sensorManager, @Nullable Context context) {
        mContext = context;
        mSensorManager = sensorManager;
    }

    public boolean isAttached() {
        return mAttachedNumber > 0;
    }

    @NonNull
    public Context getContext() {
        return mContext;
    }

    @NonNull
    public SensorManager getSensorManager() {
        return mSensorManager;
    }

    /**
     * @return {@code SystemClock.elapsedRealtime()}
     */
    protected static long getTimeNow() {
        return SystemClock.elapsedRealtime();
    }

    public abstract static class Consuming extends ActiveModeSensor {

        private static final String TAG = "ConsumingSensor";

        static final int DEFAULT_REMAINING_TIME = 3000; // 3 sec.

        private static final int START = 0;
        private static final int STOP = 1;

        private boolean mRunning;

        private final H mHandler = new H(this);

        private static class H extends WeakHandler<Consuming> {

            public H(@NonNull Consuming object) {
                super(object);
            }

            @Override
            protected void onHandleMassage(@NonNull Consuming c, Message msg) {
                if (c.mRunning == (msg.what == START)) return;
                switch (msg.what) {
                    case START:
                        c.mRunning = true;
                        c.onStart(c.getSensorManager());
                        break;
                    case STOP:
                        c.onStop();
                        c.mRunning = false;
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }
        }

        /**
         * Specifies how long sensor should be active after receiving
         * a ping.
         *
         * @return time in millis.
         * @see #DEFAULT_REMAINING_TIME
         */
        public int getRemainingTime() {
            return DEFAULT_REMAINING_TIME;
        }

        @Override
        protected void requestWakeUp() {
            // Do not allow waking up in your pocket.
            if (ProximitySensor.isNear()) {
                return;
            }

            super.requestWakeUp();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onAttached(@NonNull SensorManager sensorManager, @NonNull Context context) {
            // Register sensors only once.
            if (mAttachedNumber++ > 0) {
                return;
            }

            setup(sensorManager, context);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onDetached() {
            if (--mAttachedNumber > 0) {
                return;
            }

            // Now die
            mHandler.removeCallbacksAndMessages(null);
            mHandler.sendEmptyMessage(STOP);

            setup(null, null);
        }

        public void ping(int remainingTime) {
            start();
            mHandler.removeMessages(STOP);
            mHandler.sendEmptyMessageDelayed(STOP, remainingTime);
        }

        /**
         * Starts the consuming sensor 'forever'.
         *
         * @see #stop()
         */
        public void start() {
            mHandler.sendEmptyMessage(START);
        }

        /**
         * Stops the consuming sensor.
         *
         * @see #stop()
         */
        public void stop() {
            mHandler.sendEmptyMessage(STOP);
        }

    }

}
