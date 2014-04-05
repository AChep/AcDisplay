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
package com.achep.activedisplay.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.SystemClock;
import android.telephony.TelephonyManager;

import com.achep.activedisplay.ActiveDisplayPresenter;
import com.achep.activedisplay.Config;
import com.achep.activedisplay.NotificationIds;
import com.achep.activedisplay.R;
import com.achep.activedisplay.activities.AcDisplayActivity;
import com.achep.activedisplay.settings.Settings;
import com.achep.activedisplay.utils.PowerUtils;

/**
 * Created by Artem on 16.02.14.
 */
public class LockscreenService extends Service {

    public static long sIgnoreTillTime;

    public static void ignoreCurrentTurningOn() {
        sIgnoreTillTime = SystemClock.elapsedRealtime() + 2000;
    }

    /**
     * Starts or stops this service as required by settings and device's state.
     */
    public static void handleState(Context context) {
        Intent intent = new Intent(context, LockscreenService.class);
        Config config = Config.getInstance(context);
        if (config.isActiveDisplayEnabled() && config.isLockscreenEnabled()) {
            if (!config.isEnabledOnlyWhileCharging() || PowerUtils.isPlugged(context)) {

                context.startService(intent);
            }
        } else {
            context.stopService(intent);
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            TelephonyManager ts = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            final boolean isCall = ts.getCallState() != TelephonyManager.CALL_STATE_IDLE;

            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_ON:

                    // Somebody requested this ignoring.
                    if (SystemClock.elapsedRealtime() < sIgnoreTillTime && !isCall) {
                        sIgnoreTillTime = 0;
                        return;
                    }

                    if (isCall) {

                        // Why do we need to kill it? Because otherwise it'll
                        // be displayed after you've done with your call and
                        // closed phone app.
                        ActiveDisplayPresenter.getInstance().kill();
                    } else startGui();

                    break;
                case Intent.ACTION_SCREEN_OFF:
                    // Do not start
                    if (!isCall) startGui();
                    break;
            }
        }

    };

    private void startGui() {
        startActivity(new Intent(Intent.ACTION_MAIN, null)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        | Intent.FLAG_ACTIVITY_NO_USER_ACTION
                        | Intent.FLAG_ACTIVITY_NO_ANIMATION
                        | Intent.FLAG_FROM_BACKGROUND)
                .setClass(this, AcDisplayActivity.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.setPriority(Integer.MAX_VALUE);
        registerReceiver(mReceiver, intentFilter);

        int notificationId = NotificationIds.LOCKSCREEN_NOTIFICATION;
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                notificationId, new Intent(this, Settings.LockscreenSettingsActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.stat_lock)
                .setContentTitle(getString(
                        R.string.service_lockscreen,
                        getString(R.string.app_name)))
                .setContentText(getString(R.string.service_lockscreen_text))
                .setPriority(Notification.PRIORITY_MIN)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        startForeground(notificationId, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
