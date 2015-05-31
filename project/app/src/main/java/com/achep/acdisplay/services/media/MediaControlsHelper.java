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

import com.achep.acdisplay.Config;
import com.achep.base.async.WeakHandler;
import com.achep.base.content.ConfigBase;
import com.achep.base.interfaces.ISubscriptable;
import com.achep.base.tests.Check;

import java.util.ArrayList;
import java.util.List;

/**
 * A helper class for deciding when to show the media controls widget.
 *
 * @author Artem Chepurnoy
 */
public class MediaControlsHelper implements
        MediaController2.MediaListener,
        ISubscriptable<MediaControlsHelper.Callback>,
        ConfigBase.OnConfigChangedListener {

    private static final int DELAY = 6000; // 6 sec.

    @NonNull
    private final Handler mHandler;
    @NonNull
    private final List<Callback> mListeners;
    @NonNull
    private final MediaController2 mMediaController;

    private boolean mShowing;
    private boolean mStarted;
    private boolean mEnabled;

    public interface Callback {
        void onStateChanged(boolean showing);
    }

    public MediaControlsHelper(@NonNull MediaController2 mc) {
        mHandler = new H(this);
        mListeners = new ArrayList<>();
        mMediaController = mc;
        mShowing = false;
    }

    public void start() {
        Config.getInstance().registerListener(this);
        mMediaController.registerListener(this);
        if (mEnabled = Config.getInstance().isMediaWidgetEnabled()) mMediaController.start();
        mStarted = true;

        // Initialize
        // FIXME: Do I need to ping the playback state here?
        onPlaybackStateChanged(mMediaController.getPlaybackState());
    }

    public void stop() {
        mStarted = false;
        if (mEnabled) mMediaController.stop();
        mMediaController.unregisterListener(this);
        Config.getInstance().unregisterListener(this);
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

    @Override
    public void onConfigChanged(@NonNull ConfigBase config,
                                @NonNull String key,
                                @NonNull Object value) {
        switch (key) {
            case Config.KEY_MEDIA_WIDGET:
                mEnabled = (boolean) value;
                if (mStarted) {
                    if (mEnabled) {
                        mMediaController.start();
                    } else mMediaController.stop();
                }
                break;
        }
    }

    @NonNull
    public MediaController2 getMediaController() {
        return mMediaController;
    }

    //-- CLASSES --------------------------------------------------------------

    /**
     * @author Artem Chepurnoy
     */
    private static class H extends WeakHandler<MediaControlsHelper> {

        private static final int MSG_HIDE_MEDIA_WIDGET = 1;

        public H(@NonNull MediaControlsHelper cc) {
            super(cc);
        }

        @Override
        protected void onHandleMassage(@NonNull MediaControlsHelper cc, Message msg) {
            switch (msg.what) {
                case MSG_HIDE_MEDIA_WIDGET:
                    cc.mShowing = false;
                    cc.notifyOnStateChanged();
                    break;
            }
        }
    }
}
