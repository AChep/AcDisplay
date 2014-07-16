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
package com.achep.acdisplay.services.activemode;

import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.util.Log;

import com.achep.acdisplay.Build;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.Presenter;
import com.achep.acdisplay.R;
import com.achep.acdisplay.notifications.NotificationPresenter;
import com.achep.acdisplay.notifications.OpenNotification;
import com.achep.acdisplay.services.BathService;
import com.achep.acdisplay.services.activemode.handlers.InactiveTimeHandler;
import com.achep.acdisplay.services.activemode.handlers.ScreenHandler;
import com.achep.acdisplay.services.activemode.handlers.WithoutNotifiesHandler;
import com.achep.acdisplay.services.activemode.sensors.AccelerometerSensor;
import com.achep.acdisplay.services.activemode.sensors.GyroscopeSensor;
import com.achep.acdisplay.services.activemode.sensors.ProximitySensor;
import com.achep.acdisplay.utils.PowerUtils;

/**
 * Service that turns on AcDisplay exactly when it's needed.
 *
 * @author Artem Chepurnoy
 * @see com.achep.acdisplay.services.activemode.ActiveModeHandler
 * @see com.achep.acdisplay.services.activemode.ActiveModeSensor
 */
public class ActiveModeService extends BathService.ChildService implements
        ActiveModeSensor.Callback, ActiveModeHandler.Callback,
        NotificationPresenter.OnNotificationListChangedListener {

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
            BathService.startService(context, ActiveModeService.class);
        } else {
            BathService.stopService(context, ActiveModeService.class);
        }
    }

    /**
     * Builds the array of supported {@link ActiveModeSensor sensors}.
     *
     * @return The array of supported {@link ActiveModeSensor sensors}.
     * @see ActiveModeSensor
     */
    public static ActiveModeSensor[] buildAvailableSensorsList(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        ActiveModeSensor[] sensors = new ActiveModeSensor[]{ // all available sensors
                AccelerometerSensor.getInstance(),
                GyroscopeSensor.getInstance(),
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
        Context context = getContext();
        mSensors = buildAvailableSensorsList(context);
        mHandlers = new ActiveModeHandler[]{
                new ScreenHandler(context, this),
                new InactiveTimeHandler(context, this),
                new WithoutNotifiesHandler(context, this),
        };

        for (ActiveModeHandler handler : mHandlers) {
            handler.create();
        }

        requestActive();

        NotificationPresenter.getInstance().registerListener(this);
    }

    @Override
    public void onDestroy() {
        for (ActiveModeHandler handler : mHandlers) {
            handler.destroy();
        }

        stopListening();

        NotificationPresenter.getInstance().unregisterListener(this);
    }

    @Override
    public String getLabel() {
        return getContext().getString(R.string.service_bath_active_mode);
    }

    @Override
    public void onNotificationListChanged(NotificationPresenter np,
                                          OpenNotification osbn, int event) {
        if (Config.getInstance().isNotifyWakingUp()) {
            // Notification will wake up device without
            // any sensors' callback.
            return;
        }

        switch (event) {
            case NotificationPresenter.EVENT_CHANGED:
            case NotificationPresenter.EVENT_POSTED:
                pingConsumingSensors();
                break;
        }
    }

    @Override
    public void requestActive() {
        if (mListening) {
            return; // Already listening, no need to check all handlers.
        }

        // Check through all available handlers.
        for (ActiveModeHandler handler : mHandlers) {
            if (!handler.isCreated() || !handler.isActive()) {
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
        if (Build.DEBUG) Log.d(TAG, "Stopping listening to sensors.");

        for (ActiveModeSensor sensor : mSensors) {
            sensor.onDetached();
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
        if (Build.DEBUG) Log.d(TAG, "Starting listening to sensors.");

        Context context = getContext();
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        for (ActiveModeSensor sensor : mSensors) {
            sensor.registerCallback(this);
            sensor.onAttached(sensorManager, context);
        }
    }

    private void pingConsumingSensors() {
        for (ActiveModeSensor sensor : mSensors) {
            if (sensor.isAttached() && sensor instanceof ActiveModeSensor.Consuming) {
                ((ActiveModeSensor.Consuming) sensor).ping();
            }
        }
    }

    @Override
    public void onWakeRequested(ActiveModeSensor sensor) {
        Presenter.getInstance().start(getContext());
    }
}
