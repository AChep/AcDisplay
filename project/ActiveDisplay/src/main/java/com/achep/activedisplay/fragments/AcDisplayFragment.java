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

package com.achep.activedisplay.fragments;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.achep.activedisplay.AsyncTask;
import com.achep.activedisplay.Config;
import com.achep.activedisplay.Device;
import com.achep.activedisplay.Project;
import com.achep.activedisplay.R;
import com.achep.activedisplay.Timeout;
import com.achep.activedisplay.activities.AcDisplayActivity;
import com.achep.activedisplay.activities.KeyguardActivity;
import com.achep.activedisplay.animations.ProgressBarAnimation;
import com.achep.activedisplay.compat.SceneCompat;
import com.achep.activedisplay.fragments.components.MusicFragment;
import com.achep.activedisplay.fragments.components.NotificationFragment;
import com.achep.activedisplay.fragments.components.UnlockFragment;
import com.achep.activedisplay.notifications.NotificationPresenter;
import com.achep.activedisplay.notifications.NotificationUtils;
import com.achep.activedisplay.notifications.OpenStatusBarNotification;
import com.achep.activedisplay.utils.MathUtils;
import com.achep.activedisplay.utils.ViewUtils;
import com.achep.activedisplay.widgets.ProgressBar;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is main fragment of ActiveDisplay app.
 */
public class AcDisplayFragment extends Fragment implements
        View.OnTouchListener {

    private static final String TAG = "AcDisplayFragment";

    public static final int SCENE_UNLOCK = 1;
    public static final int SCENE_NOTIFICATION = 2;
    public static final int SCENE_MUSIC_CONTROLS = 3;

    private Config mConfig;
    private NotificationPresenter mPresenter;
    private NotificationListener mNotificationListener = new NotificationListener();

    private ViewGroup mSceneContainer;
    private LinearLayout mCollapsedViewsContainer;
    private HashMap<View, Widget> mWidgetsMap = new HashMap<>();
    private HashMap<Integer, SceneCompat> mScenesMap = new HashMap<>();
    private boolean mCollapsedViewsNeedsUpdate;

    private Widget mSelectedWidget;

    private SceneCompat mCurrentScene;
    private SceneCompat mSceneMain;
    private Transition mTransition;

    private long mFinishOnStopTime;

    // handlers
    private ImageView mPinImageView;
    private AnimatorSet mNotifyPinnedAnimation;
    private ImageView mUnlockImageView;
    private float mHandleCircleRadius;

    private boolean mTouched;
    private boolean mParamsKeyguard;

    private TimeoutGui mTimeout;
    private Handler mHandler = new Handler();
    private SelectWidgetRunnable mSelectWidgetRunnable = new SelectWidgetRunnable();
    private Runnable mFinishRunnable = new Runnable() {
        @Override
        public void run() {
            unlock();
        }
    };

    private GestureDetector mGestureDetector;

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            showMainWidget();
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (mParamsKeyguard) lock();
            return mParamsKeyguard;
        }
    }

    private class TimeoutGui extends Timeout implements Timeout.OnTimeoutEventListener {

        private static final int MAX = 300;

        private final ProgressBarAnimation mProgressBarAnimation;
        private final ProgressBar mProgressBar;

        public TimeoutGui(Context context, ProgressBar progressBar) {
            super();
            mProgressBar = progressBar;
            mProgressBar.setMax(MAX);
            mProgressBar.setProgress(mProgressBar.getMax());
            mProgressBarAnimation = new ProgressBarAnimation(mProgressBar, MAX, 0);
            mProgressBarAnimation.setInterpolator(context, android.R.anim.linear_interpolator);
            addListener(this);
        }

        @Override
        public void onTimeoutEvent(int event) {
            if (!mParamsKeyguard) return;
            switch (event) {
                case Timeout.EVENT_CLEARED:
                    mProgressBar.clearAnimation();
                    mProgressBar.setProgress(mProgressBar.getMax());
                    break;
                case Timeout.EVENT_CHANGED:
                    long remainingTime = getRemainingTime();
                    if (remainingTime > 0) {
                        mProgressBarAnimation.setDuration(remainingTime);
                        mProgressBar.startAnimation(mProgressBarAnimation);
                    }
                    break;
                case Timeout.EVENT_TIMEOUT:
                    AcDisplayFragment.this.lock();
                    break;
            }
        }
    }

    /**
     * This is needed to pause timeout while browsing status bar window,
     * system dialogs etc.
     */
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            if (!mTouched && mSelectedWidget == null) {
                mTimeout.setTimeoutDelayed(mConfig.getTimeoutNormal());
            }
        } else {
            mTimeout.clear();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mHandleCircleRadius = getResources().getDimension(R.dimen.handler_circle_radius);
        mNotifyPinnedAnimation = (AnimatorSet) AnimatorInflater.loadAnimator(
                activity, R.anim.notification_pinned);

        mParamsKeyguard = activity instanceof KeyguardActivity;
        mGestureDetector = new GestureDetector(activity, new GestureListener());
    }

    @Override
    public void onResume() {
        super.onResume();
        mTimeout.release();
        mTimeout.setTimeoutDelayed(mConfig.getTimeoutNormal());
    }

    @Override
    public void onPause() {
        super.onPause();
        mTimeout.clear();
        mTimeout.lock();
    }

    private void lock() {
        Activity activity = getActivity();
        if (activity instanceof KeyguardActivity) {
            KeyguardActivity lockscreen = (KeyguardActivity) activity;
            lockscreen.lock();
        }
    }

    private void unlock() {
        unlock(null, false);
    }

    public void unlock(Runnable runnable, boolean finishOnStop) {
        if (finishOnStop) mFinishOnStopTime = SystemClock.uptimeMillis();

        Activity activity = getActivity();
        if (activity instanceof KeyguardActivity) {
            KeyguardActivity lockscreen = (KeyguardActivity) activity;
            lockscreen.unlock(runnable, !finishOnStop);
        } else {
            runnable.run();
        }
    }

    public void dispatchSetBackground(Bitmap bitmap) {
        Activity activity = getActivity();
        if (activity instanceof AcDisplayActivity) {
            AcDisplayActivity acDisplayActivity = (AcDisplayActivity) activity;
            acDisplayActivity.dispatchSetBackground(bitmap);
        }
    }

    private SceneCompat findSceneByFragment(Widget fragment) {
        return fragment.hasExpandedView() ? mScenesMap.get(fragment.getType()) : null;
    }

    private Widget findFragmentByIcon(View view) {
        return mWidgetsMap.get(view);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.acdisplay, container, false);
        assert root != null;

        mSceneContainer = (ViewGroup) root.findViewById(R.id.container);
        mCollapsedViewsContainer = (LinearLayout) root.findViewById(R.id.list);
        mCollapsedViewsContainer.setOnTouchListener(this);

        ViewGroup sceneMain = (ViewGroup) inflater.inflate(R.layout.acdisplay_scene_clock, mSceneContainer, false);
        if (Device.hasKitKatApi()) {
            mSceneMain = new SceneCompat(mSceneContainer, sceneMain);
            if (getResources().getBoolean(R.bool.config_transition_fade)) {
                mTransition = new TransitionSet()
                        .setOrdering(TransitionSet.ORDERING_TOGETHER)
                        .addTransition(new Fade())
                        .addTransition(new ChangeBounds());
            } else {
                mTransition = new ChangeBounds();
            }
        } else {
            mSceneMain = new SceneCompat(mSceneContainer, sceneMain);
        }
        mCurrentScene = mSceneMain;
        mSceneMain.enter();

        mUnlockImageView = (ImageView) root.findViewById(R.id.unlock);
        mPinImageView = (ImageView) root.findViewById(R.id.pin);

        Config config = Config.getInstance(getActivity());

        // /////////////////
        // ~~ TIMEOUT GUI ~~
        // /////////////////
        ProgressBar progressBar;
        ViewStub progressBarStub = (ViewStub) root.findViewById(R.id.progress_bar_stub);
        if (config.isMirroredTimeoutProgressBarEnabled()) {
            progressBarStub.setLayoutResource(R.layout.acdisplay_progress_bar_mirrored);
            progressBar = (ProgressBar) progressBarStub.inflate().findViewById(R.id.progress_bar);

            // Redirect all changes from the main progress bar
            // to mirrored one.
            final ProgressBar progressBarMirrored = (ProgressBar)
                    root.findViewById(R.id.progress_bar_mirrored);
            progressBar.setOnProgressChangeListener(new ProgressBar.OnProgressChangeListener() {

                @Override
                public void onProgressChanged(ProgressBar progressBar, int progress) {
                    progressBarMirrored.setProgress(progress);
                }

                @Override
                public void onMaxChanged(ProgressBar progressBar, int max) {
                    progressBarMirrored.setMax(max);
                }
            });
        } else {
            progressBar = (ProgressBar) progressBarStub.inflate().findViewById(R.id.progress_bar);
        }

        mTimeout = new TimeoutGui(getActivity(), progressBar);
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        Context context = getActivity();
        assert context != null;

        mHandler.removeCallbacks(mFinishRunnable);

        mConfig = Config.getInstance(context);
        mPresenter = NotificationPresenter.getInstance(context);
        mPresenter.addOnNotificationListChangedListener(mNotificationListener);
        updateNotificationList();

        getView().setOnTouchListener(this);
    }

    @Override
    public void onStop() {
        mPresenter.removeOnNotificationListChangedListener(mNotificationListener);
        super.onStop();

        if (SystemClock.uptimeMillis() - mFinishOnStopTime < 600) {
            mHandler.postDelayed(mFinishRunnable, 400);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        final float rawX = event.getRawX();
        final float rawY = event.getRawY();

        if (v == mCollapsedViewsContainer) {
            boolean pin = false;
            boolean keepScene = false;
            boolean touchDown = false;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchDown = true;
                    mTimeout.clear();
                case MotionEvent.ACTION_MOVE:
                    mTouched = true;

                    boolean iconPressed = false;
                    int length = mCollapsedViewsContainer.getChildCount();
                    for (int i = 0; i < length; i++) {
                        final View child = mCollapsedViewsContainer.getChildAt(i);
                        assert child != null;
                        if (child.getVisibility() != View.VISIBLE) continue;

                        // Check if current touch is on view, simulate pressing
                        // and update its state so view can update background etc.
                        final boolean pressedOld = child.isPressed();
                        final boolean pressed = ViewUtils.isTouchPointInView(child, rawX, rawY);
                        child.setPressed(pressed);

                        if (pressed) iconPressed = true;
                        if (pressed != pressedOld && !child.isSelected()) {
                            child.refreshDrawableState();
                            if (pressed) {
                                Widget widget = findFragmentByIcon(child);
                                selectWidgetDelayed(widget, touchDown);
                            }
                        }
                    }
                    if (!iconPressed) {
                        // Don't show latest pressed notification cause this
                        // gesture is probably an accident.
                        removeSelectWidgetCallbacks();
                    }

                    handleSelectors(rawX, rawY);
                    break;
                case MotionEvent.ACTION_UP:
                    handleSelectors(rawX, rawY);

                    // Handle basic features such as pinning notification and
                    // unlocking device.
                    handlers:
                    if (true) {
                        View view;
                        if ((view = mUnlockImageView).getVisibility() == View.VISIBLE) {
                            unlock();

                            // Don't update the UI so user won't notice the lag
                            // between unlocking and calling this method.
                            keepScene = true;
                        } else if ((view = mPinImageView).getVisibility() == View.VISIBLE) {
                            pin = true;
                            mNotifyPinnedAnimation.setTarget(mSceneContainer);
                            mNotifyPinnedAnimation.start();
                        } else break handlers; // do not vibrate

                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    }

                    // ///////////////
                    // ~~ FALL DOWN ~~
                    // ///////////////
                case MotionEvent.ACTION_CANCEL:
                    length = mCollapsedViewsContainer.getChildCount();
                    for (int i = 0; i < length; i++) {
                        View child = mCollapsedViewsContainer.getChildAt(i);
                        assert child != null;
                        child.setPressed(false);
                        child.refreshDrawableState();
                    }

                    if (!pin && !keepScene) {
                        showMainWidget();
                    }

                    ViewUtils.setVisible(mUnlockImageView, false);
                    ViewUtils.setVisible(mPinImageView, false);

                    if (mCollapsedViewsNeedsUpdate) updateNotificationList();

                    mTouched = false;
                    mCollapsedViewsNeedsUpdate = false;
                    break;
            }
            return true;
        } else {
            mGestureDetector.onTouchEvent(event);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (mSelectedWidget == null) {
                        mTimeout.setTimeoutDelayed(mConfig.getTimeoutShort());
                    }
                    break;
            }
            return true;
        }
    }

    private void handleSelectors(float rawX, float rawY) {
        if (mSelectedWidget == null) {
            return;
        }

        View icon = mSelectedWidget.getCollapsedView();
        int iconX = ViewUtils.getLeft(icon) + icon.getWidth() / 2;
        int iconY = ViewUtils.getTop(icon) + icon.getHeight() / 2;

        double length = Math.hypot(rawX - iconX, rawY - iconY);
        if (length >= mHandleCircleRadius) {
            View active = rawY > iconY ? mParamsKeyguard ? mUnlockImageView : null
                    : mSelectedWidget.hasExpandedView() ? mPinImageView : null;
            View passive = rawY > iconY ? mPinImageView : mUnlockImageView;

            if (active != null) {
                float[] point = new float[2];
                calculateCrossPoint(iconX, iconY, rawX, rawY, mHandleCircleRadius, point);
                active.setTranslationX(point[0] - active.getWidth() / 2 - ViewUtils.getLeft(getView()));
                active.setTranslationY(point[1] - active.getHeight() / 2 - ViewUtils.getTop(getView()));

                if (active.getVisibility() != View.VISIBLE) {
                    active.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    active.setVisibility(View.VISIBLE);
                }
            }
            ViewUtils.setVisible(passive, false, View.INVISIBLE);
        } else {
            ViewUtils.setVisible(mUnlockImageView, false, View.INVISIBLE);
            ViewUtils.setVisible(mPinImageView, false, View.INVISIBLE);
        }
    }

    private void calculateCrossPoint(float centerX, float centerY, float x1, float y1, float radius, float[] point) {
        if ((x1 -= centerX) == 0) x1 = 0.00001f;
        if ((y1 -= centerY) == 0) y1 = 0.00001f;
        float k = y1 / x1;
        float x = radius / (float) Math.sqrt(1 + k * k);
        float y = k * x;

        point[0] = x * MathUtils.charge(x1) + centerX;
        point[1] = y * MathUtils.charge(x1) + centerY;
    }

    private class SelectWidgetRunnable implements Runnable {

        private Widget newWidget;

        @Override
        public void run() {
            selectWidget(newWidget);
        }

        public void setWidget(Widget widget) {
            this.newWidget = widget;
        }
    }

    private void removeSelectWidgetCallbacks() {
        mHandler.removeCallbacks(mSelectWidgetRunnable);
    }

    private void selectWidgetDelayed(Widget fragment, boolean immediately) {
        removeSelectWidgetCallbacks();

        mSelectWidgetRunnable.setWidget(fragment);
        mHandler.postDelayed(mSelectWidgetRunnable, immediately ? 0 : 120);
    }

    private void selectWidget(Widget widget) {
        removeSelectWidgetCallbacks();
        if (mSelectedWidget != null) {
            mSelectedWidget.getCollapsedView().setSelected(false);
            mSelectedWidget.onExpandedViewDetached();
        }

        dispatchSetBackground(null);

        mSelectedWidget = widget;

        if (mSelectedWidget == null) {
            goScene(mSceneMain);
        } else {
            SceneCompat scene = findSceneByFragment(mSelectedWidget);
            if (scene == null) {
                goScene(mSceneMain);
            } else if (mCurrentScene != scene) {
                goScene(scene);
            } else if (Device.hasKitKatApi()) {
                TransitionManager.beginDelayedTransition(mSceneContainer, mTransition);
            }

            mSelectedWidget.onExpandedViewAttached();
            mSelectedWidget.getCollapsedView().setSelected(true);
            mSelectedWidget.getCollapsedView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    public void showMainWidget() {
        mTimeout.setTimeoutDelayed(mConfig.getTimeoutNormal());
        selectWidget(null);
    }

    /**
     * Changes current scene to given one.
     */
    private void goScene(SceneCompat sceneCompat) {
        if (mCurrentScene != sceneCompat) {
            mCurrentScene = sceneCompat;
            if (Device.hasKitKatApi()) {
                TransitionManager.go(sceneCompat.scene, mTransition);
            } else sceneCompat.enter();
        }
    }

    /**
     * <pre>
     *     Items sorted by priority:
     *          [ UNLOCK ]
     *          [ MUSIC ]
     *          [ NOTIFICATION ]
     * </pre>
     * This method is full of black magic!
     */
    // TODO: Optimize it
    // Spent hours on optimizing with no result: 0h
    private void updateNotificationList() {
        long now = SystemClock.elapsedRealtime();

        ViewGroup container = mCollapsedViewsContainer;
        final int childCount = container.getChildCount();

        if (Device.hasKitKatApi()) {
            TransitionManager.beginDelayedTransition(container);
        }

        // Count the number of non-notification fragments
        // such as unlock or music controls fragments.
        int fragmentsExtraCount = 0;
        for (int i = 0; i < childCount; i++) {
            View child = container.getChildAt(i);
            Widget fragment = findFragmentByIcon(child);
            if (!(fragment instanceof NotificationFragment)) {
                fragmentsExtraCount++;
            } else {
                // Those fragments are placed at the begin of layout
                // so no reason to continue searching.
                break;
            }
        }

        final ArrayList<OpenStatusBarNotification> list = mPresenter.getList();
        final int notifyCount = list.size();

        final boolean[] notifyUsed = new boolean[notifyCount];
        final boolean[] childUsed = new boolean[childCount];

        // ///////////////////
        // ~~ NOTIFICATIONS ~~
        // ///////////////////

        // Does not need an update
        for (int i = fragmentsExtraCount; i < childCount; i++) {
            View child = container.getChildAt(i);
            NotificationFragment fragment = (NotificationFragment) findFragmentByIcon(child);
            OpenStatusBarNotification target = fragment.getNotification();

            // Try to find the notification with the same
            // id, tag and package name as in present.
            for (int j = 0; j < notifyCount; j++) {
                OpenStatusBarNotification n = list.get(j);
                if (NotificationUtils.equals(target, n)) {

                    notifyUsed[j] = true;
                    childUsed[i] = true;

                    if (target != n) {
                        fragment.setNotification(n);
                    }
                    break;
                }
            }
        }

        // Re-use free views and remove redundant views.
        boolean removeAllAfter = false;
        for (int a = fragmentsExtraCount, j = 0, offset = 0; a < childCount; a++) {
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

                        NotificationFragment fragment = (NotificationFragment) findFragmentByIcon(child);
                        fragment.setNotification(list.get(j));
                        break removing_all_next_views;
                    }
                }
                removeAllAfter = true;
                container.removeViewAt(i);
                mWidgetsMap.remove(child);
                offset--;
            }
        }

        assert getActivity() != null;
        LayoutInflater inflater = getActivity().getLayoutInflater();

        for (int i = 0; i < notifyCount; i++) {
            if (notifyUsed[i]) continue;

            NotificationFragment fragment = new NotificationFragment(this);
            View view = fragment.createCollapsedView(inflater, container);
            container.addView(view);

            fragment.setNotification(list.get(i));
            mWidgetsMap.put(view, fragment);
        }

        // ////////////
        // ~~ EXTRAS ~~
        // ////////////

        int[] extras = new int[]{
                SCENE_UNLOCK,
                SCENE_MUSIC_CONTROLS,
        };

        extras[1] = -1;

        // Show unlock widget only if there's no any
        // other views.
        if (notifyCount > 0) {
            extras[0] = -1;
        } else for (int i = 1; i < extras.length; i++)
                if (extras[i] >= 0) {
                    extras[0] = -1;
                    break;
                }

        for (int i = fragmentsExtraCount - 1; i >= 0; i--) {
            View child = container.getChildAt(i);
            Widget fragment = findFragmentByIcon(child);

            boolean found = false;
            for (int j = 0; j < extras.length; j++) {
                if (extras[j] == fragment.getType()) {
                    extras[j] = -1;
                    found = true;
                    break;
                }
            }
            if (!found) {
                container.removeViewAt(i);
                mWidgetsMap.remove(child);
            }
        }

        for (int i = 0, j = 0; i < extras.length; i++) {
            if (extras[i] >= 0) {
                Widget fragment;
                switch (extras[i]) {
                    case SCENE_UNLOCK:
                        fragment = new UnlockFragment(this);
                        break;
                    case SCENE_MUSIC_CONTROLS:
                        fragment = new MusicFragment(this);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown acfragment type found!");
                }
                View view = fragment.createCollapsedView(inflater, container);
                container.addView(view, j++);
                mWidgetsMap.put(view, fragment);
            }
        }

        // /////////////////////
        // ~~ UPDATE HASH MAP ~~
        // /////////////////////

        HashMap<Integer, SceneCompat> map = (HashMap<Integer, SceneCompat>) mScenesMap.clone();

        mScenesMap.clear();
        for (Widget fragment : mWidgetsMap.values()) {
            int type = fragment.getType();
            SceneCompat scene = map.get(type);
            if (scene != null) {
                fragment.createExpandedView(null, null, scene.getView());
            } else {
                ViewGroup sceneView = fragment.createExpandedView(inflater, mSceneContainer, null);
                if (sceneView != null) {
                    scene = new SceneCompat(mSceneContainer, sceneView);
                    map.put(type, scene);
                }
            }
            if (scene != null) {
                mScenesMap.put(type, scene);
            }
        }

        if (Project.DEBUG) {
            long delta = SystemClock.elapsedRealtime() - now;
            Log.d(TAG, "Fragment list updated in " + delta + "ms.");
        }

        if (Device.hasKitKatApi()) {

            // Make sure that container will be updated
            // to end delayed transition.
            container.invalidate();
        }
    }

    // //////////////////////////////////////////
    // ///////////// -- CLASSES -- //////////////
    // //////////////////////////////////////////

    private class NotificationListener implements NotificationPresenter.OnNotificationListChangedListener {

        @Override
        public void onNotificationListChanged(NotificationPresenter nm,
                                        OpenStatusBarNotification notification,
                                        final int event) {
            if (mTouched) {
                mCollapsedViewsNeedsUpdate = true;
            } else {
                switch (event) {
                    case NotificationPresenter.EVENT_BATH:
                    case NotificationPresenter.EVENT_POSTED:
                    case NotificationPresenter.EVENT_CHANGED:
                    case NotificationPresenter.EVENT_REMOVED:
                        mTimeout.setTimeoutDelayed(mConfig.getTimeoutNormal(), true);
                        updateNotificationList();
                        break;
                }
            }
        }
    }

    /**
     * Base class of {@link com.achep.activedisplay.fragments.AcDisplayFragment} widgets.
     */
    public static abstract class Widget {

        private AcDisplayFragment mAcDisplayFragment;

        private ViewGroup mExpandedViewGroup;
        private View mCollapsedView;

        private boolean mShown;

        public Widget(AcDisplayFragment fragment) {
            mAcDisplayFragment = fragment;
        }

        /**
         * @return an instance of {@link com.achep.activedisplay.fragments.AcDisplayFragment}.
         */
        public AcDisplayFragment getHostFragment() {
            return mAcDisplayFragment;
        }

        /**
         * @return true is this fragment is currently shown, false otherwise.
         */
        public boolean isShown() {
            return mShown;
        }

        public boolean hasExpandedView() {
            return getExpandedView() != null;
        }

        /**
         * A type of widget must be constant!
         */
        public abstract int getType();

        public View getCollapsedView() {
            return mCollapsedView;
        }

        public ViewGroup getExpandedView() {
            return mExpandedViewGroup;
        }

        protected View createCollapsedView(LayoutInflater inflater, ViewGroup container) {
            mCollapsedView = onCreateCollapsedView(inflater, container);
            return mCollapsedView;
        }

        protected ViewGroup createExpandedView(LayoutInflater inflater, ViewGroup container, ViewGroup sceneView) {
            mExpandedViewGroup = onCreateExpandedView(inflater, container, sceneView);
            return mExpandedViewGroup;
        }

        public abstract View onCreateCollapsedView(LayoutInflater inflater, ViewGroup container);

        public abstract ViewGroup onCreateExpandedView(LayoutInflater inflater, ViewGroup container, ViewGroup sceneView);

        public void onExpandedViewAttached() {
            mShown = true;
        }

        public void onExpandedViewDetached() {
            mShown = false;
        }
    }

    /**
     * Factory to prepare your background for {@link AcDisplayFragment#dispatchSetBackground(android.graphics.Bitmap)}.
     */
    public static class BackgroundFactoryThread extends AsyncTask<Void, Void, Bitmap> {

        private static final String TAG = "AcFragmentBackgroundFactory";

        public interface Callback {
            void onBackgroundCreated(Bitmap bitmap);
        }

        private final WeakReference<Context> mContext;
        private final Bitmap mBitmapOriginal;
        private final Callback mCallback;

        public BackgroundFactoryThread(Context context, Bitmap original, Callback callback) {
            mContext = new WeakReference<>(context);
            mBitmapOriginal = original;
            mCallback = callback;

            if (original == null) throw new IllegalArgumentException("Bitmap may not be null!");
            if (callback == null) throw new IllegalArgumentException("Callback may not be null!");
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            final long start = SystemClock.elapsedRealtime();

            Context context = mContext.get();
            Bitmap origin = mBitmapOriginal;

            if (context == null || !running) {
                return null;
            }

            // TODO: Reduce bitmap's size if needed.
            Bitmap bitmap = Bitmap.createBitmap(
                    origin.getWidth(),
                    origin.getHeight(),
                    origin.getConfig());

            try {
                RenderScript rs = RenderScript.create(context);
                Allocation overlayAlloc = Allocation.createFromBitmap(rs, origin);
                ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs, overlayAlloc.getElement());

                blur.setInput(overlayAlloc);
                blur.setRadius(3f);
                blur.forEach(overlayAlloc);
                overlayAlloc.copyTo(bitmap);
            } catch (Exception e) {
                Log.e(TAG, "Failed to blur bitmap.");
            }

            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(0x50000000);

            if (Project.DEBUG) {
                long delta = SystemClock.elapsedRealtime() - start;
                Log.d(TAG, "AcFragment background created in " + delta + " millis:"
                        + " width=" + bitmap.getWidth()
                        + " height=" + bitmap.getHeight());
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            mCallback.onBackgroundCreated(bitmap);
        }
    }
}
