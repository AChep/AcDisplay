/*
 * Copyright (C) 2013-2014 AChep@xda <artemchep@gmail.com>
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
package com.achep.activedisplay.sensor;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.widget.Toast;

import com.achep.activedisplay.R;
import com.achep.activedisplay.legacy.MainActivity;

/**
 * Created by Artem on 18.01.14.
 */
public class SensorMonitorService extends Service {

    private Context mContext = SensorMonitorService.this;
    private SensorManager sm;
    private SensorEventListener mAccelerometerMonitor = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }

    };
    private SensorEventListener mProximityMonitor = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            //Toast.makeText(SensorMonitorService.this, "Proximity: " + sensorEvent.values[0], Toast.LENGTH_SHORT).show();
            if (sensorEvent.values[0] < 1) {
                startActivity(new Intent(Intent.ACTION_MAIN, null)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                                | Intent.FLAG_ACTIVITY_NO_USER_ACTION
                                | Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        .setClass(mContext, MainActivity.class));
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }

    };

    @Override
    public void onCreate() {//onCreat shouldn't be used for sensor u should use onStartCommand
        Toast.makeText(this, "Service Created", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {//here u should unregister sensor
        Toast.makeText(this, "Service Stopped", Toast.LENGTH_LONG).show();
        sm.unregisterListener(mProximityMonitor);
        sm.unregisterListener(mAccelerometerMonitor);
    }

    @Override//here u should register sensor and write onStartCommand not onStart
    public int onStartCommand(Intent intent, int flags, int startId) {
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        sm.registerListener(mProximityMonitor,
                sm.getDefaultSensor(Sensor.TYPE_PROXIMITY),
                SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(mAccelerometerMonitor,
                sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

        //here u should make your service foreground so it will keep working even if app closed

        //NotificationPresenter mNotificationManager = (NotificationPresenter) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder bBuilder =
                new Notification.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Title")
                        .setContentText("Subtitle")
                        .setAutoCancel(true)
                        .setPriority(Notification.PRIORITY_MIN)
                        .setOngoing(true);
        startForeground(1, bBuilder.build());
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
