/*
 * Copyright (C) 2013-2014 AChep@xda <artemchep@gmail.com>
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
package com.achep.activedisplay.notifications;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.service.notification.StatusBarNotification;

import com.achep.activedisplay.R;

/**
 * Created by Artem on 23.01.14.
 */
public class OpenStatusBarNotification {

    private final StatusBarNotification mStatusBarNotification;
    private final NotificationData mNotificationData;

    public OpenStatusBarNotification(StatusBarNotification notification) {
        mStatusBarNotification = notification;
        mNotificationData = Parser.parse(notification);
    }

    @Override
    public int hashCode() {
        return mStatusBarNotification.hashCode();
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        return mStatusBarNotification.equals(o);
    }

    // -------------------------------------------------------------------------

    public StatusBarNotification getStatusBarNotification() {
        return mStatusBarNotification;
    }

    public NotificationData getNotificationData() {
        return mNotificationData;
    }

    // -------------------------------------------------------------------------

    // TODO: Return special icon, not the random one.
    public Drawable getSmallIcon(Context context) {
        Drawable icon = NotificationUtils.getDrawable(context,
                mStatusBarNotification,
                mStatusBarNotification.getNotification().icon);
        return icon != null ? icon : context.getResources().getDrawable(R.drawable.stat_test);
    }

    public boolean isBlacklisted(Context context) {
        return Blacklist.getInstance(context)
                .contains(mStatusBarNotification.getPackageName());
    }
}
