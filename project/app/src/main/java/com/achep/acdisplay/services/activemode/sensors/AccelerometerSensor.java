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
import android.hardware.SensorManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.achep.acdisplay.services.activemode.ActiveModeSensor;

import java.lang.ref.WeakReference;

import uk.co.jarofgreen.lib.ShakeDetector;

import static com.achep.base.Build.DEBUG;

/**
 * Basing on results of accelerometer sensor it notifies when
 * {@link com.achep.acdisplay.ui.activities.AcDisplayActivity AcDisplay}
 * should be shown.
 *
 * @author Artem Chepurnoy
 */
public final class AccelerometerSensor extends ActiveModeSensor.Consuming implements
        ShakeDetector.Listener {

    private static final String TAG = "AccelerometerSensor";

    private static WeakReference<AccelerometerSensor> sAccelerometerSensorWeak;

    private final ShakeDetector mShakeDetector;

    private AccelerometerSensor() {
        super();
        mShakeDetector = new ShakeDetector(this);
    }

    @NonNull
    public static AccelerometerSensor getInstance() {
        AccelerometerSensor sensor = sAccelerometerSensorWeak != null
                ? sAccelerometerSensorWeak.get() : null;
        if (sensor == null) {
            sensor = new AccelerometerSensor();
            sAccelerometerSensorWeak = new WeakReference<>(sensor);
        }
        return sensor;
    }

    @Override
    public int getType() {
        return Sensor.TYPE_ACCELEROMETER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRemainingTime() {
        return 15000; // 15 sec.
    }

    @Override
    public void onStart(@NonNull SensorManager sensorManager) {
        if (DEBUG) Log.d(TAG, "Starting accelerometer sensor...");

        mShakeDetector.start(sensorManager);
    }

    @Override
    public void onStop() {
        if (DEBUG) Log.d(TAG, "Stopping accelerometer sensor...");

        mShakeDetector.stop();
    }

    /**
     * Called on shake detected.
     */
    @Override
    public void onShakeDetected() {
        if (DEBUG) Log.d(TAG, "Hearing shake...");

        requestWakeUp();
    }
}
