package uk.co.jarofgreen.lib;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.achep.base.tests.Check;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * A shake detection library.
 *
 * @author James
 * @copyright 2013 JMB Technology Limited
 * @license Open Source; 3-clause BSD
 */
/*
 * Edited by Artem Chepurnoy
 *
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
// TODO: Filter slow shakes by the velocity
public class ShakeDetector implements SensorEventListener {

    private static final int SHAKE_CHECK_THRESHOLD = 200;

    /**
     * After we detect a shake, we ignore any events for a bit of time.
     * We don't want two shakes to close together.
     */
    private static final int IGNORE_EVENTS_AFTER_SHAKE = 1500; // 1.5 sec.

    private static final long KEEP_DATA_POINTS_FOR = 1400;
    private static final long MINIMUM_EACH_DIRECTION = 7;
    private static final float POSITIVE_COUNTER_THRESHOLD = 2.0f;
    private static final float NEGATIVE_COUNTER_THRESHOLD = -2.0f;

    private final List<DataPoint> mDataPoints = new ArrayList<>();
    private final Listener mListener;
    private SensorManager mSensorManager;

    private long lastUpdate;
    private long lastShake = 0;

    private float last_x = 0, last_y = 0, last_z = 0;

    private final int[] pos = new int[3];
    private final int[] neg = new int[3];
    private final int[] dir = new int[3];

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

        public float getAxis(int i) {
            switch (i) {
                case 0:
                    return x;
                case 1:
                    return y;
                default:
                    return z;
            }
        }
    }

    public ShakeDetector(@NonNull Listener listener) {
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
        long now = SystemClock.elapsedRealtime();
        // If a shake in last X seconds ignore.
        if (lastShake != 0 && (now - lastShake) < IGNORE_EVENTS_AFTER_SHAKE) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        if (last_x != 0 && last_y != 0 && last_z != 0 && (last_x != x || last_y != y || last_z != z)) {
            DataPoint dp = new DataPoint(last_x - x, last_y - y, last_z - z, now);
            mDataPoints.add(dp);

            if ((now - lastUpdate) > SHAKE_CHECK_THRESHOLD) {
                lastUpdate = now;
                checkShake();
            }
        }

        last_x = x;
        last_y = y;
        last_z = z;
    }

    private void checkShake() {
        long curTime = SystemClock.elapsedRealtime();
        long cutOffTime = curTime - KEEP_DATA_POINTS_FOR;

        // Remove outdated data points.
        while (mDataPoints.size() > 0 && mDataPoints.get(0).time < cutOffTime) {
            mDataPoints.remove(0);
        }

        Arrays.fill(pos, 0);
        Arrays.fill(neg, 0);
        Arrays.fill(dir, 0);
        for (DataPoint dp : mDataPoints) {
            for (int i = 0; i < 3; i++) {
                float v = dp.getAxis(i);
                if (v > POSITIVE_COUNTER_THRESHOLD && dir[i] < 1) {
                    pos[i]++;
                    dir[i] = 1;
                } else if (v < NEGATIVE_COUNTER_THRESHOLD && dir[i] > -1) {
                    neg[i]++;
                    dir[i] = -1;
                }
            }
        }

        for (int i = 0; i < 3; i++) {
            if (pos[i] >= MINIMUM_EACH_DIRECTION && neg[i] >= MINIMUM_EACH_DIRECTION) {
                last_x = 0;
                last_y = 0;
                last_z = 0;
                lastShake = SystemClock.elapsedRealtime();
                mDataPoints.clear();
                // Notify the listener.
                mListener.onShakeDetected();
                break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(@NonNull Sensor sensor, int accuracy) { /* unused */ }

}