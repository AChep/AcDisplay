package com.achep.activedisplay.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;

/**
 * Created by Artem on 15.02.14.
 */
public class SendNotificationService extends Service {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_TEXT = "text";
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_ICON_RESOURCE = "icon";
    public static final String EXTRA_PRIORITY = "priority";
    public static final String EXTRA_SOUND_URI = "sound_uri";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return Service.START_STICKY;
    }

    private void handleIntent(Intent intent) {
        CharSequence title = intent.getCharSequenceExtra(EXTRA_TITLE);
        CharSequence text = intent.getCharSequenceExtra(EXTRA_TEXT);
        Uri soundUri = intent.getParcelableExtra(EXTRA_SOUND_URI);
        int id = intent.getIntExtra(EXTRA_ID, 0);
        int iconRes = intent.getIntExtra(EXTRA_ICON_RESOURCE, 0);
        int priority = intent.getIntExtra(EXTRA_PRIORITY, Notification.PRIORITY_DEFAULT);

        Notification n = new Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(iconRes)
                .setPriority(priority)
                .setSound(soundUri)
                .build();

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(id, n);

        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static PendingIntent sendDelayed(Context context, Intent intent, int delayMillis) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayMillis, pendingIntent);
        return pendingIntent;
    }

    public static void cancel(Context context, PendingIntent pendingIntent) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pendingIntent);
    }
}
