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
package com.achep.acdisplay.services.switches;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.achep.acdisplay.notifications.NotificationPresenter;
import com.achep.acdisplay.notifications.OpenNotification;
import com.achep.acdisplay.services.Switch;
import com.achep.base.content.ConfigBase;

/**
 * @author Artem Chepurnoy
 * @see com.achep.acdisplay.ui.fragments.settings.ActiveModeSettings
 * @see com.achep.acdisplay.ui.fragments.settings.KeyguardSettings
 */
public final class WeakNotifiesSwitch extends Switch.Optional implements
        NotificationPresenter.OnNotificationListChangedListener {

    private static final int DURATION = 20 * 1000; // 20 sec.

    private NotificationPresenter mNotificationPresenter;
    private boolean mTriggerCallPending;
    private long mTriggerTimestamp;

    @NonNull
    private final Handler mHandler = new Handler();
    @NonNull
    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            requestInactiveInternal();
        }
    };

    public WeakNotifiesSwitch(
            @NonNull Context context,
            @NonNull Callback callback,
            @NonNull ConfigBase.Option option, boolean isOptionInverted) {
        super(context, callback, option, isOptionInverted);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationPresenter = NotificationPresenter.getInstance();
        mNotificationPresenter.registerListener(this);
    }

    @Override
    public void onDestroy() {
        mNotificationPresenter.unregisterListener(this);
        super.onDestroy();
    }

    @Override
    public boolean isActiveInternal() {
        return SystemClock.elapsedRealtime() - mTriggerTimestamp < DURATION;
    }

    @Override
    public void onNotificationListChanged(@NonNull NotificationPresenter np,
                                          OpenNotification osbn,
                                          int event, boolean isLastEventInSequence) {
        if (event != NotificationPresenter.EVENT_REMOVED) {
            if (isLastEventInSequence) {
                triggerNotification();
            } else mTriggerCallPending = true;
        } else if (isLastEventInSequence && mTriggerCallPending) {
            triggerNotification();
        }
    }

    private void triggerNotification() {
        mTriggerCallPending = false;
        mTriggerTimestamp = SystemClock.elapsedRealtime();
        mHandler.removeCallbacks(mRunnable);
        mHandler.postDelayed(mRunnable, DURATION);
    }

}
