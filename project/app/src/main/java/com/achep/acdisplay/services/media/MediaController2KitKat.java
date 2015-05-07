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
package com.achep.acdisplay.services.media;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaMetadataEditor;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.RemoteController;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.KeyEvent;

import com.achep.acdisplay.App;
import com.achep.acdisplay.services.MediaService;

import java.lang.ref.WeakReference;

/**
 * Created by Artem Chepurnoy on 26.10.2014.
 */
@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.KITKAT)
class MediaController2KitKat extends MediaController2 {

    static WeakReference<SparseIntArray> sStateSparse = new WeakReference<>(null);

    @Nullable
    private MediaService mService;
    private boolean mBound = false;

    private final RemoteController.OnClientUpdateListener mRCClientUpdateListener =
            new RemoteController.OnClientUpdateListener() {

                @Override
                public void onClientChange(boolean clearing) {
                    if (clearing) {
                        clearMetadata();
                    }
                }

                @Override
                public void onClientPlaybackStateUpdate(int state) {
                    if (mPlaybackState != state) {
                        updatePlaybackState(state);
                    }
                }

                @Override
                public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs,
                                                        long currentPosMs, float speed) {
                    onClientPlaybackStateUpdate(state);
                }

                @Override
                public void onClientTransportControlUpdate(int transportControlFlags) {
                    // TODO: Bring more music control.
                }

                @Override
                public void onClientMetadataUpdate(RemoteController.MetadataEditor metadataEditor) {
                    updateMetadata(metadataEditor);
                }

            };

    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MediaService.B binder = (MediaService.B) service;
            mService = binder.getService();
            mService.setRemoteControllerEnabled();
            mService.setClientUpdateListener(mRCClientUpdateListener);

            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }

    };

    private final SparseIntArray mStateSparse;

    /**
     * {@inheritDoc}
     */
    protected MediaController2KitKat(@NonNull Activity activity) {
        super(activity);

        SparseIntArray cachedStateSparse = sStateSparse.get();
        if (cachedStateSparse == null) {
            mStateSparse = generatePlaybackCompatSparse();
            sStateSparse = new WeakReference<>(mStateSparse);
        } else {
            mStateSparse = cachedStateSparse;
        }
    }

    @NonNull
    static SparseIntArray generatePlaybackCompatSparse() {
        SparseIntArray sia = new SparseIntArray();
        sia.put(RemoteControlClient.PLAYSTATE_BUFFERING, PlaybackStateCompat.STATE_BUFFERING);
        sia.put(RemoteControlClient.PLAYSTATE_PLAYING, PlaybackStateCompat.STATE_PLAYING);
        sia.put(RemoteControlClient.PLAYSTATE_PAUSED, PlaybackStateCompat.STATE_PAUSED);
        sia.put(RemoteControlClient.PLAYSTATE_ERROR, PlaybackStateCompat.STATE_ERROR);
        sia.put(RemoteControlClient.PLAYSTATE_REWINDING, PlaybackStateCompat.STATE_REWINDING);
        sia.put(RemoteControlClient.PLAYSTATE_FAST_FORWARDING, PlaybackStateCompat.STATE_FAST_FORWARDING);
        sia.put(RemoteControlClient.PLAYSTATE_SKIPPING_FORWARDS, PlaybackStateCompat.STATE_SKIPPING_TO_NEXT);
        sia.put(RemoteControlClient.PLAYSTATE_SKIPPING_BACKWARDS, PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS);
        return sia;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart(Object... objects) {
        super.onStart();
        Intent intent = new Intent(App.ACTION_BIND_MEDIA_CONTROL_SERVICE);
        mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop(Object... objects) {
        mMetadata.clear();

        if (mBound) {
            assert mService != null;
            mService.setClientUpdateListener(null);
            mService.setRemoteControllerDisabled();
            mService = null;
            mBound = false;
        }

        mContext.unbindService(mConnection);
        super.onStop();
    }

    /**
     * {@inheritDoc}
     */
    public void sendMediaAction(int action) {
        if (mService == null) {
            Log.w(TAG, "Sending a media action on stopped controller.");
            return;
        }

        int keyCode;
        switch (action) {
            case ACTION_PLAY_PAUSE:
                keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
                break;
            case ACTION_STOP:
                keyCode = KeyEvent.KEYCODE_MEDIA_STOP;
                break;
            case ACTION_SKIP_TO_NEXT:
                keyCode = KeyEvent.KEYCODE_MEDIA_NEXT;
                break;
            case ACTION_SKIP_TO_PREVIOUS:
                keyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS;
                break;
            default:
                throw new IllegalArgumentException();
        }

        // TODO We should think about sending these up/down events accurately with touch up/down
        // on the buttons, but in the near term this will interfere with the long press behavior.
        RemoteController rc = mService.getRemoteController();
        rc.sendMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
        rc.sendMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void seekTo(long position) {
        if (mService == null) {
            Log.w(TAG, "Seeking a media on stopped controller.");
            return;
        }

        RemoteController rc = mService.getRemoteController();
        rc.seekTo(position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getPlaybackBufferedPosition() {
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getPlaybackPosition() {
        if (mService == null) {
            Log.w(TAG, "Getting a playback position on stopped controller.");
            return -1;
        }

        RemoteController rc = mService.getRemoteController();
        return rc.getEstimatedMediaPosition();
    }

    /**
     * {@inheritDoc}
     */
    protected void updatePlaybackState(int playbackStateRcc) {
        super.updatePlaybackState(mStateSparse.get(playbackStateRcc));
    }

    /**
     * Clears {@link #mMetadata metadata}. Same as calling
     * {@link #updateMetadata(android.media.RemoteController.MetadataEditor)}
     * with {@code null} parameter.
     *
     * @see #updateMetadata(android.media.RemoteController.MetadataEditor)
     */
    private void clearMetadata() {
        updateMetadata(null);
    }

    /**
     * Updates {@link #mMetadata metadata} from given remote metadata class.
     * This also updates play state.
     *
     * @param data Object of metadata to update from, or {@code null} to clear local metadata.
     * @see #clearMetadata()
     */
    private void updateMetadata(@Nullable RemoteController.MetadataEditor data) {
        if (data == null) {
            if (mMetadata.isEmpty()) return;
            mMetadata.clear();
        } else {
            mMetadata.title = data.getString(MediaMetadataRetriever.METADATA_KEY_TITLE, null);
            mMetadata.artist = data.getString(MediaMetadataRetriever.METADATA_KEY_ARTIST, null);
            mMetadata.album = data.getString(MediaMetadataRetriever.METADATA_KEY_ALBUM, null);
            mMetadata.duration = data.getLong(MediaMetadataRetriever.METADATA_KEY_DURATION, -1);
            mMetadata.bitmap = data.getBitmap(MediaMetadataEditor.BITMAP_KEY_ARTWORK, null);
            mMetadata.generateSubtitle();
        }

        notifyOnMetadataChanged();
    }

}
