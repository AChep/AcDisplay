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
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.achep.acdisplay.R;
import com.achep.acdisplay.services.MediaService;
import com.achep.base.async.TaskQueueThread;
import com.achep.base.tests.Check;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.List;

import static com.achep.base.Build.DEBUG;

/**
 * {@inheritDoc}
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class MediaController2Lollipop extends MediaController2 {

    @NonNull
    private final ComponentName mComponent;
    @NonNull
    private final OnActiveSessionsChangedListener mSessionListener =
            new OnActiveSessionsChangedListener();
    @NonNull
    private final MediaController.Callback mCallback =
            new MediaController.Callback() {

                @Override
                public void onMetadataChanged(MediaMetadata metadata) {
                    super.onMetadataChanged(metadata);
                    Check.getInstance().isInMainThread();
                    updateMetadata(metadata);
                }

                @Override
                public void onPlaybackStateChanged(@NonNull PlaybackState state) {
                    super.onPlaybackStateChanged(state);
                    Check.getInstance().isInMainThread();
                    updatePlaybackState(state.getState());
                }
            };

    /**
     * @author Artem Chepurnoy
     */
    private static class OnActiveSessionsChangedListener implements
            MediaSessionManager.OnActiveSessionsChangedListener {

        @NonNull
        private Reference<MediaController2Lollipop> mMediaControllerRef = new WeakReference<>(null);

        public void setMediaController(@Nullable MediaController2Lollipop mc) {
            if (mc == null) {
                mMediaControllerRef.clear();
                return;
            }

            mMediaControllerRef = new WeakReference<>(mc);
        }

        @Override
        public void onActiveSessionsChanged(List<MediaController> controllers) {
            MediaController2Lollipop p = mMediaControllerRef.get();
            if (p == null) return;

            if (p.mMediaController != null) {
                for (MediaController controller : controllers) {
                    if (p.mMediaController == controller) {
                        // Current media controller is still alive.
                        return;
                    }
                }
            }

            MediaController mc = pickBestMediaController(controllers);
            if (mc != null) {
                p.setMediaController(mc);
            } else {
                p.clearMediaController(true);
            }
        }

        @Nullable
        private MediaController pickBestMediaController(
                @NonNull List<MediaController> list) {
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
    }

    @Nullable
    private MediaSessionManager mMediaSessionManager;
    @Nullable
    private MediaController mMediaController;

    private boolean mSessionListening;
    private T mThread;

    /**
     * {@inheritDoc}
     */
    protected MediaController2Lollipop(@NonNull Context context) {
        super(context);

        mComponent = new ComponentName(context, MediaService.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart(Object... objects) {
        super.onStart();

        // Init a new thread.
        mThread = new T(this);
        mThread.setPriority(Thread.MIN_PRIORITY);
        mThread.start();

        // Media session manager leaks/holds the context for too long.
        // Don't let it to leak the activity, better lak the whole app.
        final Context context = mContext.getApplicationContext();
        mMediaSessionManager = (MediaSessionManager) context
                .getSystemService(Context.MEDIA_SESSION_SERVICE);

        try {
            mMediaSessionManager.addOnActiveSessionsChangedListener(mSessionListener, mComponent);
            mSessionListener.setMediaController(this);
            mSessionListening = true;
        } catch (SecurityException exception) {
            Log.w(TAG, "Failed to start Lollipop media controller: " + exception.getMessage());
            // Try to unregister it, just it case.
            try {
                mMediaSessionManager.removeOnActiveSessionsChangedListener(mSessionListener);
            } catch (Exception e) { /* unused */ } finally {
                mMediaSessionManager = null;
                mSessionListening = false;
            }
            // Media controller needs notification listener service
            // permissions to be granted.
            return;
        }

        List<MediaController> controllers = mMediaSessionManager.getActiveSessions(mComponent);
        mSessionListener.onActiveSessionsChanged(controllers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop(Object... objects) {
        // Force stop the thread.
        mThread.finish(true);

        if (mSessionListening) {
            mSessionListening = false;
            mSessionListener.setMediaController(null);
            assert mMediaSessionManager != null;
            mMediaSessionManager.removeOnActiveSessionsChangedListener(mSessionListener);
            clearMediaController(true);
        }

        super.onStop();
        mMediaSessionManager = null;
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

        clearMediaController(true);
        mMediaController = controller;
        mMediaController.registerCallback(mCallback);
        // Get the new metadata and new playback state async-ly
        // to prevent possible ANRs.
        mThread.sendTask(new EventUpdateMetadata(mMediaController.getSessionToken()));
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
     * {@inheritDoc}
     */
    @Override
    public void seekTo(long position) {
        if (mMediaController == null) {
            // Do nothing or crash?
            return;
        }

        mMediaController.getTransportControls().seekTo(position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getPlaybackBufferedPosition() {
        if (mMediaController == null || mMediaController.getPlaybackState() == null) {
            // Do nothing or crash?
            return -1;
        }

        return mMediaController.getPlaybackState().getBufferedPosition();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getPlaybackPosition() {
        if (mMediaController == null || mMediaController.getPlaybackState() == null) {
            // Do nothing or crash?
            return -1;
        }

        return mMediaController.getPlaybackState().getPosition();
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
            String id;
            try {
                id = data.getString(MediaMetadata.METADATA_KEY_MEDIA_ID);
            } catch (RuntimeException e) {
                // This is weird, but happens on some devices
                // periodically.
                try {
                    // Try again.
                    id = data.getString(MediaMetadata.METADATA_KEY_MEDIA_ID);
                } catch (RuntimeException e2) {
                    mMetadata.clear();
                    notifyOnMetadataChanged();
                    return;
                }
            }
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

    //-- THREADING ------------------------------------------------------------

    static class T extends TaskQueueThread<E> {

        @NonNull
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

            object.run(mc);
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
    static abstract class E {
        public abstract void run(@NonNull MediaController2 mc);
    }

    /**
     * An event to seek to song's specific position.
     *
     * @author Artem Chepurnoy
     */
    private static class EventUpdateMetadata extends E {

        @NonNull
        private final MediaSession.Token mToken;
        @NonNull
        private final Handler mHandler;

        public EventUpdateMetadata(@NonNull MediaSession.Token token) {
            super();
            mHandler = new Handler(Looper.getMainLooper());
            mToken = token;
        }

        @Override
        public void run(@NonNull MediaController2 mc) {
            final MediaController2Lollipop mcl = (MediaController2Lollipop) mc;
            final MediaController source = mcl.mMediaController;

            if (source != null && mToken.equals(source.getSessionToken())) {
                long now = SystemClock.elapsedRealtime();

                final MediaMetadata metadata = source.getMetadata();
                final PlaybackState playbackState = source.getPlaybackState();

                long delta = SystemClock.elapsedRealtime() - now;
                Log.i(TAG, "Got the new metadata & playback state in " + delta + " millis. "
                        + "The media controller is " + source.getPackageName());

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mcl.updateMetadata(metadata);
                        mcl.updatePlaybackState(playbackState);
                    }
                });
            }
        }
    }
}
