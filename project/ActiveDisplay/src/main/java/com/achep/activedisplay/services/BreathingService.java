package com.achep.activedisplay.services;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.widget.Toast;

import com.achep.activedisplay.NotificationIds;
import com.achep.activedisplay.R;
import com.achep.activedisplay.activities.ActiveDisplayActivity;

/**
 * Created by Artem on 16.02.14.
 */
public class BreathingService extends Service {

    private SensorManager mSensorManager;
    private SensorEventListener mAccelerometerMonitor = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }

    };
    private SensorEventListener mProximityMonitor = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) { /* unused */ }

    };

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(mProximityMonitor);
        mSensorManager.unregisterListener(mAccelerometerMonitor);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorManager.registerListener(mProximityMonitor,
                mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),
                SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mAccelerometerMonitor,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

        Notification notification = new Notification.Builder(this)
                        .setSmallIcon(R.drawable.stat_test)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                        .setContentTitle(getString(R.string.app_name))
                        .setPriority(Notification.PRIORITY_MIN)
                        .setAutoCancel(true)
                        .setOngoing(true)
                        .build();
        startForeground(NotificationIds.BREATHING_NOTIFICATION, notification);
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
