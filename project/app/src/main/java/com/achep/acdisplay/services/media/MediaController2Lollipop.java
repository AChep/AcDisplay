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

import static com.achep.base.Build.DEBUG;

/**
 * {@inheritDoc}
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class MediaController2Lollipop extends MediaController2 {

    @Nullable
    private MediaController mMediaController;

    private boolean mSessionListening;

    private final ComponentName mComponent;
    private final MediaSessionManager mMSManager;
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
                    if (mMediaController != null) {
                        for (MediaController controller : controllers) {
                            if (mMediaController == controller) {
                                // Current media controller is still alive.
                                return;
                            }
                        }
                    }

                    MediaController mc = pickBestMediaController(controllers);
                    if (mc != null) {
                        setMediaController(mc);
                    } else {
                        clearMediaController(true);
                    }
                }

                @Nullable
                private MediaController pickBestMediaController(
                        @NonNull List<MediaController> list) {
                    if (DEBUG) Log.d(TAG, "Media controllers count:" + list.size());

                    int mediaControllerScore = -1;
                    MediaController mediaController = null;
                    for (MediaController mc : list) {
                        if (mc == null) continue;
                        int mcScore = 0;

                        // Check for the current state
                        PlaybackState state = mc.getPlaybackState();
                        if (state != null) {
                            switch (state.getState()) {
                                case PlaybackState.STATE_STOPPED:
                                case PlaybackState.STATE_ERROR:
                                    break;
                                default:
                                    mcScore++;
                                    break;
                            }
                        }

                        if (mcScore > mediaControllerScore) {
                            mediaControllerScore = mcScore;
                            mediaController = mc;
                        }
                    }
                    return mediaController;
                }

            };

    /**
     * {@inheritDoc}
     */
    protected MediaController2Lollipop(@NonNull Context context) {
        super(context);

        mMSManager = (MediaSessionManager) mContext.getSystemService(Context.MEDIA_SESSION_SERVICE);
        mComponent = new ComponentName(context, MediaService.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart(Object... objects) {
        super.onStart();

        try {
            mMSManager.addOnActiveSessionsChangedListener(mSessionListener, mComponent);
            mSessionListener.onActiveSessionsChanged(mMSManager.getActiveSessions(mComponent));
            mSessionListening = true;
        } catch (SecurityException exception) {
            Log.w(TAG, "Failed to start Lollipop media controller: " + exception.getMessage());
            mSessionListening = false;
            // Media controller needs notification listener service
            // permissions to be granted.
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop(Object... objects) {
        if (mSessionListening) {
            mMSManager.removeOnActiveSessionsChangedListener(mSessionListener);
            clearMediaController(true);
        }
        super.onStop();
    }

    private void clearMediaController(boolean clear) {
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mCallback);
            mMediaController = null;

            if (clear) {
                clearMetadata();
                updatePlaybackState(null);
            }
        }
    }

    private void setMediaController(@NonNull MediaController controller) {
        if (DEBUG) Log.d(TAG, "Switching to \'" + controller.getPackageName() + "\' controller.");

        clearMediaController(false);
        mMediaController = controller;
        mMediaController.registerCallback(mCallback);
        updatePlaybackState(mMediaController.getPlaybackState());
        updateMetadata(mMediaController.getMetadata());
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
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Clears {@link #mMetadata metadata}. Same as calling
     * {@link #updateMetadata(MediaMetadata)}
     * with {@code null} parameter.
     *
     * @see #updateMetadata(MediaMetadata)
     */
    private void clearMetadata() {
        updateMetadata(null);
    }

    /**
     * Updates {@link #mMetadata metadata} from given media metadata class.
     * This also updates play state.
     *
     * @param data Object of metadata to update from, or {@code null} to clear local metadata.
     * @see #clearMetadata()
     */
    private void updateMetadata(@Nullable MediaMetadata data) {
        if (data == null) {
            if (mMetadata.isEmpty()) {
                // No need to clear it again nor
                // notify subscribers about it.
                return;
            }
            mMetadata.clear();
        } else {
            final String id = data.getString(MediaMetadata.METADATA_KEY_MEDIA_ID);
            if (id != null && id.equals(mMetadata.id)) return;

            mMetadata.id = id;
            mMetadata.title = data.getDescription().getTitle();
            mMetadata.artist = data.getText(MediaMetadata.METADATA_KEY_ARTIST);
            mMetadata.album = data.getText(MediaMetadata.METADATA_KEY_ALBUM);
            mMetadata.duration = data.getLong(MediaMetadata.METADATA_KEY_DURATION);
            mMetadata.generateSubtitle();

            // Load the artwork
            Bitmap artwork = data.getBitmap(MediaMetadata.METADATA_KEY_ART);
            if (artwork == null) {
                artwork = data.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                // Might still be null
            }

            if (artwork != null) {
                final int size = mContext.getResources().getDimensionPixelSize(R.dimen.media_artwork_size);
                try {
                    mMetadata.bitmap = Bitmap.createScaledBitmap(artwork, size, size, true);
                } catch (OutOfMemoryError e) {
                    mMetadata.bitmap = null;
                }
            } else {
                mMetadata.bitmap = null;
                // Clear previous artwork
            }
        }

        notifyOnMetadataChanged();
    }

    private void updatePlaybackState(@Nullable PlaybackState state) {
        updatePlaybackState(state == null ? PlaybackState.STATE_NONE : state.getState());
    }

}
