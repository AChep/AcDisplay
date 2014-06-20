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

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.achep.acdisplay.Build;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.Presenter;
import com.achep.acdisplay.R;
import com.achep.acdisplay.acdisplay.AcDisplayActivity;
import com.achep.acdisplay.utils.PackageUtils;
import com.achep.acdisplay.utils.PowerUtils;

import java.util.List;

/**
 * Created by Artem on 16.02.14.
 *
 * @author Artem Chepurnoy
 */
public class KeyguardService extends BathService.ChildService {

    private static final String TAG = "KeyguardService";

    private static final int ACTIVITY_LAUNCH_MAX_TIME = 1000;

    private ActivityMonitorThread mActivityMonitorThread;

    /**
     * Starts or stops this service as required by settings and device's state.
     */
    public static void handleState(Context context) {
        Intent intent = new Intent(context, KeyguardService.class);
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
                        activityName = mActivityMonitorThread.topActivityName;
                        activityChangeTime = mActivityMonitorThread.topActivityTime;
                    }

                    stopMonitoringActivities();

                    long now = SystemClock.elapsedRealtime();
                    boolean becauseOfActivityLaunch =
                            now - activityChangeTime < ACTIVITY_LAUNCH_MAX_TIME
                                    && activityName != null && !activityName.startsWith(
                                    PackageUtils.getName(getContext()));

                    if (isCall) {
                        return;
                    }

                    if (becauseOfActivityLaunch) {

                        // Finish AcDisplay activity so it won't shown
                        // after exiting from newly launched one.
                        Presenter.getInstance().kill();
                    } else startGui();

                    if (Build.DEBUG)
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
        Context context = getContext();
        context.startActivity(new Intent(context, AcDisplayActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_NO_ANIMATION));
    }

    private void startMonitoringActivities() {
        stopMonitoringActivities();
        if (Build.DEBUG) Log.d(TAG, "Starting to monitor activities.");

        ActivityManager am = (ActivityManager) getContext()
                .getSystemService(Context.ACTIVITY_SERVICE);
        mActivityMonitorThread = new ActivityMonitorThread(am);
        mActivityMonitorThread.start();
    }

    private void stopMonitoringActivities() {
        if (mActivityMonitorThread != null) {
            if (Build.DEBUG) Log.d(TAG, "Stopping to monitor activities.");

            mActivityMonitorThread.running = false;
            mActivityMonitorThread.interrupt();
            mActivityMonitorThread = null;
        }
    }

    @Override
    public void onCreate() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1); // highest priority
        getContext().registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        getContext().unregisterReceiver(mReceiver);
        stopMonitoringActivities();
    }

    @Override
    public String getName() {
        return getContext().getString(R.string.service_bath_keyguard);
    }

    /**
     * Thread that monitors current top activity.
     * This is needed to prevent launching AcDisplay above any other
     * activity which launched with
     * {@link android.view.WindowManager.LayoutParams#FLAG_TURN_SCREEN_ON} flag.
     */
    private static class ActivityMonitorThread extends Thread {

        private static final long MONITORING_PERIOD = 15 * 60 * 1000; // 15 min.

        public volatile long topActivityTime;
        public volatile String topActivityName;
        public volatile boolean running = true;

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
            List<ActivityManager.RunningTaskInfo> tasks = mActivityManager.getRunningTasks(1);
            if (tasks == null || tasks.size() == 0) {
                return;
            }

            String topActivity = tasks.get(0).topActivity.getClassName();
            if (!topActivity.equals(topActivityName)) {

                // Update time if it's not first try.
                if (topActivityName != null) {
                    topActivityTime = SystemClock.elapsedRealtime();
                }

                topActivityName = topActivity;

                if (Build.DEBUG) Log.d(TAG, "Current latest activity is " + topActivityName);
            }
        }
    }

}
