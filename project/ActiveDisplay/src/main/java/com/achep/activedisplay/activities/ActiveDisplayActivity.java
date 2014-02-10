/*
 * Copyright (C) 2013-2014 AChep@xda <artemchep@gmail.com>
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
package com.achep.activedisplay.activities;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.achep.activedisplay.Project;
import com.achep.activedisplay.R;
import com.achep.activedisplay.Timeout;
import com.achep.activedisplay.fragments.activedisplay.ActiveDisplayFragment;
import com.achep.activedisplay.fragments.activedisplay.NotificationFragment;
import com.achep.activedisplay.notifications.NotificationHelper;
import com.achep.activedisplay.notifications.NotificationPresenter;
import com.achep.activedisplay.notifications.OpenStatusBarNotification;
import com.achep.activedisplay.utils.LogUtils;
import com.achep.activedisplay.utils.ViewUtils;
import com.achep.activedisplay.widgets.WaveView;

/**
 * Created by Artem on 25.01.14.
 */
public class ActiveDisplayActivity extends Activity implements SensorEventListener,
        ActiveDisplayFragment.OnEventListener,
        Timeout.OnTimeoutEventListener {

    private static final String TAG = "ActiveDisplayActivity";

    private static final int SCREEN_OFF_TIMEOUT = 10000; // ms.
    private static final int SCREEN_OFF_TIMEOUT_SHORT = 6000; // ms.
    private static final int SCREEN_OFF_TIMEOUT_SUPER_SHORT = 3500; // ms.

    private WaveView mWaveView;

    private NotificationFragment mFragmentNotification;
    private View mFragmentActiveDisplayContainer;
    private View mFragmentNotificationContainer;
    private View mFragmentPreviewContainer;
    private ImageView mUnlockImageView;
    private ImageView mLockImageView;

    private View[] mClickableViews;

    private AnimatorSet mFragmentNotificationAnimation;
    private AnimatorSet mFragmentPreviewAnimation;

    private final BroadcastReceiver mTurnScreenOffReceiver = new TurnScreenOffReceiver();
    private final NotificationListener mNotificationListener = new NotificationListener();

    private Timeout mTimeout;
    private SensorManager mSensorManager;
    private boolean mUnlocking;
    private boolean mLocking;
    private boolean mPaused;

    private volatile boolean mLockFeature;

    private final Runnable mPauseShieldRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mPaused || mUnlocking || mLocking) return;
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm.isScreenOn()) {
                lock();
            }
        }
    };

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        handleWindowFocusChanged(hasFocus);
    }

    private void handleWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);

        setContentView(R.layout.activity_active_display);

        FragmentManager fm = getFragmentManager();
        mFragmentNotification = (NotificationFragment)
                fm.findFragmentById(R.id.notification_fragment);

        mWaveView = (WaveView) findViewById(R.id.wave);
        mUnlockImageView = (ImageView) findViewById(R.id.unlock);
        mUnlockImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                unlock();
            }
        });
        mLockImageView = (ImageView) findViewById(R.id.lock);
        mLockImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lock();
            }
        });

        mFragmentActiveDisplayContainer = findViewById(R.id.active_display_fragment_container);
        mFragmentPreviewContainer = findViewById(R.id.preview_fragment_container);
        mFragmentNotificationContainer = findViewById(R.id.notification_fragment_container);
        mFragmentNotificationContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // I should not care about syncing with NotificationPresenter because
                // getting current notification from NotificationFragment will return
                // current VISIBLE to user notification.
                // Or is should...
                OpenStatusBarNotification notification = mFragmentNotification.getNotification();
                if (notification != null) {
                    boolean successful = NotificationHelper.startContentIntent(notification);
                    if (successful) {
                        unlock();
                    }
                }
            }
        });

        mTimeout = new Timeout();
        mTimeout.addListener(this);
        ActiveDisplayFragment activeDisplayFragment = (ActiveDisplayFragment)
                fm.findFragmentById(R.id.active_display_fragment);
        activeDisplayFragment.setTimeoutPresenter(mTimeout);
        activeDisplayFragment.setActiveDisplayActionsListener(this);

        mFragmentNotificationAnimation = (AnimatorSet) AnimatorInflater.loadAnimator(
                this, R.anim.card_flip_in_from_top);
        mFragmentNotificationAnimation.setTarget(mFragmentNotificationContainer);
        mFragmentPreviewAnimation = (AnimatorSet) AnimatorInflater.loadAnimator(
                this, R.anim.card_flip_in_from_bottom);
        mFragmentPreviewAnimation.setTarget(mFragmentPreviewContainer);

        mClickableViews = new View[]{mUnlockImageView, mLockImageView, mFragmentNotificationContainer};

        // Register listeners
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        registerReceiver(mTurnScreenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        NotificationPresenter np = NotificationPresenter.getInstance();
        synchronized (np.monitor) {
            np.addOnNotificationListChangedListener(mNotificationListener);
        }
    }

    private Handler mHandler = new Handler();

    @Override
    protected void onResume() {
        super.onResume();
        mPaused = false;
        mLocking = false;
        mUnlocking = false;
        if (mSensorManager != null)
            mSensorManager.registerListener(this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),
                    SensorManager.SENSOR_DELAY_NORMAL);

        mHandler.removeCallbacks(mPauseShieldRunnable);

        setLockTimeout(SCREEN_OFF_TIMEOUT, true);
        handleWindowFocusChanged(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPaused = true;
        clearLockTimeout();

        if (mSensorManager != null)
            mSensorManager.unregisterListener(this);
        if (!mUnlocking && !mLocking) {

            // Cause i can't disable navigation bar's
            // buttons - the only thing i can to do is to turn
            // screen off to prevent random calls and porn loading.
            // Why delayed? Maybe onPause() is because of turning screen off
            // or something like that.
            mHandler.postDelayed(mPauseShieldRunnable, 600);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mTurnScreenOffReceiver);
        NotificationPresenter np = NotificationPresenter.getInstance();
        synchronized (np.monitor) {
            np.removeOnNotificationListChangedListener(mNotificationListener);
        }
    }

    @Override
    public void onTimeoutEvent(int event) {
        switch (event) {
            case Timeout.EVENT_TIMEOUT:
                lock();
                break;
        }
    }

    private void lock() {
        mLocking = true;
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm.isScreenOn()) {
            DevicePolicyManager dpm = (DevicePolicyManager)
                    getSystemService(Context.DEVICE_POLICY_SERVICE);
            dpm.lockNow();
        }
    }

    private void unlock() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        mUnlocking = true;
        clearLockTimeout();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
                overridePendingTransition(0, 0);
            }
        }, 100);
    }

    @Override
    public boolean onTouchHandleEvent(View view, MotionEvent event) {
        float rawX = event.getRawX();
        float rawY = event.getRawY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mLockFeature)
                    mFragmentNotificationAnimation.start();
                mFragmentPreviewAnimation.start();
                toggleVisibilities(false);

                View decorView = getWindow().getDecorView();
                mWaveView.init(
                        ViewUtils.getBottom(mLockFeature ?
                                mLockImageView : mFragmentNotificationContainer, decorView),
                        ViewUtils.getTop(view, decorView) + view.getHeight() / 2,
                        ViewUtils.getTop(mUnlockImageView, decorView),
                        view.getHeight() / 2);
                mWaveView.animateExpand();

                clearLockTimeout();
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                break;
            case MotionEvent.ACTION_MOVE:
                for (View v : mClickableViews) {
                    if (v.getVisibility() != View.VISIBLE) continue;

                    boolean pressed = v.isPressed();
                    v.setPressed(ViewUtils.isTouchPointInView(v, rawX, rawY));
                    v.refreshDrawableState();

                    if (pressed != v.isPressed() && v.isPressed()) {
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                for (View v : mClickableViews) {
                    if (ViewUtils.isTouchPointInView(v, rawX, rawY)
                            && v.getVisibility() == View.VISIBLE) {
                        v.setPressed(false);
                        v.performClick();
                        v.refreshDrawableState();
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        if (mUnlocking || mLocking) return true;
                        break;
                    }
                }

                mWaveView.cancelExpand();

                if (!mLockFeature)
                    mFragmentNotificationAnimation.cancel();
                mFragmentPreviewAnimation.cancel();
                toggleVisibilities(true);

                if (!mPaused) {
                    setLockTimeout(SCREEN_OFF_TIMEOUT_SHORT);
                }
                break;
            default:
                return false;
        }
        return true;
    }

    private void toggleVisibilities(boolean showMain) {
        ViewUtils.setVisible(mFragmentActiveDisplayContainer, showMain, View.INVISIBLE);
        ViewUtils.setVisible(mFragmentNotificationContainer, !mLockFeature && !showMain, View.INVISIBLE);
        ViewUtils.setVisible(mFragmentPreviewContainer, !mLockFeature && !showMain, View.INVISIBLE);
        ViewUtils.setVisible(mUnlockImageView, !showMain, View.INVISIBLE);
        ViewUtils.setVisible(mLockImageView, mLockFeature && !showMain, View.INVISIBLE);
    }

    // //////////////////////////////////////////
    // /////// -- ADDITIONAL SECURITY -- ////////
    // //////////////////////////////////////////

    private void setLockTimeout(int delayMillis) {
        setLockTimeout(delayMillis, false);
    }

    private void setLockTimeout(int delayMillis, boolean resetOld) {
        if (!mUnlocking) {
            mTimeout.setTimeoutDelayed(delayMillis, resetOld);
        }
    }

    private void clearLockTimeout() {
        mTimeout.clear();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_PROXIMITY:

                // This is only needed to determinate
                // proximity level on activity's start.
                // I don't think that reacting on this
                // event after can be useful.
                mSensorManager.unregisterListener(this);
                mSensorManager = null;

                float distance = event.values[0];
                if (distance < 2 /* cm */) {

                    // Well, the device is probably somewhere in bag.
                    setLockTimeout(SCREEN_OFF_TIMEOUT_SUPER_SHORT);

                    if (Project.DEBUG)
                        LogUtils.d(TAG, "Device is in pocket[proximity=" + distance
                                + "cm] --> delayed turning screen off.");
                }
                break;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN)
            setLockTimeout(SCREEN_OFF_TIMEOUT_SHORT);
        return super.onTouchEvent(event);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { /* unused */ }

    @Override
    public void onBackPressed() { /* override back button */ }

    // //////////////////////////////////////////
    // ///////////// -- CLASSES -- //////////////
    // //////////////////////////////////////////

    private class TurnScreenOffReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, Intent intent) {

            // Finish the activity to avoid of displaying on turning screen on.
            clearLockTimeout();
            finish();
        }
    }

    private class NotificationListener extends NotificationPresenter.SimpleOnNotificationListChangedListener {

        @Override
        // running on wrong thread
        public void onNotificationEvent(NotificationPresenter nm,
                                        OpenStatusBarNotification notification,
                                        final int event) {
            super.onNotificationEvent(nm, notification, event);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setLockTimeout(SCREEN_OFF_TIMEOUT, true);
                }
            });
            if (event == SELECTED) {
                mLockFeature = notification == null;
            }
        }
    }
}
