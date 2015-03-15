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
package com.achep.acdisplay;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;

import com.achep.acdisplay.ui.animations.ProgressBarAnimation;
import com.achep.base.utils.MathUtils;

import java.util.ArrayList;

/**
 * Created by Artem on 26.01.14.
 *
 * @author Artem Chepurnoy
 */
public class Timeout {

    private static final String TAG = "Timeout";

    public static final int EVENT_TIMEOUT = 0;
    public static final int EVENT_CHANGED = 1;
    public static final int EVENT_CLEARED = 2;
    public static final int EVENT_PAUSED = 3;
    public static final int EVENT_RESUMED = 4;

    private final ArrayList<OnTimeoutEventListener> mListeners = new ArrayList<>();
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                default:
                    timeout();
                    break;
            }
        }
    };

    private long mTimeoutLockedAt;
    private long mTimeoutStart;
    private long mTimeoutAt;

    public interface OnTimeoutEventListener {
        void onTimeoutEvent(Timeout timeout, int event);
    }

    public final void registerListener(OnTimeoutEventListener listener) {
        mListeners.add(listener);
    }

    public final void unregisterListener(OnTimeoutEventListener listener) {
        mListeners.remove(listener);
    }

    private void notifyListeners(int event) {
        for (OnTimeoutEventListener l : mListeners) {
            l.onTimeoutEvent(this, event);
        }
    }

    private long getTime() {
        return SystemClock.uptimeMillis();
    }

    private void timeout() {
        notifyListeners(EVENT_TIMEOUT);
        mTimeoutAt = 0;
    }

    /**
     * Pauses running timer and sends {@link #EVENT_PAUSED}.
     * While it's paused all methods except of
     * {@link #resume() resuming} will be ignored!
     *
     * @see #resume()
     * @see #EVENT_PAUSED
     */
    public void pause() {
        if (!isPaused()) {
            mTimeoutLockedAt = getTime();
            mHandler.removeMessages(0);
            notifyListeners(EVENT_PAUSED);
        }
    }

    public void resume() {
        if (!isPaused()) {
            return;
        }

        long offset = getTime() - mTimeoutLockedAt;

        mTimeoutLockedAt = 0;

        if (mTimeoutAt > 0) {
            mTimeoutStart += offset;
            mTimeoutAt += offset;

            mHandler.sendEmptyMessageAtTime(0, mTimeoutAt);
        }

        notifyListeners(EVENT_RESUMED);
    }

    public void clear() {
        if (!isPaused()) {
            mTimeoutAt = 0;
            mHandler.removeMessages(0);
            notifyListeners(EVENT_CLEARED);
        }
    }

    /**
     * If timer isn't {@link #pause() paused} delays current
     * timeout.
     *
     * @param delayMillis millis to delay
     * @see #pause()
     * @see #EVENT_CHANGED
     */
    public void delay(long delayMillis) {
        if (!isPaused() && isOngoing()) {
            mTimeoutStart += delayMillis;
            mTimeoutAt += delayMillis;

            mHandler.removeMessages(0);
            mHandler.sendEmptyMessageAtTime(0, mTimeoutAt);

            notifyListeners(EVENT_CHANGED);
        }
    }

    /**
     * @return True if countdown timer is active at this moment, False otherwise.
     */
    public boolean isOngoing() {
        return mTimeoutAt > 0;
    }

    /**
     * @return True if countdown timer is paused, False otherwise.
     */
    public boolean isPaused() {
        return mTimeoutLockedAt > 0;
    }

    public void setTimeoutDelayed(long delayMillis) {
        setTimeoutDelayed(delayMillis, false);
    }

    public void setTimeoutDelayed(long delayMillis, boolean resetOld) {
        long timeoutStart = getTime();
        long timeoutAt = timeoutStart + delayMillis;

        if (isOngoing() && mTimeoutAt < timeoutAt && !resetOld || isPaused()) {
            return;
        }

        mTimeoutStart = timeoutStart;
        mTimeoutAt = timeoutAt;

        mHandler.removeMessages(0);
        mHandler.sendEmptyMessageAtTime(0, mTimeoutAt);

        notifyListeners(EVENT_CHANGED);
    }

    private long getTimeoutNow() {
        return isPaused() ? mTimeoutLockedAt : getTime();
    }

    public long getRemainingTime() {
        return mTimeoutAt - getTimeoutNow();
    }

    public float getProgress() {
        float max = mTimeoutAt - mTimeoutStart;
        float now = getRemainingTime();
        return MathUtils.range(1f - now / max, 0, 1);
    }

    /**
     * Displays timeout's events in given {@link com.achep.acdisplay.ui.widgets.ProgressBar}.
     */
    public static class Gui implements Timeout.OnTimeoutEventListener {

        private static final int MAX = 300;

        private final ProgressBarAnimation mProgressBarAnimation;
        private final ProgressBar mProgressBar;

        public Gui(ProgressBar progressBar) {
            mProgressBar = progressBar;
            mProgressBar.setMax(MAX);
            mProgressBar.setProgress(MAX);
            mProgressBarAnimation = new ProgressBarAnimation(mProgressBar, MAX, 0);
            mProgressBarAnimation.setInterpolator(new LinearInterpolator());
        }

        @Override
        public void onTimeoutEvent(Timeout timeout, int event) {
            switch (event) {
                case Timeout.EVENT_PAUSED:
                    mProgressBar.clearAnimation();
                    break;
                case Timeout.EVENT_RESUMED:
                case Timeout.EVENT_CHANGED:
                    long remainingTime = timeout.getRemainingTime();
                    if (remainingTime > 0 && !timeout.isPaused()) {
                        int progress = (int) (
                                mProgressBar.getMax() * (1f - timeout.getProgress())
                        );
                        mProgressBarAnimation.setRange(progress, 0);
                        mProgressBarAnimation.setDuration(remainingTime);
                        mProgressBar.setProgress(progress);
                        mProgressBar.startAnimation(mProgressBarAnimation);
                    }
                    break;
                case Timeout.EVENT_CLEARED:
                    mProgressBar.clearAnimation();
                    mProgressBar.setProgress(mProgressBar.getMax());
                    break;
            }
        }
    }

}
