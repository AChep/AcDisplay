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
import android.support.annotation.NonNull;
import android.util.Log;

import com.achep.acdisplay.services.activemode.ActiveModeSensor;

import java.lang.ref.WeakReference;

import static com.achep.base.Build.DEBUG;

/**
 * Basing on results of gyroscope sensor it notifies when
 * {@link com.achep.acdisplay.ui.activities.AcDisplayActivity AcDisplay}
 * should be shown.
 *
 * @author Artem Chepurnoy
 */
public final class GyroscopeSensor extends ActiveModeSensor.Consuming implements
        SensorEventListener {

    private static final String TAG = "GyroscopeSensor";

    private static WeakReference<GyroscopeSensor> sGyroscopeSensorWeak;

    private GyroscopeSensor() {
        super();
    }

    @NonNull
    public static GyroscopeSensor getInstance() {
        GyroscopeSensor sensor = sGyroscopeSensorWeak != null
                ? sGyroscopeSensorWeak.get() : null;
        if (sensor == null) {
            sensor = new GyroscopeSensor();
            sGyroscopeSensorWeak = new WeakReference<>(sensor);
        }
        return sensor;
    }

    @Override
    public int getType() {
        return Sensor.TYPE_GYROSCOPE;
    }

    @Override
    public boolean isSupported(@NonNull SensorManager sensorManager) {
        return false;
    }

    @Override
    public void onStart(@NonNull SensorManager sensorManager) {
        if (DEBUG) Log.d(TAG, "Starting gyroscope sensor...");

        Sensor accelerationSensor = sensorManager.getDefaultSensor(getType());
        sensorManager.registerListener(this, accelerationSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onStop() {
        if (DEBUG) Log.d(TAG, "Stopping gyroscope sensor...");

        SensorManager sensorManager = getSensorManager();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) { /* placeholder */ }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { /* unused */ }

}
