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
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaMetadataEditor;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.RemoteController;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import com.achep.acdisplay.App;
import com.achep.acdisplay.Build;
import com.achep.acdisplay.Device;
import com.achep.acdisplay.services.MediaService;

import java.util.ArrayList;

/**
 * Class-wrapper of {@link android.media.RemoteController}.
 *
 * @author Artem Chepurnoy
 */
@TargetApi(android.os.Build.VERSION_CODES.KITKAT)
public class MediaController {

    private static final String TAG = "MediaController";
    private static final boolean DEBUG = false && Build.DEBUG;

    public static final int EVENT_METADATA_CHANGED = 1;
    public static final int EVENT_PLAYSTATE_CHANGED = 2;
    public static final int EVENT_UISTATE_CHANGED = 4;

    public static final int UISTATE_NORMAL = 0;
    public static final int UISTATE_MUSIC = 1;

    private static final int MSG_MEDIA_HIDE = 0;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_MEDIA_HIDE:
                    mUiState = UISTATE_NORMAL;
                    notifyListeners(EVENT_UISTATE_CHANGED);
                    break;
            }
        }
    };

    private Context mContext;
    private MediaService mService;
    private boolean mBound = false;

    private final ArrayList<MediaListener> mListeners = new ArrayList<>();
    private boolean mStarted;

    private int mCurrentPlayState = RemoteControlClient.PLAYSTATE_STOPPED;
    private int mUiState = UISTATE_NORMAL;

    private final Metadata mMetadata = new Metadata();
    private RemoteController.MetadataEditor mPopulateMetadataWhenStarted;
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
                    updatePlayPauseState(state);
                }

                @Override
                public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs,
                                                        long currentPosMs, float speed) {
                    updatePlayPauseState(state);
                    // TODO: Bring more music control.
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


    /**
     * Interface definition for a callback to be invoked
     * when media state changed.
     *
     * @see #registerListener(MediaController.MediaListener)
     * @see #unregisterListener(MediaController.MediaListener)
     */
    public interface MediaListener {

        /**
         * Called when something is changed.
         *
         * @param event one of following:
         *              {@link #EVENT_PLAYSTATE_CHANGED},
         *              {@link #EVENT_METADATA_CHANGED},
         *              {@link #EVENT_UISTATE_CHANGED}
         */
        void onMediaChanged(MediaController controller, int event);
    }

    public void registerListener(MediaListener listener) {
        mListeners.add(listener);
    }

    public void unregisterListener(MediaListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Notifies all listeners with given {@code event} as a parameter.
     *
     * @param event parameter of {@link MediaListener#onMediaChanged(MediaController, int)}.
     */
    private void notifyListeners(int event) {
        for (MediaListener listener : mListeners) {
            listener.onMediaChanged(this, event);
        }
    }

    /**
     * @see #onStart()
     * @see #onDestroy()
     */
    public void onCreate(Context context) {
        mContext = context;
    }

    /**
     * Called when media controller may register all listeners
     * and setup all bindings. You must call {@link #onStop()} later!
     *
     * @see #onStop()
     */
    public void onStart() {
        mStarted = true;

        if (mPopulateMetadataWhenStarted != null) {
            updateMetadata(mPopulateMetadataWhenStarted);
            mPopulateMetadataWhenStarted = null;
        }

        mMetadata.clear();

        if (Device.hasLemonCakeApi()) {
            // TODO: Bring media controls to Android L
        } else if (Device.hasKitKatApi()) {
            Intent intent = new Intent(App.ACTION_BIND_MEDIA_CONTROL_SERVICE);
            mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * Called when media controller should unregister all listeners
     * and close all bindings.
     *
     * @see #onStart()
     * @see #onDestroy()
     */
    public void onStop() {
        mStarted = false;
        mMetadata.clear();
        mHandler.removeCallbacksAndMessages(null);

        if (Device.hasLemonCakeApi()) {
            // TODO: Bring media controls to Android L
        } else if (Device.hasKitKatApi()) {
            if (mBound) {
                mService.setClientUpdateListener(null);
                mService.setRemoteControllerDisabled();
            }

            mContext.unbindService(mConnection);
        }
    }

    /**
     *
     */
    public void onDestroy() {
        mContext = null;
    }

    private void updatePlayPauseState(int state) {
        if (DEBUG) Log.d(TAG, "state: old=" + mCurrentPlayState + ", state=" + state);
        if (state == mCurrentPlayState) {
            return;
        }

        // boolean clientSupportsSeek = mMetadata != null && mMetadata.duration > 0;
        // setSeekBarsEnabled(clientSupportsSeek);

        switch (state) {
            case RemoteControlClient.PLAYSTATE_PLAYING:
                mHandler.removeMessages(MSG_MEDIA_HIDE);
                mUiState = UISTATE_MUSIC;
                notifyListeners(EVENT_UISTATE_CHANGED);
                break;
            default:

                // Hide media widget delayed to give user some
                // time to start playing music.
                int msg = MSG_MEDIA_HIDE;
                if (!mHandler.hasMessages(msg)) {
                    mHandler.sendEmptyMessageDelayed(msg, 3000);
                }
                break;
        }

        mCurrentPlayState = state;
        notifyListeners(EVENT_PLAYSTATE_CHANGED);
    }

    public int getPlayState() {
        return mCurrentPlayState;
    }

    /**
     * If {@link #getPlayState() play state} equals to
     * {@link android.media.RemoteControlClient#PLAYSTATE_PLAYING}, then
     * ui state is {@link #UISTATE_MUSIC}, otherwise it switches (with a little delay)
     * to {@link #UISTATE_NORMAL}.
     *
     * @return the state of media user interface
     */
    public int getUiState() {
        return mUiState;
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
    private void updateMetadata(RemoteController.MetadataEditor data) {
        if (data == null) {
            mMetadata.clear();
        } else {
            if (mStarted) {
                mMetadata.artist = trim(data.getString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, mMetadata.artist));
                mMetadata.trackTitle = trim(data.getString(MediaMetadataRetriever.METADATA_KEY_TITLE, mMetadata.trackTitle));
                mMetadata.albumTitle = trim(data.getString(MediaMetadataRetriever.METADATA_KEY_ALBUM, mMetadata.albumTitle));
                mMetadata.duration = data.getLong(MediaMetadataRetriever.METADATA_KEY_DURATION, -1);
                mMetadata.bitmap = data.getBitmap(MediaMetadataEditor.BITMAP_KEY_ARTWORK, mMetadata.bitmap);
            } else {
                mPopulateMetadataWhenStarted = data;
                return;
            }
        }

        updatePlayPauseState(mCurrentPlayState);
        notifyListeners(EVENT_METADATA_CHANGED);
    }

    /**
     * @return {@code string == null ? null : TextUtils.isEmpty(string) ? null : string.trim()}
     */
    private String trim(String string) {
        return string == null ? null : TextUtils.isEmpty(string) ? null : string.trim();
    }

    /**
     * @return {@link com.achep.acdisplay.services.media.Metadata} of playing track,
     * or {@code null} if music is not playing.
     */
    public Metadata getMetadata() {
        return mMetadata;
    }

    /**
     * Sends media button's click with given key code.
     *
     * @param keyCode May be one of media key events.
     * @see android.view.KeyEvent#KEYCODE_MEDIA_NEXT
     * @see android.view.KeyEvent#KEYCODE_MEDIA_PLAY_PAUSE
     * @see android.view.KeyEvent#KEYCODE_MEDIA_PREVIOUS
     */
    public void sendMediaButtonClick(int keyCode) {
        if (Device.hasLemonCakeApi()) {
            // TODO: Bring media controls to Android L
        } else if (Device.hasKitKatApi()) {
            // TODO We should think about sending these up/down events accurately with touch up/down
            // on the buttons, but in the near term this will interfere with the long press behavior.
            // Note from Artem Chepurnoy: This is from Android sources, so check them if this fixed.
            RemoteController rc = mService.getRemoteController();
            rc.sendMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
            rc.sendMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
        } else {
            Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            KeyEvent keyDown = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
            KeyEvent keyUp = new KeyEvent(KeyEvent.ACTION_UP, keyCode);

            mContext.sendOrderedBroadcast(intent.putExtra(Intent.EXTRA_KEY_EVENT, keyDown), null);
            mContext.sendOrderedBroadcast(intent.putExtra(Intent.EXTRA_KEY_EVENT, keyUp), null);
        }
    }

}
