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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.achep.acdisplay.Device;
import com.achep.acdisplay.utils.PackageUtils;

import org.apache.commons.lang.builder.EqualsBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Artem on 23.01.14.
 */
public class OpenNotification {

    private StatusBarNotification mStatusBarNotification;

    private Notification mNotification;
    private NotificationData mNotificationData;

    private boolean mMine;

    /**
     * Creates empty notification instance.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static OpenNotification newInstance(@NonNull StatusBarNotification sbn) {
        return new OpenNotification(sbn, sbn.getNotification());
    }

    /**
     * Creates empty notification instance.
     *
     * @deprecated Use {@link #newInstance(android.service.notification.StatusBarNotification)}
     * instead.
     */
    @Deprecated
    public static OpenNotification newInstance(@NonNull Notification n) {
        return new OpenNotificationCompat(n);
    }

    protected OpenNotification(StatusBarNotification sbn, @NonNull Notification n) {
        mStatusBarNotification = sbn;
        mNotification = n;

        mNotificationData = new NotificationData();
    }

    public void loadData(Context context) {
        mNotificationData.loadNotification(context, this, false);
        mMine = TextUtils.equals(getPackageName(), PackageUtils.getName(context));
    }

    /**
     * @return The {@link android.service.notification.StatusBarNotification} or
     * {@code null}.
     */
    @Nullable
    public StatusBarNotification getStatusBarNotification() {
        return mStatusBarNotification;
    }

    /**
     * @return The {@link Notification} supplied to
     * {@link android.app.NotificationManager#notify(int, Notification)}.
     */
    @NonNull
    public Notification getNotification() {
        return mNotification;
    }

    @NonNull
    public NotificationData getNotificationData() {
        return mNotificationData;
    }

    //-- COMPARING INSTANCES --------------------------------------------------

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
    @SuppressLint("NewApi")
    @SuppressWarnings("ConstantConditions")
    public boolean hasIdenticalIds(@Nullable OpenNotification n) {
        if (n == null) return false;
        StatusBarNotification sbn = getStatusBarNotification();
        StatusBarNotification sbn2 = n.getStatusBarNotification();
        if (Device.hasLemonCakeApi()) {
            // FIXME: Android L reflections.
            // service.cancelNotification(notification.getKey());
            try {
                Method method = sbn.getClass().getMethod("getKey");
                method.setAccessible(true);
                String key = (String) method.invoke(sbn);
                String key2 = (String) method.invoke(sbn2);

                return new EqualsBuilder()
                        .append(key, key2)
                        .isEquals();
            } catch (NoSuchMethodException
                    | InvocationTargetException
                    | IllegalAccessException e) { /* sad, but true */ }
        }
        return new EqualsBuilder()
                .append(sbn.getId(), sbn2.getId())
                .append(getPackageName(), n.getPackageName())
                .append(sbn.getTag(), sbn2.getTag())
                .isEquals();
    }

    //-- BASICS ---------------------------------------------------------------

    /**
     * Dismisses this notification from system.
     *
     * @see NotificationUtils#dismissNotification(OpenNotification)
     */
    public void dismiss() {
        NotificationUtils.dismissNotification(this);
    }

    /**
     * Performs a click on notification.<br/>
     * To be clear it is not a real click but launching its content intent.
     *
     * @return {@code true} if succeed, {@code false} otherwise
     * @see NotificationUtils#startContentIntent(OpenNotification)
     */
    public boolean click() {
        return NotificationUtils.startContentIntent(this);
    }

    /**
     * Clears all notification's resources.
     */
    public void recycle() {
        mNotificationData.recycle();
    }

    /**
     * @return {@code true} if notification has been posted from my own application,
     * {@code false} otherwise (or the package name can not be get).
     */
    public boolean isMine() {
        return mMine;
    }

    /**
     * @return {@code true} if notification can be dismissed by user, {@code false} otherwise.
     */
    public boolean isDismissible() {
        return isClearable();
    }

    /**
     * Convenience method to check the notification's flags for
     * either {@link Notification#FLAG_ONGOING_EVENT} or
     * {@link Notification#FLAG_NO_CLEAR}.
     */
    public boolean isClearable() {
        return ((mNotification.flags & Notification.FLAG_ONGOING_EVENT) == 0)
                && ((mNotification.flags & Notification.FLAG_NO_CLEAR) == 0);
    }

    /**
     * @return the package name of notification's parent.
     */
    @SuppressLint("NewApi")
    @NonNull
    public String getPackageName() {
        return mStatusBarNotification.getPackageName();
    }

}
