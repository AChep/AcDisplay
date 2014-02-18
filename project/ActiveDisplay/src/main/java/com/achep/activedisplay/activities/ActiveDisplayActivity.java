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

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
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
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.ImageView;

import com.achep.activedisplay.Config;
import com.achep.activedisplay.DebugLayerView;
import com.achep.activedisplay.Project;
import com.achep.activedisplay.R;
import com.achep.activedisplay.Timeout;
import com.achep.activedisplay.fragments.activedisplay.ActiveDisplayFragment;
import com.achep.activedisplay.fragments.activedisplay.NotificationFragment;
import com.achep.activedisplay.notifications.NotificationHelper;
import com.achep.activedisplay.notifications.NotificationPresenter;
import com.achep.activedisplay.notifications.OpenStatusBarNotification;
import com.achep.activedisplay.utils.LogUtils;
import com.achep.activedisplay.utils.MathUtils;
import com.achep.activedisplay.utils.ViewUtils;
import com.achep.activedisplay.widgets.WaveView;

/**
 * Created by Artem on 25.01.14.
 */
public class ActiveDisplayActivity extends Activity implements SensorEventListener,
        ActiveDisplayFragment.OnEventListener,
        Timeout.OnTimeoutEventListener {

    private static final String TAG = "ActiveDisplayActivity";

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

    private Config mConfig;
    private Timeout mTimeout;
    private SensorManager mSensorManager;
    private boolean mUnlocking;
    private boolean mLocking;
    private boolean mPaused;

    private volatile boolean mLockFeature;

    // swipe to dismiss notification
    private float[] mTouchHyperbola = new float[2];
    private float[] mTouchDownHyperbola = new float[2];
    private DebugLayerView mDebugLayerView;
    private VelocityTracker mVelocityTracker;

    private float mMaxFlingVelocity;
    private float mMinFlingVelocity;

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
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);

        setContentView(R.layout.activity_active_display);

        FragmentManager fm = getFragmentManager();
        mFragmentNotification = (NotificationFragment)
                fm.findFragmentById(R.id.notification_fragment);

        mDebugLayerView = (DebugLayerView) findViewById(R.id.debug);
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

        ViewConfiguration vc = ViewConfiguration.get(this);
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();

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
        mConfig = Config.getInstance(this);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        registerReceiver(mTurnScreenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        NotificationPresenter np = NotificationPresenter.getInstance(this);
        synchronized (np.monitor) {
            np.addOnNotificationListChangedListener(mNotificationListener);
        }
    }

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

        setLockTimeout(mConfig.getTimeoutNormal(), true);
        handleWindowFocusChanged(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPaused = true;
        clearLockTimeout();

        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mTurnScreenOffReceiver);
        NotificationPresenter np = NotificationPresenter.getInstance(this);
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
        try {
            DevicePolicyManager dpm = (DevicePolicyManager)
                    getSystemService(Context.DEVICE_POLICY_SERVICE);
            dpm.lockNow();

            mLocking = true;
        } catch (SecurityException e) {
            mLocking = false;
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
        final View notification = mFragmentNotificationContainer;
        toSwipeCoordinates(mTouchHyperbola,
                event.getX() - view.getWidth() / 2,
                event.getY() - view.getHeight() / 2);

        float rawX = event.getRawX();
        float rawY = event.getRawY();
        main:
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

                // ///////// -- SWIPE TO DISMISS -- /////////

                if (!mLockFeature) {
                    toSwipeCoordinates(mTouchDownHyperbola, 0, 0);

                    mVelocityTracker = VelocityTracker.obtain();
                    mVelocityTracker.addMovement(event);
                }

                /* FALL DOWN */
            case MotionEvent.ACTION_MOVE:

                // ///////// -- SWIPE TO DISMISS -- /////////

                if (!mLockFeature) {
                    float deltaX = mTouchHyperbola[0] - mTouchDownHyperbola[0];
                    notification.setTranslationX(deltaX);
                    notification.setAlpha(Math.max(0f, Math.min(1f,
                            1f - 2f * Math.abs(deltaX) / notification.getWidth())));

                    mVelocityTracker.addMovement(event);
                }

                // //////////// -- PRESSING -- //////////////

                for (View v : mClickableViews) {
                    if (v.getVisibility() != View.VISIBLE || !v.isClickable()) continue;

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

                // ///////// -- SWIPE TO DISMISS -- /////////

                if (!mLockFeature) {
                    mVelocityTracker.addMovement(event);
                    mVelocityTracker.computeCurrentVelocity(1000);

                    boolean dismiss = false;
                    boolean dismissRight = false;

                    float velocityX = mVelocityTracker.getXVelocity();
                    float absVelocityX = Math.abs(velocityX);
                    float absVelocityY = Math.abs(mVelocityTracker.getYVelocity());
                    float deltaX = mTouchHyperbola[0] - mTouchDownHyperbola[0];
                    if (Math.abs(deltaX) > notification.getWidth() / 3) {
                        dismiss = true;
                        dismissRight = deltaX > 0;
                    } else if (mMinFlingVelocity <= absVelocityX
                            && absVelocityX <= mMaxFlingVelocity
                            && absVelocityY < absVelocityX
                            && absVelocityY < absVelocityX) {
                        // dismiss only if flinging in the same direction as dragging
                        dismiss = (velocityX < 0) == (deltaX < 0);
                        dismissRight = mVelocityTracker.getXVelocity() > 0;
                    }

                    if (dismiss) {
                        notification.animate()
                                .alpha(0f)
                                .translationX(notification.getTranslationX()
                                        + notification.getWidth()
                                        * MathUtils.charge(deltaX))
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        onAnimationCancel(animation);
                                    }

                                    @Override
                                    public void onAnimationCancel(Animator animation) {
                                        NotificationHelper.dismissNotification(mFragmentNotification
                                                .getNotification()
                                                .getStatusBarNotification());

                                        // Reset view presentation
                                        notification.setAlpha(1f);
                                        notification.setTranslationX(0);

                                        showMainFragment();
                                    }
                                }).start();

                        break;
                    }
                }

                // //////////// -- PRESSING -- //////////////

                for (View v : mClickableViews) {
                    if (ViewUtils.isTouchPointInView(v, rawX, rawY)
                            && v.getVisibility() == View.VISIBLE
                            && v.isClickable()) {
                        v.setPressed(false);
                        v.performClick();
                        v.refreshDrawableState();
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        if (mUnlocking || mLocking) break main;
                        break;
                    }
                }

                showMainFragment();
                break;
            default:
                return false;
        }
        return true;
    }

    private void showMainFragment() {
        mWaveView.cancelExpand();

        if (!mLockFeature)
            mFragmentNotificationAnimation.cancel();
        mFragmentPreviewAnimation.cancel();
        toggleVisibilities(true);

        if (!mPaused) {
            setLockTimeout(mConfig.getTimeoutShort());
        }
    }

    private void toggleVisibilities(boolean showMain) {
        View main = mFragmentActiveDisplayContainer;
        ViewUtils.setVisible(main, showMain, View.INVISIBLE);
        main.animate().cancel();
        if (showMain) {
            main.setAlpha(0);
            main.animate().alpha(1).start();
        }

        ViewUtils.setVisible(mFragmentNotificationContainer, !mLockFeature && !showMain, View.INVISIBLE);
        ViewUtils.setVisible(mFragmentPreviewContainer, !mLockFeature && !showMain, View.INVISIBLE);
        ViewUtils.setVisible(mUnlockImageView, !showMain, View.INVISIBLE);
        ViewUtils.setVisible(mLockImageView, mLockFeature && !showMain, View.INVISIBLE);
    }


    private void toSwipeCoordinates(float[] out, float originX, float originY) {
        // x = (y0 - k1 * x0) / (k2 - k1)
        // y = k2 * x

        float chargeX = MathUtils.charge(originX);
        float chargeY = MathUtils.charge(originY);
        originX = Math.abs(originX);
        originY = Math.abs(originY);

        float k1 = 1;
        float k2 = 0.005f;
        float x = Math.max(0, (originY - k1 * originX) / (k2 - k1));
        float y = k2 * x;

        out[0] = x * chargeX;
        out[1] = y * chargeY;
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
                    setLockTimeout(mConfig.getTimeoutInstant());

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
            setLockTimeout(mConfig.getTimeoutShort());
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
                    setLockTimeout(mConfig.getTimeoutNormal(), true);
                }
            });
            if (event == SELECTED) {
                mLockFeature = notification == null;
            }
        }
    }
}
