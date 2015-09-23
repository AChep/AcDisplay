package com.achep.base.services;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.achep.base.Device;

import timber.log.Timber;

/**
 * @author Artem Chepurnoy
 */
public abstract class BaseStickyService extends BaseService {

    private static final long PENDING_INTENT_RESTART_DELAY = 4000; // 4 sec.

    /**
     * @return {@code true} if service is enabled and should be running,
     * {@code false} otherwise.
     */
    public abstract boolean isEnabled();

    public abstract int code();

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        remoteRestartServiceIfNeeded();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        remoteRestartServiceIfNeeded();
    }

    private void remoteRestartServiceIfNeeded() {
        if (isEnabled()) remoteRestartService();
    }

    @SuppressLint("NewApi")
    private void remoteRestartService() {
        Timber.d("Remote restart service.");
        final Intent intent = new Intent(this, getClass());
        final PendingIntent pi = PendingIntent.getService(this, code(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Ask alarm manger to restart us.
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (Device.hasMarshmallowApi()) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + PENDING_INTENT_RESTART_DELAY,
                    pi);
        } else alarm.set(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + PENDING_INTENT_RESTART_DELAY,
                pi);
    }

}
