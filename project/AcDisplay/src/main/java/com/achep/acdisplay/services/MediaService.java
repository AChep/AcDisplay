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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.achep.acdisplay.App;
import com.achep.acdisplay.Device;
import com.achep.acdisplay.R;
import com.achep.acdisplay.notifications.NotificationPresenter;

/**
 * Created by achep on 07.06.14.
 *
 * @author Artem Chepurnoy
 */
@SuppressLint("NewApi")
public class MediaService extends NotificationListenerService implements
        RemoteController.OnClientUpdateListener {

    private static final String TAG = "MediaService";

    public static MediaService sService;

    private final IBinder mBinder = new B();
    private AudioManager mAudioManager;

    private boolean mRegistered;
    private RemoteController mRemoteController;
    private RemoteController.OnClientUpdateListener mExternalListener;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public class B extends Binder {

        public MediaService getService() {
            return MediaService.this;
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        switch (intent.getAction()) {
            case App.ACTION_BIND_MEDIA_CONTROL_SERVICE:
                if (!Device.hasKitKatApi() && !Device.hasLemonCakeApi()) {
                    throw new RuntimeException("Required Android API version 19 or 20!");
                }
                return mBinder;
            default:
                sService = this;

                // What is the idea of init notification?
                // Well the main goal is to access #getActiveNotifications()
                // what seems to be not possible without dirty and buggy
                // workarounds.
                NotificationPresenter
                        .getInstance()
                        .tryStartInitProcess();

                return super.onBind(intent);
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        switch (intent.getAction()) {
            case App.ACTION_BIND_MEDIA_CONTROL_SERVICE:
                break;
            default:
                sService = null;
                break;
        }
        return super.onUnbind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification notification) {
        rockNotification(notification, true);
    }

    @Override
    public void onNotificationRemoved(final StatusBarNotification notification) {
        rockNotification(notification, false);
    }

    private void rockNotification(final StatusBarNotification sbn, final boolean post) {
        final StatusBarNotification[] activeNotifies = getActiveNotifications();
        runOnMainLooper(new Runnable() {
            @Override
            public void run() {
                NotificationPresenter np = NotificationPresenter.getInstance();
                np.postOrRemoveNotification(MediaService.this, sbn, post);
                np.tryInit(MediaService.this, sbn, activeNotifies);
            }
        });
    }

    private void runOnMainLooper(Runnable runnable) {
        mHandler.post(runnable);
    }

    @Override
    public void onCreate() {
        if (Device.hasKitKatApi() && !Device.hasLemonCakeApi()) {
            mRemoteController = new RemoteController(this, this);
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }
    }

    @Override
    public void onDestroy() {
        if (Device.hasKitKatApi() && !Device.hasLemonCakeApi()) {
            setRemoteControllerDisabled();
        }
    }

    public void setRemoteControllerEnabled() {
        if (mRegistered) {
            return;
        }

        mRegistered = mAudioManager.registerRemoteController(mRemoteController);

        if (mRegistered) {
            final int size = getResources().getDimensionPixelSize(R.dimen.artwork_size);
            mRemoteController.setArtworkConfiguration(size, size);
            //setSynchronizationMode(mRemoteController, RemoteController.POSITION_SYNCHRONIZATION_CHECK);
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
    public void setClientUpdateListener(RemoteController.OnClientUpdateListener listener) {
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
