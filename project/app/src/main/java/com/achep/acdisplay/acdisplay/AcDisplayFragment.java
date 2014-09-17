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

package com.achep.acdisplay.acdisplay;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.GridLayout;

import com.achep.acdisplay.Build;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.Device;
import com.achep.acdisplay.R;
import com.achep.acdisplay.acdisplay.components.NotifyWidget;
import com.achep.acdisplay.acdisplay.components.Widget;
import com.achep.acdisplay.animations.AnimationListenerAdapter;
import com.achep.acdisplay.compat.SceneCompat;
import com.achep.acdisplay.notifications.NotificationPresenter;
import com.achep.acdisplay.notifications.NotificationUtils;
import com.achep.acdisplay.notifications.OpenNotification;
import com.achep.acdisplay.utils.MathUtils;
import com.achep.acdisplay.utils.ViewUtils;
import com.achep.acdisplay.view.ForwardingLayout;
import com.achep.acdisplay.view.ForwardingListener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is main fragment of ActiveDisplay app.
 */
// TODO: Put main scene inside of widget.
public class AcDisplayFragment extends Fragment implements
        NotificationPresenter.OnNotificationListChangedListener,
        ForwardingLayout.OnForwardedEventListener, View.OnTouchListener,
        Widget.Callback, Config.OnConfigChangedListener {

    private static final String TAG = "AcDisplayFragment";

    private static final int MSG_RESET_SCENE = 0;

    private View mDividerView;
    private ForwardingLayout mSceneContainer;
    private ForwardingLayout mIconsForwarder;
    private GridLayout mIconsContainer;
    private ForwardingListener mSceneForwardingListener;
    private ForwardingListener mIconsForwardingListener;
    private final Handler mTouchHandler = new Handler();

    // Pinnable widgets
    private boolean mHasPinnedWidget;

    private int mConfigWidgetPinDuration;
    private int mConfigWidgetSelectDelay;

    // Animations
    private DismissAnimation mSceneContainerDismissAnim;

    // Swipe to dismiss
    private VelocityTracker mVelocityTracker;
    private int mMaxFlingVelocity;
    private int mMinFlingVelocity;

    private final HashMap<View, Widget> mWidgetsMap = new HashMap<>();
    private final HashMap<String, SceneCompat> mScenesMap = new HashMap<>();
    private Widget mSelectedWidget;
    private View mPressedIconView;

    private SceneCompat mCurrentScene;
    private SceneCompat mSceneMain;
    private Transition mTransition;

    /**
     * Handler to control delayed events.
     *
     * @see #MSG_RESET_SCENE
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_RESET_SCENE:
                    resetScene();
                    break;
            }
        }
    };

    /**
     * Controller of dismiss animation.
     */
    private class DismissAnimation extends Animation {

        private float start;
        private Widget widget;

        public DismissAnimation() {
            super();
            setAnimationListener(new AnimationListenerAdapter() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    Widget widget = DismissAnimation.this.widget;

                    onWidgetDismiss(widget);
                    if (mSelectedWidget == widget) {
                        resetScene();
                    }
                }
            });
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            float value = (start + (1f - start) * interpolatedTime);
            populateSceneContainerDismissAnimation(value);
        }

        public void setup(int duration, float start, Widget widget) {
            if (!hasEnded()) Log.wtf(TAG, "Setting up running animation!!!");
            setDuration(duration);
            this.start = start;
            this.widget = widget;
        }

    }

    private boolean isPinnable() {
        return getConfig().isWidgetPinnable();
    }

    private boolean isReadable() {
        return getConfig().isWidgetReadable();
    }

    protected Config getConfig() {
        return Config.getInstance();
    }

    public View getDividerView() {
        return mDividerView;
    }

    /**
     * @return Layout resource to be inflated as fragment's view.
     * @see #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    protected int getViewResource() {
        return R.layout.acdisplay_fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        ViewConfiguration vc = ViewConfiguration.get(activity);
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();

        mSceneContainerDismissAnim = new DismissAnimation();

        Resources res = getResources();
        mConfigWidgetPinDuration = res.getInteger(R.integer.config_maxPinTime);
        mConfigWidgetSelectDelay = res.getInteger(R.integer.config_iconSelectDelayMillis);
    }

    @TargetApi(android.os.Build.VERSION_CODES.KITKAT)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(getViewResource(), container, false);
        assert root != null;

        mDividerView = root.findViewById(R.id.divider);
        mSceneContainer = (ForwardingLayout) root.findViewById(R.id.scene);
        mIconsForwarder = (ForwardingLayout) root.findViewById(R.id.forwarding);
        mIconsContainer = (GridLayout) root.findViewById(R.id.grid);
        mIconsForwarder.setOnForwardedEventListener(this);
        mIconsForwarder.setAllViewsForwardable(true, 1);
        mIconsForwarder.setOnTouchListener(this);

        mSceneForwardingListener = new ForwardingListener(mIconsForwarder, false, mSceneContainer);
        mIconsForwardingListener = new ForwardingListener(mIconsForwarder, true, mIconsForwarder);

        ViewGroup sceneMain = (ViewGroup) inflater.inflate(R.layout.acdisplay_scene_clock, mSceneContainer, false);
        mSceneMain = new SceneCompat(mSceneContainer, sceneMain);

        if (Device.hasKitKatApi()) {
            mTransition = new TransitionSet()
                    .setOrdering(TransitionSet.ORDERING_TOGETHER)
                    .addTransition(new Fade())
                    .addTransition(new ChangeBounds())
            ;
        }
        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        showWidget(getFirstWidget(), false);
    }

    @Override
    public void onStart() {
        super.onStart();

        NotificationPresenter.getInstance().registerListener(this);
        updateNotificationList();

        Config config = Config.getInstance();
        config.registerListener(this);
        updateIconsSize();

        resetScene();
    }

    @Override
    public void onStop() {
        mHandler.removeCallbacksAndMessages(null);
        mTouchHandler.removeCallbacksAndMessages(null);
        mSceneContainer.clearAnimation();

        NotificationPresenter.getInstance().unregisterListener(this);

        Config config = Config.getInstance();
        config.unregisterListener(this);
        super.onStop();
    }

    @Override
    public void onConfigChanged(Config config, String key, Object value) {
        switch (key) {
            case Config.KEY_UI_ICON_SIZE:
                updateIconsSize();
                break;
        }
    }

    @Override
    public void onNotificationListChanged(NotificationPresenter np,
                                          OpenNotification osbn,
                                          int event) {
        switch (event) { // don't update on spam-change.
            case NotificationPresenter.EVENT_REMOVED:
                // If widget related to removed notification is pinned - unpin it.
                if (hasPinnedWidget() && mSelectedWidget instanceof NotifyWidget) {
                    NotifyWidget widget = (NotifyWidget) mSelectedWidget;
                    if (NotificationUtils.hasIdenticalIds(widget.getNotification(), osbn)) {
                        showMainWidget(); // Unpin
                    }
                }
            case NotificationPresenter.EVENT_POSTED:
            case NotificationPresenter.EVENT_CHANGED:
            case NotificationPresenter.EVENT_BATH:
                if (getActivity() != null) {
                    updateNotificationList();
                } else {
                    Log.wtf(TAG, "List of notifications changed while fragment doesn't have an Activity! ");
                }
                break;
        }
    }

    @Override
    public void requestBackgroundUpdate(Widget widget) { /* unused */ }

    @Override
    public void requestTimeoutRestart(Widget widget) { /* unused */ }

    public void unlock(Runnable runnable, boolean pendingFinish) {
        if (runnable != null) {
            runnable.run();
        }
    }

    @Override
    public void onPressedView(MotionEvent event, int activePointerId, View view) {
        mTouchHandler.removeCallbacksAndMessages(null);
        mHandler.removeMessages(MSG_RESET_SCENE);
        mPressedIconView = view;

        if (view != null) {
            final Widget widget = findWidgetByIcon(view);
            if (!isCurrentWidget(widget)) { // otherwise redundant
                mTouchHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showWidget(widget);
                    }
                }, event.getActionMasked() == MotionEvent.ACTION_DOWN
                        ? 0 : mConfigWidgetSelectDelay);
            }
        }
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
        boolean dismiss = false;

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // Track the velocity of movement, so we
                // can do swipe-to-dismiss.
                mVelocityTracker = VelocityTracker.obtain();
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                populateSwipeToDismiss(event);

                if (action != MotionEvent.ACTION_UP) {
                    return; // Don't fall down.
                }

                //noinspection LoopStatementThatDoesntLoop
                while (isDismissible(mSelectedWidget)) {
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
                        break; // Exits from loop not from the switch!
                    } else if (absDeltaY < height / 2) {
                        if (mMinFlingVelocity <= absVelocityY
                                && absVelocityY <= mMaxFlingVelocity
                                && absVelocityY > absVelocityX * 2
                                && absDeltaY > height / 5) {
                            // Dismiss only if flinging in the same direction as dragging
                            dismiss = (velocityY < 0) == (deltaY < 0);
                        }

                        if (!dismiss) {
                            break; // Exits from loop not from the switch!
                        }
                    }

                    dismiss = true;

                    if (height > absDeltaY) {
                        int duration;
                        duration = Math.round(1000f /* ms. */ * (height - absDeltaY) / absVelocityX);
                        duration = Math.min(duration, 300);

                        mSceneContainerDismissAnim.setup(duration,
                                MathUtils.range(deltaY / height, 0f, 1f),
                                mSelectedWidget);
                        mSceneContainer.startAnimation(mSceneContainerDismissAnim);
                        break; // Exits from loop not from the switch!
                    }

                    // Instant dismissing.
                    onWidgetDismiss(mSelectedWidget);
                    resetScene();
                    break;
                }

                // Don't not reset scene while dismissing, or if
                // pinnable.
                if (!dismiss) {
                    if (mPressedIconView == null || !isPinnable()) {
                        resetScene();
                    } else {
                        onWidgetPinned(mSelectedWidget);
                    }
                }
            case MotionEvent.ACTION_CANCEL:
                mTouchHandler.removeCallbacksAndMessages(null);
                mVelocityTracker.recycle();
                mVelocityTracker = null;

                if (action == MotionEvent.ACTION_CANCEL) {
                    resetScene();
                }
                break;
        }
    }

    protected boolean hasPinnedWidget() {
        return mHasPinnedWidget;
    }

    protected void onWidgetPinned(Widget widget) {
        mHandler.sendEmptyMessageDelayed(MSG_RESET_SCENE, mConfigWidgetPinDuration);
        mHasPinnedWidget = true;
    }

    protected void onWidgetReadAloud(Widget widget) {
        // TODO: Read widget aloud
    }

    /**
     * Called when widget is going to be dismissed.
     */
    protected void onWidgetDismiss(Widget widget) {
        widget.onDismiss();
        // TODO: Clear widget from different maps and layouts
    }

    /**
     * Resets {@link #mSceneContainer scene container}'s params and
     * {@link #showMainWidget() shows main widget}.
     *
     * @see #resetSceneContainerParams()
     */
    protected void resetScene() {
        resetSceneContainerParams();
        showMainWidget();
    }

    /**
     * Resets {@link #mSceneContainer scene container}'s params, such
     * as: animation, alpha level, translation, rotation etc.
     *
     * @see #resetScene()
     */
    private void resetSceneContainerParams() {
        mSceneContainer.clearAnimation();
        mSceneContainer.setAlpha(1f);
        mSceneContainer.setTranslationY(0);
        mSceneContainer.setRotationX(0);
    }

    private void populateSceneContainerDismissAnimation(float progress) {
        float height = getSceneView().getHeight();
        float y = height * progress;
        double degrees = Math.toDegrees(Math.acos((height - y) / height));

        mSceneContainer.setAlpha(1f - progress);
        mSceneContainer.setTranslationY(y);
        mSceneContainer.setRotationX((float) (-degrees / 2f));
    }

    private void populateSwipeToDismiss(MotionEvent srcEvent) {
        float y = srcEvent.getY() - mIconsContainer.getHeight();

        MotionEvent dstEvent = MotionEvent.obtainNoHistory(srcEvent);
        mVelocityTracker.addMovement(MotionEvent.obtainNoHistory(srcEvent));
        dstEvent.recycle();

        if (!isDismissible(mSelectedWidget)) {
            return;
        }

        if (y < 0) {
            if (mSceneContainer.getTranslationY() != 0) {
                resetSceneContainerParams();
            }
            return;
        }

        float height = getSceneView().getHeight();
        float progress = MathUtils.range(y / height, 0f, 1f);
        populateSceneContainerDismissAnimation(progress);

        if (Build.DEBUG) Log.d(TAG, "dismiss_progress=" + progress + " height=" + height);
    }

    /**
     * Shows main widget.
     *
     * @see #showWidget(com.achep.acdisplay.acdisplay.components.Widget)
     * @see #showWidget(com.achep.acdisplay.acdisplay.components.Widget, boolean)
     * @see #getCurrentWidget()
     */
    public void showMainWidget() {
        showWidget(null);
    }

    /**
     * @see #showMainWidget()
     * @see #showWidget(com.achep.acdisplay.acdisplay.components.Widget, boolean)
     * @see #getCurrentWidget()
     */
    protected void showWidget(Widget widget) {
        showWidget(widget, true);
    }

    /**
     * @see #showMainWidget()
     * @see #showWidget(com.achep.acdisplay.acdisplay.components.Widget)
     * @see #getCurrentWidget()
     */
    @SuppressLint("NewApi")
    protected void showWidget(Widget widget, boolean animate) {
        mHandler.removeMessages(MSG_RESET_SCENE);
        mHasPinnedWidget = false;

        if (mSelectedWidget != null) {
            if (mSelectedWidget.getIconView() != null) {
                mSelectedWidget.getIconView().setSelected(false);
            }

            mSelectedWidget.onViewDetached();
        }

        mSelectedWidget = widget;
        resetSceneContainerParams();

        if (mSelectedWidget == null) {
            goScene(getMainScene(), animate);
        } else {
            SceneCompat scene = findSceneByWidget(mSelectedWidget);
            if (scene == null) {

                // Widget doesn't have a large view, so
                // display clock.
                goScene(getMainScene(), animate);
            } else if (mCurrentScene != scene) {
                goScene(scene, animate);
            } else if (Device.hasKitKatApi() && animate) {
                ViewGroup viewGroup = mSelectedWidget.getView();
                if (viewGroup != null && viewGroup.isLaidOut()) {
                    // Automatically animate content change.
                    TransitionManager.beginDelayedTransition(viewGroup, mTransition);
                }
            }

            mSelectedWidget.onViewAttached();

            if (mSelectedWidget.getIconView() != null) {
                mSelectedWidget.getIconView().setSelected(true);
                mSelectedWidget.getIconView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            }
        }
    }

    /**
     * @return The widget to be shown on create, or {@code null} to
     * show main scene.
     */
    protected Widget getFirstWidget() {
        return null;
    }

    /**
     * @return Currently displayed widget, or {@code null} if main widget is displayed.
     * @see #showWidget(com.achep.acdisplay.acdisplay.components.Widget)
     * @see #showMainWidget()
     */
    protected final Widget getCurrentWidget() {
        return mSelectedWidget;
    }

    /**
     * @return {@code true} if current widget equals to given one, {@code false} otherwise.
     * @see #getCurrentWidget()
     */
    protected final boolean isCurrentWidget(Widget widget) {
        return widget == mSelectedWidget;
    }

    /**
     * @return {@code true} if widget is not {@code null} and
     * {@link Widget#isDismissible() dismissible}, {@code false} otherwise.
     */
    public final boolean isDismissible(Widget widget) {
        return widget != null && widget.isDismissible();
    }

    /**
     * Changes current scene to given one.
     *
     * @see #showWidget(com.achep.acdisplay.acdisplay.components.Widget)
     * @see #showMainWidget()
     */
    @SuppressLint("NewApi")
    protected final void goScene(SceneCompat sceneCompat, boolean transitions) {
        if (mCurrentScene != sceneCompat) {
            mCurrentScene = sceneCompat;
            if (transitions) {
                if (Device.hasKitKatApi()) {
                    try {
                        // This must be a synchronization problem with Android's Scene or TransitionManager,
                        // but those were declared as final classes, so I have no idea how to fix it.
                        TransitionManager.go(sceneCompat.scene, mTransition);
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "TransitionManager has failed switching scenes.");

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
                            throw new RuntimeException(e2);
                        }

                        TransitionManager.go(sceneCompat.scene, mTransition);
                    }
                } else {
                    sceneCompat.enter();

                    // Animate newly applied scene.
                    if (getActivity() != null) {
                        // TODO: Better animation for Jelly Bean users.
                        float density = getResources().getDisplayMetrics().density;
                        getSceneView().setAlpha(0.4f);
                        getSceneView().setRotationX(10f);
                        getSceneView().setTranslationY(10f * density);
                        getSceneView().animate().alpha(1).rotationX(0).translationY(0);
                    } else {
                        Log.w(TAG, "Changing scene when fragment is single!");
                    }
                }
            } else sceneCompat.enter();
        }
    }

    /**
     * @return The main scene with huge clock.
     */
    protected final SceneCompat getMainScene() {
        return mSceneMain;
    }

    /**
     * @return Currently displayed scene.
     * @see #goScene(com.achep.acdisplay.compat.SceneCompat, boolean)
     * @see #getMainScene()
     */
    protected final SceneCompat getScene() {
        return mCurrentScene;
    }

    protected final ViewGroup getSceneContainer() {
        return mSceneContainer;
    }

    /**
     * @return The view of the {@link #getScene() current scene}.
     * @see #getSceneContainer()
     * @see #getScene()
     */
    private View getSceneView() {
        return getScene().getView();
    }

    protected SceneCompat findSceneByWidget(Widget widget) {
        if (widget.getView() != null) {
            String className = widget.getClass().getName();
            return mScenesMap.get(className);
        }
        return null;
    }

    private Widget findWidgetByIcon(View view) {
        return mWidgetsMap.get(view);
    }

    /**
     * Updates the size of all widget's icons as
     * {@link com.achep.acdisplay.Config#getIconSizePx() set} in config.
     */
    private void updateIconsSize() {
        final int sizePx = Config.getInstance().getIconSizePx();
        final int childCount = mIconsContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mIconsContainer.getChildAt(i);
            updateIconSize(child, sizePx);
        }
    }

    private void updateIconSize(View view, int size) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.height = size;
        lp.width = size;
        view.setLayoutParams(lp);
    }

    /**
     * Updates the visibility of divider between
     * scenes and icons.
     */
    private void updateDividerVisibility() {
        ViewUtils.setVisible(getDividerView(), mIconsContainer.getChildCount() > 0);
    }

    // TODO: Optimize it
    // Spent hours on optimizing with no result: 0h
    private void updateNotificationList() {

        long now = SystemClock.elapsedRealtime();

        ViewGroup container = mIconsContainer;
        final int childCount = container.getChildCount();

        if (Device.hasKitKatApi()) {
            TransitionManager.beginDelayedTransition(container);
        }

        // Count the number of non-notification fragments
        // such as unlock or music controls fragments.
        int fragmentsExtraCount = 0;
        for (int i = 0; i < childCount; i++) {
            View child = container.getChildAt(i);
            Widget fragment = findWidgetByIcon(child);
            if (!(fragment instanceof NotifyWidget)) {
                fragmentsExtraCount++;
            } else {
                // Those fragments are placed at the begin of layout
                // so no reason to continue searching.
                break;
            }
        }

        final ArrayList<OpenNotification> list = NotificationPresenter.getInstance().getList();
        final int notifyCount = list.size();

        final boolean[] notifyUsed = new boolean[notifyCount];
        final boolean[] childUsed = new boolean[childCount];

        // ///////////////////
        // ~~ NOTIFICATIONS ~~
        // ///////////////////

        // Does not need an update
        for (int i = fragmentsExtraCount; i < childCount; i++) {
            View child = container.getChildAt(i);
            NotifyWidget widget = (NotifyWidget) findWidgetByIcon(child);
            OpenNotification target = widget.getNotification();

            // Try to find the notification with the same
            // id, tag and package name as in present.
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

                        NotifyWidget fragment = (NotifyWidget) findWidgetByIcon(child);
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
        Config config = Config.getInstance();
        int iconSize = config.getIconSizePx();

        for (int i = 0; i < notifyCount; i++) {
            if (notifyUsed[i]) continue;

            NotifyWidget fragment = new NotifyWidget(this, this);
            View view = fragment.createIconView(inflater, container);
            updateIconSize(view, iconSize);
            container.addView(view);

            fragment.setNotification(list.get(i));
            mWidgetsMap.put(view, fragment);
        }

        // ////////////
        // ~~ EXTRAS ~~
        // ////////////
/*
        int[] extras = new int[]{
                SCENE_MUSIC_CONTROLS,
        };

        extras[0] = -1;

        // Show unlock widget only if there's no any
        // other views.
        boolean empty = true;
        if (notifyCount > 0) {
            empty = false;
        } else for (int i = 1; i < extras.length; i++)
            if (extras[i] >= 0) {
                empty = false;
                break;
            }


        for (int i = fragmentsExtraCount - 1; i >= 0; i--) {
            View child = container.getChildAt(i);
            Widget fragment = findWidgetByIcon(child);

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
                View view = fragment.createIconView(inflater, container);
                container.addView(view, j++);
                mWidgetsMap.put(view, fragment);
            }
        }*/

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

        if (Build.DEBUG) {
            long delta = SystemClock.elapsedRealtime() - now;
            Log.d(TAG, "Fragment list updated in " + delta + "ms.");
        }

        updateDividerVisibility();
    }
}
