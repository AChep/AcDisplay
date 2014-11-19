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

import android.app.Activity;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.acdisplay.Timeout;
import com.achep.acdisplay.acdisplay.components.MediaWidget;
import com.achep.acdisplay.acdisplay.components.NotifyWidget;
import com.achep.acdisplay.acdisplay.components.Widget;
import com.achep.acdisplay.compat.SceneCompat;
import com.achep.acdisplay.notifications.NotificationPresenter;
import com.achep.acdisplay.services.media.MediaController;
import com.achep.acdisplay.utils.ViewUtils;
import com.achep.acdisplay.widgets.CircleView;
import com.achep.acdisplay.widgets.ProgressBar;

/**
 * This is main fragment of ActiveDisplay app.
 */
public class AcDisplayFragment2 extends AcDisplayFragment implements
        CircleView.Callback, MediaController.MediaListener {

    private AcDisplayActivity mActivity;
    private Config mConfig;

    private Timeout mTimeout;
    private Timeout.Gui mTimeoutGui;

    private View mDividerView;
    private CircleView mCircleView;

    private PulsingThread pulsingThread;

    // Media widget
    private MediaController mMediaController;
    private MediaWidget mMediaWidget;
    private SceneCompat mSceneMainMedia;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (AcDisplayActivity) activity;

        mMediaController = mActivity.getCustomMediaController();
        mMediaController.registerListener(this);

        mConfig = mActivity.getConfig();
        mTimeout = mActivity.getTimeout();

        pulsingThread = new PulsingThread(activity.getContentResolver());
        if (Config.getInstance().isPulsingNotificationAnimationEnabled()) {
            pulsingThread.start();
        }

    }

    @Override
    public View getDividerView() {
        return mDividerView;
    }

    @Override
    protected int getViewResource() {
        return R.layout.fragment_acdisplay;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        assert root != null;
        root.setOnTouchListener(new View.OnTouchListener() {

            private boolean transferring;
            private GestureDetector gestureDetector =
                    new GestureDetector(mActivity, new GestureListener());

            class GestureListener extends GestureDetector.SimpleOnGestureListener {

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return false;
                }

                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    return mActivity.lock();
                }
            }

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    float x = event.getX();
                    float y = event.getY();
                    int padding = 20;
                    transferring = ViewUtils.isTouchPointInView(v, x - padding, y - padding)
                            && ViewUtils.isTouchPointInView(v, x + padding, y + padding);
                }
                if (transferring) {
                    mCircleView.onTouchEvent2(event);
                }
                return transferring;
            }
        });

        View divider = root.findViewById(R.id.divider);
        ViewGroup vg = (ViewGroup) divider.getParent();

        int position = 0;
        int length = vg.getChildCount();
        for (int i = 0; i < length; i++) {
            View child = vg.getChildAt(i);
            assert child != null;

            if (child.getId() == divider.getId()) {
                position = i;
                break;
            }
        }

        boolean mirrored = mConfig.isMirroredTimeoutProgressBarEnabled();
        mDividerView = inflater.inflate(mirrored
                ? R.layout.acdisplay_progress_bar_mirrored
                : R.layout.acdisplay_progress_bar, vg, false);
        vg.removeViewAt(position);
        vg.addView(mDividerView, position);
        ProgressBar progressBar = (ProgressBar) mDividerView.findViewById(R.id.progress_bar);
        if (mirrored) {
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
        }
        mTimeoutGui = new Timeout.Gui(progressBar);
        mTimeout.registerListener(mTimeoutGui);

        mCircleView = (CircleView) root.findViewById(R.id.circle);
        mCircleView.setCallback(this);

        mMediaWidget = new MediaWidget(this, this);
        mMediaWidget.onCreate();
        ViewGroup sceneContainer = getSceneContainer();
        ViewGroup sceneMainMusic = mMediaWidget.createView(inflater, sceneContainer, null);
        mSceneMainMedia = new SceneCompat(sceneContainer, sceneMainMusic);

        return root;
    }

    @Override
    public void onDestroyView() {
        pulsingThread.requestStop();
        mMediaWidget.onDestroy();
        mTimeout.unregisterListener(mTimeoutGui);
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        mMediaController.unregisterListener(this);
        super.onDetach();
    }

    @Override
    public void onCircleEvent(float radius, float ratio, int event) {
        // if user touches display, stop pulsing
        pulsingThread.requestStop();

        switch (event) {
            case CircleView.ACTION_START:
                if (hasPinnedWidget()) {
                    resetScene();
                }

                mTimeout.setTimeoutDelayed(mConfig.getTimeoutShort());
                mTimeout.pause();
                break;
            case CircleView.ACTION_UNLOCK_START:
                mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                break;
            case CircleView.ACTION_UNLOCK_CANCEL:
                mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                break;
            case CircleView.ACTION_UNLOCK:
                mActivity.unlock(null);
            case CircleView.ACTION_CANCELED:
                mTimeout.resume();

                int delta = (int) (2200 - mTimeout.getRemainingTime());
                if (delta > 0) {
                    mTimeout.delay(delta);
                }
                break;
        }
    }

    public MediaController getCustomMediaController() {
        return mActivity.getCustomMediaController();
    }

    /**
     *
     */
    @Override
    public void unlock(Runnable runnable, boolean pendingFinish) {
        mActivity.unlockWithPendingFinish(runnable);
    }

    /**
     * Updates dynamic background as requested by widget.
     */
    @Override
    public void requestBackgroundUpdate(Widget widget) {
        // Allow changing background to current widget only.
        if (isCurrentWidget(widget)) {
            mActivity.dispatchSetBackground(
                    widget.getBackground(),
                    widget.getBackgroundMask());
        }
    }

    /**
     * Restarts timeout to {@link com.achep.acdisplay.Config#getTimeoutShort()}
     * as requested by widget.
     */
    @Override
    public void requestTimeoutRestart(Widget widget) {
        if (isCurrentWidget(widget)) {
            mTimeout.setTimeoutDelayed(mConfig.getTimeoutShort(), true);
        }
    }

    @Override
    protected void showWidget(Widget widget) {
        super.showWidget(widget);

        // Add support for dynamic background
        if (widget == null) {
            mActivity.dispatchClearBackground();
        } else {
            mActivity.dispatchSetBackground(
                    widget.getBackground(),
                    widget.getBackgroundMask());
        }

        // Start timeout on main or media widgets, and
        // pause it otherwise.
        if (widget == null || widget == mMediaWidget) {
            mTimeout.resume();
        } else {
            mTimeout.setTimeoutDelayed(mConfig.getTimeoutNormal(), true);
            mTimeout.pause();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onWidgetDismiss(Widget widget) {
        boolean lock = false;

        // Option: Screen off on dismiss last notification.
        if (mConfig.isScreenOffAfterLastNotify() && widget instanceof NotifyWidget) {
            NotificationPresenter np = NotificationPresenter.getInstance();
            lock = np.getList().size() <= 1;
        }

        super.onWidgetDismiss(widget);

        if (lock) {
            mActivity.lock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMediaChanged(MediaController controller, int event) {
        switch (event) {
            case MediaController.EVENT_UISTATE_CHANGED:
                updateUiState(controller.getUiState());
                break;
        }
    }

    /**
     * Shows or hides media widget.
     *
     * @see #isUiStateMedia()
     */
    private void updateUiState(int currentUiState) {
        Widget currentWidget = getCurrentWidget();
        switch (currentUiState) {
            case MediaController.UISTATE_MUSIC:
                if (currentWidget == null) {
                    showWidget(mMediaWidget);
                }
                break;
            case MediaController.UISTATE_NORMAL:
                if (currentWidget == mMediaWidget) {
                    showMainWidget();
                }
                break;
        }
    }

    /**
     * @return {@code true} is media widget may be shown, {@code false} otherwise.
     * @see com.achep.acdisplay.services.media.MediaController#getUiState()
     */
    private boolean isUiStateMedia() {
        int state = mMediaController.getUiState();
        return state == MediaController.UISTATE_MUSIC;
    }

    @Override
    public void showMainWidget() {
        if (isUiStateMedia()) {
            showWidget(mMediaWidget);
            return;
        }

        super.showMainWidget();
    }

    @Override
    protected Widget getFirstWidget() {
        return isUiStateMedia() ? mMediaWidget : super.getFirstWidget();
    }

    @Override
    protected SceneCompat findSceneByWidget(Widget widget) {
        // Manually add media widget's scene
        return widget == mMediaWidget ? mSceneMainMedia : super.findSceneByWidget(widget);
    }
}
