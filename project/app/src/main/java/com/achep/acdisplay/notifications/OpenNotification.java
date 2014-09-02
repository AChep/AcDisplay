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

import android.app.Notification;
import android.content.Context;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.achep.acdisplay.utils.PackageUtils;

import org.apache.commons.lang.builder.EqualsBuilder;

/**
 * Created by Artem on 23.01.14.
 */
public class OpenNotification {

    private StatusBarNotification mStatusBarNotification;
    private NotificationData mNotificationData;

    private boolean mMine;

    public OpenNotification(StatusBarNotification notification) {
        mStatusBarNotification = notification;
    }

    public void loadData(Context context) {
        mNotificationData = new NotificationData();
        mNotificationData.loadNotification(context, mStatusBarNotification, false);
        mMine = TextUtils.equals(getPackageName(), PackageUtils.getName(context));
    }

    public StatusBarNotification getStatusBarNotification() {
        return mStatusBarNotification;
    }

    public Notification getNotification() {
        return mStatusBarNotification.getNotification();
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
     * Note, that method is not equals with {@link #equals(Object)} method.
     *
     * @param n notification to compare with.
     * @return {@code true} if notifications are from the same source and will
     * be handled by system as same notifications, {@code false} otherwise.
     */
    public boolean hasIdenticalIds(OpenNotification n) {
        if (n == null) return false;
        return new EqualsBuilder()
                .append(getStatusBarNotification().getId(), n.getStatusBarNotification().getId())
                .append(getPackageName(), n.getPackageName())
                .append(getStatusBarNotification().getTag(), n.getStatusBarNotification().getTag())
                .isEquals();
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

    /**
     * @return {@code true} if notification has been posted from my own application,
     * {@code false} otherwise.
     */
    public boolean isMine() {
        return mMine;
    }

    /**
     * @return {@code true} if notification can be dismissed by user, {@code false} otherwise.
     */
    public boolean isClearable() {
        return mStatusBarNotification.isClearable();
    }

    /**
     * @return the package name of notification's parent.
     */
    @NonNull
    public String getPackageName() {
        return mStatusBarNotification.getPackageName();
    }

}
