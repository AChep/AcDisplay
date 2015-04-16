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
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class OpenNotificationJellyBeanMR2 extends OpenNotification {

    OpenNotificationJellyBeanMR2(@NonNull StatusBarNotification sbn, @NonNull Notification n) {
        super(sbn, n);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public StatusBarNotification getStatusBarNotification() {
        assert super.getStatusBarNotification() != null;
        return super.getStatusBarNotification();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getStatusBarNotification().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        return getStatusBarNotification().equals(o);
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
                .append(sbn.getId(), sbn2.getId())
                .append(getPackageName(), n.getPackageName())
                .append(sbn.getTag(), sbn2.getTag())
                .isEquals();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClearable() {
        return getStatusBarNotification().isClearable();
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public String getPackageName() {
        return getStatusBarNotification().getPackageName();
    }

}
