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
package com.achep.activedisplay;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

import com.achep.activedisplay.utils.LogUtils;

import java.util.ArrayList;

/**
 * Created by Artem on 26.01.14.
 */
public class Timeout {

    private static final String TAG = "Timeout";

    public static final int EVENT_TIMEOUT = 0;
    public static final int EVENT_CHANGED = 1;
    public static final int EVENT_CLEARED = 2;

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

    private long mTimeoutAt;
    private boolean mLocked;

    public interface OnTimeoutEventListener {
        public void onTimeoutEvent(int event);
    }

    public final void addListener(OnTimeoutEventListener listener) {
        mListeners.add(listener);
    }

    public final void removeListener(OnTimeoutEventListener listener) {
        mListeners.remove(listener);
    }

    private void notifyOnEvent(int event) {
        LogUtils.track();
        for (OnTimeoutEventListener l : mListeners) {
            l.onTimeoutEvent(event);
        }
    }

    private void timeout() {
        notifyOnEvent(EVENT_TIMEOUT);
    }

    public void lock() {
        mLocked = true;
        clear();
    }

    public void release() {
        mLocked = false;
    }

    public void clear() {
        mTimeoutAt = 0;
        mHandler.removeMessages(0);
        notifyOnEvent(EVENT_CLEARED);
    }

    public void setTimeoutAt(long millis) {
        setTimeoutAt(millis, false);
    }

    public void setTimeoutDelayed(long delayMillis) {
        setTimeoutAt(SystemClock.uptimeMillis() + delayMillis);
    }

    public void setTimeoutDelayed(long delayMillis, boolean resetOld) {
        setTimeoutAt(SystemClock.uptimeMillis() + delayMillis, resetOld);
    }

    public void setTimeoutAt(long millis, boolean resetOld) {
        if (mTimeoutAt > 0 && mTimeoutAt < millis && !resetOld || mLocked) {
            return;
        }

        mTimeoutAt = millis;
        mHandler.removeMessages(0);
        mHandler.sendEmptyMessageAtTime(0, millis);

        notifyOnEvent(EVENT_CHANGED);
    }

    public long getRemainingTime() {
        return getTimeout() - SystemClock.uptimeMillis();
    }

    public long getTimeout() {
        return mTimeoutAt;
    }

}
