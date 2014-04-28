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
 * Created by Artem on 28.03.2014.
 */
public abstract class ActiveSensor {

    public interface SensorCallback {
        public boolean onShowEvent(ActiveSensor sensor);

        public boolean onHideEvent(ActiveSensor sensor);
    }

    private ArrayList<SensorCallback> mCallbacks;

    public ActiveSensor() {
        mCallbacks = new ArrayList<>(4);
    }

    public void registerCallback(SensorCallback callback) {
        mCallbacks.add(callback);
    }

    public void unregisterCallback(SensorCallback callback) {
        mCallbacks.remove(callback);
    }

    protected void notifyShowEvent() {
        for (SensorCallback callback : mCallbacks) {
            callback.onShowEvent(this);
        }
    }

    protected void notifyHideEvent() {
        for (SensorCallback callback : mCallbacks) {
            callback.onHideEvent(this);
        }
    }

    protected boolean isSupported(SensorManager sensorManager, Context context) {
        return sensorManager.getSensorList(getType()).size() > 0;
    }

    public abstract int getType();

    protected abstract void onAttached(SensorManager sensorManager, Context context);

    protected abstract void onDetached(SensorManager sensorManager);

    protected static long getTimeNow() {
        return SystemClock.elapsedRealtime();
    }

}
