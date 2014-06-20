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

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
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

import com.achep.acdisplay.Build;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.Device;
import com.achep.acdisplay.R;
import com.achep.acdisplay.acdisplay.components.NotifyWidget;
import com.achep.acdisplay.acdisplay.components.Widget;
import com.achep.acdisplay.compat.SceneCompat;
import com.achep.acdisplay.notifications.NotificationPresenter;
import com.achep.acdisplay.notifications.NotificationUtils;
import com.achep.acdisplay.notifications.OpenNotification;
import com.achep.acdisplay.utils.MathUtils;
import com.achep.acdisplay.view.ForwardingLayout;
import com.achep.acdisplay.view.ForwardingListener;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is main fragment of ActiveDisplay app.
 */
public class AcDisplayFragment extends Fragment implements
        NotificationPresenter.OnNotificationListChangedListener,
        ForwardingLayout.OnForwardedEventListener, View.OnTouchListener,
        Widget.Callback, Config.OnConfigChangedListener {

    private static final String TAG = "AcDisplayFragment";

    private static final int MSG_RESET_SCENE = 0;

    private ForwardingLayout mSceneContainer;
    private ForwardingLayout mIconsContainer;
    private ForwardingListener mSceneForwardingListener;
    private ForwardingListener mIconsForwardingListener;
    private Handler mTouchHandler = new Handler();

    // Pinnable widgets
    private boolean mPinCanReadAloud = false;
    private Handler mPinHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_RESET_SCENE:
                    resetScene();
            }
        }
    };

    // Animations
    private AnimatorSet mSceneContainerPinAnim;

    // Swipe to dismiss
    private VelocityTracker mVelocityTracker;
    private int mMaxFlingVelocity;
    private int mMinFlingVelocity;

    private HashMap<View, Widget> mWidgetsMap = new HashMap<>();
    private HashMap<String, SceneCompat> mScenesMap = new HashMap<>();
    private Widget mSelectedWidget;
    private View mPressedIconView;

    private SceneCompat mCurrentScene;
    private SceneCompat mSceneMain;
    private Transition mTransition;

    private boolean isPinnable() {
        return Config.getInstance().isWidgetPinnable();
    }

    private boolean isReadable() {
        return Config.getInstance().isWidgetReadable();
    }

    protected int getViewResource() {
        return R.layout.acdisplay_fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        ViewConfiguration vc = ViewConfiguration.get(activity);
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();

        mSceneContainerPinAnim = (AnimatorSet) AnimatorInflater.loadAnimator(activity, R.animator.pin);
    }

    @TargetApi(android.os.Build.VERSION_CODES.KITKAT)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(getViewResource(), container, false);
        assert root != null;

        mSceneContainer = (ForwardingLayout) root.findViewById(R.id.scene);
        mIconsContainer = (ForwardingLayout) root.findViewById(R.id.list);
        mIconsContainer.setOnForwardedEventListener(this);
        mIconsContainer.setAllViewsForwardable(true, 0);
        mIconsContainer.setOnTouchListener(this);

        mSceneForwardingListener = new ForwardingListener(mIconsContainer, false, mSceneContainer);
        mIconsForwardingListener = new ForwardingListener(mIconsContainer, true, mIconsContainer);

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
        mPinHandler.removeCallbacksAndMessages(null);
        mTouchHandler.removeCallbacksAndMessages(null);

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
            case NotificationPresenter.EVENT_POSTED:
            case NotificationPresenter.EVENT_CHANGED:
            case NotificationPresenter.EVENT_REMOVED:
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
        mPressedIconView = view;

        if (view != null) {
            final boolean isTouchDown = event.getActionMasked() == MotionEvent.ACTION_DOWN;
            final Widget widget = findWidgetByIcon(view);
            if (mSelectedWidget != widget) { // otherwise redundant
                int delay = 0;
                if (!isTouchDown) {
                    delay = getResources().getInteger(R.integer.config_iconSelectDelayMillis);
                }

                mPinCanReadAloud = false;
                mTouchHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showWidget(widget);
                    }
                }, delay);
            } else if (mSelectedWidget != null) {
                // Speech engine
                mPinCanReadAloud = isTouchDown && mSelectedWidget.isReadable();
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v == mIconsContainer) {
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
                updateIconsSize();
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
                        float rotation = mSceneContainer.getRotationX() - 30;

                        // TODO: Make animation use #populateSceneContainerDismissAnimation(float) method.
                        final Widget widget = mSelectedWidget;
                        mSceneContainer.animate()
                                .alpha(0)
                                .rotationX(rotation)
                                .translationY(height)
                                .setDuration(duration)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        onAnimationCancel(animation);
                                        resetScene();
                                    }

                                    @Override
                                    public void onAnimationCancel(Animator animation) {
                                        onWidgetDismiss(widget);
                                        resetSceneContainerParams();
                                    }
                                });

                        break; // Exits from loop not from the switch!
                    }

                    // Instant dismissing.
                    onWidgetDismiss(mSelectedWidget);
                    resetScene();
                }

                // Don't not reset scene while dismissing, or if
                // pinnable.
                if (!dismiss) {
                    if (mPressedIconView == null || !isPinnable()){
                        resetScene();
                    } else {
                        onWidgetPinned(mSelectedWidget);

                        // TODO: Detect animation by Android API, not by delay.
                        long elapsedTime = event.getEventTime() - event.getDownTime();
                        if (elapsedTime > 150) {
                            mSceneContainerPinAnim.setTarget(getSceneView());
                            mSceneContainerPinAnim.start();
                        } else if (mPinCanReadAloud && isReadable()) {
                            onWidgetReadAloud(mSelectedWidget);
                        }
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

    protected boolean isWidgetPinned() {
        return mPinHandler.hasMessages(MSG_RESET_SCENE);
    }

    protected void onWidgetPinned(Widget widget) {
        mPinHandler.sendEmptyMessageDelayed(MSG_RESET_SCENE, 4000);
    }

    protected void onWidgetReadAloud(Widget widget) {
        // TODO: Read widget aloud
    }

    /**
     * Called when widget is going to be dismissed.
     */
    protected void onWidgetDismiss(Widget widget) {
        widget.onDismissed();
        // TODO: Clear widget from different maps and layouts
    }

    private View getSceneView() {
        return mSceneContainer.getChildAt(0);
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
     * as: alpha level, translation, rotation etc.
     *
     * @see #resetScene()
     */
    private void resetSceneContainerParams() {
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

    @SuppressLint("NewApi")
    protected void showWidget(Widget widget) {
        showWidget(widget, true);
    }

    @SuppressLint("NewApi")
    protected void showWidget(Widget widget, boolean animate) {
        mPinHandler.removeMessages(MSG_RESET_SCENE);

        if (mSelectedWidget != null) {
            if (mSelectedWidget.getCollapsedView() != null) {
                mSelectedWidget.getCollapsedView().setSelected(false);
            }

            mSelectedWidget.onExpandedViewDetached();
        }

        if ((mSelectedWidget = widget) == null) {
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
                ViewGroup viewGroup = mSelectedWidget.getExpandedView();
                if (viewGroup != null && viewGroup.isLaidOut()) {

                    // Automatically animate content change.
                    beginDelayedTransition(viewGroup, mTransition);
                }
            }

            mSelectedWidget.onExpandedViewAttached();

            if (mSelectedWidget.getCollapsedView() != null) {
                mSelectedWidget.getCollapsedView().setSelected(true);
                mSelectedWidget.getCollapsedView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            }
        }
    }

    @TargetApi(android.os.Build.VERSION_CODES.KITKAT)
    private static void beginDelayedTransition(ViewGroup viewGroup) {
        beginDelayedTransition(viewGroup, null);
    }

    @TargetApi(android.os.Build.VERSION_CODES.KITKAT)
    private static void beginDelayedTransition(ViewGroup viewGroup, Transition transition) {
        TransitionManager.beginDelayedTransition(viewGroup, transition);
    }

    /**
     * Shows main clock widget.<br/>
     * Same as calling {@code showWidget(null)}.
     *
     * @see #showWidget(com.achep.acdisplay.acdisplay.components.Widget)
     */
    public void showMainWidget() {
        showWidget(null);
    }

    /**
     * @return The widget to be shown on create, or {@code null} to
     * show main scene.
     */
    protected Widget getFirstWidget() {
        return null;
    }

    /**
     * @return Currently displayed widget, or {@code null} if on main widget.
     * @see #showWidget(com.achep.acdisplay.acdisplay.components.Widget)
     * @see #showMainWidget()
     */
    protected final Widget getCurrentWidget() {
        return mSelectedWidget;
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
                    TransitionManager.go(sceneCompat.scene, mTransition);
                } else {
                    // TODO: Better animation for Jelly Bean users.
                    sceneCompat.enter();

                    float density = getResources().getDisplayMetrics().density;
                    sceneCompat.getView().setAlpha(0.4f);
                    sceneCompat.getView().setRotationX(10f);
                    sceneCompat.getView().setTranslationY(10f * density);
                    sceneCompat.getView().animate().alpha(1).rotationX(0).translationY(0);
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
    protected final SceneCompat getCurrentScene() {
        return mCurrentScene;
    }

    protected final ViewGroup getSceneContainer() {
        return mSceneContainer;
    }

    /**
     * @return {@code true} if widget is not null and
     * {@link Widget#isDismissible() dismissible}, {@code false} otherwise.
     */
    public final boolean isDismissible(Widget widget) {
        return widget != null && widget.isDismissible();
    }

    protected SceneCompat findSceneByWidget(Widget widget) {
        if (widget.hasExpandedView()) {
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

    // TODO: Optimize it
    // Spent hours on optimizing with no result: 0h
    private void updateNotificationList() {

        long now = SystemClock.elapsedRealtime();

        ViewGroup container = mIconsContainer;
        final int childCount = container.getChildCount();

        if (Device.hasKitKatApi()) {
            beginDelayedTransition(container);
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
                if (NotificationUtils.equals(target, n)) {

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
            View view = fragment.createCollapsedView(inflater, container);
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
                View view = fragment.createCollapsedView(inflater, container);
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

        if (Build.DEBUG) {
            long delta = SystemClock.elapsedRealtime() - now;
            Log.d(TAG, "Fragment list updated in " + delta + "ms.");
        }
    }
}
