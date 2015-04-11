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
package com.achep.acdisplay.supervisor;

import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;

import com.achep.acdisplay.ui.animations.ProgressBarAnimation;
import com.achep.base.async.WeakHandler;
import com.achep.base.interfaces.ISubscriptable;
import com.achep.base.tests.Check;
import com.achep.base.utils.MathUtils;

import java.util.ArrayList;

/**
 * @author Artem Chepurnoy
 */
public class Timeout implements ISubscriptable<Timeout.OnTimeoutEventListener> {

    private static final String TAG = "Timeout";

    public static final int EVENT_TIMEOUT = 0;
    public static final int EVENT_SET = 1;
    public static final int EVENT_CLEARED = 2;
    public static final int EVENT_PAUSED = 3;
    public static final int EVENT_RESUMED = 4;

    public interface OnTimeoutEventListener {

        void onTimeoutEvent(@NonNull Timeout timeout, int event);

    }

    private static final int SET = 0;
    private static final int RESUME = 1;
    private static final int PAUSE = 2;
    private static final int CLEAR = 3;
    private static final int TIMEOUT = 4;

    private final ArrayList<OnTimeoutEventListener> mListeners = new ArrayList<>(3);
    private final H mHandler = new H(this);

    private long mTimeoutPausedAt;
    private long mTimeoutDuration;
    private long mTimeoutAt;

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerListener(@NonNull OnTimeoutEventListener listener) {
        synchronized (this) {
            mListeners.add(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterListener(@NonNull OnTimeoutEventListener listener) {
        synchronized (this) {
            mListeners.remove(listener);
        }
    }

    private void notifyListeners(final int event) {
        synchronized (this) {
            for (OnTimeoutEventListener l : mListeners) l.onTimeoutEvent(this, event);
        }
    }

    //-- MAIN -----------------------------------------------------------------

    /**
     * Same as calling {@link #set(int, boolean)} with {@code override = false}.
     *
     * @see #set(int, boolean)
     * @see #resume()
     * @see #clear()
     */
    public void set(final int delay) {
        set(delay, false);
    }

    /**
     * Configures the timeout.
     *
     * @param override {@code true} to rewrite previous timeout\'s time, {@code false} to
     *                 set the nearest one.
     * @see #set(int)
     * @see #resume()
     * @see #clear()
     */
    public void set(final int delay, boolean override) {
        Message msg = Message.obtain(mHandler, SET, delay, MathUtils.bool(override));
        mHandler.sendMessage(msg);
    }

    /**
     * Pauses the timeout.
     *
     * @see #resume()
     */
    public void pause() {
        mHandler.sendEmptyMessage(PAUSE);
    }

    /**
     * Resumes the timeout (does nothing if the timeout if cleared).
     *
     * @see #set(int, boolean)
     * @see #pause()
     * @see #clear()
     */
    public void resume() {
        mHandler.sendEmptyMessage(RESUME);
    }

    /**
     * Clears the timeout.
     *
     * @see #set(int, boolean)
     * @see #pause()
     */
    public void clear() {
        mHandler.sendEmptyMessage(CLEAR);
    }

    //-- OTHER -----------------------------------------------------------------

    private long uptimeMillis() {
        return SystemClock.uptimeMillis();
    }

    private void checkThread() {
        final Thread handlerThread = mHandler.getLooper().getThread();
        final Thread currentThread = Thread.currentThread();
        Check.getInstance().isTrue(handlerThread.equals(currentThread));
    }

    //-- HANDLER --------------------------------------------------------------

    /**
     * @author Artem Chepurnoy
     */
    private static class H extends WeakHandler<Timeout> {

        public H(@NonNull Timeout object) {
            super(object);
        }

        @Override
        protected void onHandleMassage(@NonNull Timeout timeout, Message msg) {
            switch (msg.what) {
                case SET:
                    timeout.internalSet(msg.arg1, msg.arg2 != 0);
                    break;
                case RESUME:
                    timeout.internalResume();
                    break;
                case PAUSE:
                    timeout.internalPause();
                    break;
                case CLEAR:
                    timeout.internalClear();
                    break;
                case TIMEOUT:
                    timeout.internalTimeout();
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }

    }

    private void internalSet(long delayMillis, boolean resetOld) {
        checkThread();
        final boolean isPaused = mTimeoutPausedAt != 0;
        final long timeoutAt = uptimeMillis() + delayMillis;
        final long timeoutAtOld = mTimeoutAt + (isPaused
                ? uptimeMillis() - mTimeoutPausedAt
                : 0);

        if (mTimeoutAt == 0 || timeoutAtOld > timeoutAt || resetOld) {
            mTimeoutDuration = delayMillis;
            mTimeoutAt = timeoutAt;

            if (isPaused) {
                mTimeoutPausedAt = uptimeMillis();
                notifyListeners(EVENT_SET);
            } else {
                notifyListeners(EVENT_SET);
                mHandler.removeMessages(TIMEOUT);
                mHandler.sendEmptyMessageAtTime(TIMEOUT, mTimeoutAt);
            }
        }
    }

    private void internalResume() {
        if (mTimeoutPausedAt != 0) {
            checkThread();

            final long pausedAt = mTimeoutPausedAt;
            mTimeoutPausedAt = 0;

            if (mTimeoutAt > 0) {
                long delta = uptimeMillis() - pausedAt;
                mTimeoutAt += delta;
                mHandler.sendEmptyMessageAtTime(TIMEOUT, mTimeoutAt);
                notifyListeners(EVENT_RESUMED);
            }
        }
    }

    private void internalPause() {
        if (mTimeoutPausedAt == 0) {
            checkThread();
            mTimeoutPausedAt = uptimeMillis();
            mHandler.removeMessages(TIMEOUT);
            notifyListeners(EVENT_PAUSED);
        }
    }

    private void internalClear() {
        checkThread();
        mTimeoutAt = 0;
        mTimeoutDuration = 0;
        mTimeoutPausedAt = 0;
        mHandler.removeMessages(TIMEOUT);
        notifyListeners(EVENT_CLEARED);
    }

    private void internalTimeout() {
        checkThread();
        mTimeoutAt = 0;
        mTimeoutDuration = 0;
        Check.getInstance().isTrue(mTimeoutPausedAt == 0);
        notifyListeners(EVENT_TIMEOUT);
    }

    //-- GUI ------------------------------------------------------------------

    /**
     * @author Artem Chepurnoy
     */
    public static class Gui implements OnTimeoutEventListener {

        private static final int MAX = 300;

        private final ProgressBarAnimation mProgressBarAnimation;
        private final ProgressBar mProgressBar;

        public Gui(@NonNull ProgressBar progressBar) {
            mProgressBar = progressBar;
            mProgressBar.setMax(MAX);
            mProgressBar.setProgress(MAX);
            mProgressBarAnimation = new ProgressBarAnimation(mProgressBar, MAX, 0);
            mProgressBarAnimation.setInterpolator(new LinearInterpolator());
        }

        @Override
        public void onTimeoutEvent(@NonNull Timeout timeout, int event) {
            // TODO: Write the timeout's gui
        }
    }

}
