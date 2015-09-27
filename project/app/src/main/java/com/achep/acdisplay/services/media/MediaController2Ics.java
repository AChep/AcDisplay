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
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.IRemoteControlDisplay;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseIntArray;

import com.achep.acdisplay.R;
import com.achep.base.utils.MathUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import static com.achep.acdisplay.services.media.MediaController2KitKat.sStateSparse;

/**
 * {@inheritDoc}
 */
/*
    Thanks to Dr.Alexander_Breen@xda for his research:
    http://forum.xda-developers.com/showthread.php?t=2401597
 */
class MediaController2Ics extends MediaController2 {

    private final SparseIntArray mStateSparse;

    private RemoteControlDisplay mRemoteDisplay;
    private Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        private int mHostId;

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case RemoteControlDisplay.MSG_HOST_ID:
                    mHostId = msg.arg1;
                    break;
                default:
                    if (mHostId != msg.arg1) break;
                    switch (msg.what) {
                        case RemoteControlDisplay.MSG_UPDATE_STATE: // Update playback state
                            updatePlaybackState(msg.arg2);
                            break;
                        case RemoteControlDisplay.MSG_SET_METADATA: // Update metadata
                            updateMetadata((Bundle) msg.obj);
                            break;
                        case RemoteControlDisplay.MSG_SET_ARTWORK:
                            updateMetadataArtwork((Bitmap) msg.obj);
                            break;
                    }
            }
            return true;
        }
    });

    /**
     * {@inheritDoc}
     */
    protected MediaController2Ics(@NonNull Activity activity) {
        super(activity);

        SparseIntArray cachedStateSparse = sStateSparse.get();
        if (cachedStateSparse == null) {
            mStateSparse = MediaController2KitKat.generatePlaybackCompatSparse();
            sStateSparse = new WeakReference<>(mStateSparse);
        } else {
            mStateSparse = cachedStateSparse;
        }
    }

    @Override
    public void onStart(Object... objects) {
        super.onStart(objects);
        mHandler = new Handler();
        mRemoteDisplay = new RemoteControlDisplay(mHandler);
        AudioManager manager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        try {
            Method method = manager.getClass().getDeclaredMethod(
                    "registerRemoteControlDisplay",
                    IRemoteControlDisplay.class);
            method.setAccessible(true);
            method.invoke(manager, mRemoteDisplay);
        } catch (Exception e) {
            Log.w(TAG, "Failed to register remote control display.");
            e.printStackTrace();

            mRemoteDisplay = null; // clean-up
        }
    }

    @Override
    public void onStop(Object... objects) {
        if (mRemoteDisplay != null) {
            AudioManager manager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            try {
                Method method = manager.getClass().getDeclaredMethod(
                        "unregisterRemoteControlDisplay",
                        IRemoteControlDisplay.class);
                method.setAccessible(true);
                method.invoke(manager, mRemoteDisplay);
            } catch (Exception e) {
                Log.e(TAG, "Failed to unregister remote control display.");
                e.printStackTrace();
            } finally {
                mRemoteDisplay = null; // clean-up
                mHandler.removeMessages(RemoteControlDisplay.MSG_HOST_ID);
                mHandler.removeMessages(RemoteControlDisplay.MSG_SET_METADATA);
                mHandler.removeMessages(RemoteControlDisplay.MSG_SET_TRANSPORT_CONTROLS);
                mHandler.removeMessages(RemoteControlDisplay.MSG_UPDATE_STATE);
            }
        }
        mMetadata.clear();
        super.onStop(objects);
    }

    /**
     * {@inheritDoc}
     */
    public void sendMediaAction(int action) {
        broadcastMediaAction(mContext, action);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void seekTo(long position) { /* do nothing */ }

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
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updatePlaybackState(int playbackStateRcc) {
        super.updatePlaybackState(mStateSparse.get(playbackStateRcc));
    }

    /**
     * Clears {@link #mMetadata metadata}. Same as calling
     * {@link #updateMetadata(Bundle)} with {@code null} parameter.
     *
     * @see #updateMetadata(Bundle)
     */
    private void clearMetadata() {
        updateMetadata(null);
    }

    /**
     * Updates {@link #mMetadata metadata} from given bundle.
     *
     * @param data Object of metadata to update from, or {@code null} to clear local metadata.
     * @see #clearMetadata()
     */
    private void updateMetadata(@Nullable Bundle data) {
        if (data == null) {
            if (mMetadata.isEmpty()) return;
            mMetadata.clear();
        } else {
            mMetadata.title = data.getString("" + MediaMetadataRetriever.METADATA_KEY_TITLE, null);
            mMetadata.artist = data.getString("" + MediaMetadataRetriever.METADATA_KEY_ARTIST, null);
            mMetadata.album = data.getString("" + MediaMetadataRetriever.METADATA_KEY_ALBUM, null);
            mMetadata.duration = data.getLong("" + MediaMetadataRetriever.METADATA_KEY_DURATION, -1);
            mMetadata.generateSubtitle();
        }

        notifyOnMetadataChanged();
    }

    private void updateMetadataArtwork(Bitmap artwork) {
        if (artwork != null) {
            int size = mContext.getResources().getDimensionPixelSize(R.dimen.media_artwork_size);
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

    /*
     * This class is required to have weak linkage
     * because the remote process can hold a strong reference to this binder object and
     * we can't predict when it will be GC'd in the remote process. Without this code, it
     * would allow a heavyweight object to be held on this side of the binder when there's
     * no requirement to run a GC on the other side.
     */
    private static class RemoteControlDisplay extends IRemoteControlDisplay.Stub {

        public static final int MSG_UPDATE_STATE = 100;
        public static final int MSG_SET_METADATA = 101;
        public static final int MSG_SET_TRANSPORT_CONTROLS = 102;
        public static final int MSG_HOST_ID = 103;
        public static final int MSG_SET_ARTWORK = 104;

        /*
         * The reference should be weak as we can't predict when the process of GC
         * will happen in remote object.
         */
        @NonNull
        private WeakReference<Handler> mHandlerRef;

        public RemoteControlDisplay(@NonNull Handler handler) {
            super();
            mHandlerRef = new WeakReference<>(handler);
        }

        @Override
        public void setAllMetadata(int generationId, Bundle metadata, Bitmap bitmap) {
            Handler handler = mHandlerRef.get();
            if (handler == null) return;
            handler.obtainMessage(MSG_SET_METADATA, generationId, 0, metadata).sendToTarget();
            handler.obtainMessage(MSG_SET_ARTWORK, generationId, 0, bitmap).sendToTarget();
        }

        @Override
        public void setArtwork(int generationId, Bitmap bitmap) {
            Handler handler = mHandlerRef.get();
            if (handler == null) return;
            handler.obtainMessage(MSG_SET_ARTWORK, generationId, 0, bitmap).sendToTarget();

        }

        @Override
        public void setCurrentClientId(int clientGeneration, PendingIntent mediaIntent,
                                       boolean clearing) throws RemoteException {
            Handler handler = mHandlerRef.get();
            if (handler == null) return;
            handler.obtainMessage(
                    MSG_HOST_ID, clientGeneration,
                    MathUtils.bool(clearing), mediaIntent).sendToTarget();
        }

        @Override
        public void setMetadata(int generationId, Bundle metadata) {
            Handler handler = mHandlerRef.get();
            if (handler == null) return;
            handler.obtainMessage(MSG_SET_METADATA, generationId, 0, metadata).sendToTarget();
        }

        @Override
        public void setPlaybackState(int generationId, int state, long stateChangeTimeMs) {
            Handler handler = mHandlerRef.get();
            if (handler == null) return;
            handler.obtainMessage(MSG_UPDATE_STATE, generationId, state).sendToTarget();
        }

        @Override
        public void setTransportControlFlags(int generationId, int flags) {
            Handler handler = mHandlerRef.get();
            if (handler == null) return;
            handler.obtainMessage(MSG_SET_TRANSPORT_CONTROLS, generationId, flags).sendToTarget();
        }

    }

}
