/*
 * Copyright (C) 2013 AChep@xda <artemchep@gmail.com>
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.achep.activedisplay.ActiveDisplayPresenter;
import com.achep.activedisplay.Config;
import com.achep.activedisplay.DebugLayerView;
import com.achep.activedisplay.Project;
import com.achep.activedisplay.R;
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
public class ActiveDisplayActivity extends KeyguardActivity implements
        SensorEventListener,
        ActiveDisplayFragment.OnEventListener {

    private static final String TAG = "ActiveDisplayActivity";

    public static final String EXTRA_TURN_SCREEN_ON = "turn_screen_on";

    private WaveView mWaveView;

    private NotificationFragment mFragmentNotification;
    private View mFragmentActiveDisplayContainer;
    private View mFragmentNotificationContainer;
    private View mFragmentPreviewContainer;
    private ImageView mUnlockImageView;
    private ImageView mLockImageView;
    private View mContent;

    private View[] mClickableViews;

    private AnimatorSet mFragmentNotificationAnimation;
    private AnimatorSet mFragmentPreviewAnimation;

    private final BroadcastReceiver mTurnScreenOffReceiver = new TurnScreenOffReceiver();
    private final NotificationListener mNotificationListener = new NotificationListener();

    private Config mConfig;
    private NotificationPresenter mPresenter;

    private SensorManager mSensorManager;

    private volatile boolean mLockFeature;

    // swipe to dismiss notification
    private float[] mTouchHyperbola = new float[2];
    private float[] mTouchDownHyperbola = new float[2];
    private float[] mTouchDownOffset = new float[2];
    private DebugLayerView mDebugLayerView;
    private VelocityTracker mVelocityTracker;

    // fade in/out effect
    private View mBackgroundView;
    private View mForegroundView;

    private float mMaxFlingVelocity;
    private float mMinFlingVelocity;
    private GestureDetector mGestureDetector;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        handleWindowFocusChanged(hasFocus);
    }

    private void handleWindowFocusChanged(boolean hasFocus) {
        int windowFlags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        if (hasFocus) {
            int visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            getWindow().getDecorView().setSystemUiVisibility(visibility);
            getWindow().addFlags(windowFlags);

            getTimeout().release();
            getTimeout().setTimeoutDelayed(mConfig.getTimeoutShort());
        } else {
            getWindow().clearFlags(windowFlags);
            getTimeout().lock();
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        int turnScreenOnFlag = 0;
        if (intent != null) {
            turnScreenOnFlag = WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON *
                    MathUtils.bool(intent.getBooleanExtra(EXTRA_TURN_SCREEN_ON, false));
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES
                | turnScreenOnFlag);

        mConfig = Config.getInstance(this);
        mPresenter = NotificationPresenter.getInstance(this);

        if (mConfig.isWallpaperShown()) {
            setTheme(R.style.ActiveDisplayTheme_Wallpaper);
        }

        setContentView(R.layout.activity_active_display);

        mContent = findViewById(R.id.content);
        mBackgroundView = findViewById(R.id.background);
        mForegroundView = findViewById(R.id.foreground);
        mDebugLayerView = (DebugLayerView) findViewById(R.id.debug);
        mWaveView = (WaveView) findViewById(R.id.wave);
        mUnlockImageView = (ImageView) mContent.findViewById(R.id.unlock);
        mUnlockImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                unlock(null);
            }
        });
        mLockImageView = (ImageView) mContent.findViewById(R.id.lock);
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
                // Or I should...
                final OpenStatusBarNotification notification = mPresenter.getSelectedNotification();
                if (notification != null) {
                    unlock(new Runnable() {
                        @Override
                        public void run() {
                            NotificationHelper.startContentIntent(notification);
                        }
                    });
                }
            }
        });

        ViewConfiguration vc = ViewConfiguration.get(this);
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mGestureDetector = new GestureDetector(this, new GestureListener());


        FragmentManager fm = getFragmentManager();
        mFragmentNotification = (NotificationFragment)
                fm.findFragmentById(R.id.notification_fragment);
        ActiveDisplayFragment activeDisplayFragment = (ActiveDisplayFragment)
                fm.findFragmentById(R.id.active_display_fragment);
        activeDisplayFragment.setTimeoutPresenter(getTimeout());
        activeDisplayFragment.setActiveDisplayActionsListener(this);

        mFragmentNotificationAnimation = (AnimatorSet) AnimatorInflater.loadAnimator(
                this, R.anim.card_flip_in_from_top);
        mFragmentNotificationAnimation.setTarget(mFragmentNotificationContainer);
        mFragmentPreviewAnimation = (AnimatorSet) AnimatorInflater.loadAnimator(
                this, R.anim.card_flip_in_from_bottom);
        mFragmentPreviewAnimation.setTarget(mFragmentPreviewContainer);

        mClickableViews = new View[]{
                mUnlockImageView,
                mLockImageView,
                mFragmentNotificationContainer};

        // Register listeners
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mTurnScreenOffReceiver, filter);

        mPresenter.addOnNotificationListChangedListener(mNotificationListener);
        handleSelectedNotificationChanged(mPresenter.getSelectedNotification());
        handleNotificationCountChanged(mPresenter.getCount());

        ActiveDisplayPresenter.getInstance().attachActivity(this);
    }

    private void handleSelectedNotificationChanged(OpenStatusBarNotification notification) {
        boolean lockFeature = notification == null;
        if (mLockFeature == lockFeature) return;
        mLockFeature = lockFeature;
    }

    private void handleNotificationCountChanged(int count) {
        ViewUtils.setVisible(mBackgroundView, count <= 1 && mConfig.isWallpaperShown());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mForegroundView.animate().alpha(0f).setDuration(800);
        if (mSensorManager != null) mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),
                SensorManager.SENSOR_DELAY_NORMAL);

        getTimeout().setTimeoutDelayed(mConfig.getTimeoutNormal(), true);
        handleWindowFocusChanged(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSensorManager != null) mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mTurnScreenOffReceiver);
        NotificationPresenter np = NotificationPresenter.getInstance(this);
        np.removeOnNotificationListChangedListener(mNotificationListener);

        ActiveDisplayPresenter.getInstance().detachActivity();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean onTouchHandleEvent(View view, MotionEvent event) {
        final View notification = mFragmentNotificationContainer;

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
                                mLockImageView : notification, decorView),
                        ViewUtils.getTop(view, decorView) + view.getHeight() / 2,
                        ViewUtils.getTop(mUnlockImageView, decorView),
                        view.getHeight() / 2
                );
                mWaveView.animateExpand();

                getTimeout().lock();
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

                // ///////// -- SWIPE TO DISMISS -- /////////

                if (!mLockFeature) {
                    toSwipeCoordinates(mTouchDownHyperbola, 0, 0);

                    mTouchDownOffset[0] = event.getX();
                    mTouchDownOffset[1] = event.getY();

                    mVelocityTracker = VelocityTracker.obtain();
                    mVelocityTracker.addMovement(event);
                }

                /* FALL DOWN */
            case MotionEvent.ACTION_MOVE:

                // ///////// -- SWIPE TO DISMISS -- /////////

                if (!mLockFeature) {
                    toSwipeCoordinates(mTouchHyperbola,
                            event.getX() - mTouchDownOffset[0],
                            event.getY() - mTouchDownOffset[1]);

                    float deltaX = mTouchHyperbola[0] - mTouchDownHyperbola[0];
                    float alpha = Math.max(0f, Math.min(1f,
                            1f - 2f * Math.abs(deltaX) / notification.getWidth()));
                    notification.setTranslationX(deltaX);
                    mBackgroundView.setAlpha(1 - alpha);
                    mContent.setAlpha(alpha);

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
                getTimeout().release();

                // ///////// -- SWIPE TO DISMISS -- /////////

                if (!mLockFeature) {
                    toSwipeCoordinates(mTouchHyperbola,
                            event.getX() - mTouchDownOffset[0],
                            event.getY() - mTouchDownOffset[1]);

                    mVelocityTracker.addMovement(event);
                    mVelocityTracker.computeCurrentVelocity(1000);

                    boolean dismiss = false;
                    boolean dismissRight = false;

                    float velocityX = mVelocityTracker.getXVelocity();
                    float absVelocityX = Math.abs(velocityX);
                    float absVelocityY = Math.abs(mVelocityTracker.getYVelocity());
                    final float deltaX = mTouchHyperbola[0] - mTouchDownHyperbola[0];
                    float absDeltaX = Math.abs(deltaX);
                    if (absDeltaX > notification.getWidth() / 3) {
                        dismiss = true;
                        dismissRight = deltaX > 0;
                    } else if (mMinFlingVelocity <= absVelocityX
                            && absVelocityX <= mMaxFlingVelocity
                            && absVelocityY * 2 < absVelocityX
                            && absDeltaX > notification.getWidth() / 5) {
                        // dismiss only if flinging in the same direction as dragging
                        dismiss = (velocityX < 0) == (deltaX < 0);
                        dismissRight = mVelocityTracker.getXVelocity() > 0;
                    }

                    if (dismiss) {
                        int duration = Math.round(absDeltaX * 1000f / Math.max(absVelocityX, 500f));
                        final StatusBarNotification statusBarNotification = mPresenter
                                .getSelectedNotification()
                                .getStatusBarNotification();

                        mBackgroundView.animate().alpha(1f).setDuration(duration);
                        mContent.animate().alpha(0f).setDuration(duration);
                        notification.animate().setDuration(duration)
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
                                        final int count = mPresenter.getCount();

                                        NotificationHelper.dismissNotification(statusBarNotification);
                                        if (count <= 1) {
                                            lock();

                                            // Do not update user interface to default
                                            // screen cause it'll make some visual lags.
                                            if (isLocking()) return;
                                        }

                                        // Reset view presentation
                                        notification.setAlpha(1f);
                                        notification.setTranslationX(0);

                                        showMainFragment();
                                    }
                                });

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
                        if (isLocking() || isUnlocking()) break main;
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
        mFragmentNotificationAnimation.cancel();
        mFragmentPreviewAnimation.cancel();
        toggleVisibilities(true);

        getTimeout().setTimeoutDelayed(mConfig.getTimeoutShort());
    }

    private void toggleVisibilities(boolean showMain) {
        View main = mFragmentActiveDisplayContainer;
        if (showMain && main.getVisibility() != View.VISIBLE) {
            main.setAlpha(0);
            main.animate().alpha(1).setDuration(180);
        }

        mContent.setAlpha(1f);
        mBackgroundView.setAlpha(0f);

        ViewUtils.setVisible(main, showMain, View.INVISIBLE);
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
//                    getTimeout().setTimeoutDelayed(mConfig.getTimeoutInstant());

                    if (Project.DEBUG)
                        LogUtils.d(TAG, "Device is in pocket[proximity=" + distance
                                + "cm] --> delayed turning screen off.");
                }
                break;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getTimeout().setTimeoutDelayed(mConfig.getTimeoutShort());
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { /* unused */ }


    // //////////////////////////////////////////
    // ///////////// -- CLASSES -- //////////////
    // //////////////////////////////////////////

    private class TurnScreenOffReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, Intent intent) {
            // Finish the activity to avoid of displaying on turning screen on.
            finish();
        }
    }

    private class NotificationListener extends
            NotificationPresenter.SimpleOnNotificationListChangedListener {

        @Override
        // running on wrong thread & already synced
        public void onNotificationEvent(final NotificationPresenter nm,
                                        final OpenStatusBarNotification notification,
                                        final int event) {
            super.onNotificationEvent(nm, notification, event);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (event) {
                        case SELECTED:
                            handleSelectedNotificationChanged(notification);
                            break;
                        case POSTED:
                        case REMOVED:
                            handleNotificationCountChanged(nm.getCount());
                            break;
                    }

                    getTimeout().setTimeoutDelayed(mConfig.getTimeoutNormal(), true);

                }
            });
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            lock();
            return true;
        }
    }
}
