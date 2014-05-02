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

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import com.achep.activedisplay.Config;
import com.achep.activedisplay.InactiveHoursHelper;
import com.achep.activedisplay.NotificationIds;
import com.achep.activedisplay.Presenter;
import com.achep.activedisplay.Project;
import com.achep.activedisplay.R;
import com.achep.activedisplay.settings.Settings;
import com.achep.activedisplay.utils.PowerUtils;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Artem on 16.02.14.
 */
public class ActiveModeService extends Service implements Config.OnConfigChangedListener, ActiveSensor.SensorCallback {

    private static final String TAG = "ActiveModeService";

    // TODO: Implement event based inactive time handling (using AlarmManager).
    private static final int INACTIVE_HOURS_CHECK_PERIOD = 1000 * 60 * 5; // ms.

    private Timer mTimer;
    private ActiveSensor[] mSensors;

    private boolean mListening;
    private boolean mInactiveTime;

    private Receiver mReceiver = new Receiver();

    private class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Presenter presenter = Presenter.getInstance();
            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_ON:
                    presenter.registerListener(mStateListener);

                    if (!presenter.isActivityAttached()) {
                        stopListening();
                    }
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    presenter.unregisterListener(mStateListener);
                    startListeningDelayed(250);
                    break;
            }
        }

    }

    private Handler mHandler = new Handler();
    private Runnable mStartListeningRunnable = new Runnable() {
        @Override
        public void run() {
            startListening();
        }
    };

    private Presenter.OnActiveDisplayStateChangedListener mStateListener =
            new Presenter.OnActiveDisplayStateChangedListener() {
                @Override
                public void OnActiveDisplayStateChanged(Activity activity) {
                    if (activity == null) {
                        stopListening();
                    } else {
                        startListening();
                    }
                }
            };

    /**
     * Starts or stops this service as required by settings and device's state.
     */
    public static void handleState(Context context) {
        Intent intent = new Intent(context, ActiveModeService.class);
        Config config = Config.getInstance(context);

        boolean onlyWhileChangingOption = !config.isEnabledOnlyWhileCharging()
                || PowerUtils.isPlugged(context);

        if (config.isEnabled()
                && config.isActiveModeEnabled()
                && onlyWhileChangingOption) {
            context.startService(intent);
        } else {
            context.stopService(intent);
        }
    }

    public static ActiveSensor[] buildSensorsList(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        ActiveSensor[] sensors = new ActiveSensor[]{
                new AccelerometerSensor(),
                new ProximitySensor()
        };

        int count = sensors.length;
        boolean[] supportList = new boolean[sensors.length];
        for (int i = 0; i < sensors.length; i++) {
            supportList[i] = sensors[i].isSupported(sensorManager, context);
            if (!supportList[i]) {
                count--;
            }
        }

        ActiveSensor[] sensorsSupported = new ActiveSensor[count];
        for (int i = 0, j = 0; i < sensors.length; i++) {
            if (supportList[i]) {
                sensorsSupported[j++] = sensors[i];
            }
        }
        return sensorsSupported;
    }

    private void handleInactiveHoursChanged(boolean enabled) {
        if (mTimer != null) mTimer.cancel();
        if (enabled) {
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {

                private static final String TAG = "InactiveTimeTicker";

                @Override
                public void run() {
                    Config config = Config.getInstance(ActiveModeService.this);
                    boolean inactive = InactiveHoursHelper.isInactiveTime(config);
                    boolean changed = inactive != mInactiveTime;

                    if (Project.DEBUG)
                        Log.d(TAG, "On timer tick: elapsed_real_time="
                                + SystemClock.elapsedRealtime());

                    if (changed) {
                        mInactiveTime = inactive;

                        if (Project.DEBUG)
                            Log.d(TAG, "is_inactive_time=" + inactive);

                        if (inactive) {
                            stopListening();
                        } else {
                            start();
                        }
                    }
                }
            }, 0, INACTIVE_HOURS_CHECK_PERIOD);
        } else {
            mInactiveTime = false;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSensors = buildSensorsList(this);

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        registerReceiver(mReceiver, intentFilter);

        Config config = Config.getInstance(this);
        handleInactiveHoursChanged(config.isInactiveTimeEnabled());
        config.addOnConfigChangedListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        stopListening();

        Config config = Config.getInstance(this);
        config.removeOnConfigChangedListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        start();

        int notificationId = NotificationIds.ACTIVE_MODE_NOTIFICATION;
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                notificationId, new Intent(this, Settings.ActiveModeSettingsActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.stat_active)
                .setContentTitle(getString(
                        R.string.service_active_mode,
                        getString(R.string.app_name)))
                .setContentText(getString(R.string.service_active_mode_text))
                .setPriority(Notification.PRIORITY_MIN)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        startForeground(notificationId, notification);
        return Service.START_STICKY;
    }

    @Override
    public boolean onShowEvent(ActiveSensor sensor) {
        Presenter.getInstance().start(this);
        return false;
    }

    @Override
    public boolean onHideEvent(ActiveSensor sensor) {
        Presenter.getInstance().stop(this);
        return false;
    }

    @Override
    public void onConfigChanged(Config config, String key, Object value) {
        boolean inactiveTimeEnabled = config.isInactiveTimeEnabled();
        switch (key) {
            case Config.KEY_INACTIVE_TIME_FROM:
            case Config.KEY_INACTIVE_TIME_TO:
                if (!inactiveTimeEnabled) {
                    break;
                }

                // Immediately update sensors' blocker.
            case Config.KEY_INACTIVE_TIME_ENABLED:
                handleInactiveHoursChanged(inactiveTimeEnabled);
                break;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    void start() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        Presenter presenter = Presenter.getInstance();
        if (pm.isScreenOn()) {
            mStateListener.OnActiveDisplayStateChanged(presenter.getActivity());
        } else {
            startListening();
        }
    }

    /**
     * Stops all monitoring sensors and removes delayed start event.
     */
    private void stopListening() {
        mHandler.removeCallbacks(mStartListeningRunnable);

        if (!mListening & !(mListening = false)) return;
        if (Project.DEBUG) Log.d(TAG, "Stopping listening to sensors.");

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        for (ActiveSensor sensor : mSensors) {
            sensor.onDetached(sensorManager);
            sensor.unregisterCallback(this);
        }
    }

    /**
     * Starts all monitoring sensors after a delay.
     *
     * @param millis the delay before monitoring started.
     */
    private void startListeningDelayed(int millis) {
        mHandler.postDelayed(mStartListeningRunnable, millis);
    }

    private void startListening() {
        if (mListening & (mListening = true) | mInactiveTime) return;
        if (Project.DEBUG) Log.d(TAG, "Starting listening to sensors.");

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        for (ActiveSensor sensor : mSensors) {
            sensor.onAttached(sensorManager, this);
            sensor.registerCallback(this);
        }
    }

}
