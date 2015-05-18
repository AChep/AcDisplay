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
package com.achep.acdisplay.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.base.AppHeap;
import com.achep.base.utils.FileUtils;
import com.achep.base.utils.power.PowerUtils;

import java.io.File;
import java.util.LinkedList;

import static com.achep.base.Build.DEBUG;

/**
 * Created by achep on 24.08.14.
 */
public class SensorsDumpService extends BathService.ChildService implements
        SensorEventListener {

    private static final String TAG = "SensorsDumpService";

    private static final char DIVIDER = ';';
    private static final char NEW_LINE = '\n';

    private static final int MAX_SIZE = 2500;

    private SensorManager mSensorManager;
    private final int[] mSensorTypes = new int[]{
            Sensor.TYPE_GYROSCOPE, Sensor.TYPE_ACCELEROMETER,
    };

    private final LinkedList<Event> mEventList = new LinkedList<>();

    private static class Event {
        long timestamp;
        float[] values;
        int sensor;
    }

    private Handler mHandler = new Handler();
    private Receiver mReceiver = new Receiver();

    private class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_ON:
                    startListening();

                    // Stop listening after some minutes to keep battery.
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (mEventList) {
                                stopListening();
                                mEventList.clear();
                            }
                        }
                    }, 120 * 1000);
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    stopListening();
                    dropToStorage();
                    break;
            }
        }
    }

    /**
     * Starts or stops this service as required by settings and device's state.
     */
    public static void handleState(@NonNull Context context) {
        Config config = Config.getInstance();

        boolean onlyWhileChangingOption = !config.isEnabledOnlyWhileCharging()
                || PowerUtils.isPlugged(context);

        if (config.isEnabled()
                && config.isDevSensorsDumpEnabled()
                && onlyWhileChangingOption) {
            BathService.startService(context, SensorsDumpService.class);
        } else {
            BathService.stopService(context, SensorsDumpService.class);
        }
    }

    @Override
    public void onCreate() {
        Context context = getContext();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
        context.registerReceiver(mReceiver, intentFilter);

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    public void onDestroy() {
        getContext().unregisterReceiver(mReceiver);
        stopListening();

        // Watch for the leaks
        AppHeap.getRefWatcher().watch(this);
    }

    @Override
    public String getLabel() {
        return getContext().getString(R.string.service_bath_active_mode_dump);
    }

    private void startListening() {
        for (int type : mSensorTypes) {
            Sensor sensor = mSensorManager.getDefaultSensor(type);
            if (sensor != null) {
                if (DEBUG) Log.d(TAG, "Listening to " + sensor.getName() + " sensor...");
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
            }
        }
    }

    private void stopListening() {
        if (DEBUG) Log.d(TAG, "Stopping listening...");
        mSensorManager.unregisterListener(this);
        mHandler.removeCallbacksAndMessages(null);
    }

    private void dropToStorage() {
        synchronized (mEventList) {
            if (DEBUG) Log.d(TAG, "Dumping sensors data to file...");
            if (mEventList.size() == 0) {
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (Event event : mEventList) {
                sb.append(event.timestamp).append(DIVIDER);
                sb.append(event.sensor).append(DIVIDER);
                for (float f : event.values) sb.append(f).append(DIVIDER);
                sb.append(NEW_LINE);
            }

            String filename = "dump_sensors_" + SystemClock.elapsedRealtime() + ".txt";
            File file = new File(getContext().getFilesDir(), filename);
            FileUtils.writeToFile(file, sb);

            mEventList.clear();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        synchronized (mEventList) {
            Event event = new Event();
            event.timestamp = SystemClock.elapsedRealtime();
            event.values = sensorEvent.values.clone();
            event.sensor = sensorEvent.sensor.getType();
            mEventList.add(event);

            int size = mEventList.size();
            if (size > MAX_SIZE) {
                mEventList.remove(0);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { /* unused */ }

}
