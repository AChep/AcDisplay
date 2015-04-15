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
package com.achep.acdisplay.services.media;

import android.support.annotation.NonNull;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Artem Chepurnoy
 */
public class MediaControllerAsyncWrapper extends MediaController2 {

    private final Object monitor = new Object();
    private final MediaController2 mMediaController;
    private T mThread;

    public MediaControllerAsyncWrapper(@NonNull MediaController2 mc) {
        super(mc.mContext);
        mMediaController = mc;
    }

    @Override
    public void onStart(Object... objects) {
        super.onStart(objects);
        synchronized (monitor) {
            // Init a new thread.
            mThread = new T();
            mThread.start();
        }
    }

    @Override
    public void onStop(Object... objects) {
        synchronized (monitor) {
            // Force stop the thread.
            if (mThread.isAlive()) {
                while (true) {
                    mThread.mRunning = false;
                    try {
                        mThread.join();
                        break;
                    } catch (InterruptedException e) { /* pretty please! */ }
                }
            }
        }
        super.onStop(objects);
    }

    //-- THREADING ------------------------------------------------------------

    private static class T extends Thread {

        private final Queue<E> mQueue = new ConcurrentLinkedQueue<>();
        private boolean mQueueWaiting;
        private boolean mRunning = true;

        @Override
        public void run() {
            super.run();

            Queue<E> queue = new ConcurrentLinkedQueue<>();
            while (mRunning) {
                synchronized (mQueue) {
                    if (mQueue.isEmpty())
                        try {
                            // Wait for a next #sendEvent(Event),
                            // where this thread will be unlocked.
                            mQueueWaiting = true;
                            mQueue.wait();
                        } catch (InterruptedException ignored) {
                        } finally {
                            mQueueWaiting = false;
                        }

                    // Move all pending events to a local copy, so we don't need
                    // to block main queue.
                    while (!mQueue.isEmpty()) {
                        queue.add(mQueue.poll());
                    }
                }

                Iterator<E> iterator = queue.iterator();
                while (iterator.hasNext()) {
                    E e = iterator.next();
                    // ~~
                    e.run();
                    // ~~
                    iterator.remove();
                }
            }
        }

        public void sendEvent(@NonNull E e) {
            synchronized (mQueue) {
                mQueue.add(e);

                // Release the thread lock if needed.
                if (mQueueWaiting) mQueue.notifyAll();
            }
        }
    }

    /**
     * Represents one single event.
     */
    private static abstract class E implements Runnable {
        public final Reference<MediaController2> mcRef;
        public final String id;

        public E(@NonNull MediaController2 mc, @NonNull String id) {
            this.mcRef = new WeakReference<>(mc);
            this.id = id;
        }
    }

    /**
     * An event to seek to song's specific position.
     *
     * @author Artem Chepurnoy
     */
    private static class EventSeekTo extends E {
        public final long position;

        public EventSeekTo(@NonNull MediaController2 mc, @NonNull String id, long position) {
            super(mc, id);
            this.position = position;
        }

        @Override
        public void run() {
            MediaController2 mc = mcRef.get();
            if (mc == null) return;
            mc.seekTo(position);
        }
    }

    /**
     * An event to send a media action.
     *
     * @author Artem Chepurnoy
     */
    private static class EventMediaAction extends E {
        public final int action;

        public EventMediaAction(@NonNull MediaController2 mc, @NonNull String id, int action) {
            super(mc, id);
            this.action = action;
        }

        @Override
        public void run() {
            MediaController2 mc = mcRef.get();
            if (mc == null) return;
            mc.sendMediaAction(action);
        }
    }


    //-- BASIC METHODS --------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendMediaAction(int action) {
        synchronized (monitor) {
            mThread.sendEvent(new EventMediaAction(
                    mMediaController,
                    mMediaController.getMetadata().id,
                    action));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void seekTo(long position) {
        synchronized (monitor) {
            mThread.sendEvent(new EventSeekTo(
                    mMediaController,
                    mMediaController.getMetadata().id,
                    position));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getPlaybackBufferedPosition() {
        return mMediaController.getPlaybackBufferedPosition();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getPlaybackPosition() {
        return mMediaController.getPlaybackPosition();
    }

}
