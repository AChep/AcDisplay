/*
 * Copyright (C) 2013 AChep@xda <artemchep@gmail.com>
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
package com.achep.activedisplay.activemode;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import com.achep.activedisplay.Project;

/**
 * Created by Artem on 08.03.14.
 */
public class ProximitySensor extends ActiveModeService.ActiveModeSensor implements
        SensorEventListener {

    private static final String TAG = "ProximitySensor";

    private static final long CHANGE_TO_FAR_MIN_DELAY = 2500; // ms.
    private static final long CHANGE_TO_NEAR_MIN_DELAY = 0; // ms.
    private static final long LOCK_DELAY = 1400; // ms.
    private static final long LAUNCH_DELAY = 0; // ms.

    private static boolean sProximityNear;
    private static boolean sAttached;
    private static long sLastEventTime;
    private float mMaximumRange;

    private Context mContext;

    private Handler mHandler = new Handler();
    private Runnable mLockRunnable = new Runnable() {
        @Override
        public void run() {
            stopActiveDisplay(mContext);
        }
    };
    private Runnable mLaunchRunnable = new Runnable() {
        @Override
        public void run() {
            launchActiveDisplay(mContext);
        }
    };

    /**
     * True if sensor is in "near" position, and False otherwise
     * (or if data is too old / sensor unsupported).
     *
     * @return True if sensor is in "near" position, and False otherwise
     * (or if data is too old / sensor unsupported).
     */
    public static boolean isNear() {
        return (getTimeNow() - sLastEventTime < 1000 || sAttached) && sProximityNear;
    }

    private static long getTimeNow() {
        return SystemClock.elapsedRealtime();
    }

    @Override
    protected boolean isSupported(SensorManager sensorManager, Context context) {
        return sensorManager.getSensorList(Sensor.TYPE_PROXIMITY).size() > 0;
    }

    @Override
    protected void onAttach(SensorManager sensorManager, Context context) {
        Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        assert proximitySensor != null; // Otherwise excluded by Service.

        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_FASTEST);
        mMaximumRange = proximitySensor.getMaximumRange();
        mContext = context;

        sAttached = true;
        sProximityNear = false;
        sLastEventTime = getTimeNow();

        if (Project.DEBUG) Log.d(TAG, "maximum_range=" + mMaximumRange);
    }

    @Override
    protected void onDetach(SensorManager sensorManager) {
        sensorManager.unregisterListener(this);
        mHandler.removeCallbacks(mLockRunnable);
        mHandler.removeCallbacks(mLaunchRunnable);
        mContext = null;

        sAttached = false;
        sLastEventTime = getTimeNow();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        final float distance = event.values[0];
        final boolean isNear = distance < mMaximumRange || distance < 1.0f;
        final boolean changed = sProximityNear != (sProximityNear = isNear);

        if (Project.DEBUG)
            Log.d(TAG, "distance=" + distance + " is_near=" + isNear + " changed=" + changed);

        if (!changed) {
            // Well just in cause if proximity sensor NOT always sends
            // binary results. This should not happen, but who knows...
            // Maximum range is buggy enough :) :P
            return;
        }

        mHandler.removeCallbacks(mLockRunnable);
        mHandler.removeCallbacks(mLaunchRunnable);

        long now = getTimeNow();
        if (now > sLastEventTime + (isNear ? CHANGE_TO_NEAR_MIN_DELAY : CHANGE_TO_FAR_MIN_DELAY) && isNear == isScreenOn()) {
            if (isNear) {
                mHandler.postDelayed(mLockRunnable, LOCK_DELAY);
            } else {
                mHandler.postDelayed(mLaunchRunnable, LAUNCH_DELAY);
            }
        }

        // TODO: Implement double swipe gesture.

        sLastEventTime = now;
    }

    private boolean isScreenOn() {
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        return pm.isScreenOn();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { /* unused */ }
}
