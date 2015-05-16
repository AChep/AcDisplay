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
package com.achep.acdisplay.services;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.RemoteController;
import android.os.Binder;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.achep.acdisplay.App;
import com.achep.acdisplay.R;
import com.achep.acdisplay.notifications.NotificationListener;
import com.achep.base.AppHeap;
import com.achep.base.Device;

/**
 * Created by achep on 07.06.14.
 *
 * @author Artem Chepurnoy
 */
@SuppressWarnings("deprecation") // RemoteController is completely outdated.
@SuppressLint("NewApi") // RemoteController is a new thing.
public class MediaService extends NotificationListenerService implements
        RemoteController.OnClientUpdateListener {

    private static final String TAG = "MediaService";

    public static MediaService sService;

    private final NotificationListener mNotificationListener = NotificationListener.newInstance();

    private final IBinder mBinder = new B();
    private AudioManager mAudioManager;

    private boolean mRegistered;
    private RemoteController mRemoteController;
    private RemoteController.OnClientUpdateListener mExternalListener;

    public class B extends Binder {

        public MediaService getService() {
            return MediaService.this;
        }

    }

    private static boolean isRemoteControllerSupported() {
        return Device.hasKitKatApi() && !Device.hasLollipopApi();
    }

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        switch (intent.getAction()) {
            case App.ACTION_BIND_MEDIA_CONTROL_SERVICE:
                if (!isRemoteControllerSupported()) {
                    // Should never happen normally.
                    throw new RuntimeException("Not supported Android version!");
                }
                return mBinder;
            default:
                sService = this;
                mNotificationListener.onListenerBind(this);
                return super.onBind(intent);
        }
    }

    @Override
    public boolean onUnbind(@NonNull Intent intent) {
        switch (intent.getAction()) {
            case App.ACTION_BIND_MEDIA_CONTROL_SERVICE:
                break;
            default:
                mNotificationListener.onListenerUnbind(this);
                sService = null;
                break;
        }
        return super.onUnbind(intent);
    }

    //-- HANDLING NOTIFICATIONS -----------------------------------------------

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        mNotificationListener.onListenerConnected(this);
    }

    @Override
    public void onNotificationPosted(@NonNull StatusBarNotification notification) {
        mNotificationListener.onNotificationPosted(this, notification);
    }

    @Override
    public void onNotificationRemoved(@NonNull StatusBarNotification notification) {
        mNotificationListener.onNotificationRemoved(this, notification);
    }

    //-- REMOTE CONTROLLER ----------------------------------------------------

    @Override
    public void onCreate() {
        if (isRemoteControllerSupported()) {
            mRemoteController = new RemoteController(this, this);
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }
    }

    @Override
    public void onDestroy() {
        if (isRemoteControllerSupported()) {
            setRemoteControllerDisabled();
        }

        AppHeap.getRefWatcher().watch(this);
    }

    public void setRemoteControllerEnabled() {
        if (mRegistered) {
            return;
        }

        mRegistered = mAudioManager.registerRemoteController(mRemoteController);

        if (mRegistered) {
            final int size = getResources().getDimensionPixelSize(R.dimen.media_artwork_size);
            mRemoteController.setArtworkConfiguration(size, size);
//            mRemoteController.setSynchronizationMode(RemoteController.POSITION_SYNCHRONIZATION_CHECK);
        } else {
            Log.e(TAG, "Error while registering RemoteController!");
        }
    }

    public void setRemoteControllerDisabled() {
        if (!mRegistered) {
            return;
        }

        mAudioManager.unregisterRemoteController(mRemoteController);
        mRegistered = false;
    }

    public RemoteController getRemoteController() {
        return mRemoteController;
    }

    /**
     * Sets up external callback for client update events.
     */
    public void setClientUpdateListener(@Nullable RemoteController.OnClientUpdateListener listener) {
        mExternalListener = listener;
    }

    @Override
    public void onClientChange(boolean clearing) {
        if (mExternalListener != null) {
            mExternalListener.onClientChange(clearing);
        }
    }

    @Override
    public void onClientPlaybackStateUpdate(int state) {
        if (mExternalListener != null) {
            mExternalListener.onClientPlaybackStateUpdate(state);
        }
    }

    @Override
    public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs, long currentPosMs, float speed) {
        if (mExternalListener != null) {
            mExternalListener.onClientPlaybackStateUpdate(state, stateChangeTimeMs, currentPosMs, speed);
        }
    }

    @Override
    public void onClientTransportControlUpdate(int transportControlFlags) {
        if (mExternalListener != null) {
            mExternalListener.onClientTransportControlUpdate(transportControlFlags);
        }
    }

    @Override
    public void onClientMetadataUpdate(RemoteController.MetadataEditor metadataEditor) {
        if (mExternalListener != null) {
            mExternalListener.onClientMetadataUpdate(metadataEditor);
        }
    }

}
