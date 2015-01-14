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
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.achep.acdisplay.Atomic;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.Presenter;
import com.achep.acdisplay.R;
import com.achep.acdisplay.notifications.NotificationPresenter;
import com.achep.acdisplay.notifications.OpenNotification;
import com.achep.acdisplay.ui.activities.KeyguardActivity;
import com.achep.base.content.ConfigBase;
import com.achep.base.utils.power.PowerUtils;

import static com.achep.base.Build.DEBUG;

/**
 * Created by Artem on 16.02.14.
 *
 * @author Artem Chepurnoy
 */
public class KeyguardService extends BathService.ChildService implements
        ConfigBase.OnConfigChangedListener,
        NotificationPresenter.OnNotificationListChangedListener {

    private static final String TAG = "KeyguardService";

    private static final int ACTIVITY_LAUNCH_MAX_TIME = 2500; // ms.

    public static boolean isActive = false;

    /**
     * Starts or stops this service as required by settings and device's state.
     */
    public static void handleState(Context context) {
        Config config = Config.getInstance();

        boolean onlyWhileChargingOption = !config.isEnabledOnlyWhileCharging()
                || PowerUtils.isPlugged(context);

        if (config.isEnabled()
                && config.isKeyguardEnabled()
                && onlyWhileChargingOption) {
            BathService.startService(context, KeyguardService.class);
        } else {
            BathService.stopService(context, KeyguardService.class);
        }
    }

    private final Atomic mAtomicOption = new Atomic(new Atomic.Callback() {

        private final NotificationPresenter mNp = NotificationPresenter.getInstance();

        @Override
        public void onStart(Object... objects) {
            mNp.registerListener(KeyguardService.this);
            updateState();
        }

        @Override
        public void onStop(Object... objects) {
            mNp.unregisterListener(KeyguardService.this);
            mAtomicMain.start();
        }

    });

    private final Atomic mAtomicMain = new Atomic(new Atomic.Callback() {

        @Override
        public void onStart(Object... objects) {
            final Context context = getContext();

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_SCREEN_ON);
            intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
            intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1); // highest priority
            context.registerReceiver(mReceiver, intentFilter);

            if (!PowerUtils.isScreenOn(context)) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SCREEN_OFF);
                mReceiver.onReceive(context, intent);
            }

            isActive = true;
        }

        @Override
        public void onStop(Object... objects) {
            final Context context = getContext();

            getContext().unregisterReceiver(mReceiver);

            if (!PowerUtils.isScreenOn(context)) {
                Presenter.getInstance().kill();
            }

            isActive = false;
        }

    });

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            TelephonyManager ts = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            final boolean isCall = ts.getCallState() != TelephonyManager.CALL_STATE_IDLE;

            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_ON:
                    long delta = SystemClock.elapsedRealtime() - KeyguardActivity.focusLooseTime;
                    boolean becauseOfActivityLaunch = delta < ACTIVITY_LAUNCH_MAX_TIME;

                    if (DEBUG) Log.d(TAG, "Screen is on: is_call=" + isCall +
                            " activity_flag=" + becauseOfActivityLaunch);

                    if (isCall) {
                        return;
                    }

                    if (becauseOfActivityLaunch) {

                        // Finish AcDisplay activity so it won't shown
                        // after exiting from newly launched one.
                        Presenter.getInstance().kill();
                    } else if (KeyguardActivity.focusLooseTime != -1) startGui(); // Normal launch
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    if (!isCall) startGuiGhost(); // Ghost launch
                    break;
            }
        }

    };

    private void startGuiGhost() {
        startGui();
    }

    private void startGui() {
        Presenter.getInstance().tryStartGuiCauseKeyguard(getContext());
    }

    @Override
    public void onCreate() {
        Config config = Config.getInstance();
        config.registerListener(this);
        (config.isKeyguardWithoutNotifiesEnabled() ? mAtomicMain : mAtomicOption).start();
    }

    @Override
    public void onDestroy() {
        Config config = Config.getInstance();
        config.unregisterListener(this);
        mAtomicOption.stop();
        mAtomicMain.stop();
    }

    @Override
    public String getLabel() {
        return getContext().getString(R.string.service_bath_keyguard);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConfigChanged(@NonNull ConfigBase config,
                                @NonNull String key,
                                @NonNull Object value) {
        switch (key) {
            case Config.KEY_KEYGUARD_WITHOUT_NOTIFICATIONS:
                boolean start = !(boolean) value;
                mAtomicOption.react(start);
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNotificationListChanged(@NonNull NotificationPresenter np,
                                          OpenNotification osbn,
                                          int event, boolean f) {
        switch (event) {
            case NotificationPresenter.EVENT_POSTED:
            case NotificationPresenter.EVENT_REMOVED:
            case NotificationPresenter.EVENT_BATH:
                updateState();
                break;
        }
    }

    private void updateState() {
        boolean start = NotificationPresenter.getInstance().size() >= 1;
        mAtomicMain.react(start);
    }

}
