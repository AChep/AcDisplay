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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
        try {
            Method method = Notification.class.getDeclaredMethod("isGroupChild");
            method.setAccessible(true);
            return (boolean) method.invoke(getNotification());
        } catch (NoSuchMethodException
                | InvocationTargetException
                | IllegalAccessException e) {
            Log.e(TAG, "Failed to check for group child.");
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isGroupSummary() {
        try {
            Method method = Notification.class.getDeclaredMethod("isGroupSummary");
            method.setAccessible(true);
            return (boolean) method.invoke(getNotification());
        } catch (NoSuchMethodException
                | InvocationTargetException
                | IllegalAccessException e) {
            Log.e(TAG, "Failed to check for group summary.");
        }
        return false;
    }

}
