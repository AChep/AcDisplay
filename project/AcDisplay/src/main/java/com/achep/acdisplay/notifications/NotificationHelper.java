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
package com.achep.acdisplay.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.achep.acdisplay.Device;
import com.achep.acdisplay.Operator;
import com.achep.acdisplay.services.MediaService;
import com.achep.acdisplay.utils.PendingIntentUtils;

/**
 * Created by Artem on 19.01.14.
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";

    public static boolean startContentIntent(StatusBarNotification notification) {
        if (notification != null) {
            PendingIntent pi = notification.getNotification().contentIntent;
            boolean successful = PendingIntentUtils.sendPendingIntent(pi);
            if (successful && Operator.bitAnd(
                    notification.getNotification().flags,
                    Notification.FLAG_AUTO_CANCEL)) {
                dismissNotification(notification);
            }
            return successful;
        }
        return false;
    }

    public static void dismissNotification(StatusBarNotification notification) {
        MediaService service = MediaService.sService;
        if (service != null) {
            if (Device.hasLemonCakeApi()) {
                service.cancelNotification(notification.getKey());
            } else {
                service.cancelNotification(
                        notification.getPackageName(),
                        notification.getTag(),
                        notification.getId());
            }
            NotificationPresenter.getInstance().removeNotification(service, notification);
        } else {
            Log.e(TAG, "Failed to dismiss notification because notification service is offline.");
        }
    }

}
