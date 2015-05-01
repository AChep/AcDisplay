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
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.achep.base.async.TaskQueueThread;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

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
    public void registerListener(@NonNull MediaListener listener) {
        mMediaController.registerListener(listener);
    }

    @Override
    public void unregisterListener(@NonNull MediaListener listener) {
        mMediaController.unregisterListener(listener);
    }

    @Override
    public void onStart(Object... objects) {
        mMediaController.onStart(objects);
        synchronized (monitor) {
            // Init a new thread.
            mThread = new T(mMediaController);
//            mThread.start();
        }
    }

    @Override
    public void onStop(Object... objects) {
        synchronized (monitor) {
            // Force stop the thread.
//            mThread.finish(true);
        }
        mMediaController.onStop(objects);
    }

    //-- THREADING ------------------------------------------------------------

    private static class T extends TaskQueueThread<E> {
        private final Reference<MediaController2> mMediaControllerRef;

        public T(@NonNull MediaController2 mc) {
            mMediaControllerRef = new WeakReference<>(mc);
        }

        @Override
        protected void onHandleTask(E object) {
            MediaController2 mc = mMediaControllerRef.get();
            if (mc == null) {
                mRunning = false;
                return;
            }

            if (TextUtils.equals(object.id, mc.getMetadata().id)) {
                object.run(mc);
            }
        }

        @Override
        public void sendTask(@NonNull E object) {
            onHandleTask(object);
        }

        @Override
        protected boolean isLost() {
            return false;
        }
    }

    /**
     * Represents one single event.
     */
    private static abstract class E {
        @Nullable
        public final String id;

        public E(@Nullable String id) {
            this.id = id;
        }

        public abstract void run(@NonNull MediaController2 mc);
    }

    /**
     * An event to seek to song's specific position.
     *
     * @author Artem Chepurnoy
     */
    private static class EventSeekTo extends E {
        public final long position;

        public EventSeekTo(@Nullable String id, long position) {
            super(id);
            this.position = position;
        }

        @Override
        public void run(@NonNull MediaController2 mc) {
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

        public EventMediaAction(@Nullable String id, int action) {
            super(id);
            this.action = action;
        }

        @Override
        public void run(@NonNull MediaController2 mc) {
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
            mThread.sendTask(new EventMediaAction(
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
            mThread.sendTask(new EventSeekTo(
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

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Metadata getMetadata() {
        return mMediaController.getMetadata();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPlaybackState() {
        return mMediaController.getPlaybackState();
    }

}
