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

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;

import com.achep.activedisplay.Device;

/**
 * Created by Artem on 15.02.14.
 */
public class SendNotificationService extends Service {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_TEXT = "text";
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_ICON_RESOURCE = "icon";
    public static final String EXTRA_LARGE_ICON_RESOURCE = "large_icon";
    public static final String EXTRA_PRIORITY = "priority";
    public static final String EXTRA_SOUND_URI = "sound_uri";
    public static final String EXTRA_CONTENT_INTENT = "content_intent";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return Service.START_STICKY;
    }

    private void handleIntent(Intent intent) {
        CharSequence title = intent.getCharSequenceExtra(EXTRA_TITLE);
        CharSequence text = intent.getCharSequenceExtra(EXTRA_TEXT);
        Uri soundUri = intent.getParcelableExtra(EXTRA_SOUND_URI);
        PendingIntent pi = intent.getParcelableExtra(EXTRA_CONTENT_INTENT);
        int id = intent.getIntExtra(EXTRA_ID, 0);
        int iconRes = intent.getIntExtra(EXTRA_ICON_RESOURCE, 0);
        int largeIconRes = intent.getIntExtra(EXTRA_LARGE_ICON_RESOURCE, 0);
        int priority = intent.getIntExtra(EXTRA_PRIORITY, Notification.PRIORITY_DEFAULT);

        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(iconRes)
                .setPriority(priority)
                .setAutoCancel(true)
                .setSound(soundUri);

        if (largeIconRes != 0) {
            builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), largeIconRes));
        }
        if (pi != null) {
            builder.setContentIntent(pi);
        }

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(id, builder.build());

        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static PendingIntent notify(Context context, Intent intent, int delayMillis) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(context,
                intent.getIntExtra(EXTRA_ID, 0), intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        if (Device.hasKitKatApi()) {
            am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayMillis, pendingIntent);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayMillis, pendingIntent);
        }
        return pendingIntent;
    }

    public static void cancel(Context context, PendingIntent pendingIntent) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pendingIntent);
    }

    /**
     * Simple notification builder with all possible args.
     *
     * @see #notify(android.content.Context, android.content.Intent, int)
     * @see #cancel(android.content.Context, android.app.PendingIntent)
     */
    public static Intent createNotificationIntent(Context context, String title, String text,
                                                  int id, int iconRes, int largeIconRes, int priority,
                                                  Uri soundUri, PendingIntent pi) {
        return new Intent(context, SendNotificationService.class)
                .putExtra(SendNotificationService.EXTRA_TITLE, title)
                .putExtra(SendNotificationService.EXTRA_TEXT, text)
                .putExtra(SendNotificationService.EXTRA_ID, id)
                .putExtra(SendNotificationService.EXTRA_ICON_RESOURCE, iconRes)
                .putExtra(SendNotificationService.EXTRA_LARGE_ICON_RESOURCE, largeIconRes)
                .putExtra(SendNotificationService.EXTRA_PRIORITY, priority)
                .putExtra(SendNotificationService.EXTRA_SOUND_URI, soundUri)
                .putExtra(SendNotificationService.EXTRA_CONTENT_INTENT, pi);
    }
}
