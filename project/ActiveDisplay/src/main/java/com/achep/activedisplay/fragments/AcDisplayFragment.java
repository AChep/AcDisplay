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
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.Log;
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
import com.achep.activedisplay.compat.SceneCompat;
import com.achep.activedisplay.fragments.components.MusicFragment;
import com.achep.activedisplay.fragments.components.NotificationFragment;
import com.achep.activedisplay.fragments.components.UnlockFragment;
import com.achep.activedisplay.notifications.NotificationPresenter;
import com.achep.activedisplay.notifications.NotificationUtils;
import com.achep.activedisplay.notifications.OpenStatusBarNotification;
import com.achep.activedisplay.utils.BitmapUtils;
import com.achep.activedisplay.utils.ViewUtils;
import com.achep.activedisplay.view.ForwardingLayout;
import com.achep.activedisplay.view.ForwardingListener;
import com.achep.activedisplay.widgets.ProgressBar;

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

    private ForwardingLayout mSceneContainer;
    private LinearLayout mCollapsedViewsContainer;
    private HashMap<View, Widget> mWidgetsMap = new HashMap<>();
    private HashMap<Integer, SceneCompat> mScenesMap = new HashMap<>();
    private boolean mCollapsedViewsNeedsUpdate;
    private ImageView mCollapsedUnlockFake;

    private Widget mSelectedWidget;

    private SceneCompat mCurrentScene;
    private SceneCompat mSceneMain;
    private Transition mTransition;

    private boolean mTouched;

    private Handler mHandler = new Handler();
    private SelectWidgetRunnable mSelectWidgetRunnable = new SelectWidgetRunnable();

    private Timeout mTimeout;
    private Timeout.Gui mTimeoutGui;
    private ForwardingListener mForwardingListener;

    public void unlock(Runnable runnable, boolean finish) {
        Activity activity = getActivity();
        if (activity instanceof KeyguardActivity) {
            KeyguardActivity keyguard = (KeyguardActivity) activity;
            keyguard.unlock(runnable, finish);
        } else {
            if (runnable != null) {
                runnable.run();
            }
        }
    }

    public void dispatchSetBackground(Bitmap bitmap) {
        Activity activity = getActivity();
        if (activity instanceof AcDisplayActivity) {
            AcDisplayActivity acDisplayActivity = (AcDisplayActivity) activity;
            acDisplayActivity.dispatchSetBackground(bitmap);
        }
    }

    public Config getConfig() {
        return mConfig;
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

        mSceneContainer = (ForwardingLayout) root.findViewById(R.id.container);
        mSceneContainer.setVibrateOnForwardedEventEnabled(true);
        mCollapsedUnlockFake = (ImageView) root.findViewById(R.id.unlock_icon);
        mCollapsedViewsContainer = (LinearLayout) root.findViewById(R.id.list);
        mCollapsedViewsContainer.setOnTouchListener(this);

        mForwardingListener = new ForwardingListener(mCollapsedViewsContainer) {
            @Override
            public ForwardingLayout getForwardingLayout() {
                return mSceneContainer;
            }
        };

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

        Activity activity = getActivity();
        if (activity instanceof AcDisplayActivity) {
            mTimeoutGui = new Timeout.Gui(progressBar);

            AcDisplayActivity a = (AcDisplayActivity) activity;
            mTimeout = a.getTimeout();
            mTimeout.registerListener(mTimeoutGui);
        } else {
            mTimeout = new Timeout(); // fake timeout that does nothing
            progressBar.setProgress(progressBar.getMax());
        }

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        Context context = getActivity();
        assert context != null;

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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mTimeout != null) {
            mTimeout.unregisterListener(mTimeoutGui);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        final float rawX = event.getRawX();
        final float rawY = event.getRawY();

        if (v == mCollapsedViewsContainer) {
            mForwardingListener.onTouch(mCollapsedViewsContainer, event);

            boolean touchDown = false;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchDown = true;
                    mTimeout.setTimeoutDelayed(mConfig.getTimeoutShort(), true);
                    mTimeout.pause();
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

                    break;
                case MotionEvent.ACTION_UP:

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

                    showMainWidget();

                    if (mCollapsedViewsNeedsUpdate) updateNotificationList();
                    if (mTimeout != null) {
                        mTimeout.resume();
                    }

                    mTouched = false;
                    mCollapsedViewsNeedsUpdate = false;
                    break;
            }
            return true;
        }
        return false;
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
        if (getActivity() == null) {
            return;
        }

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

        ViewUtils.setVisible(mCollapsedUnlockFake, empty);

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
     * Base class of {@link AcDisplayFragment} widgets.
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
         * @return an instance of {@link AcDisplayFragment}.
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
     * Factory to prepare your background for
     * {@link com.achep.activedisplay.fragments.AcDisplayFragment#dispatchSetBackground(android.graphics.Bitmap)}.
     */
    public static class BackgroundFactoryThread extends AsyncTask<Void, Void, Bitmap> {

        private static final String TAG = "DynamicBackgroundFactory";

        public interface Callback {
            void onBackgroundCreated(Bitmap bitmap);
        }

        private final int mForegroundColor;
        private final Bitmap mBitmapOriginal;
        private final Callback mCallback;

        public BackgroundFactoryThread(Context context, Bitmap original, Callback callback) {
            mForegroundColor = context.getResources().getColor(R.color.keyguard_background_semi);
            mBitmapOriginal = original;
            mCallback = callback;

            if (original == null) throw new IllegalArgumentException("Bitmap may not be null!");
            if (callback == null) throw new IllegalArgumentException("Callback may not be null!");
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            final long start = SystemClock.elapsedRealtime();

            Bitmap origin = mBitmapOriginal;
            Bitmap bitmap = BitmapUtils.doBlur(origin, 3, false);

            if (!running) {
                return null;
            }

            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(mForegroundColor);

            if (Project.DEBUG) {
                long delta = SystemClock.elapsedRealtime() - start;
                Log.d(TAG, "Dynamic background created in " + delta + " millis:"
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
