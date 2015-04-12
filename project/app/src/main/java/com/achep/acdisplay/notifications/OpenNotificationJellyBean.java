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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.RemoteViews;

import org.apache.commons.lang.builder.EqualsBuilder;

/**
 * @author Artem Chepurnoy
 */
// TODO: Find the way to get notification's ID and TAG.
class OpenNotificationJellyBean extends OpenNotification {

    @SuppressWarnings("NullableProblems")
    @NonNull
    private volatile String mPackageName;

    OpenNotificationJellyBean(@NonNull Notification n) {
        super(null, n);
    }

    @Override
    public void load(@NonNull Context context) {
        RemoteViews rvs = getNotification().contentView;
        if (rvs == null) rvs = getNotification().bigContentView;
        if (rvs == null) //noinspection deprecation
            rvs = getNotification().tickerView;
        mPackageName = rvs != null ? rvs.getPackage() : "!2#$%^&*()";

        super.load(context);
    }

    //-- COMPARING INSTANCES --------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getNotification().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        return getNotification().equals(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasIdenticalIds(@Nullable OpenNotification n) {
        if (n == null) {
            return false;
        }

        EqualsBuilder builder = new EqualsBuilder();

        RemoteViews cv = getNotification().contentView;
        RemoteViews cv2 = n.getNotification().contentView;
        if (cv != null && cv2 != null) {
            builder.append(cv.getLayoutId(), cv2.getLayoutId());
        }

        return builder
                .append(getNotification().ledARGB, n.getNotification().ledARGB)
                .append(getPackageName(), n.getPackageName())
                .append(titleText, n.titleText)
                .isEquals();

    }

    //-- OTHER ----------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public String getPackageName() {
        return mPackageName;
    }

}
