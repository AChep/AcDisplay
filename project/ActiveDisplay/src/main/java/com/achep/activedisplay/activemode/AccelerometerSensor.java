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
package com.achep.activedisplay.activemode;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Created by Artem on 08.03.14.
 */
public class AccelerometerSensor extends ActiveSensor implements
        SensorEventListener {

    private static final String TAG = "AccelerometerSensor";
    private static final float CHANGE_THRESHOLD = 2f;

    private static boolean sAttached;
    private static boolean sAccelerationNorm;
    private static long sLastEventTime;

    private Context mContext;
    private AccelerationPool mAccelerationPoolNorm = new AccelerationPool(20);
    private AccelerationPool mAccelerationPool = new AccelerationPool(5);
    private float mAccelerationDeltaOld;

    private final float[] gravity = new float[3];
    private final float[] acceleration = new float[3];

    public static boolean isNorm() {
        return sAttached && sAccelerationNorm;
    }

    @Override
    public int getType() {
        return ACCELEROMETER;
    }

    @Override
    protected boolean isSupported(SensorManager sensorManager, Context context) {
        return sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() > 0 && false;
    }

    @Override
    protected void onAttached(SensorManager sensorManager, Context context) {
        Sensor accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        assert accelSensor != null; // Otherwise excluded by Service.

        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mAccelerationPoolNorm.reset();
        mAccelerationPool.reset();
        mContext = context;

        sAttached = true;
        sAccelerationNorm = true;
        sLastEventTime = getTimeNow();
    }

    @Override
    protected void onDetached(SensorManager sensorManager) {
        sensorManager.unregisterListener(this);
        mContext = null;

        sAttached = false;
        sLastEventTime = getTimeNow();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.

        final float alpha = 0.8f;

        // Isolate the force of gravity with the low-pass filter.
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Remove the gravity contribution with the high-pass filter.
        acceleration[0] = Math.abs(event.values[0] - gravity[0]);
        acceleration[1] = Math.abs(event.values[1] - gravity[1]);
        acceleration[2] = Math.abs(event.values[2] - gravity[2]);

        mAccelerationPoolNorm.put(acceleration);
        mAccelerationPool.put(acceleration);

        final float averageNorm = mAccelerationPoolNorm.average();
        final float average = mAccelerationPool.average();
        float delta = Math.abs(averageNorm - average);
        final boolean isNorm = Math.abs(delta - mAccelerationDeltaOld) < CHANGE_THRESHOLD;
        final boolean changed = sAccelerationNorm != (sAccelerationNorm = isNorm);
        mAccelerationDeltaOld = delta;
        Log.d(TAG, "Delta=" + mAccelerationDeltaOld);

        if (!changed) {
            return;
        }

        long now = getTimeNow();
        if (now - sLastEventTime >= 5000 && !isNorm) {
            notifyShowEvent();
        }
        sLastEventTime = now;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { /* unused */ }

    private static class AccelerationPool {

        private final float[][] acceleration;
        private int pos;
        private int len;

        public AccelerationPool(int length) {
            acceleration = new float[3][length];
        }

        public void put(float[] a) {
            if (pos + 1 < len) {
                pos++;
            } else {
                int max = acceleration[0].length;
                if (len < max) {
                    pos = len++;
                } else {
                    pos = 0;
                }
            }

            acceleration[0][pos] = a[0];
            acceleration[1][pos] = a[1];
            acceleration[2][pos] = a[2];
        }

        public void reset() {
            pos = 0;
            len = 0;
        }

        public float average() {
            float a = 0;
            for (int i = 0; i < len; i++) {
                for (int j = 0; j < 3; j++) {
                    a += acceleration[j][i];
                }
            }
            return a / len / 3f;
        }

        /*public void average(float[] a) {
            for (int i = 0; i < 3; i++) a[i] = 0;
            for (int i = 0; i < len; i++) {
                for (int j = 0; j < 3; j++) {
                    a[j] += acceleration[j][i];
                }
            }
            for (int i = 0; i < 3; i++) a[i] /= len;
        }*/
    }
}
