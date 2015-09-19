/*
 * Copyright (C) 2015 AChep@xda <artemchep@gmail.com>
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
package com.achep.acdisplay.notifications;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.achep.base.Build;
import com.achep.base.Device;
import com.achep.base.utils.Operator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.achep.acdisplay.notifications.NotificationPresenter.FLAG_IMMEDIATELY;

/**
 * Processes the notification flow, and re-transfers it through the
 * {@link Looper#getMainLooper() main thread}.
 *
 * @author Artem Chepurnoy
 */
class NotificationPrProxy {

    private static final String TAG = "NotificationPrProxy";

    private static final long DELAY = 400; // 0.4 sec.
    private static final long MAX_DELAY = DELAY * 5; // 2 sec.

    @NonNull
    private final Object mMonitor = new Object();
    @NonNull
    private final NotificationPresenter mPresenter;
    @NonNull
    private final Handler mHandler;
    @NonNull
    private final List<NotificationPrTask> mTasks;
    private final Runnable mProcessRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (mMonitor) {
                mStartTime = 0;
                mProcessing = true;
                optimizePrTasks(mTasks);
                sendPrTasks(mTasks);
                mTasks.clear();
                mProcessing = false;
            }
        }
    };

    private volatile long mStartTime;
    private volatile boolean mProcessing;

    public NotificationPrProxy(
            @NonNull NotificationPresenter presenter,
            @NonNull Looper looper) {
        mPresenter = presenter;
        mHandler = new Handler(looper);
        mTasks = new ArrayList<>(Device.hasLollipopApi()
                ? 15 /* multiply notifications levels have
                 introduced a much larger flow of notifications */
                : 4);
    }

    /**
     * Called on {@link NotificationPresenter#postNotification(Context, OpenNotification, int)}
     * (direct posting a notification).
     *
     * @see #onRemoved(OpenNotification)
     * @see #onClear()
     */
    void onPosted(@NonNull OpenNotification n) {
        onRemoveDuplicates(n);
    }

    public void postNotification(
            @NonNull Context context,
            @NonNull OpenNotification n, int flags) {
        synchronized (mMonitor) {
            boolean immediately = Operator.bitAnd(flags, FLAG_IMMEDIATELY);
            addTask(context, n, flags, true, immediately);
        }
    }

    /**
     * Called on {@link NotificationPresenter#removeNotification(OpenNotification, int)}
     * (direct removing a notification).
     *
     * @see #onPosted(OpenNotification) (OpenNotification)
     * @see #onClear()
     */
    void onRemoved(@NonNull OpenNotification n) {
        onRemoveDuplicates(n);
    }

    public void removeNotification(@NonNull OpenNotification n, int flags) {
        synchronized (mMonitor) {
            boolean immediately = Operator.bitAnd(flags, FLAG_IMMEDIATELY);
            addTask(null, n, flags, false, immediately);
        }
    }

    /**
     * Called on {@link NotificationPresenter#clear(boolean)}
     * (direct clean-up).
     *
     * @see #onPosted(OpenNotification)
     * @see #onRemoved(OpenNotification)
     */
    void onClear() {
        synchronized (mMonitor) {
            mStartTime = 0;
            mTasks.clear();
            mHandler.removeCallbacks(mProcessRunnable);
        }
    }

    private void addTask(@Nullable Context context, @NonNull OpenNotification notification,
                         int flags, boolean posts, boolean immediately) {
        mTasks.add(new NotificationPrTask(context, notification, posts, flags));
        // Do not allow an infinitive loop here.
        final long now = SystemClock.elapsedRealtime();
        if (mStartTime == 0) mStartTime = now;
        final long delta = now - mStartTime;
        // Delay the processing.
        mHandler.removeCallbacks(mProcessRunnable);
        mHandler.postDelayed(mProcessRunnable, immediately || delta > MAX_DELAY ? 0 : DELAY);
    }

    private void onRemoveDuplicates(@NonNull OpenNotification n) {
        synchronized (mMonitor) {
            if (!mProcessing) removeOverridingTasks(n);
        }
    }

    private void removeOverridingTasks(@NonNull OpenNotification n) {
        Iterator<NotificationPrTask> iterator = mTasks.iterator();
        while (iterator.hasNext()) {
            if (NotificationUtils.hasIdenticalIds(
                    iterator.next().notification,
                    n)) iterator.remove();
        }
    }

    /**
     * Optimize the {@link NotificationPrTask post/remove tasks} list by removing redundant
     * tasks and sorting families.
     */
    public void optimizePrTasks(@NonNull List<NotificationPrTask> list) {
        if (Build.DEBUG) Log.d(TAG, "Optimizing post/remove tasks... " + list.toString());
        int size = list.size();
        //noinspection ConstantConditions
        NotificationPrTask empty = new NotificationPrTask(null, null, false, 0);
        // 1. Remove overriding tasks.
        for (int i = size - 1; i >= 0; i--) {
            NotificationPrTask task = list.get(i);
            if (task == empty) continue;
            for (int j = i - 1; j >= 0; j--) {
                NotificationPrTask sub = list.get(j);
                if (sub == empty) continue;
                if (NotificationUtils.hasIdenticalIds(
                        task.notification,
                        sub.notification)) {
                    Log.i(TAG, "Removed overridden task on pre-processing tasks list. ");
                    list.set(j, empty);
                }
            }
        }
        // 2. Remove empty objects.
        Iterator<NotificationPrTask> iterator = list.iterator();
        while (iterator.hasNext()) {
            NotificationPrTask task = iterator.next();
            if (task == empty) iterator.remove();
        }
        size = list.size();
        // 3. Sort families.
        // FIXME: Check if it works correctly.
        for (int i = 0; i < size; i++) {
            NotificationPrTask task = list.get(i);
            if (task == empty || !task.notification.isGroupChild()) continue;
            for (int j = i + 1; j < size; j++) {
                NotificationPrTask sub = list.get(j);
                if (sub == empty || !sub.notification.isGroupSummary()) continue;
                String subGroupKey = sub.notification.getGroupKey();
                String taskGroupKey = task.notification.getGroupKey();
                assert taskGroupKey != null;
                if (taskGroupKey.equals(subGroupKey)) {
                    Log.d(TAG, "Swapped two tasks on pre-processing tasks list.");
                    // Swap two tasks.
                    list.set(j, task);
                    list.set(i, sub);
                    break;
                }
            }
        }
        // 4. Anything else?
        if (Build.DEBUG) Log.d(TAG, "Done optimizing post/remove tasks... " + list.toString());
    }

    /**
     * Materializes the tasks by {@link #postNotification(Context, OpenNotification, int) posting}
     * or {@link #removeNotification(OpenNotification, int) removing} appropriate notifications.
     */
    public void sendPrTasks(@NonNull List<NotificationPrTask> list) {
        for (NotificationPrTask task : list) {
            if (task.posts) {
                assert task.context != null;
                mPresenter.postNotification(task.context, task.notification, task.flags);
            } else {
                mPresenter.removeNotification(task.notification, task.flags);
            }
        }
    }

}
