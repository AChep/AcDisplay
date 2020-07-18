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
package com.achep.acdisplay.notifications;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.achep.base.utils.Operator;

import java.util.List;

/**
 * @author Artem Chepurnoy
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class OpenNotificationLollipop extends OpenNotificationKitKatWatch {

    private static final String TAG = "OpenNotificationLp";

    @NonNull
    private final NotificationList mGroupNotifications;

    OpenNotificationLollipop(@NonNull StatusBarNotification sbn, @NonNull Notification n) {
        super(sbn, n);
        mGroupNotifications = new NotificationList(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        for (OpenNotification n : mGroupNotifications) n.onLowMemory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getVisibility() {
        return getNotification().visibility;
    }

    @Override
    protected void loadBrandColor(@NonNull Context context) {
        int color = getNotification().color;
        if (color == Color.BLACK || color == Color.WHITE) {
            super.loadBrandColor(context);
        } else setBrandColor(getNotification().color | 0xFF000000);
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    public String getGroupKey() {
        return getStatusBarNotification().getGroupKey();
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public List<OpenNotification> getGroupNotifications() {
        return mGroupNotifications;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isGroupChild() {
        return getNotification().getGroup() != null && !isGroupSummary();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isGroupSummary() {
        return getNotification().getGroup() != null &&
                Operator.bitAnd(getNotification().flags, Notification.FLAG_GROUP_SUMMARY);
    }

}
