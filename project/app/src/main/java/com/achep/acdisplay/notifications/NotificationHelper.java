/*
 * Copyright (C) 2015 AChep@xda <artemchep@gmail.com>
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
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import com.achep.acdisplay.App;
import com.achep.acdisplay.R;
import com.achep.acdisplay.ui.activities.MainActivity;

/**
 * Created by Artem Chepurnoy on 07.05.2015.
 */
public class NotificationHelper {

    @NonNull
    public static Notification buildNotification(@NonNull Context context, final int id,
                                                 @NonNull Object... objects) {
        final Resources res = context.getResources();
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.stat_acdisplay)
                .setColor(App.ACCENT_COLOR)
                .setAutoCancel(true);

        PendingIntent pi = null;
        switch (id) {
            case App.ID_NOTIFY_TEST: {
                NotificationCompat.BigTextStyle bts = new NotificationCompat.BigTextStyle()
                        .bigText(res.getString(R.string.notification_test_message_large));
                builder.setStyle(bts)
                        .setContentTitle(res.getString(R.string.app_name))
                        .setContentText(res.getString(R.string.notification_test_message))
                        .setLargeIcon(BitmapFactory.decodeResource(res, R.mipmap.ic_launcher))
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                break;
            }
            case App.ID_NOTIFY_BATH: {
                CharSequence contentText = (CharSequence) objects[0];
                Intent contentIntent = (Intent) objects[1];
                // Build notification
                pi = PendingIntent.getActivity(context, id, contentIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setContentTitle(res.getString(R.string.service_bath))
                        .setContentText(contentText)
                        .setPriority(Notification.PRIORITY_MIN);
                break;
            }
            case App.ID_NOTIFY_INIT: {
                builder.setSmallIcon(R.drawable.stat_notify)
                        .setContentTitle(res.getString(R.string.app_name))
                        .setContentText(res.getString(R.string.notification_init_text))
                        .setPriority(Notification.PRIORITY_MIN);
                break;
            }
            case App.ID_NOTIFY_APP_AUTO_DISABLED: {
                CharSequence summary = (CharSequence) objects[0];
                NotificationCompat.BigTextStyle bts = new NotificationCompat.BigTextStyle()
                        .bigText(res.getString(R.string.permissions_auto_disabled))
                        .setSummaryText(summary);
                builder.setLargeIcon(BitmapFactory.decodeResource(res, R.mipmap.ic_launcher))
                        .setContentTitle(res.getString(R.string.app_name))
                        .setContentText(res.getString(R.string.permissions_auto_disabled))
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setStyle(bts);
                break;
            }
            default:
                throw new IllegalArgumentException();
        }
        if (pi == null) {
            pi = PendingIntent.getActivity(context,
                    id, new Intent(context, MainActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return builder.setContentIntent(pi).build();
    }

    public static void sendNotification(@NonNull Context context, final int id,
                                        @NonNull Object... objects) {
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(id, buildNotification(context, id, objects));
    }

}
