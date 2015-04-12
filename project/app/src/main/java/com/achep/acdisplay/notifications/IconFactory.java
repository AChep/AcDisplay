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
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.achep.base.tests.Check;

import java.util.concurrent.ConcurrentLinkedQueue;

import static com.achep.acdisplay.graphics.IconFactory.generate;
import static com.achep.base.Build.DEBUG;

/**
 * Simple single-thread icon factory.
 *
 * @author Artem Chepurnoy
 */
public class IconFactory {

    private static final String TAG = "IconFactory";

    public interface IconAsyncListener {
        void onGenerated(@NonNull Bitmap bitmap);
    }

    /**
     * @author Artem Chepurnoy
     */
    private static final class Worker extends Thread {

        private static class Task {
            @NonNull
            private Context context;
            @NonNull
            private IconAsyncListener listener;
            @NonNull
            private OpenNotification notification;

            public Task(@NonNull Context context,
                        @NonNull IconAsyncListener listener,
                        @NonNull OpenNotification notification) {
                this.context = context;
                this.listener = listener;
                this.notification = notification;
            }
        }

        private final ConcurrentLinkedQueue<Task> mQueue = new ConcurrentLinkedQueue<>();
        private final IconFactory mFactory;
        private final Object mMonitor;

        private volatile boolean mStopping;

        Worker(IconFactory factory, @NonNull Object monitor) {
            mFactory = factory;
            mMonitor = monitor;
        }

        @Override
        public void run() {
            super.run();
            final long start = SystemClock.elapsedRealtime();
            int n = 0;
            while (true) {
                // Get the next task from
                // queue.
                final Task task;
                synchronized (mMonitor) {
                    if (mQueue.isEmpty()) {
                        mStopping = true;
                        if (DEBUG) {
                            long delta = SystemClock.elapsedRealtime() - start;
                            Log.d(TAG, "Done loading icons: "
                                    + " delta=" + delta + "ms."
                                    + " count=" + n);
                        }
                        return;
                    }
                    task = mQueue.poll();
                    assert task != null;
                }

                final Bitmap bitmap = mFactory.onGenerate(task.context, task.notification);
                mFactory.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Check.getInstance().isInMainThread();
                        task.listener.onGenerated(bitmap);
                    }
                });

                n++;
            }
        }

        void add(@NonNull Context context,
                 @NonNull OpenNotification notification,
                 @NonNull IconAsyncListener listener) {
            Task task = new Task(context, listener, notification);
            Check.getInstance().isFalse(mStopping);
            mQueue.add(task);
        }

        void remove(@NonNull OpenNotification notification) {
            Check.getInstance().isFalse(mStopping);
            for (Task task : mQueue) {
                if (task.notification == notification) {
                    mQueue.remove(task);
                    return;
                }
            }
        }

        /**
         * @return {@code true} if the current worker may accept and handle any
         * of incoming tasks, {@code false} otherwise.
         */
        boolean isActive() {
            return !mStopping;
        }

    }

    @Nullable
    private Worker mWorker;

    @NonNull
    private Handler handler = new Handler(Looper.getMainLooper());

    @NonNull
    private final Object mMonitor = new Object();

    @NonNull
    protected Bitmap onGenerate(@NonNull Context context, @NonNull OpenNotification notification) {
        return generate(context, notification);
    }

    /**
     * Adds the notification to the tasks list.
     *
     * @param notification a notification to load from
     * @param listener     a callback
     * @see #remove(OpenNotification)
     */
    public void add(@NonNull Context context,
                    @NonNull OpenNotification notification,
                    @NonNull IconAsyncListener listener) {
        synchronized (mMonitor) {
            boolean create = isWorkerInactive();
            if (create) {
                mWorker = new Worker(this, mMonitor);
                mWorker.setPriority(Thread.MAX_PRIORITY);
            }
            assert mWorker != null;
            mWorker.add(context, notification, listener);
            if (create) mWorker.start();
        }
    }

    /**
     * Removes the notification from the task list (if available).
     *
     * @see #add(android.content.Context, OpenNotification, IconFactory.IconAsyncListener)
     */
    public void remove(@NonNull OpenNotification notification) {
        synchronized (mMonitor) {
            if (isWorkerInactive()) {
                // It's too late :(
                return;
            }

            assert mWorker != null;
            mWorker.remove(notification);
        }
    }

    private boolean isWorkerInactive() {
        return mWorker == null || !mWorker.isActive();
    }

}
