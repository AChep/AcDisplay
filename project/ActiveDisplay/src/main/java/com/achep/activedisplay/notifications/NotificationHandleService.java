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
package com.achep.activedisplay.notifications;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.achep.activedisplay.Project;
import com.achep.activedisplay.utils.LogUtils;

/**
 * Created by Artem on 27.12.13.
 */
public class NotificationHandleService extends NotificationListenerService {

    private static final String TAG = "NotificationHandleService";

    public static NotificationHandleService notificationHandleService;
    public static boolean isNotificationAccessEnabled = false;

    @Override
    public IBinder onBind(Intent intent) {
        IBinder binder = super.onBind(intent);

        if (Project.DEBUG) LogUtils.track();
        NotificationPresenter.getInstance(this).tryStartInitProcess(this);

        isNotificationAccessEnabled = true;
        notificationHandleService = this;
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        boolean unbind = super.onUnbind(intent);

        if (Project.DEBUG) LogUtils.track();

        isNotificationAccessEnabled = false;
        notificationHandleService = null;
        return unbind;
    }

    public static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (NotificationHandleService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        try {
            NotificationPresenter.getInstance(this).postNotification(this, statusBarNotification);
        } catch (Exception e) { // don't die
            Log.wtf(TAG, "The world crashed. The details below:");
            e.printStackTrace();
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification statusBarNotification) {
        try {
            NotificationPresenter.getInstance(this).removeNotification(this, statusBarNotification);
        } catch (Exception e) { // don't die
            Log.wtf(TAG, "The world crashed. The details below:");
            e.printStackTrace();
        }
    }

}
