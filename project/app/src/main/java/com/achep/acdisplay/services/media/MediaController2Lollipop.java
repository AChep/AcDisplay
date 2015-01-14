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
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.achep.acdisplay.R;
import com.achep.acdisplay.services.MediaService;

import java.util.List;

/**
 * {@inheritDoc}
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class MediaController2Lollipop extends MediaController2 {

    private MediaController mMediaController;

    private boolean mSessionListening;

    private final ComponentName mComponent;
    private final MediaSessionManager mMediaSessionManager;
    private final MediaController.Callback mCallback =
            new MediaController.Callback() {

                @Override
                public void onMetadataChanged(MediaMetadata metadata) {
                    super.onMetadataChanged(metadata);
                    updateMetadata(metadata);
                }

                @Override
                public void onPlaybackStateChanged(PlaybackState state) {
                    super.onPlaybackStateChanged(state);
                    updatePlaybackState(state.getState());
                }
            };

    private final MediaSessionManager.OnActiveSessionsChangedListener mSessionListener =
            new MediaSessionManager.OnActiveSessionsChangedListener() {

                @Override
                public void onActiveSessionsChanged(List<MediaController> controllers) {
                    int size = controllers.size();
                    if (size == 0) {
                        updateMetadata(null);
                        updatePlaybackState(PlaybackState.STATE_NONE);
                    } else {
                        if (mMediaController != null) {
                            for (MediaController controller : controllers) {
                                if (mMediaController == controller) {
                                    // Current media controller is still alive.
                                    return;
                                }
                            }
                        }

                        // TODO: Maybe check which one is better instead of selecting first one?
                        switchMediaController(controllers.get(0));
                    }
                }

            };

    /**
     * {@inheritDoc}
     */
    protected MediaController2Lollipop(@NonNull Context context) {
        super(context);

        //noinspection ResourceType
        mMediaSessionManager = (MediaSessionManager) mContext
                .getSystemService(Context.MEDIA_SESSION_SERVICE);
        mComponent = new ComponentName(context, MediaService.class);
    }

    /**
     * {@inheritDoc}
     */
    public void onStart() {
        super.onStart();

        try {
            mMediaSessionManager.addOnActiveSessionsChangedListener(mSessionListener, mComponent);
            List<MediaController> sessions = mMediaSessionManager.getActiveSessions(mComponent);
            mSessionListener.onActiveSessionsChanged(sessions);
            mSessionListening = true;
        } catch (SecurityException exception) {
            Log.i(TAG, "Caught SecurityException on start: " + exception.getMessage());
            mSessionListening = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onStop() {
        if (mSessionListening) {
            mMediaSessionManager.removeOnActiveSessionsChangedListener(mSessionListener);
            clearMediaController();
        }
        super.onStop();
    }

    /**
     * {@inheritDoc}
     */
    public void sendMediaAction(int action) {
        if (mMediaController == null) {
            // Maybe somebody is waiting to start his player by
            // this lovely event.
            // TODO: Check if it works as expected.
            MediaController2.broadcastMediaAction(mContext, action);
            return;
        }

        MediaController.TransportControls controls = mMediaController.getTransportControls();
        switch (action) {
            case ACTION_PLAY_PAUSE:
                if (mPlaybackState == PlaybackState.STATE_PLAYING) {
                    controls.pause();
                } else {
                    controls.play();
                }
                break;
            case ACTION_STOP:
                controls.stop();
                break;
            case ACTION_SKIP_TO_NEXT:
                controls.skipToNext();
                break;
            case ACTION_SKIP_TO_PREVIOUS:
                controls.skipToPrevious();
                break;
        }
    }

    private void updateMetadata(MediaMetadata metadata) {
        if (metadata == null) {
            mMetadata.clear();
        } else {
            MediaDescription description = metadata.getDescription();
            mMetadata.title = description.getTitle();
            mMetadata.artist = metadata.getText(MediaMetadata.METADATA_KEY_ARTIST);
            mMetadata.album = metadata.getText(MediaMetadata.METADATA_KEY_ALBUM);
            mMetadata.bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
            mMetadata.duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
            mMetadata.updateSubtitle();

            if (mMetadata.bitmap != null) {
                final int size = mContext.getResources().getDimensionPixelSize(R.dimen.media_artwork_size);
                mMetadata.bitmap = Bitmap.createScaledBitmap(mMetadata.bitmap, size, size, true);
            }
        }

        notifyOnMetadataChanged();
    }

    private void updatePlaybackState(@Nullable PlaybackState state) {
        updatePlaybackState(state == null ? PlaybackState.STATE_NONE : state.getState());
    }

    private void updatePlaybackState(int state) {
        mPlaybackState = state;
        notifyOnPlaybackStateChanged();
    }

    private void switchMediaController(MediaController controller) {
        clearMediaController();

        mMediaController = controller;
        mMediaController.registerCallback(mCallback);
        updateMetadata(mMediaController.getMetadata());
        updatePlaybackState(mMediaController.getPlaybackState());

        Log.i(TAG, "Switching to " + mMediaController.getPackageName() + " controller.");
    }

    private void clearMediaController() {
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mCallback);
            mMediaController = null;
            mMetadata.clear();
        }
    }

}
