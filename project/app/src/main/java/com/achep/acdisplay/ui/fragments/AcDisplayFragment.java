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
package com.achep.acdisplay.ui.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.Log;
import android.util.Property;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.acdisplay.Timeout;
import com.achep.acdisplay.compat.SceneCompat;
import com.achep.acdisplay.notifications.NotificationPresenter;
import com.achep.acdisplay.notifications.NotificationUtils;
import com.achep.acdisplay.notifications.OpenNotification;
import com.achep.acdisplay.services.media.MediaController2;
import com.achep.acdisplay.services.media.MediaControlsHelper;
import com.achep.acdisplay.ui.CornerHelper;
import com.achep.acdisplay.ui.DynamicBackground;
import com.achep.acdisplay.ui.activities.AcDisplayActivity;
import com.achep.acdisplay.ui.components.ClockWidget;
import com.achep.acdisplay.ui.components.HostWidget;
import com.achep.acdisplay.ui.components.MediaWidget;
import com.achep.acdisplay.ui.components.NotifyWidget;
import com.achep.acdisplay.ui.components.Widget;
import com.achep.acdisplay.ui.view.ForwardingLayout;
import com.achep.acdisplay.ui.view.ForwardingListener;
import com.achep.acdisplay.ui.widgets.CircleView;
import com.achep.base.Device;
import com.achep.base.async.WeakHandler;
import com.achep.base.content.ConfigBase;
import com.achep.base.tests.Check;
import com.achep.base.ui.activities.ActivityBase;
import com.achep.base.ui.fragments.leakcanary.LeakWatchFragment;
import com.achep.base.ui.widgets.TextView;
import com.achep.base.utils.FloatProperty;
import com.achep.base.utils.MathUtils;
import com.achep.base.utils.ViewUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import static com.achep.base.Build.DEBUG;

/**
 * This is main fragment of ActiveDisplay app.
 */
// TODO: Put main scene inside of widget.
public class AcDisplayFragment extends LeakWatchFragment implements
        NotificationPresenter.OnNotificationListChangedListener,
        ForwardingLayout.OnForwardedEventListener,
        View.OnTouchListener,
        Widget.Callback,
        ConfigBase.OnConfigChangedListener,
        CircleView.Callback {

    private static final String TAG = "AcDisplayFragment";

    private static final int MSG_SHOW_HOME_WIDGET = 0;
    private static final int MSG_HIDE_MEDIA_WIDGET = 1;

    private static final Property<AcDisplayFragment, Float> TRANSFORM =
            new FloatProperty<AcDisplayFragment>("populateStdAnimation") {

                private float mValue;

                @Override
                public void setValue(AcDisplayFragment fragment, float value) {
                    fragment.populateStdAnimation(mValue = value);
                }

                @Override
                public Float get(AcDisplayFragment fragment) {
                    return mValue;
                }

            };

    // Views
    private CircleView mCircleView;
    private TextView mStatusClockTextView;
    private ProgressBar mProgressBar;
    private ViewGroup mDividerView;
    private ForwardingLayout mSceneContainer;
    private ForwardingLayout mIconsForwarder;
    private GridLayout mIconsContainer;

    // Main
    private ActivityBase mActivity;
    private AcDisplayActivity mActivityAcd;

    private final HashMap<View, Widget> mWidgetsMap = new HashMap<>();
    private final HashMap<String, SceneCompat> mScenesMap = new HashMap<>();
    private SceneCompat mCurrentScene;
    private Widget mSelectedWidget;
    private View mPressedIconView;
    private boolean mHasPinnedWidget;

    private ForwardingListener mSceneForwardingListener;
    private ForwardingListener mIconsForwardingListener;
    private final Handler mTouchHandler = new Handler();
    private boolean mTouchSticky;

    private int mConfigWidgetPinDuration;
    private int mConfigWidgetSelectDelay;

    // Quick glance
    private int mNotificationHashGlanced;
    private long mNotificationHashTime;

    // Animations and transitions
    private TransitionSet mTransitionJit;
    private Transition mTransitionSwitchScene;
    private ObjectAnimator mStdAnimator;

    // Clock widget
    private SceneCompat mSceneMainClock;
    private Widget mClockWidget;

    // Media widget
    private SceneCompat mSceneMainMedia;
    private MediaControlsHelper mMediaControlsHelper;
    private MediaWidget mMediaWidget;
    private boolean mMediaWidgetActive;

    // Timeout
    private Timeout.Gui mTimeoutGui;
    private Timeout mTimeout;
    private int mTimeoutNormal;
    private int mTimeoutShort;

    // Swipe to dismiss
    private VelocityTracker mVelocityTracker;
    private int mMaxFlingVelocity;
    private int mMinFlingVelocity;

    // Dynamic background
    private DynamicBackground mBackground;

    /**
     * Handler to control delayed events.
     *
     * @see #MSG_HIDE_MEDIA_WIDGET
     * @see #MSG_SHOW_HOME_WIDGET
     */
    private final Handler mHandler = new H(this);

    private boolean mPendingIconsSizeChange;
    private boolean mPendingNotifyChange;

    private boolean mResuming;

    private boolean isPinnable() {
        return getConfig().isWidgetPinnable();
    }

    private boolean isReadable() {
        return getConfig().isWidgetReadable();
    }

    /**
     * Unlocks the keyguard and runs {@link Runnable runnable} when unlocked.
     *
     * @param finish {@code true} to finish activity, {@code false} to keep it
     * @see com.achep.acdisplay.ui.activities.KeyguardActivity
     */
    public void unlock(Runnable runnable, boolean finish) {
        if (!isNotDemo()) {
            if (runnable != null) runnable.run();
            return;
        }

        mActivityAcd.unlock(runnable, finish);
    }

    public Config getConfig() {
        return Config.getInstance();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (ActivityBase) activity;
        mActivityAcd = isNotDemo() ? (AcDisplayActivity) activity : null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Resources res = getResources();
        mConfigWidgetPinDuration = res.getInteger(R.integer.config_maxPinTime);
        mConfigWidgetSelectDelay = res.getInteger(R.integer.config_iconSelectDelayMillis);
        ViewConfiguration vc = ViewConfiguration.get(getActivity());
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();

        // Clock widget
        mClockWidget = getConfig().isCustomWidgetEnabled()
                ? new HostWidget(this, this)
                : new ClockWidget(this, this);

        // Media widget
        MediaController2 mc = MediaController2.newInstance(getActivity()).asyncWrap();
        mMediaControlsHelper = new MediaControlsHelper(mc);
        mMediaControlsHelper.registerListener(new MediaControlsHelper.Callback() {
            @Override
            public void onStateChanged(boolean showing) {
                if (showing) {
                    makeMediaWidgetActive();
                } else makeMediaWidgetInactive();
            }
        });
        mMediaWidget = new MediaWidget(this, this);

        // Transitions
        if (Device.hasKitKatApi()) {
            mTransitionJit = new TransitionSet()
                    .setOrdering(TransitionSet.ORDERING_TOGETHER)
                    .addTransition(new Fade())
                    .addTransition(new ChangeBounds());
            mTransitionSwitchScene = new TransitionSet()
                    .setOrdering(TransitionSet.ORDERING_TOGETHER)
                    .addTransition(new Fade(Fade.OUT).setDuration(200))
                    .addTransition(new Fade(Fade.IN).setStartDelay(80))
                    .addTransition(new ChangeBounds().setStartDelay(80));
        }

        // Timeout
        mTimeout = isNotDemo()
                ? mActivityAcd.getTimeout()
                : new Timeout();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (DEBUG) Log.d(TAG, "Creating view...");

        View root = inflater.inflate(isNotDemo()
                ? R.layout.acdisplay_fragment_full
                : R.layout.acdisplay_fragment, container, false);
        assert root != null;

        // Initialize secondary views
        mStatusClockTextView = (TextView) root.findViewById(R.id.clock_small);
        mCircleView = (CircleView) root.findViewById(R.id.circle);
        mBackground = DynamicBackground.newInstance(this,
                (ImageView) root.findViewById(R.id.background));

        // Initialize main views
        View c = root.findViewById(R.id.container);
        mDividerView = (ViewGroup) c.findViewById(R.id.divider);
        mProgressBar = (ProgressBar) mDividerView.findViewById(R.id.progress);
        mSceneContainer = (ForwardingLayout) c.findViewById(R.id.scene);
        mIconsForwarder = (ForwardingLayout) c.findViewById(R.id.forwarding);
        mIconsContainer = (GridLayout) c.findViewById(R.id.grid);

        // Initialize home widgets.
        mSceneMainClock = new SceneCompat(mSceneContainer, mClockWidget
                .createView(inflater, mSceneContainer, null));
        mSceneMainMedia = new SceneCompat(mSceneContainer, mMediaWidget
                .createView(inflater, mSceneContainer, null));

        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (DEBUG) Log.d(TAG, "Creating view (created)...");

        mSceneForwardingListener = new ForwardingListener(mIconsForwarder, false, mSceneContainer);
        mIconsForwardingListener = new ForwardingListener(mIconsForwarder, true, mIconsForwarder);
        mIconsForwarder.setOnForwardedEventListener(this);
        mIconsForwarder.setAllViewsForwardable(true, 1 /* the touch depth */);
        mIconsForwarder.setOnTouchListener(this);

        if (isNotDemo()) {
            // Init the timeout
            mTimeoutGui = new Timeout.Gui(mProgressBar);
            mTimeout.registerListener(mTimeoutGui);

            // Init the touch forwarding.
            View.OnTouchListener listener = new TouchForwarder(getActivity(), mCircleView, mActivityAcd);
            view.setOnTouchListener(listener);
            mCircleView.setCallback(this);
            mCircleView.setSupervisor(new CircleView.Supervisor() {
                @Override
                public boolean isAnimationEnabled() {
                    return isAnimatable();
                }

                @Override
                public boolean isAnimationUnlockEnabled() {
                    return isAnimationEnabled() && getConfig().isUnlockAnimationEnabled();
                }

            });
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        showWidget(mClockWidget, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        NotificationPresenter.getInstance().registerListener(this);
        getConfig().registerListener(this);
        mPendingNotifyChange = true;
        mPendingIconsSizeChange = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        mResuming = true;

        // Start all available widgets.
        for (Widget widget : mWidgetsMap.values()) widget.start();
        mClockWidget.start();
        mMediaWidget.start();

        // Update notifications list & config.
        if (mPendingNotifyChange) rebuildNotifications();
        if (mPendingIconsSizeChange) updateIconsSize();
        updateTimeouts();
        mPendingNotifyChange = false;
        mPendingIconsSizeChange = false;

        // Media controller.
        mMediaControlsHelper.start();

        // Show the notification that is the cause of AcDisplay being shown. This
        // allows user to see that damn notification in no time.
        if (isNotDemo() && getConfig().isNotifyGlanceEnabled()) {
            long now = SystemClock.elapsedRealtime();
            int hash = mActivityAcd.getCause();
            if (hash != 0 && (hash != mNotificationHashGlanced || now - mNotificationHashTime < 1000)) {
                // Find the appropriate notification widget.
                for (Widget widget : mWidgetsMap.values()) {
                    if (widget instanceof NotifyWidget) {
                        NotifyWidget nw = (NotifyWidget) widget;
                        OpenNotification n = nw.getNotification();
                        if (n != null && n.hashCode() == hash) {
                            mNotificationHashGlanced = hash;
                            if (!n.isContentSecret(getActivity())) {
                                // Show the appropriate widget.
                                if (DEBUG) Log.d(TAG, "Doing the quick glance on " + nw);
                                showWidget(widget);
                                onWidgetPin(widget);
                            } // Otherwise there's nothing helpful to show.
                            break;
                        }
                    }
                }
            }
            // Avoid of an issue when the #onResume() is being called
            // twice.
            mNotificationHashTime = now;
            // Logs
            if (mNotificationHashGlanced != hash) {
                mNotificationHashGlanced = hash;
                Log.w(TAG, "The glance notification was not shown!");
            }
        }

        mResuming = false;
    }

    @Override
    public void onPause() {
        // Back to the home widget.
        showWidget(mClockWidget, false);

        // Clear all ongoing events such as handling media widget,
        // handing pinned widget, handing the touch delay, etc...
        mMediaWidgetActive = false;
        mHandler.removeCallbacksAndMessages(null);
        mTouchHandler.removeCallbacksAndMessages(null);

        // Stop all widgets.
        for (Widget widget : mWidgetsMap.values()) widget.stop();
        mClockWidget.stop();
        mMediaWidget.stop();

        // Media controller.
        mMediaControlsHelper.stop();
        super.onPause();
    }

    @Override
    public void onStop() {

        // Unregister everything.
        NotificationPresenter.getInstance().unregisterListener(this);
        getConfig().unregisterListener(this);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (DEBUG) Log.d(TAG, "Destroying view...");
        if (isNotDemo()) {
            mTimeout.unregisterListener(mTimeoutGui);
        }

        super.onDestroyView();
    }

    //-- CONFIG ---------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConfigChanged(@NonNull ConfigBase config,
                                @NonNull String key,
                                @NonNull Object value) {
        switch (key) {
            case Config.KEY_UI_ICON_SIZE:
                updateIconsSize();
                break;
            case Config.KEY_TIMEOUT_NORMAL:
                mTimeoutNormal = (int) value;
                break;
            case Config.KEY_TIMEOUT_SHORT:
                mTimeoutShort = (int) value;
                break;
        }
    }

    private void updateTimeouts() {
        mTimeoutNormal = getConfig().getTimeoutNormal();
        mTimeoutShort = getConfig().getTimeoutShort();
    }

    /**
     * Updates the size of all widget's icons as
     * {@link com.achep.acdisplay.Config#getIconSizePx() set} in config.
     */
    private void updateIconsSize() {
        if (!isResumed()) {
            mPendingIconsSizeChange = true;
            return;
        }

        final int sizePx = getConfig().getIconSizePx();
        final int childCount = mIconsContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mIconsContainer.getChildAt(i);
            ViewUtils.setSize(child, sizePx);
        }
    }

    //-- TIMEOUT --------------------------------------------------------------

    @Override
    public void requestTimeoutRestart(@NonNull Widget widget) {
        Check.getInstance().isTrue(isCurrentWidget(widget));
        mTimeout.setTimeoutDelayed(mTimeoutNormal, true);
    }

    //-- TOUCH HANDLING -------------------------------------------------------

    @Override
    public void onCircleEvent(float radius, float ratio, int event, final int actionId) {
        switch (event) {
            case CircleView.ACTION_START:
                if (mHasPinnedWidget) {
                    showHomeWidget();
                }

                mTimeout.setTimeoutDelayed(mTimeoutShort);
                mTimeout.pause();
                break;
            case CircleView.ACTION_UNLOCK_START:
                mActivityAcd.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                break;
            case CircleView.ACTION_UNLOCK_CANCEL:
                mActivityAcd.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                break;
            case CircleView.ACTION_UNLOCK:
                mActivityAcd.unlock(new Runnable() {
                    @Override
                    public void run() {
                        Context context = getActivity();
                        assert context != null;
                        CornerHelper.perform(context, actionId);
                    }
                });
            case CircleView.ACTION_CANCELED:
                // Clear the pinned widget on short tap in emulator
                // (and probably something in real life too).
                if (mHasPinnedWidget) showHomeWidget();

                mTimeout.resume();

                int delta = (int) (2200 - mTimeout.getRemainingTime());
                if (delta > 0) {
                    mTimeout.delay(delta);
                }
                break;
        }
    }

    @Override
    public void onPressedView(MotionEvent event, int activePointerId, View view) {
        mTouchHandler.removeCallbacksAndMessages(null);
        mPressedIconView = view;

        if (view == null) {
            return;
        }

        final Widget widget = findWidgetByIcon(view);
        if (isCurrentWidget(widget)) {
            // We need to reset this, cause current widget may be
            // pinned.
            mHandler.removeMessages(MSG_SHOW_HOME_WIDGET);
            return;
        } else if (widget == null && mSelectedWidget.isHomeWidget()) {
            return;
        }

        int action = event.getActionMasked();
        int delay = action != MotionEvent.ACTION_DOWN ? mConfigWidgetSelectDelay : 0;
        mTouchHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (widget == null) {
                    showHomeWidget();
                } else {
                    showWidget(widget);
                }
            }
        }, delay);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v == mIconsForwarder) {
            mSceneForwardingListener.onTouch(v, event);
            mIconsForwardingListener.onTouch(v, event);
            return true;
        }
        return false;
    }

    @Override
    public void onForwardedEvent(MotionEvent event, int activePointerId) {
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // Track the velocity of movement, so we
                // can do swipe-to-dismiss.
                mVelocityTracker = VelocityTracker.obtain();
                mTouchSticky = false;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                populateStdMotion(event);

                if (action != MotionEvent.ACTION_UP) {
                    return; // Don't fall down.
                }

                boolean dismissing = swipeToDismiss();
                if (!dismissing) {
                    if (mTouchSticky) {
                        // Disable the default timeout mechanism and let
                        // the selected widget to stay for a while.
                        onWidgetStick(mSelectedWidget);
                    } else if (mPressedIconView == null || !isPinnable()) {
                        showHomeWidget();
                    } else {
                        onWidgetPin(mSelectedWidget);
                    }
                }
            case MotionEvent.ACTION_CANCEL:
                mTouchHandler.removeCallbacksAndMessages(null);
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                mTouchSticky = false;

                if (action == MotionEvent.ACTION_CANCEL) {
                    showHomeWidget();
                }
                break;
        }
    }

    @Override
    public void requestWidgetStick(@NonNull Widget widget) {
        Check.getInstance().isTrue(isCurrentWidget(widget));
        mTouchSticky = true;
    }

    //-- SWIPE-TO-DISMISS -----------------------------------------------------

    private boolean swipeToDismiss() {
        if (!isDismissible(mSelectedWidget)) return false;
        mVelocityTracker.computeCurrentVelocity(1000);

        float velocityX = mVelocityTracker.getXVelocity();
        float velocityY = mVelocityTracker.getYVelocity();
        float absVelocityX = Math.abs(velocityX);
        float absVelocityY = Math.abs(velocityY);

        float deltaY = mSceneContainer.getTranslationY();
        float absDeltaY = Math.abs(deltaY);

        int height = getSceneView().getHeight();
        if (height == 0) {
            // Scene view is not measured yet.
            return false;
        } else if (absDeltaY < height / 2) {
            boolean dismiss = false;
            if (mMinFlingVelocity <= absVelocityY
                    && absVelocityY <= mMaxFlingVelocity
                    && absVelocityY > absVelocityX * 2
                    && absDeltaY > height / 5) {
                // Dismiss only if flinging in the same direction as dragging
                dismiss = (velocityY < 0) == (deltaY < 0);
            }

            if (!dismiss) {
                return false;
            }
        }

        // /////////////////////
        // ~~    DISMISS      ~~
        // /////////////////////

        if (height > absDeltaY && isAnimatable()) {
            int duration;
            duration = Math.round(1000f /* ms. */ * (height - absDeltaY) / absVelocityX);
            duration = Math.min(duration, 300);

            final Widget widget = mSelectedWidget;
            float progress = MathUtils.range(deltaY / height, 0f, 1f);
            if (mStdAnimator != null) mStdAnimator.cancel();
            mStdAnimator = ObjectAnimator.ofFloat(this, TRANSFORM, progress, 1f);
            mStdAnimator.setDuration(duration);
            mStdAnimator.addListener(new AnimatorListenerAdapter() {

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    onWidgetDismiss(widget);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    super.onAnimationCancel(animation);
                    onWidgetDismiss(widget);
                }

            });
            mStdAnimator.start();
        } else {
            onWidgetDismiss(mSelectedWidget);
        }

        return true;
    }

    private void populateStdMotion(@NonNull MotionEvent srcEvent) {
        // Track current movement to be able to handle
        // flings correctly.
        MotionEvent dstEvent = MotionEvent.obtainNoHistory(srcEvent);
        mVelocityTracker.addMovement(MotionEvent.obtainNoHistory(srcEvent));
        dstEvent.recycle();

        // No need to handle swipe-to-dismiss if the
        // widget is not dismissible.
        if (!isDismissible(mSelectedWidget)) {
            return;
        }

        final float y = srcEvent.getY() - mIconsContainer.getHeight();

        if (y <= 0) {
            if (mSceneContainer.getTranslationY() != 0) {
                resetSceneContainerParams();
            }
            return;
        }

        // Populate current animation
        float height = getSceneView().getHeight();
        float progress = MathUtils.range(y / height, 0f, 1f);
        populateStdAnimation(progress);
    }

    private void populateStdAnimation(float progress) {
        float height = getSceneView().getHeight();
        float y = height * progress;
        double degrees = Math.toDegrees(Math.acos((height - y) / height));

        mSceneContainer.setAlpha(1f - progress);
        mSceneContainer.setTranslationY(y);
        mSceneContainer.setRotationX((float) (-degrees / 2f));
    }

    //-- MANAGING WIDGETS -----------------------------------------------------

    /**
     * Resets {@link #mSceneContainer scene container}'s params, such
     * as: animation, alpha level, translation, rotation etc.
     */
    private void resetSceneContainerParams() {
        if (mStdAnimator != null) mStdAnimator.cancel();
        mSceneContainer.setAlpha(1f);
        mSceneContainer.setTranslationY(0);
        mSceneContainer.setRotationX(0);
    }

    /**
     * @return {@code true} if current widget equals to given one, {@code false} otherwise.
     */
    protected final boolean isCurrentWidget(Widget widget) {
        return widget == mSelectedWidget;
    }

    /**
     * @return {@code true} if widget is not {@code null} and
     * {@link Widget#isDismissible() dismissible}, {@code false} otherwise.
     */
    public final boolean isDismissible(@Nullable Widget widget) {
        return widget != null && widget.isDismissible();
    }

    /**
     * @return The view of the {@link #mCurrentScene current scene}.
     */
    @NonNull
    private View getSceneView() {
        return mCurrentScene.getView();
    }

    @Nullable
    private SceneCompat findSceneByWidget(@NonNull Widget widget) {
        if (widget == mMediaWidget) {
            return mSceneMainMedia;
        } else if (widget == mClockWidget) {
            return mSceneMainClock;
        } else if (widget.getView() != null) {
            String className = widget.getClass().getName();
            return mScenesMap.get(className);
        }
        return null;
    }

    private Widget findWidgetByIcon(@NonNull View view) {
        return mWidgetsMap.get(view);
    }

    //-- DISPLAYING WIDGETS ---------------------------------------------------

    public void showHomeWidget() {
        showHomeWidget(true);
    }

    public void showHomeWidget(boolean animate) {
        Widget widget = isMediaWidgetHome()
                ? mMediaWidget
                : mClockWidget;
        showWidget(widget, animate);
    }


    /**
     * @see #showWidget(com.achep.acdisplay.ui.components.Widget, boolean)
     */
    protected void showWidget(@NonNull Widget widget) {
        showWidget(widget, true);
    }

    /**
     * @see #showWidget(com.achep.acdisplay.ui.components.Widget)
     */
    protected void showWidget(@NonNull Widget widget, boolean animate) {
        mHandler.removeMessages(MSG_SHOW_HOME_WIDGET);
        mHasPinnedWidget = false;

        Log.d(TAG, "showing widget " + widget);

        View iconView;

        if (mSelectedWidget != null) {
            iconView = mSelectedWidget.getIconView();
            if (iconView != null) {
                iconView.setSelected(false);
            }

            mSelectedWidget.onViewDetached();
        }

        mSelectedWidget = widget;
        resetSceneContainerParams();
        animate &= isAnimatableAuto();

        SceneCompat scene = findSceneByWidget(mSelectedWidget);
        if (scene == null) scene = mSceneMainClock;
        if (mCurrentScene != scene) {
            goScene(scene, animate);
        } else if (animate) {
            final ViewGroup viewGroup = mSelectedWidget.getView();
            maybeBeginDelayedTransition(viewGroup, mTransitionJit);
        }

        mSelectedWidget.onViewAttached();
        mBackground.dispatchSetBackground(
                mSelectedWidget.getBackground(),
                mSelectedWidget.getBackgroundMask());
        updateStatusClockVisibility(!mSelectedWidget.hasClock() && getConfig().isFullScreen());

        iconView = mSelectedWidget.getIconView();
        if (iconView != null) {
            iconView.setSelected(true);
            iconView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }

        if (!isResumed()) {
            return;
        }

        // Start timeout on main or media widgets, and
        // pause it otherwise.
        if (widget.isHomeWidget()) {
            mTimeout.resume();
        } else {
            mTimeout.setTimeoutDelayed(mTimeoutNormal, true);
            mTimeout.pause();
        }
    }

    /**
     * Updates the visibility of status clock (appears above on top of the screen).
     */
    private void updateStatusClockVisibility(boolean visibleNow) {
        if (mStatusClockTextView == null) {
            return;
        }

        View view = mStatusClockTextView;
        boolean visible = view.getVisibility() == View.VISIBLE;

        if (visible == visibleNow) return;
        if (isAnimatable()) {
            final float[] values;
            if (visibleNow) {
                values = new float[]{
                        0.0f, 1.0f,
                        0.8f, 1.0f,
                        0.8f, 1.0f,
                };
                view.setVisibility(View.VISIBLE);
                view.animate().setListener(null);
            } else {
                values = new float[]{
                        1.0f, 0.0f,
                        1.0f, 0.8f,
                        1.0f, 0.8f,
                };
                view.animate().setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mStatusClockTextView.setVisibility(View.GONE);
                    }
                });
            }

            view.setAlpha(values[0]);
            view.setScaleX(values[2]);
            view.setScaleY(values[4]);
            view.animate()
                    .alpha(values[1])
                    .scaleX(values[3])
                    .scaleY(values[5]);
        } else {
            ViewUtils.setVisible(view, visibleNow);
        }
    }

    //-- WIDGETS MANAGEMENT ---------------------------------------------------

    protected void onWidgetPin(@NonNull Widget widget) {
        mHandler.sendEmptyMessageDelayed(MSG_SHOW_HOME_WIDGET, mConfigWidgetPinDuration);
        mHasPinnedWidget = true;
    }

    protected void onWidgetStick(@NonNull Widget widget) {
        // mHandler.sendEmptyMessageDelayed(MSG_SHOW_HOME_WIDGET, mConfigWidgetPinDuration);
        mHasPinnedWidget = true;
    }

    protected void onWidgetReadAloud(@NonNull Widget widget) { /* reading aloud */ }

    /**
     * Called on widget's dismissal. The code here {@link #internalRemoveWidget(Widget) removes}
     * {@code widget} and provides the "Turn screen off on last widget dismissal" feature.
     */
    protected void onWidgetDismiss(@NonNull Widget widget) {
        internalRemoveWidget(widget);
        widget.onDismiss();
        updateDividerVisibility(true);

        // The "Turn screen off on last widget dismissal" feature.
        // Previously this feature was working only for notifications.
        if (isNotDemo()
                && getConfig().isScreenOffAfterLastWidget()
                && mWidgetsMap.isEmpty() /* checking if there are any widgets */) {
            mActivityAcd.lock();
        }
    }

    //-- SCENES MANAGEMENT ----------------------------------------------------

    /**
     * Changes current scene to given one.
     *
     * @see #showWidget(com.achep.acdisplay.ui.components.Widget)
     */
    @SuppressLint("NewApi")
    protected synchronized final void goScene(@NonNull SceneCompat sceneCompat, boolean animate) {
        if (mCurrentScene == sceneCompat) return;
        mCurrentScene = sceneCompat;
        if (DEBUG) Log.d(TAG, "Going to " + sceneCompat);

        if (Device.hasKitKatApi()) animate &= mSceneContainer.isLaidOut();
        if (!animate) {
            sceneCompat.enter();
            return;
        }

        if (Device.hasKitKatApi()) {
            final Scene scene = sceneCompat.getScene();
            try {
                // This must be a synchronization problem with Android's Scene or TransitionManager,
                // but those were declared as final classes, so I have no idea how to fix it.
                TransitionManager.go(scene, mTransitionSwitchScene);
            } catch (IllegalStateException e) {
                Log.w(TAG, "TransitionManager has failed switching scenes!");

                ViewGroup viewGroup = (ViewGroup) getSceneView().getParent();
                viewGroup.removeView(getSceneView());

                try {
                    // Reset internal scene's tag to make it work again.
                    int id = Resources.getSystem().getIdentifier("current_scene", "id", "android");
                    Method method = View.class.getMethod("setTagInternal", int.class, Object.class);
                    method.setAccessible(true);
                    method.invoke(viewGroup, id, null);
                } catch (NoSuchMethodException
                        | IllegalAccessException
                        | InvocationTargetException e2) {
                    throw new RuntimeException("An attempt to fix the TransitionManager has failed.");
                }

                TransitionManager.go(scene, mTransitionSwitchScene);
            }
        } else {
            sceneCompat.enter();

            if (getActivity() != null) {
                // TODO: Better animation for Jelly Bean users.
                float density = getResources().getDisplayMetrics().density;
                getSceneView().setAlpha(0.6f);
                getSceneView().setRotationX(6f);
                getSceneView().setTranslationY(6f * density);
                getSceneView().animate().alpha(1).rotationX(0).translationY(0);
            }
        }
    }

    //-- DYNAMIC BACKGROUND ---------------------------------------------------

    /**
     * Updates current background. The widget must be actually selected, otherwise it
     * will crash.
     */
    @Override
    public void requestBackgroundUpdate(@NonNull Widget widget) {
        Check.getInstance().isTrue(isCurrentWidget(widget));
        final int mask = widget.getBackgroundMask();
        mBackground.dispatchSetBackground(widget.getBackground(), mask);
    }

    //-- MEDIA ----------------------------------------------------------------

    /**
     * Gets the controller which should be receiving media events
     * while this fragment is in the foreground. The controller supports
     * all platforms starting from Android 4.3 and does nothing on older
     * versions.
     *
     * @return The controller which should receive events.
     */
    @NonNull
    public MediaController2 getMediaController2() {
        return mMediaControlsHelper.getMediaController();
    }

    private void makeMediaWidgetActive() {
        if (mMediaWidgetActive == (mMediaWidgetActive = true)) return;

        // Update home widget if the current widget is
        // the clock / media widget.
        if (mSelectedWidget.isHomeWidget()) showHomeWidget();
    }

    private void makeMediaWidgetInactive() {
        if (mMediaWidgetActive == (mMediaWidgetActive = false)) return;

        // Update home widget if the current widget
        // is the media widget.
        if (isCurrentWidget(mMediaWidget)) showHomeWidget();
    }

    /**
     * Defines if media widget replaces home widget
     * or no.
     *
     * @return {@code true} if media widget replaces the home widget,
     * {@code false} otherwise.
     */
    private boolean isMediaWidgetHome() {
        return mMediaWidgetActive;
    }

    //-- LOLLIPOP -------------------------------------------------------------

    /**
     * Returns {@code true} if the device is currently in power save mode.
     * When in this mode, applications should reduce their functionality
     * in order to conserve battery as much as possible.
     *
     * @return {@code true} if the device is currently in power save mode, {@code false} otherwise.
     * @see com.achep.base.utils.power.PowerSaveDetector
     */
    public boolean isPowerSaveMode() {
        return mActivity.isPowerSaveMode();
    }

    /**
     * @return {@code true} if this fragment is attached to {@link com.achep.acdisplay.ui.activities.AcDisplayActivity} and
     * matches parent layout, {@code false} if this is only preview.
     */
    public boolean isNotDemo() {
        return getActivity() instanceof AcDisplayActivity;
    }

    public boolean isAnimatable() {
        return !isPowerSaveMode() && isResumed();
    }

    public boolean isAnimatableAuto() {
        return isAnimatable() && !mResuming;
    }

    //-- NOTIFICATION HANDLING ------------------------------------------------

    @Nullable
    private NotifyWidget find(@Nullable OpenNotification n) {
        if (n == null) return null;
        // Find the widget of this or previous notification,
        // so we can manage it.
        for (Widget item : mWidgetsMap.values()) {
            if (item instanceof NotifyWidget) {
                // Check if notification has the same key.
                NotifyWidget nw = (NotifyWidget) item;
                if (nw.hasIdenticalIds(n)) {
                    return nw;
                }
            }
        }
        return null;
    }

    @Override
    public void onNotificationListChanged(@NonNull NotificationPresenter np,
                                          OpenNotification osbn,
                                          int event, boolean isLastEventInSequence) {
        if (DEBUG) Log.d(TAG, "Handling notification list changed event: "
                + NotificationPresenter.getEventName(event));

        if (!isResumed()) {
            mPendingNotifyChange = true;
            return;
        }

        NotifyWidget widgetPrev = null;

        if (event == NotificationPresenter.EVENT_REMOVED
                || event == NotificationPresenter.EVENT_CHANGED) {

            // Find the widget of this or previous notification,
            // so we can manage it.
            widgetPrev = find(osbn);
        }

        switch (event) { // don't update on spam-change.
            case NotificationPresenter.EVENT_CHANGED:
                if (widgetPrev != null) {
                    if (DEBUG) Log.d(TAG, "[Event] Updating notification widget...");
                    if (isCurrentWidget(widgetPrev)) {
                        final ViewGroup viewGroup = widgetPrev.getView();
                        maybeBeginDelayedTransition(viewGroup, mTransitionJit);
                    }
                    widgetPrev.setNotification(osbn);
                    break;
                }
            case NotificationPresenter.EVENT_POSTED:
                if (DEBUG) Log.d(TAG, "[Event] Adding new notification widget...");
                event = NotificationPresenter.EVENT_POSTED;

                // Create new widget and inflate its
                // icon view.
                NotifyWidget nw = new NotifyWidget(this, this);
                nw.start();
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View iconView = nw.createIconView(inflater, mIconsContainer);

                // Check if widget's scene is available.
                String name = nw.getClass().getName();
                SceneCompat scene = mScenesMap.get(name);

                // Setup widget & view.
                ViewUtils.setSize(iconView, getConfig().getIconSizePx());
                nw.setNotification(osbn);
                if (scene != null) {
                    // Initialize widget with previously created
                    // scene. This is possible by design.
                    nw.createView(null, null, scene.getView());
                } else {
                    // Create scene view and put to map of scenes.
                    ViewGroup sceneView = nw.createView(inflater, mSceneContainer, null);
                    if (sceneView != null) {
                        scene = new SceneCompat(mSceneContainer, sceneView);
                        mScenesMap.put(name, scene);
                    }
                }

                mWidgetsMap.put(iconView, nw);
                maybeBeginDelayedTransition(mIconsContainer, mTransitionJit);
                mIconsContainer.addView(iconView);
                break;
            case NotificationPresenter.EVENT_REMOVED:
                if (widgetPrev != null) {
                    if (DEBUG) Log.d(TAG, "[Event] Removing notification widget...");
                    internalRemoveWidget(widgetPrev);
                    internalCleanPressedIconViewIfRemovedFromContainer();
                }
                break;
            case NotificationPresenter.EVENT_BATH:
                if (DEBUG) Log.d(TAG, "[Event] Rebuilding notifications...");
                rebuildNotifications();
                break;
        }

        if (event == NotificationPresenter.EVENT_POSTED
                || event == NotificationPresenter.EVENT_REMOVED) {
            if (isLastEventInSequence) updateDividerVisibility(true);
            // NotificationPresenter#EVENT_BATH causes #rebuildNotifications() to be run,
            // which calls #updateDividerVisibility() and begins delayed
            // transition by itself.
        }
    }

    private void rebuildNotifications() {
        final long now = SystemClock.elapsedRealtime();

        ViewGroup container = mIconsContainer;

        final int childCount = container.getChildCount();

        // Count the number of non-notification fragments
        // such as unlock or music controls fragments.
        int start = 0;
        for (int i = 0; i < childCount; i++) {
            View child = container.getChildAt(i);
            Widget fragment = findWidgetByIcon(child);
            if (fragment instanceof NotifyWidget) {
                // Those fragments are placed at the begin of layout
                // so no reason to continue searching.
                break;
            } else {
                start++;
            }
        }

        final ArrayList<OpenNotification> list = NotificationPresenter.getInstance().getList();
        final int notifyCount = list.size();

        final boolean[] notifyUsed = new boolean[notifyCount];
        final boolean[] childUsed = new boolean[childCount];

        for (int i = start; i < childCount; i++) {
            View child = container.getChildAt(i);
            NotifyWidget widget = (NotifyWidget) findWidgetByIcon(child);
            OpenNotification target = widget.getNotification();

            for (int j = 0; j < notifyCount; j++) {
                OpenNotification n = list.get(j);
                if (NotificationUtils.hasIdenticalIds(target, n)) {

                    notifyUsed[j] = true;
                    childUsed[i] = true;

                    if (target != n) {
                        widget.setNotification(n);
                    }
                    break;
                }
            }
        }

        // Re-use free views and remove redundant views.
        boolean removeAllAfter = false;
        for (int a = start, j = 0, offset = 0; a < childCount; a++) {
            if (childUsed[a]) continue;
            final int i = a + offset;

            View child = container.getChildAt(i);
            removing_all_next_views:
            {
                if (!removeAllAfter) {
                    for (; j < notifyCount; j++) {
                        if (notifyUsed[j]) continue;

                        assert child != null;
                        notifyUsed[j] = true;

                        NotifyWidget nw = (NotifyWidget) findWidgetByIcon(child);
                        nw.setNotification(list.get(j));
                        break removing_all_next_views;
                    }
                }
                removeAllAfter = true;
                internalReleaseWidget(child);

                // Remove widget's icon.
                container.removeViewAt(i);
                offset--;
            }
        }

        assert getActivity() != null;
        LayoutInflater inflater = getActivity().getLayoutInflater();

        final int iconSize = getConfig().getIconSizePx();
        for (int i = 0; i < notifyCount; i++) {
            if (notifyUsed[i]) continue;

            NotifyWidget nw = new NotifyWidget(this, this);
            if (isResumed()) nw.start();

            View iconView = nw.createIconView(inflater, container);
            ViewUtils.setSize(iconView, iconSize);
            container.addView(iconView);

            nw.setNotification(list.get(i));
            mWidgetsMap.put(iconView, nw);
        }

        // /////////////////////
        // ~~ UPDATE HASH MAP ~~
        // /////////////////////

        HashMap<String, SceneCompat> map = (HashMap<String, SceneCompat>) mScenesMap.clone();

        mScenesMap.clear();
        for (Widget fragment : mWidgetsMap.values()) {
            String type = fragment.getClass().getName();
            SceneCompat scene = map.get(type);
            if (scene != null) {
                fragment.createView(null, null, scene.getView());
            } else {
                ViewGroup sceneView = fragment.createView(inflater, mSceneContainer, null);
                if (sceneView != null) {
                    scene = new SceneCompat(mSceneContainer, sceneView);
                    map.put(type, scene);
                }
            }
            if (scene != null) {
                mScenesMap.put(type, scene);
            }
        }

        internalCleanPressedIconViewIfRemovedFromContainer();

        if (DEBUG) {
            long delta = SystemClock.elapsedRealtime() - now;
            Log.d(TAG, "Fragment list updated in " + delta + "ms.");
        }

        // Do not animate divider's visibility change on
        // pause/resume, cause it _somehow_ confuses people.
        boolean animate = !mResuming;
        updateDividerVisibility(animate);
    }

    /**
     * Stops the widget, which icon view has been passed as parameter, and removes it
     * from the {@link #mWidgetsMap map}.
     */
    private void internalReleaseWidget(@NonNull View iconView) {
        if (isResumed()) findWidgetByIcon(iconView).stop();
        mWidgetsMap.remove(iconView);
    }

    /**
     * Stops the widget and removes it from the {@link #mWidgetsMap map}.
     */
    private void internalReleaseWidget(@NonNull Widget widget) {
        if (isResumed()) widget.stop();
        mWidgetsMap.remove(widget.getIconView());
    }

    private void internalRemoveWidget(@NonNull Widget widget) {
        internalReleaseWidget(widget);
        maybeBeginDelayedTransition(mIconsContainer, mTransitionJit);
        mIconsContainer.removeView(widget.getIconView());

        // Remove widget's scene if it's not needed anymore.
        boolean removeScene = true;
        String name = widget.getClass().getName();
        for (Widget item : mWidgetsMap.values()) {
            if (name.equals(item.getClass().getName())) {
                removeScene = false;
                break;
            }
        }
        if (removeScene) mScenesMap.remove(name);
        if (isCurrentWidget(widget)) showHomeWidget();
    }

    private void internalCleanPressedIconViewIfRemovedFromContainer() {
        if (mPressedIconView == null) {
            return;
        }

        int length = mIconsContainer.getChildCount();
        for (int i = 0; i < length; i++) {
            View view = mIconsContainer.getChildAt(i);
            if (mPressedIconView == view) {
                return;
            }
        }

        mPressedIconView = null;
    }

    /**
     * Updates the visibility of divider between
     * the scene and icons.
     */
    @SuppressLint("NewApi")
    private void updateDividerVisibility(boolean animate) {
        final View view = mDividerView;

        final boolean visible = view.getVisibility() == View.VISIBLE;
        final boolean visibleNow = !mWidgetsMap.isEmpty();

        if (animate && isAnimatable()) {
            int visibleInt = MathUtils.bool(visible);
            int visibleNowInt = MathUtils.bool(visibleNow);
            float[] values = {1.0f, 0.1f, 1.0f, 0.5f};

            ViewUtils.setVisible(view, true);
            view.setScaleX(values[1 - visibleInt]);
            view.setAlpha(values[3 - visibleInt]);
            view.animate()
                    .scaleX(values[1 - visibleNowInt])
                    .alpha(values[3 - visibleNowInt])
                    .setInterpolator(new AccelerateInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            ViewUtils.setVisible(view, visibleNow, View.INVISIBLE);
                            view.setAlpha(1);
                            view.setScaleX(1);
                        }
                    });
        } else {
            ViewUtils.setVisible(view, visibleNow, View.INVISIBLE);
        }
    }

    //-- OTHER CLASSES --------------------------------------------------------

    @SuppressLint("NewApi")
    private void maybeBeginDelayedTransition(@Nullable ViewGroup sceneRoot,
                                             @Nullable Transition transition) {
        if (Device.hasKitKatApi()
                && isAnimatableAuto()
                && sceneRoot != null
                && sceneRoot.isLaidOut()) {
            TransitionManager.beginDelayedTransition(sceneRoot, transition);
        }
    }

    /**
     * Transfers the touch between views, and implements double-tap-to-lock.
     *
     * @author Artem Chepurnoy
     */
    private static class TouchForwarder implements View.OnTouchListener {

        private final PocketFragment.OnSleepRequestListener mListener;
        private final CircleView mCircleView;
        private final GestureDetector mGestureDetector;

        /**
         * {@code true} if redirecting all touches to the {@link #mCircleView},
         * {@code false} otherwise.
         */
        private boolean mCircling;

        public TouchForwarder(@NonNull Context context,
                              @NonNull CircleView circleView,
                              @NonNull PocketFragment.OnSleepRequestListener listener) {
            mListener = listener;
            mCircleView = circleView;
            mGestureDetector = new GestureDetector(context, new GestureListener());
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mGestureDetector.onTouchEvent(event);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    float x = event.getX();
                    float y = event.getY();
                    mCircling = ViewUtils.pointInView(v, x, y, -20);
                default:
                    if (mCircling) mCircleView.sendTouchEvent(event);
            }

            return mCircling;
        }

        /**
         * Implements double-tap gesture.
         *
         * @author Artem Chepurnoy
         */
        class GestureListener extends GestureDetector.SimpleOnGestureListener {

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                Config config = Config.getInstance();
                return config.isDoubleTapToSleepEnabled() && mListener.onSleepRequest();
            }

        }

    }

    private static class H extends WeakHandler<AcDisplayFragment> {

        public H(@NonNull AcDisplayFragment fragment) {
            super(fragment);
        }

        @Override
        protected void onHandleMassage(@NonNull AcDisplayFragment fragment, Message msg) {
            switch (msg.what) {
                case MSG_HIDE_MEDIA_WIDGET:
                    fragment.makeMediaWidgetInactive();
                    break;
                case MSG_SHOW_HOME_WIDGET:
                    fragment.showHomeWidget();
                    break;
            }
        }

    }

}
