/*
 * Copyright (C) 2015 AChep@xda <artemchep@gmail.com>
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
package com.achep.acdisplay.services.activemode.detectors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.achep.base.Build;
import com.achep.base.tests.Check;

import java.util.ArrayList;
import java.util.List;

public class IdleExitDetector implements SensorEventListener {

    @SuppressWarnings("PointlessBooleanExpression")
    private static final boolean DEBUG_ALGORITHM = true && Build.DEBUG;
    private static final String TAG = "ShakeDetector";

    /**
     * After we detect a shake, we ignore any events for a bit of time.
     * We don't want two shakes to close together.
     */
    private static final int IGNORE_EVENTS_AFTER_SHAKE = 1500; // 1.5 sec.

    private static final long KEEP_DATA_POINTS_FOR = 1400;

    private final List<DataPoint> mDataPoints = new ArrayList<>();
    private final Listener mListener;
    private SensorManager mSensorManager;

    private long lastShake = 0;

    private float last_x = 0, last_y = 0, last_z = 0;
    private float ave_x = 0, ave_y = 0, ave_z = 0;

    public interface Listener {

        /**
         * Called on shake detected.
         */
        void onShakeDetected();
    }

    private static class DataPoint {
        public float x, y, z;
        public long time;

        public DataPoint(float x, float y, float z, long time) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.time = time;
        }
    }

    public IdleExitDetector(@NonNull Listener listener) {
        mListener = listener;
    }

    public void start(@NonNull SensorManager sensorManager) {
        mSensorManager = sensorManager;
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Check.getInstance().isNonNull(sensor);
        mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
    }

    public void stop() {
        mSensorManager.unregisterListener(this);
        mSensorManager = null;
    }

    @Override
    public void onSensorChanged(@NonNull SensorEvent event) {
        Check.getInstance().isTrue(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER);
        final long now = SystemClock.elapsedRealtime();
        final long deltaTime = now - KEEP_DATA_POINTS_FOR;
        // If a shake in last X seconds ignore.
        if (lastShake != 0 && (now - lastShake) < IGNORE_EVENTS_AFTER_SHAKE) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        if (last_x != 0 && last_y != 0 && last_z != 0 && (last_x != x || last_y != y || last_z != z)) {
            DataPoint dp = new DataPoint(last_x - x, last_y - y, last_z - z, now);
            mDataPoints.add(dp);

            // Remove outdated data points.
            while (mDataPoints.size() > 0 && mDataPoints.get(0).time < deltaTime) {
                mDataPoints.remove(0);
            }

            // Calculate average threshold.
            final int length = mDataPoints.size();
            if (length > 10) {
                for (DataPoint i : mDataPoints) {
                    ave_x += Math.abs(i.x);
                    ave_y += Math.abs(i.y);
                    ave_z += Math.abs(i.z);
                }
                ave_x /= length;
                ave_y /= length;
                ave_z /= length;

//                if (DEBUG_ALGORITHM) {
//                    Log.d(TAG, "ave_x=" + ave_x
//                            + " ave_y=" + ave_y
//                            + " ave_z=" + ave_z
//                            + " length=" + length);
//                }

                final float ave = (ave_x + ave_y + ave_z) / 3f;
                final float aveDp = (Math.abs(dp.x) + Math.abs(dp.x) + Math.abs(dp.x)) / 3f;
                final float ratio = aveDp / ave;

                if (DEBUG_ALGORITHM) {
                    Log.d(TAG, "ave=" + ave
                            + " ave_dp=" + aveDp
                            + " ave_ratio=" + ratio
                            + " delta=" + (aveDp - ave));
                }

                if (Math.abs(ratio) > 6 && Math.abs(aveDp - ave) > 0.5f && ave <= 0.3f) {
                    mListener.onShakeDetected();

                    Log.e(TAG, "ave=" + ave
                            + " ave_dp=" + aveDp
                            + " ave_ratio=" + ratio
                            + " delta=" + (aveDp - ave));
                }
            }
        }

        last_x = x;
        last_y = y;
        last_z = z;
    }

    @Override
    public void onAccuracyChanged(@NonNull Sensor sensor, int accuracy) { /* unused */ }

}