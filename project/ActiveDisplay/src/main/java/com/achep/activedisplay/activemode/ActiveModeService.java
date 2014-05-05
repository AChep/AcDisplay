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

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import com.achep.activedisplay.Config;
import com.achep.activedisplay.NotificationIds;
import com.achep.activedisplay.Presenter;
import com.achep.activedisplay.Project;
import com.achep.activedisplay.R;
import com.achep.activedisplay.activemode.handlers.InactiveTimeHandler;
import com.achep.activedisplay.activemode.handlers.ScreenHandler;
import com.achep.activedisplay.activemode.handlers.WithoutNotifiesHandler;
import com.achep.activedisplay.activemode.sensors.AccelerometerSensor;
import com.achep.activedisplay.activemode.sensors.ProximitySensor;
import com.achep.activedisplay.settings.Settings;
import com.achep.activedisplay.utils.PowerUtils;

/**
 * Service that turns on AcDisplay exactly when it's needed.
 *
 * @author Artem Chepurnoy
 * @see com.achep.activedisplay.activemode.ActiveModeHandler
 * @see com.achep.activedisplay.activemode.ActiveModeSensor
 */
public class ActiveModeService extends Service implements
        ActiveModeSensor.Callback, ActiveModeHandler.Callback {

    private static final String TAG = "ActiveModeService";

    private ActiveModeSensor[] mSensors;
    private ActiveModeHandler[] mHandlers;

    private boolean mListening;

    /**
     * Starts or stops this service as required by settings and device's state.
     */
    public static void handleState(Context context) {
        Intent intent = new Intent(context, ActiveModeService.class);
        Config config = Config.getInstance();

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

    /**
     * Builds the array of supported {@link ActiveModeSensor sensors}.
     *
     * @return The array of supported {@link ActiveModeSensor sensors}.
     * @see ActiveModeSensor
     */
    public static ActiveModeSensor[] buildAvailableSensorsList(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        ActiveModeSensor[] sensors = new ActiveModeSensor[] { // all available sensors
                AccelerometerSensor.getInstance(),
                ProximitySensor.getInstance()
        };

        // Count the number of supported sensors, and
        // mark unsupported.
        int count = sensors.length;
        boolean[] supportList = new boolean[sensors.length];
        for (int i = 0; i < sensors.length; i++) {
            supportList[i] = sensors[i].isSupported(sensorManager, context);
            if (!supportList[i]) {
                count--;
            }
        }

        // Create the list of proven sensors.
        ActiveModeSensor[] sensorsSupported = new ActiveModeSensor[count];
        for (int i = 0, j = 0; i < sensors.length; i++) {
            if (supportList[i]) {
                sensorsSupported[j++] = sensors[i];
            }
        }

        return sensorsSupported;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSensors = buildAvailableSensorsList(this);
        mHandlers = new ActiveModeHandler[]{
                new ScreenHandler(this, this),
                new InactiveTimeHandler(this, this),
                new WithoutNotifiesHandler(this, this),
        };

        for (ActiveModeHandler handler : mHandlers) {
            handler.onCreate();
        }

        requestActive();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (ActiveModeHandler handler : mHandlers) {
            handler.onDestroy();
        }

        stopListening();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Show foreground notification to prove that
        // this service will not be killed when system
        // needs some RAM or whatever.
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
    public void requestActive() {
        if (mListening) {
            return; // Already listening, no need to check all handlers.
        }

        // Check through all available handlers.
        for (ActiveModeHandler handler : mHandlers) {
            if (!handler.isActive()) {
                return;
            }
        }

        startListening();
    }

    @Override
    public void requestInactive() {
        stopListening();
    }

    /**
     * Stops listening to {@link ActiveModeSensor sensors} (if not stopped already.)
     *
     * @see #buildAvailableSensorsList(android.content.Context)
     * @see #startListening()
     */
    private void stopListening() {
        if (!mListening & !(mListening = false)) return;
        if (Project.DEBUG) Log.d(TAG, "Stopping listening to sensors.");

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        for (ActiveModeSensor sensor : mSensors) {
            sensor.onDetached(sensorManager);
            sensor.unregisterCallback(this);
        }
    }

    /**
     * Starts listening to {@link ActiveModeSensor sensors} (if not started already.)
     *
     * @see #buildAvailableSensorsList(android.content.Context)
     * @see #stopListening()
     */
    private void startListening() {
        if (mListening & (mListening = true)) return;
        if (Project.DEBUG) Log.d(TAG, "Starting listening to sensors.");

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        for (ActiveModeSensor sensor : mSensors) {
            sensor.registerCallback(this);
            sensor.onAttached(sensorManager, this);
        }
    }

    @Override
    public void show(ActiveModeSensor sensor) {
        Presenter.getInstance().start(this);
    }

    @Override
    public void hide(ActiveModeSensor sensor) { /* handled by AcDisplay activity */ }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
