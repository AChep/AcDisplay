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

import android.app.ActivityManager;
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
import android.util.Log;

import com.achep.activedisplay.Config;
import com.achep.activedisplay.NotificationIds;
import com.achep.activedisplay.Presenter;
import com.achep.activedisplay.Project;
import com.achep.activedisplay.R;
import com.achep.activedisplay.activities.AcDisplayActivity;
import com.achep.activedisplay.settings.Settings;
import com.achep.activedisplay.utils.PowerUtils;

/**
 * Created by Artem on 16.02.14.
 */
public class KeyguardService extends Service {

    private static final String TAG = "KeyguardService";

    private static final int ACTIVITY_LAUNCH_MAX_TIME = 1000;

    private ActivityMonitorThread mActivityMonitorThread;

    public static long sIgnoreTillTime;

    /**
     * Prevents launching keyguard on soonest turning screen on.
     *
     * @deprecated hopefully the bug with it is fixed now, so no need to use it. Just in case...
     */
    @Deprecated
    public static void ignoreCurrentTurningOn() {
        sIgnoreTillTime = SystemClock.elapsedRealtime() + 2000;
    }

    /**
     * Starts or stops this service as required by settings and device's state.
     */
    public static void handleState(Context context) {
        Intent intent = new Intent(context, KeyguardService.class);
        Config config = Config.getInstance();

        boolean onlyWhileChangingOption = !config.isEnabledOnlyWhileCharging()
                || PowerUtils.isPlugged(context);

        if (config.isEnabled()
                && config.isKeyguardEnabled()
                && onlyWhileChangingOption) {
            context.startService(intent);
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
                    String activityName = null;
                    long activityChangeTime = 0;
                    if (mActivityMonitorThread != null) {
                        mActivityMonitorThread.monitor();
                        activityName = mActivityMonitorThread.activityName;
                        activityChangeTime = mActivityMonitorThread.activityChangeTime;
                    }

                    stopMonitoringActivities();

                    long now = SystemClock.elapsedRealtime();
                    boolean becauseOfIgnoringPolicy = now < sIgnoreTillTime;
                    boolean becauseOfActivityLaunch =
                            now - activityChangeTime < ACTIVITY_LAUNCH_MAX_TIME
                                    && activityName != null && !activityName.startsWith(
                                    Project.getPackageName(KeyguardService.this));

                    if (isCall || becauseOfIgnoringPolicy) {
                        sIgnoreTillTime = 0;
                        return;
                    }

                    if (becauseOfActivityLaunch) {

                        // Finish AcDisplay activity so it won't shown
                        // after exiting from newly launched one.
                        Presenter.getInstance().kill();
                    } else startGui();

                    if (Project.DEBUG)
                        Log.d(TAG, "Screen is on: is_call=" + isCall +
                                " activity_flag=" + becauseOfActivityLaunch);
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    if (!isCall) startGui();

                    startMonitoringActivities();
                    break;
            }
        }

    };

    private void startGui() {
        startActivity(new Intent(this, AcDisplayActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        | Intent.FLAG_ACTIVITY_NO_ANIMATION));
    }

    private void startMonitoringActivities() {
        stopMonitoringActivities();
        if (Project.DEBUG) Log.d(TAG, "Starting to monitor activities.");

        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        mActivityMonitorThread = new ActivityMonitorThread(am);
        mActivityMonitorThread.start();
    }

    private void stopMonitoringActivities() {
        if (mActivityMonitorThread != null) {
            if (Project.DEBUG) Log.d(TAG, "Stopping to monitor activities.");

            mActivityMonitorThread.running = false;
            mActivityMonitorThread.interrupt();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1); // highest priority
        registerReceiver(mReceiver, intentFilter);

        int notificationId = NotificationIds.LOCKSCREEN_NOTIFICATION;
        Intent intent = new Intent(this, Settings.LockscreenSettingsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
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
        stopMonitoringActivities();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Thread that monitors current top activity.
     * This is needed to prevent launching AcDisplay on any other
     * activity launched with
     * {@link android.view.WindowManager.LayoutParams#FLAG_TURN_SCREEN_ON} flag.
     */
    private static class ActivityMonitorThread extends Thread {

        private static final long MONITORING_PERIOD = 15 * 60 * 1000; // ms.

        public volatile boolean running = true;
        public volatile long activityChangeTime;
        public volatile String activityName;

        private final ActivityManager mActivityManager;

        public ActivityMonitorThread(ActivityManager activityManager) {
            mActivityManager = activityManager;
        }

        @Override
        public void run() {
            super.run();

            while (running) {
                monitor();

                try {
                    Thread.sleep(MONITORING_PERIOD);
                } catch (InterruptedException e) { /* unused */ }
            }
        }

        /**
         * Checks what activity is the latest.
         */
        public synchronized void monitor() {
            String latestActivityName;
            try {
                ActivityManager.RunningTaskInfo task = mActivityManager.getRunningTasks(1).get(0);
                latestActivityName = task.topActivity.getClassName();
            } catch (NullPointerException e) {
                return; // Not a problem, just too lazy :)
            }

            assert latestActivityName != null;

            if (!latestActivityName.equals(activityName)) {
                if (activityName != null) { // first start
                    this.activityChangeTime = SystemClock.elapsedRealtime(); // deep sleep
                }

                activityName = latestActivityName;

                if (Project.DEBUG) Log.d(TAG, "Current latest activity is " + activityName);
            }
        }
    }

}
