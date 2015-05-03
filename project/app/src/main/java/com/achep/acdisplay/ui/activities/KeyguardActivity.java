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
package com.achep.acdisplay.ui.activities;

import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.WindowManager;

import com.achep.acdisplay.App;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.acdisplay.Timeout;
import com.achep.acdisplay.services.KeyguardService;
import com.achep.base.Device;
import com.achep.base.tests.Check;
import com.achep.base.ui.activities.ActivityBase;
import com.achep.base.utils.LogUtils;

import static com.achep.base.Build.DEBUG;


/**
 * Activity that contains some methods to emulate system keyguard.
 */
public abstract class KeyguardActivity extends ActivityBase implements
        Timeout.OnTimeoutEventListener {

    private static final String TAG = "KeyguardActivity";
    public static final String EXTRA_TURN_SCREEN_ON = "turn_screen_on";

    private static final int UNLOCKING_MAX_TIME = 150; // ms.
    private static final int PF_MAX_TIME = 2000; // ms.

    private BroadcastReceiver mScreenOffReceiver;
    private KeyguardManager mKeyguardManager;
    private long mUnlockingTime;
    private boolean mAttachedToWindow;
    private boolean mResumed;

    private boolean mTimeoutPaused = true;
    private final Timeout mTimeout = new Timeout();
    private final Handler mTimeoutHandler = new Handler();
    private final Handler mHandler = new Handler();

    private PowerManager.WakeLock mWakeLock;

    @Override
    public void onWindowFocusChanged(boolean windowHasFocus) {
        super.onWindowFocusChanged(windowHasFocus);
        if (DEBUG) Log.d(TAG, "On window focus changed " + windowHasFocus);

        if (isUnlocking()) {
            if (windowHasFocus) {
                mUnlockingTime = 0;
            } else {
                finish();
                return;
            }
        }
        if (mResumed) {
            populateFlags(windowHasFocus);
        }
    }

    private void populateFlags(boolean manualControl) {
        int windowFlags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        int timeoutDelay = Config.getInstance().getTimeoutNormal();

        if (manualControl) {
            getWindow().addFlags(windowFlags);

            mTimeoutPaused = false;
            mTimeoutHandler.removeCallbacksAndMessages(null);

            mTimeout.resume();
            mTimeout.setTimeoutDelayed(timeoutDelay, true);
        } else {
            getWindow().clearFlags(windowFlags);

            mTimeoutPaused = true;
            mTimeout.setTimeoutDelayed(timeoutDelay, true);
            mTimeout.pause();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.d(TAG, "Creating keyguard activity...");

        mTimeout.registerListener(this);
        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        registerScreenOffReceiver();

        int flags = 0;

        // Handle intents
        if (hasWakeUpExtra()) {
            flags |= WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
        }

        // FIXME: Android dev team broke the DISMISS_KEYGUARD flag.
        // https://code.google.com/p/android-developer-preview/issues/detail?id=1902
        if (Device.hasLollipopApi() /* bugs monster */ && !mKeyguardManager.isKeyguardSecure()) {
            getWindow().addFlags(flags | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        } else {
            getWindow().addFlags(flags |
                    // Show activity above the system keyguard.
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Handle intents
        if (hasWakeUpExtra(intent)) {
            acquireWakeUpLock();
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttachedToWindow = true;

        // Handle intents
        if (hasWakeUpExtra()) {
            acquireWakeUpFlags();
        }
    }

    private void acquireWakeUpLock() {
        Check.getInstance().isTrue(mAttachedToWindow);
        int flags = PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK;
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(flags, "Turn the keyguard on.");
        mWakeLock.acquire(500); // 0.5 sec.
    }

    private void acquireWakeUpFlags() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    /**
     * @return {@code true} if the {@link #getIntent() intent} includes the
     * {@link #EXTRA_TURN_SCREEN_ON} set to {@code true}, {@code false}
     * otherwise.
     */
    private boolean hasWakeUpExtra() {
        return hasWakeUpExtra(getIntent());
    }

    private boolean hasWakeUpExtra(@Nullable Intent intent) {
        return intent != null && intent.getBooleanExtra(EXTRA_TURN_SCREEN_ON, false);
    }

    @Override
    public void onDetachedFromWindow() {
        mAttachedToWindow = false;
        if (mWakeLock != null && mWakeLock.isHeld()) mWakeLock.release();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying keyguard activity...");

        unregisterScreenOffReceiver();

        mTimeout.unregisterListener(this);
        mTimeout.clear();

        super.onDestroy();
    }

    /**
     * Registers a receiver to finish activity when screen goes off.
     * You will need to {@link #unregisterScreenOffReceiver() unregister} it
     * later.
     *
     * @see #unregisterScreenOffReceiver()
     */
    private void registerScreenOffReceiver() {
        mScreenOffReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (!KeyguardService.isActive) {
                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Finalize the keyguard.").acquire(200);
                    KeyguardActivity.this.finish();
                }
            }

        };

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1); // max allowed priority
        registerReceiver(mScreenOffReceiver, intentFilter);
    }

    /**
     * Unregisters the screen off receiver if it was registered previously.
     *
     * @see #registerScreenOffReceiver()
     */
    private void unregisterScreenOffReceiver() {
        if (mScreenOffReceiver != null) {
            unregisterReceiver(mScreenOffReceiver);
            mScreenOffReceiver = null;
        }
    }

    @Override
    protected void onResume() {
        if (DEBUG) Log.d(TAG, "Resuming keyguard activity...");
        super.onResume();

        mResumed = true;
        mUnlockingTime = 0;
        populateFlags(true);
        overrideHomePress(true);

        /*
        // Read the system's screen off timeout setting.
        try {
            mSystemScreenOffTimeout = Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.SCREEN_OFF_TIMEOUT);
        } catch (Settings.SettingNotFoundException e) {
            mSystemScreenOffTimeout = -1;
        }
        */
    }

    @Override
    protected void onPause() {
        if (DEBUG) Log.d(TAG, "Pausing keyguard activity...");
        mResumed = false;
        populateFlags(false);
        overrideHomePress(false);

        mHandler.removeCallbacksAndMessages(null);

        super.onPause();
    }

    /**
     * Notifies Xposed {@link com.achep.acdisplay.plugins.xposed.OverrideHomeButton module}
     * to start ignoring home button press. Please, notice that it will ignore home button
     * click everywhere until you call {@code overrideHomePress(false)}
     *
     * @param override {@code true} to start ignoring, {@code false} to stop.
     * @see com.achep.acdisplay.plugins.xposed.OverrideHomeButton
     * @see #sendBroadcast(android.content.Intent)
     */
    private void overrideHomePress(boolean override) {
        Intent intent = new Intent(override
                ? App.ACTION_EAT_HOME_PRESS_START
                : App.ACTION_EAT_HOME_PRESS_STOP);
        sendBroadcast(intent);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (DEBUG) Log.d(TAG, "User is leaving...");
        /*
        // The user has tried to fool the keyguard.
        // Blame him ("You shall not pass!")
        // and turn the screen off.
        if (PowerUtils.isScreenOn(this)) {
            if (!mUnlocking) {
                Log.i(TAG, "You shall not pass!");
                lock();
            } else if (!mLocking) {
                finish();
            }
        }
        */
    }

    @Override
    public void onTimeoutEvent(@NonNull Timeout timeout, int event) {
        if (DEBUG) LogUtils.v(TAG, "TIMEOUT: " + event, 5);
        switch (event) {
            case Timeout.EVENT_CHANGED:
            case Timeout.EVENT_RESUMED:
                if (mTimeoutPaused) {
                    mTimeoutHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mTimeout.pause();
                        }
                    });
                }
                break;
            case Timeout.EVENT_TIMEOUT:
                Check.getInstance().isFalse(mTimeoutPaused);
                lock();
                break;
        }
    }

    /**
     * Locks the device (and turns screen off).
     *
     * @return {@code true} if successful, {@code false} otherwise.
     * @see DevicePolicyManager#lockNow()
     */
    public boolean lock() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        try {
            // TODO: Respect secure lock timeout settings.
            mUnlockingTime = 0;
            dpm.lockNow();
            return true;
        } catch (SecurityException e) {
            return false; // Screw you owner!
        }
    }

    /**
     * Unlocks keyguard and runs {@link Runnable runnable} when unlocked.
     */
    public void unlock(@Nullable Runnable runnable) {
        unlock(runnable, true);
    }

    /**
     * Unlocks keyguard and runs {@link Runnable runnable} when unlocked.
     *
     * @param finish {@code true} to finish activity, {@code false} to keep it
     * @see #unlock(Runnable)
     */
    public void unlock(final @Nullable Runnable runnable, final boolean finish) {
        if (DEBUG) Log.d(TAG, "Unlocking with params: finish=" + finish);

        // If keyguard is disabled no need to make
        // a delay between calling this method and
        // unlocking.
        // Otherwise we need this delay to get new
        // flags applied.
        final long now = SystemClock.elapsedRealtime();

        mUnlockingTime = now;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        mHandler.post(new Runnable() {

            @Override
            public void run() {
                // Loop until flag gets applied.
                // TODO: Use somewhat trigger for detecting unlocking.
                int delta = (int) (SystemClock.elapsedRealtime() - now);
                if (isLocked() && !isSecure() && delta < UNLOCKING_MAX_TIME) {
                    mHandler.postDelayed(this, 30);
                    return;
                }

                if (runnable != null) runOnUiThread(runnable);
                if (finish) {
                    finish();

                    Config config = Config.getInstance();
                    boolean animate = config.isUnlockAnimationEnabled() && !isPowerSaveMode();
                    overridePendingTransition(0, animate
                            ? R.anim.activity_unlock
                            : 0);
                }
            }
        });
    }

    /**
     * Return whether the keyguard requires a password to unlock.
     *
     * @return {@code true} is keyguard is secure, {@code false} otherwise.
     */
    public boolean isSecure() {
        return mKeyguardManager.isKeyguardSecure();
    }

    /**
     * Return whether the keyguard presents.
     *
     * @return {@code true} if device is locked, {@code false} otherwise.
     */
    public boolean isLocked() {
        return mKeyguardManager.isKeyguardLocked();
    }

    /**
     * Return whether the keyguard is unlocking.
     *
     * @return {@code true} if the keyguard is unlocking atm, {@code false} otherwise.
     * @see #unlock(Runnable)
     * @see #PF_MAX_TIME
     */
    public boolean isUnlocking() {
        return SystemClock.elapsedRealtime() - mUnlockingTime <= PF_MAX_TIME;
    }

    @NonNull
    public Timeout getTimeout() {
        return mTimeout;
    }

    @Override
    public void onBackPressed() { /* override back button */ }

}
