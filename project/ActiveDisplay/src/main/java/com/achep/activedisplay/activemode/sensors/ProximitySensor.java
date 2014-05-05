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
package com.achep.activedisplay.activemode.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;

import com.achep.activedisplay.Project;
import com.achep.activedisplay.activemode.ActiveModeSensor;

import java.lang.ref.WeakReference;

/**
 * Basing on results of proximity sensor it notifies when
 * {@link com.achep.activedisplay.activities.AcDisplayActivity AcDisplay}
 * should be shown or hidden.
 *
 * @author Artem Chepurnoy
 */
public class ProximitySensor extends ActiveModeSensor implements
        SensorEventListener {

    private static final String TAG = "ProximitySensor";

    private static final long CHANGE_TO_FAR_MIN_DELAY = 2500; // ms.
    private static final long CHANGE_TO_NEAR_MIN_DELAY = 0; // ms.

    /**
     * The delay in millis between sensor's change and hiding
     * {@link com.achep.activedisplay.activities.AcDisplayActivity AcDisplay}
     */
    private static final long REQUEST_HIDE_ACDISPLAY_DELAY = 1400; // ms.

    /**
     * The delay in millis between sensor's change and showing
     * {@link com.achep.activedisplay.activities.AcDisplayActivity AcDisplay}
     */
    private static final long REQUEST_SHOW_ACDISPLAY_DELAY = 200; // ms.

    private static WeakReference<ProximitySensor> sProximitySensorWeak;
    private static long sLastEventTime;
    private static boolean sAttached;
    private static boolean sNear;

    private float mAttachedNumber;
    private float mMaximumRange;
    private boolean mFirstChange;

    private PowerManager mPowerManager;

    private Handler mHandler = new Handler();
    private Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            requestHideAcDisplay();
        }
    };
    private Runnable mShowRunnable = new Runnable() {
        @Override
        public void run() {
            requestShowAcDisplay();
        }
    };

    private ProximitySensor() {
        super();
    }

    public static synchronized ProximitySensor getInstance() {
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
        return (getTimeNow() - sLastEventTime < 1000 || sAttached) && sNear;
    }

    @Override
    public int getType() {
        return Sensor.TYPE_PROXIMITY;
    }

    @Override
    public void onAttached(SensorManager sensorManager, Context context) {
        synchronized (this) {
            // Register sensors only once.
            if (mAttachedNumber++ > 0) {
                return;
            }

            Sensor proximitySensor = sensorManager.getDefaultSensor(getType());
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);

            mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mMaximumRange = proximitySensor.getMaximumRange();

            sAttached = true;
            mFirstChange = true;
        }
    }

    @Override
    public void onDetached(SensorManager sensorManager) {
        synchronized (this) {
            if (--mAttachedNumber > 0) {
                return;
            }

            sensorManager.unregisterListener(this);
            mHandler.removeCallbacks(mHideRunnable);
            mHandler.removeCallbacks(mShowRunnable);

            sAttached = false;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        final float distance = event.values[0];
        final boolean isNear = distance < mMaximumRange || distance < 1.0f;
        final boolean changed = sNear != (sNear = isNear) || mFirstChange;

        if (Project.DEBUG) {
            Log.d(TAG, "distance=" + distance
                    + " is_near=" + isNear
                    + " changed=" + changed);
        }

        if (!changed) {
            // Well just in cause if proximity sensor is NOT always eventual.
            // This should not happen, but who knows... I found maximum
            // range buggy enough.
            return;
        }

        mHandler.removeCallbacks(mHideRunnable);
        mHandler.removeCallbacks(mShowRunnable);
        long now = getTimeNow();

        long delay = isNear ? CHANGE_TO_NEAR_MIN_DELAY : CHANGE_TO_FAR_MIN_DELAY;
        if (now > sLastEventTime + delay && isNear == isScreenOn() && !mFirstChange) {

            // Hide or show the AcDisplay after a short delay
            // during which this action can be canceled.
            if (isNear) {
                mHandler.postDelayed(mHideRunnable,
                        REQUEST_HIDE_ACDISPLAY_DELAY);
            } else {
                mHandler.postDelayed(mShowRunnable,
                        REQUEST_SHOW_ACDISPLAY_DELAY);
            }
        }

        sLastEventTime = now;
        mFirstChange = false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { /* unused */ }

    private boolean isScreenOn() {
        return mPowerManager.isScreenOn();
    }
}
