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
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang.builder.EqualsBuilder;

/**
 * @author Artem Chepurnoy
 */
@TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
class OpenNotificationKitKatWatch extends OpenNotificationJellyBeanMR2 {

    OpenNotificationKitKatWatch(@NonNull StatusBarNotification sbn, @NonNull Notification n) {
        super(sbn, n);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasIdenticalIds(@Nullable OpenNotification n) {
        if (n == null) return false;
        StatusBarNotification sbn = getStatusBarNotification();
        StatusBarNotification sbn2 = n.getStatusBarNotification();
        assert sbn2 != null;
        return new EqualsBuilder()
                .append(sbn2.getKey(), sbn.getKey())
                .isEquals();
    }

}
