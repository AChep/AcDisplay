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
package com.achep.acdisplay.notifications;

import android.content.Context;
import android.service.notification.StatusBarNotification;

/**
 * Created by Artem on 23.01.14.
 */
public class OpenNotification {

    private StatusBarNotification mStatusBarNotification;
    private NotificationData mNotificationData;

    public OpenNotification(StatusBarNotification notification) {
        mStatusBarNotification = notification;
    }

    public void loadData(Context context) {
        mNotificationData = new NotificationData();
        mNotificationData.loadNotification(context, mStatusBarNotification, false);
    }

    public StatusBarNotification getStatusBarNotification() {
        return mStatusBarNotification;
    }

    public NotificationData getNotificationData() {
        return mNotificationData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return mStatusBarNotification.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        return mStatusBarNotification.equals(o);
    }

    /**
     * Dismisses this notification from system.
     *
     * @see NotificationHelper#dismissNotification(StatusBarNotification)
     */
    public void dismiss() {
        NotificationHelper.dismissNotification(mStatusBarNotification);
    }

    /**
     * Performs a click on notification.<br/>
     * To be clear it is not a real click but launching its content intent.
     *
     * @return {@code true} if succeed, {@code false} otherwise
     * @see NotificationHelper#startContentIntent(StatusBarNotification)
     */
    public boolean click() {
        return NotificationHelper.startContentIntent(mStatusBarNotification);
    }

    /**
     * Clears all notification's resources.
     */
    public void recycle() {
        mNotificationData.recycle();
    }

}
