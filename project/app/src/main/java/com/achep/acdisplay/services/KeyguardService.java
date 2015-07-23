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
import android.os.AsyncTask;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.Presenter;
import com.achep.acdisplay.R;
import com.achep.acdisplay.services.switches.AlarmSwitch;
import com.achep.acdisplay.services.switches.InactiveTimeSwitch;
import com.achep.acdisplay.services.switches.NoNotifiesSwitch;
import com.achep.acdisplay.services.switches.PhoneCallSwitch;
import com.achep.acdisplay.utils.tasks.RunningTasks;
import com.achep.base.AppHeap;
import com.achep.base.content.ConfigBase;
import com.achep.base.tests.Check;
import com.achep.base.utils.PackageUtils;
import com.achep.base.utils.power.PowerUtils;

import static com.achep.base.Build.DEBUG;

/**
 * Created by Artem on 16.02.14.
 *
 * @author Artem Chepurnoy
 */
public class KeyguardService extends SwitchService {

    private static final String TAG = "KeyguardService";

    /**
     * {@code true} if the {@link KeyguardService keyguard service is running}
     * and functioning, {@code false} otherwise.
     */
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

    private static final int ACTIVITY_LAUNCH_MAX_TIME = 1000;

    private PhoneCallSwitch mPhoneCallSwitch;
    private ActivityMonitorThread mActivityMonitorThread;
    private String mPackageName;

    private final Presenter mPresenter = Presenter.getInstance();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            mScreenReceiver.onReceive(context, intent);
            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_ON:
                    String activityName = null;
                    long activityChangeTime = 0;
                    if (mActivityMonitorThread != null) {
                        //noinspection SynchronizeOnNonFinalField
                        synchronized (mActivityMonitorThread) {
                            mActivityMonitorThread.monitor();
                            activityName = mActivityMonitorThread.topActivityName;
                            activityChangeTime = mActivityMonitorThread.topActivityTime;
                        }
                    }

                    stopMonitorThread();

                    if (mPhoneCallSwitch.isCalling()) {
                        mPresenter.kill();
                        return;
                    }

                    long now = SystemClock.elapsedRealtime();
                    boolean becauseOfActivityLaunch =
                            now - activityChangeTime < ACTIVITY_LAUNCH_MAX_TIME
                                    && activityName != null
                                    && !activityName.startsWith(mPackageName);

                    if (DEBUG) Log.d(TAG, "Screen is on: activity_flag=" + becauseOfActivityLaunch);

                    if (becauseOfActivityLaunch) {

                        // Finish AcDisplay activity so it won't shown
                        // after exiting from newly launched one.
                        mPresenter.kill();
                    } else if (mLocked) startGui(); // Normal launch
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    mLocked = mPresenter.isLocked();
                    performLockWithDelay();
                    break;
            }
        }

    };

    private boolean mLocked;

    /**
     * {@code true} if the screen if actually off,
     * {@code false} otherwise.
     */
    private boolean mScreenOff;

    /**
     * The {@link SystemClock#elapsedRealtime()} of when the screen has
     * been turned off.
     */
    private long mScreenOffTimestamp;

    @Nullable
    private AsyncTask<Void, Void, Void> mDelayedLockTask;

    @NonNull
    private final BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_ON:
                    if (DEBUG) Log.i(TAG, "Screen on");
                    mScreenOffTimestamp = 0;
                    mScreenOff = false;
                    cancelLockWithDelay();
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    if (DEBUG) Log.i(TAG, "Screen off");
                    mScreenOffTimestamp = SystemClock.elapsedRealtime();
                    mScreenOff = true;
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

    private void performLockWithDelay() {
        if (DEBUG) Log.i(TAG, "Trying to perform the delayed lock...");

        final long now = SystemClock.elapsedRealtime();
        final int d = Config.getInstance().getKeyguardLockDelayMillis();
        final int delay = d - (int) (now - mScreenOffTimestamp);
        final int delayMax = 30000;
        if (delay <= 0 || delay > delayMax) {
            performLock();
        } else if (delay > delayMax) {
            // TODO: Allow delays more than 5/30s without lags.
            // I can do it using the Android Alarm system.
        } else mDelayedLockTask = new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if (DEBUG) Log.i(TAG, "Starting the delay thread...");
                if (delay > delayMax) throw new RuntimeException(); // Do you really want it?
                // Don't let the OS to go into a deep sleep until we
                // finish our job.
                PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
                pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Delayed lock.").acquire(delay + 100);
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) { /* totally fine */ }
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                super.onPostExecute(v);
                Check.getInstance().isTrue(mScreenOff);
                performLock();
            }

        }.execute();
    }

    private void performLock() {
        if (DEBUG) Log.i(TAG, "Trying to perform the lock: screen_off=" + mScreenOff);

        if (mScreenOff) {
            mLocked = true;

            startGuiGhost();
            startMonitorThread();
        }
    }

    private void cancelLockWithDelay() {
        if (mDelayedLockTask != null) mDelayedLockTask.cancel(true);
    }

    @NonNull
    @Override
    public Switch[] onBuildSwitches() {
        Config config = Config.getInstance();
        ConfigBase.Option noNotifies = config.getOption(Config.KEY_KEYGUARD_WITHOUT_NOTIFICATIONS);
        ConfigBase.Option respectIt = config.getOption(Config.KEY_KEYGUARD_RESPECT_INACTIVE_TIME);
        return new Switch[]{
                mPhoneCallSwitch = new PhoneCallSwitch(getContext(), this),
                new AlarmSwitch(getContext(), this),
                // Options
                new NoNotifiesSwitch(getContext(), this, noNotifies, true),
                new InactiveTimeSwitch(getContext(), this, respectIt),
        };
    }

    @NonNull
    @Override
    public String getLabel() {
        return getContext().getString(R.string.service_bath_keyguard);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final Context context = getContext();
        mPackageName = PackageUtils.getName(context);
        mScreenOff = !PowerUtils.isScreenOn(context);
        // Register base receiver that is watching ACTION_SCREEN_ON,
        // ACTION_SCREEN_OFF and does completely nothing except
        // saving the screen off timestamp.
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1); // highest priority
        context.registerReceiver(mScreenReceiver, intentFilter);
    }

    @Override
    public void onStart(@Nullable Object... objects) {
        final Context context = getContext();
        // Register receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1); // highest priority
        context.registerReceiver(mReceiver, intentFilter);

        if (mScreenOff) {
            // Make sure the app is launched
            mLocked = mPresenter.isLocked();
            performLockWithDelay();
        }

        isActive = true;
    }

    @Override
    public void onStop(@Nullable Object... objects) {
        final Context context = getContext();
        context.unregisterReceiver(mReceiver);
        stopMonitorThread();
        cancelLockWithDelay();

        if (mScreenOff) {
            // Make sure that the app is not
            // waiting in the shade.
            mPresenter.kill();
        }

        isActive = false;
    }

    @Override
    public void onDestroy() {
        final Context context = getContext();
        context.unregisterReceiver(mScreenReceiver);

        super.onDestroy();

        // Watch for the leaks
        AppHeap.getRefWatcher().watch(this);
    }

    private void startMonitorThread() {
        stopMonitorThread();
        mActivityMonitorThread = new ActivityMonitorThread(getContext());
        mActivityMonitorThread.start();
    }

    private void stopMonitorThread() {
        if (mActivityMonitorThread == null) {
            return; // Nothing to stop
        }

        mActivityMonitorThread.running = false;
        mActivityMonitorThread.interrupt();
        mActivityMonitorThread = null;
    }

    /**
     * Thread that monitors the current top activity.
     *
     * @author Artem Chepurnoy
     */
    private static class ActivityMonitorThread extends Thread {

        /**
         * How frequently should we check top running activity. The
         * values is in millis.
         */
        private static final long PERIOD = 15 * 60 * 1000; // 15 min.

        public volatile long topActivityTime;
        public volatile String topActivityName;
        public volatile boolean running = true;

        @NonNull
        private final Context mContext;

        public ActivityMonitorThread(@NonNull Context context) {
            if (DEBUG) Log.d(TAG, "Activity monitor thread has been initiated.");
            mContext = context;
        }

        @Override
        public void run() {
            super.run();
            while (running) {
                monitor();

                try {
                    Thread.sleep(PERIOD);
                } catch (InterruptedException e) { /* unused */ }
            }
        }

        /**
         * Checks what activity is the latest.
         */
        public synchronized void monitor() {
            String topActivity = RunningTasks.getInstance().getRunningTasksTop(mContext);
            if (TextUtils.equals(topActivity, topActivityName)) {
                return;
            }

            topActivityName = topActivity;
            topActivityTime = SystemClock.elapsedRealtime();
            Log.i(TAG, "New top activity is " + topActivityName);
        }

        //-- DEBUG ----------------------------------------------------------------

        /* Only for debug purposes! */
        private final Object dFinalizeWatcher = DEBUG ? new Object() {

            /**
             * Logs the notifications' removal.
             */
            @Override
            protected void finalize() throws Throwable {
                try {
                    Log.d(TAG, "Activity monitor thread has died.");
                } finally {
                    super.finalize();
                }
            }

        } : null;
    }

}
