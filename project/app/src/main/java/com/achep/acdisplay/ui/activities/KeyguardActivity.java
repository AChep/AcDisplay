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
import android.view.View;
import android.view.WindowManager;

import com.achep.acdisplay.App;
import com.achep.acdisplay.R;
import com.achep.acdisplay.Timeout;
import com.achep.acdisplay.services.KeyguardService;
import com.achep.acdisplay.ui.activities.base.BaseActivity;
import com.achep.base.Device;
import com.achep.base.tests.Check;
import com.achep.base.utils.KeyguardUtils;
import com.achep.base.utils.ToastUtils;
import com.achep.base.utils.logs.TracingLog;
import com.achep.base.utils.power.PowerUtils;

import static com.achep.base.Build.DEBUG;

/**
 * Activity that contains some methods to emulate system keyguard.
 *
 * @author Artem Chepurnoy
 */
public abstract class KeyguardActivity extends BaseActivity implements
        Timeout.OnTimeoutEventListener {

    private static final String TAG = "KeyguardActivity";

    /**
     * An optional extra that contains the reason of this
     * wake up.
     */
    public static final String EXTRA_CAUSE = "cause";
    public static final String EXTRA_TURN_SCREEN_ON = "turn_screen_on";

    /**
     * This constant is CyanogenMod specific and should do nothing on a
     * stock Android.
     */
    private static final int PREVENT_POWER_KEY = 0x80000000;

    private static final int SYSTEM_UI_BASIC_FLAGS;

    static {
        final int f = Device.hasKitKatApi() ? View.SYSTEM_UI_FLAG_HIDE_NAVIGATION : 0;
        SYSTEM_UI_BASIC_FLAGS = f
                | View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
    }

    private static final int UNLOCKING_MAX_TIME = 150; // ms.
    private static final int PF_MAX_TIME = 2000; // ms.

    private BroadcastReceiver mScreenOffReceiver;
    private KeyguardManager mKeyguardManager;
    private long mUnlockingTime;
    private boolean mResumed;
    private int mExtraCause;

    private boolean mTimeoutPaused = true;
    private final Timeout mTimeout = new Timeout();
    private final Handler mHandler = new Handler();

    private PowerManager.WakeLock mWakeUpLock;

    private boolean mKeyguardDismissed;
    private View.OnSystemUiVisibilityChangeListener mSystemUiListener;

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

        // Update system ui visibility: hide nav bar and optionally the
        // status bar.
        setSystemUiVisibilityFake();
    }

    private void populateFlags(boolean manualControl) {
        int windowFlags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        int timeoutDelay = getConfig().getTimeoutNormal();

        if (manualControl) {
            getWindow().addFlags(windowFlags);

            mTimeoutPaused = false;

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
        registerScreenEventsReceiver();

        int flags = 0;

        // Handle intents
        final Intent intent = getIntent();
        if (intent != null) {
            if (hasWakeUpExtra(intent)) flags |= WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
            mExtraCause = intent.getIntExtra(EXTRA_CAUSE, 0);
        }

        // FIXME: Android dev team broke the DISMISS_KEYGUARD flag.
        // https://code.google.com/p/android-developer-preview/issues/detail?id=1902
        if (Device.hasLollipopApi()
                && !Device.hasMarshmallowApi() // Should be fine now
                && !mKeyguardManager.isKeyguardSecure()) {
            getWindow().addFlags(flags);
            requestDismissSystemKeyguard();
        } else {
            getWindow().addFlags(flags |
                    // Show activity above the system keyguard.
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }

        // Update system ui visibility: hide nav bar and optionally the
        // status bar.
        final View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(
                mSystemUiListener = new View.OnSystemUiVisibilityChangeListener() {
                    public final void onSystemUiVisibilityChange(int f) {
                        setSystemUiVisibilityFake();
                        decorView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                int visibility = SYSTEM_UI_BASIC_FLAGS
                                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                                if (getConfig().isFullScreen()) {
                                    visibility |= View.SYSTEM_UI_FLAG_FULLSCREEN;
                                }
                                decorView.setSystemUiVisibility(visibility);
                            }
                        }, 100);
                    }
                }
        );
    }

    public void setSystemUiVisibilityFake() {
        int visibility = SYSTEM_UI_BASIC_FLAGS | View.SYSTEM_UI_FLAG_IMMERSIVE;
        if (getConfig().isFullScreen()) {
            visibility |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }

        getWindow().getDecorView().setSystemUiVisibility(visibility);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Handle intents
        if (hasWakeUpExtra(intent)) acquireWakeUpLock();
        mExtraCause = intent.getIntExtra(EXTRA_CAUSE, 0);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Handle intents
        if (hasWakeUpExtra(getIntent())) acquireWakeUpFlags();
    }

    /**
     * @see #releaseWakeUpLock()
     */
    private void acquireWakeUpLock() {
        int flags = PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK;
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeUpLock = pm.newWakeLock(flags, "Turn the keyguard on.");
        mWakeUpLock.acquire(500); // 0.5 sec.
    }

    /**
     * Releases previously acquired {@link #mWakeUpLock wake up lock}, does
     * nothing if it's {@code null} or not held.
     *
     * @see #acquireWakeUpLock()
     */
    private void releaseWakeUpLock() {
        if (mWakeUpLock != null && mWakeUpLock.isHeld()) mWakeUpLock.release();
    }

    private void acquireWakeUpFlags() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    /**
     * @return {@code true} if the passed intent is not {@code null} and includes the
     * {@link #EXTRA_TURN_SCREEN_ON} set to {@code true}, {@code false}
     * otherwise.
     */
    private boolean hasWakeUpExtra(@Nullable Intent intent) {
        return intent != null && intent.getBooleanExtra(EXTRA_TURN_SCREEN_ON, false);
    }

    @Override
    public void onDetachedFromWindow() {
        releaseWakeUpLock();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying keyguard activity...");

        unregisterScreenEventsReceiver();

        mTimeout.unregisterListener(this);
        mTimeout.clear();

        super.onDestroy();
    }

    /**
     * Registers a receiver to finish activity when screen goes off and to
     * refresh window flags on screen on. You will need to
     * {@link #unregisterScreenEventsReceiver() unregister} it later.
     *
     * @see #unregisterScreenEventsReceiver()
     */
    private void registerScreenEventsReceiver() {
        mScreenOffReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case Intent.ACTION_SCREEN_ON:
                        if (mResumed) {
                            // Fake system ui visibility state change to
                            // update flags again.
                            mSystemUiListener.onSystemUiVisibilityChange(0);
                        }
                        break;
                    case Intent.ACTION_SCREEN_OFF:
                        if (!KeyguardService.isActive) {
                            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                            pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Finalize the keyguard.").acquire(200);
                            KeyguardActivity.this.finish();
                        }
                        break;
                }
            }

        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1); // max allowed priority
        registerReceiver(mScreenOffReceiver, intentFilter);
    }

    /**
     * Unregisters the screen off receiver if it was registered previously.
     *
     * @see #registerScreenEventsReceiver()
     */
    private void unregisterScreenEventsReceiver() {
        if (mScreenOffReceiver != null) {
            unregisterReceiver(mScreenOffReceiver);
            mScreenOffReceiver = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        sendBroadcast(App.ACTION_STATE_START);
    }

    @Override
    protected void onResume() {
        if (DEBUG) Log.d(TAG, "Resuming keyguard activity...");
        super.onResume();

        mResumed = true;
        mUnlockingTime = 0;
        populateFlags(true);
        overrideHomePress(true);

//        getWindow().addFlags(PREVENT_POWER_KEY);

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

        sendBroadcast(App.ACTION_STATE_RESUME);
    }

    @Override
    protected void onPause() {
        sendBroadcast(App.ACTION_STATE_PAUSE);

//        getWindow().clearFlags(PREVENT_POWER_KEY);

        if (DEBUG) Log.d(TAG, "Pausing keyguard activity...");
        mResumed = false;
        populateFlags(false);
        overrideHomePress(false);

        mHandler.removeCallbacksAndMessages(null);

        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();

        // Workarounds this bug:
        //
        // This annoying bug is driving me crazy! After any AcDisplay notification,
        // no matter how much time I wait, when I press the power button I will
        // directly go to launcher. Only if I turn the screen off and on again
        // I will get the lockscreen (as it should always be).
        //
        // Read more: https://plus.google.com/110348136204265282325/posts/ZYfWURptt2V
        if (mKeyguardDismissed /* only after setting the FLAG_DISMISS_KEYGUARD flag           */
                && !isFinishing() /* otherwise flags can not be set, case it'd turn screen on */
                && !KeyguardService.isActive /* otherwise it's fine to leave device unlocked  */
                && !PowerUtils.isScreenOn(this) /* screen is off and it WILL kill us later    */) {
            if (DEBUG) Log.d(TAG, "Clearing the FLAG_DISMISS_KEYGUARD flag.");
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            mKeyguardDismissed = false;
        }

        sendBroadcast(App.ACTION_STATE_STOP);
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
        sendBroadcast(override
                ? App.ACTION_EAT_HOME_PRESS_START
                : App.ACTION_EAT_HOME_PRESS_STOP);
    }

    /**
     * Same as calling {@code sendBroadcast(new Intent(action))}.
     */
    private void sendBroadcast(@NonNull String action) {
        sendBroadcast(new Intent(action));
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
        if (DEBUG) TracingLog.v(TAG, "TIMEOUT: " + event, 5);
        switch (event) {
            case Timeout.EVENT_CHANGED:
            case Timeout.EVENT_RESUMED:
                if (mTimeoutPaused) {
                    runOnUiThread(new Runnable() {
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
            String errorMessage = "Failed to lock the screen due to a security exception.";
            ToastUtils.showLong(this, errorMessage);
            Log.e(TAG, errorMessage);
            // Clear the FLAG_KEEP_SCREEN_ON flag to prevent the situation when
            // AcDisplay stays forever on. Normally this should never happen.
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        requestDismissSystemKeyguard();

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
                    performUnlock();
                }
            }
        });
    }

    private void performUnlock() {
        mUnlockingTime = SystemClock.elapsedRealtime();
        mKeyguardDismissed = false;

        finish();

        boolean animate = getConfig().isUnlockAnimationEnabled() && !isPowerSaveMode();
        overridePendingTransition(0, animate
                ? R.anim.activity_unlock
                : 0);
    }

    /**
     * Sets the {@link WindowManager.LayoutParams#FLAG_DISMISS_KEYGUARD} flag
     * and marks the {@link #mKeyguardDismissed} as {@code true}.
     */
    private void requestDismissSystemKeyguard() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        mKeyguardDismissed = true;
    }

    /**
     * Returns whether the device is currently locked and requires a PIN,
     * pattern or password to unlock.
     *
     * @return {@code true} if device is locked, {@code false} otherwise.
     */
    public boolean isSecure() {
        return KeyguardUtils.isDeviceLocked(mKeyguardManager);
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

    public int getCause() {
        return mExtraCause;
    }

    @NonNull
    public Timeout getTimeout() {
        return mTimeout;
    }

    @Override
    public void onBackPressed() { /* override back button */ }

}
