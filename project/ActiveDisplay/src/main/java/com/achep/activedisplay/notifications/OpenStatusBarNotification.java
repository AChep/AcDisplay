/*
 * Copyright (C) 2013 AChep@xda <artemchep@gmail.com>
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
import com.achep.activedisplay.blacklist.AppConfig;
import com.achep.activedisplay.blacklist.Blacklist;
import com.achep.activedisplay.notifications.parser.Parser;

/**
 * Created by Artem on 23.01.14.
 */
public class OpenStatusBarNotification {

    private final StatusBarNotification mStatusBarNotification;
    private NotificationData mNotificationData;

    private OpenStatusBarNotification(StatusBarNotification notification) {
        mStatusBarNotification = notification;
    }

    public void parse(Context context) {
        mNotificationData = Parser.parse(context, mStatusBarNotification);
    }

    public static OpenStatusBarNotification wrap(StatusBarNotification notification) {
        return new OpenStatusBarNotification(notification);
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

    public AppConfig getAppConfig(Blacklist blacklist) {
        return blacklist.fill(AppConfig.wrap(mStatusBarNotification.getPackageName()));
    }

    public boolean isHidden(Blacklist blacklist) {
        return getAppConfig(blacklist).isHiddenReal();
    }

    public boolean isRestricted(Blacklist blacklist) {
        return getAppConfig(blacklist).isRestrictedReal();
    }
}
