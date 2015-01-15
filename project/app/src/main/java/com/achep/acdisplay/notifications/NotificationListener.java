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

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;

import com.achep.acdisplay.services.MediaService;
import com.achep.base.Device;

/**
 * Created by Artem Chepurnoy on 15.01.2015.
 */
public abstract class NotificationListener {

    public static NotificationListener newInstance() {
        if (Device.hasLollipopApi()) {
            return new NotificationListenerLollipop();
        } else if (Device.hasJellyBeanMR2Api()) {
            return new NotificationListenerJellyBeanMR2();
        }

        throw new UnsupportedOperationException();
    }

    public abstract void onListenerBind(
            @NonNull MediaService service);

    public abstract void onListenerConnected(
            @NonNull NotificationListenerService service);

    public abstract void onNotificationPosted(
            @NonNull NotificationListenerService service,
            @NonNull StatusBarNotification sbn);

    public abstract void onNotificationRemoved(
            @NonNull NotificationListenerService service,
            @NonNull StatusBarNotification sbn);

    public abstract void onListenerUnbind(
            @NonNull MediaService mediaService);
}
