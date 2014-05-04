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
import android.hardware.SensorManager;
import android.os.SystemClock;

import java.util.ArrayList;

/**
 * Provides a callback when {@link com.achep.activedisplay.activities.AcDisplayActivity}
 * should be started and stopped.
 *
 * @author Artem Chepurnoy
 */
public abstract class ActiveModeSensor {

    /**
     * Provides a callback when AcDisplay should be shown / hidden.
     *
     * @author Artem Chepurnoy
     */
    public interface Callback {

        /**
         * Requests to show the AcDisplay.
         */
        public void show(ActiveModeSensor sensor);

        /**
         * Requests to hide the AcDisplay.
         */
        public void hide(ActiveModeSensor sensor);
    }

    private ArrayList<Callback> mCallbacks;

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
     * Unregisters given callback from listening to this sensor.
     *
     * @see #registerCallback(ActiveModeSensor.Callback)
     */
    public void unregisterCallback(Callback callback) {
        mCallbacks.remove(callback);
    }

    protected void requestShowAcDisplay() {
        for (Callback callback : mCallbacks) {
            callback.show(this);
        }
    }

    protected void requestHideAcDisplay() {
        for (Callback callback : mCallbacks) {
            callback.hide(this);
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

    /**
     * Called when the sensor is attached to main class.
     * Tou may start to listen to your sensor here.
     */
    public abstract void onAttached(SensorManager sensorManager, Context context);

    /**
     * Called when the sensor is detached from main class.
     * You must unregister all sensors here.
     */
    public abstract void onDetached(SensorManager sensorManager);

    /**
     * @return {@code SystemClock.elapsedRealtime()}
     */
    protected static long getTimeNow() {
        return SystemClock.elapsedRealtime();
    }

}
