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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import com.achep.base.Build;
import com.achep.base.Device;

import java.util.ArrayList;

import static com.achep.base.Build.DEBUG;

/**
 * Allows an app to interact with an ongoing media session. Media buttons and
 * other commands can be sent to the session. A callback may be registered to
 * receive updates from the session, such as metadata and play state changes.
 *
 * @author Artem Chepurnoy
 */
public abstract class MediaController2 {

    protected static final String TAG = "MediaController";

    public static final int ACTION_SKIP_TO_NEXT = 2;
    public static final int ACTION_SKIP_TO_PREVIOUS = 3;
    public static final int ACTION_PLAY_PAUSE = 0;
    public static final int ACTION_STOP = 1;

    /**
     * Creates new instance, created for working on this device's
     * Android version.
     *
     * @return new instance.
     */
    @NonNull
    public static MediaController2 newInstance(@NonNull Activity activity) {
        if (Device.hasLollipopApi()) {
            return new MediaController2Lollipop(activity);
        } else if (Device.hasKitKatApi()) {
            return new MediaController2KitKat(activity);
        }

        return new MediaController2Empty(activity);
    }

    /**
     * Emulates hardware buttons' click via broadcast system.
     *
     * @see android.view.KeyEvent
     */
    public static void broadcastMediaAction(@NonNull Context context, int action) {
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
                Log.d(TAG, "Received unknown media action(" + action + ").");
                return;
        }

        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        KeyEvent keyDown = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        KeyEvent keyUp = new KeyEvent(KeyEvent.ACTION_UP, keyCode);

        if (Device.hasKitKatApi()) Log.i(TAG, "Broadcasting this (" + action + ") media action.");
        context.sendOrderedBroadcast(intent.putExtra(Intent.EXTRA_KEY_EVENT, keyDown), null);
        context.sendOrderedBroadcast(intent.putExtra(Intent.EXTRA_KEY_EVENT, keyUp), null);
    }

    /**
     * Callback for receiving updates on from the session. A Callback can be
     * registered using {@link #registerListener(MediaListener)}
     */
    public interface MediaListener {

        /**
         * Override to handle changes to the current metadata. <br/><br/><b>Warning:</b> You must NOT call
         * {@link #registerListener(MediaListener)} nor {@link #unregisterListener(MediaListener)}
         * from here, otherwise it will crash!
         *
         * @param metadata The current metadata for the session.
         * @see com.achep.acdisplay.services.media.Metadata
         * @see #getMetadata()
         */
        void onMetadataChanged(@NonNull Metadata metadata);

        /**
         * Override to handle changes in playback state. <br/><br/><b>Warning:</b> You must NOT call
         * {@link #registerListener(MediaListener)} nor {@link #unregisterListener(MediaListener)}
         * from here, otherwise it will crash!
         *
         * @param state The new playback state of the session
         * @see #getPlaybackState()
         */
        void onPlaybackStateChanged(int state);

    }

    @NonNull
    protected final Context mContext;
    @NonNull
    protected final ArrayList<MediaListener> mListeners;
    @NonNull
    protected final Metadata mMetadata;

    protected int mPlaybackState;

    protected MediaController2(@NonNull Context context) {
        mContext = context;

        mListeners = new ArrayList<>();
        mMetadata = new Metadata();
    }

    public void onStart() {
        if (DEBUG) Log.d(TAG, "Starting media controller: [" + toString() + "]");
    }

    public void onStop() {
        if (DEBUG) Log.d(TAG, "Stopping media controller: [" + toString() + "]");

        mPlaybackState = PlaybackStateCompat.STATE_NONE;
    }

    /**
     * Registers the specified callback.
     */
    public final void registerListener(@NonNull MediaListener listener) {
        synchronized (this) {
            mListeners.add(listener);
        }
    }

    /**
     * Unregisters the specified callback.
     */
    public final void unregisterListener(@NonNull MediaListener listener) {
        synchronized (this) {
            mListeners.remove(listener);
        }
    }

    /**
     * Sends media action. One of the following:
     * <ul>
     * <li> {@link #ACTION_PLAY_PAUSE}</li>
     * <li> {@link #ACTION_STOP}</li>
     * <li> {@link #ACTION_SKIP_TO_NEXT}</li>
     * <li> {@link #ACTION_SKIP_TO_PREVIOUS}</li>
     * </ul>
     */
    public abstract void sendMediaAction(int action);

    protected void notifyOnMetadataChanged() {
        synchronized (this) {
            for (MediaListener listener : mListeners) {
                listener.onMetadataChanged(mMetadata);
            }
        }
    }

    protected void updatePlaybackState(int playbackState) {
        if (mPlaybackState == (mPlaybackState = playbackState)) return;
        notifyOnPlaybackStateChanged();
    }

    protected void notifyOnPlaybackStateChanged() {
        synchronized (this) {
            for (MediaListener listener : mListeners) {
                listener.onPlaybackStateChanged(mPlaybackState);
            }
        }
    }

    /**
     * Get the current metadata for this session.
     *
     * @return {@link Metadata the metadata} of playing track.
     */
    @NonNull
    public final Metadata getMetadata() {
        return mMetadata;
    }

    /**
     * Get the current state of playback. One of the following:
     * <ul>
     * <li> {@link android.media.session.PlaybackState#STATE_NONE}</li>
     * <li> {@link android.media.session.PlaybackState#STATE_STOPPED}</li>
     * <li> {@link android.media.session.PlaybackState#STATE_PLAYING}</li>
     * <li> {@link android.media.session.PlaybackState#STATE_PAUSED}</li>
     * <li> {@link android.media.session.PlaybackState#STATE_FAST_FORWARDING}</li>
     * <li> {@link android.media.session.PlaybackState#STATE_REWINDING}</li>
     * <li> {@link android.media.session.PlaybackState#STATE_BUFFERING}</li>
     * <li> {@link android.media.session.PlaybackState#STATE_ERROR}</li>
     * </ul>
     * You also may use {@link android.support.v4.media.session.PlaybackStateCompat} to
     * access those values.
     */
    public final int getPlaybackState() {
        return mPlaybackState;
    }

}
