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

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.media.session.PlaybackStateCompat;

import com.achep.base.async.WeakHandler;
import com.achep.base.interfaces.ISubscriptable;
import com.achep.base.tests.Check;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Artem Chepurnoy on 30.04.2015.
 */
public class MediaControlsUiListener implements
        MediaController2.MediaListener,
        ISubscriptable<MediaControlsUiListener.Callback> {

    private static final int DELAY = 6000; // 6 sec.

    @NonNull
    private final Handler mHandler;
    @NonNull
    private final List<Callback> mListeners;
    @NonNull
    private final MediaController2 mMediaController;

    private boolean mShowing;

    public interface Callback {
        void onStateChanged(boolean showing);
    }

    public MediaControlsUiListener(@NonNull MediaController2 mc) {
        mHandler = new H(this);
        mListeners = new ArrayList<>();
        mMediaController = mc;
        mShowing = false;
    }

    public void start() {
        mMediaController.registerListener(this);
        onPlaybackStateChanged(mMediaController.getPlaybackState());
    }

    public void stop() {
        mMediaController.unregisterListener(this);
    }

    @Override
    public void registerListener(@NonNull Callback listener) {
        mListeners.add(listener);
    }

    @Override
    public void unregisterListener(@NonNull Callback listener) {
        mListeners.remove(listener);
    }

    @Override
    public void onMetadataChanged(@NonNull Metadata metadata) {
        // This event is handled by
        // the media widget.
    }

    @Override
    public void onPlaybackStateChanged(int state) {
        Check.getInstance().isInMainThread();
        switch (state) {
            case PlaybackStateCompat.STATE_PLAYING:
                mHandler.removeMessages(H.MSG_HIDE_MEDIA_WIDGET);
                if (!mShowing) {
                    mShowing = true;
                    notifyOnStateChanged();
                }
                break;
            default:
                if (mShowing) {
                    int delay = state == PlaybackStateCompat.STATE_NONE ? 500 : DELAY;
                    mHandler.sendEmptyMessageDelayed(H.MSG_HIDE_MEDIA_WIDGET, delay);
                }
                break;
        }
    }

    /**
     * @return {@code true} if the media controls should be shown,
     * {@code false} otherwise.
     */
    public boolean isShown() {
        return mShowing;
    }

    private void notifyOnStateChanged() {
        for (Callback callback : mListeners) {
            callback.onStateChanged(mShowing);
        }
    }

    //-- CLASSES --------------------------------------------------------------

    /**
     * @author Artem Chepurnoy
     */
    private static class H extends WeakHandler<MediaControlsUiListener> {

        private static final int MSG_HIDE_MEDIA_WIDGET = 1;

        public H(@NonNull MediaControlsUiListener cc) {
            super(cc);
        }

        @Override
        protected void onHandleMassage(@NonNull MediaControlsUiListener cc, Message msg) {
            switch (msg.what) {
                case MSG_HIDE_MEDIA_WIDGET:
                    cc.mShowing = false;
                    cc.notifyOnStateChanged();
                    break;
            }
        }
    }
}
