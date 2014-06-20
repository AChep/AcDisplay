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
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;

import java.util.ArrayList;

/**
 * Provides a callback when {@link com.achep.acdisplay.acdisplay.AcDisplayActivity}
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
        public void onWakeRequested(ActiveModeSensor sensor);

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
    public void registerCallback(Callback callback) {
        mCallbacks.add(callback);
    }

    /**
     * Unregisters given callback from this sensor.
     *
     * @see #registerCallback(ActiveModeSensor.Callback)
     */
    public void unregisterCallback(Callback callback) {
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
    protected boolean isSupported(SensorManager sensorManager, Context context) {
        return sensorManager.getSensorList(getType()).size() > 0;
    }

    /**
     * @return The type of used sensor.
     * @see android.hardware.Sensor#TYPE_ACCELEROMETER
     * @see android.hardware.Sensor#TYPE_PROXIMITY
     */
    public abstract int getType();

    public abstract void onStart();

    public abstract void onStop();

    /**
     * Called when the sensor is attached to main class.
     */
    public void onAttached(SensorManager sensorManager, Context context) {
        // Register sensors only once.
        if (mAttachedNumber++ > 0) {
            return;
        }

        setup(sensorManager, context);
        onStart();
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

    void setup(SensorManager sensorManager, Context context) {
        mContext = context;
        mSensorManager = sensorManager;
    }

    public boolean isAttached() {
        return mAttachedNumber > 0;
    }

    public Context getContext() {
        return mContext;
    }

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
        private static final String WAKE_LOCK_TAG = TAG;

        private static final int START = 0;
        private static final int STOP = 1;

        private PowerManager.WakeLock mWakeLock;
        private boolean mRunning;
        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case START:
                        mRunning = true;
                        onStart();
                        break;
                    case STOP:
                        mRunning = false;
                        onStop();
                        break;
                }
            }
        };

        public abstract int getRemainingTime();

        @Override
        public void onAttached(SensorManager sensorManager, Context context) {
            // Register sensors only once.
            if (mAttachedNumber++ > 0) {
                return;
            }

            setup(sensorManager, context);
        }

        @Override
        public void onDetached() {
            if (--mAttachedNumber > 0) {
                return;
            }

            if (mRunning) { // Now die
                mHandler.sendEmptyMessage(STOP);
                mHandler.removeCallbacksAndMessages(null);
            }

            setup(null, null);
        }

        public void ping() {
            int remainingTime = getRemainingTime();

            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release();
            }

            PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
            mWakeLock.acquire(remainingTime);

            if (!mRunning) {
                mHandler.sendEmptyMessage(START);
            }

            mHandler.removeMessages(STOP);
            mHandler.sendEmptyMessageDelayed(STOP, remainingTime);
        }
    }

}
