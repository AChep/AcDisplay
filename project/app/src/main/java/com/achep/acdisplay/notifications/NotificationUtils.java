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
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.util.Log;

import com.achep.acdisplay.Device;
import com.achep.acdisplay.Operator;
import com.achep.acdisplay.services.MediaService;
import com.achep.acdisplay.utils.PendingIntentUtils;

import org.apache.commons.lang.builder.EqualsBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Artem on 30.12.13.
 */
public class NotificationUtils {

    private static final String TAG = "NotificationUtils";

    /**
     * Imitates a click on given notification: launches content intent and
     * dismisses notification from app and system.
     *
     * @return {@code true} if launched successfully, {@code false} otherwise.
     */
    public static boolean startContentIntent(@NonNull OpenNotification n) {
        PendingIntent pi = n.getNotification().contentIntent;
        boolean successful = PendingIntentUtils.sendPendingIntent(pi);
        if (successful && Operator.bitAnd(
                n.getNotification().flags,
                Notification.FLAG_AUTO_CANCEL)) {
            dismissNotification(n);
        }
        return successful;
    }

    /**
     * Dismisses given notification from system and app.
     */
    @SuppressLint("NewApi")
    public static void dismissNotification(@NonNull OpenNotification n) {
        NotificationPresenter.getInstance().removeNotification(n);

        StatusBarNotification sbn = n.getStatusBarNotification();
        if (sbn != null && Device.hasJellyBeanMR2Api()) {
            MediaService service = MediaService.sService;
            if (service != null) {
                if (Device.hasLemonCakeApi()) {
                    // FIXME: Android L reflections.
                    // service.cancelNotification(notification.getKey());
                    try {
                        // Get notification's key.
                        Method method = sbn.getClass().getMethod("getKey");
                        method.setAccessible(true);
                        String key = (String) method.invoke(sbn);

                        // Cancel notification.
                        method = service.getClass().getMethod(
                                "cancelNotification", String.class);
                        method.setAccessible(true);
                        method.invoke(service, key);
                    } catch (NoSuchMethodException
                            | InvocationTargetException
                            | IllegalAccessException e) {
                        Log.wtf(TAG, "Failed to cancel notification:");
                        e.printStackTrace();
                    }
                } else {
                    service.cancelNotification(
                            sbn.getPackageName(),
                            sbn.getTag(),
                            sbn.getId());
                }
            } else {
                Log.e(TAG, "Failed to dismiss notification because notification service is offline.");
            }
        }
    }

    public static Drawable getDrawable(Context context, OpenNotification n, int iconRes) {
        Context pkgContext = createContext(context, n);
        if (pkgContext != null)
            try {
                return pkgContext.getResources().getDrawable(iconRes);
            } catch (Resources.NotFoundException nfe) { /* unused */ }
        return null;
    }

    public static Context createContext(Context context, OpenNotification n) {
        try {
            return context.createPackageContext(n.getPackageName(), Context.CONTEXT_RESTRICTED);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Failed to create notification\'s context");
            return null;
        }
    }

    /**
     * @see com.achep.acdisplay.notifications.OpenNotification#hasIdenticalIds(OpenNotification)
     */
    public static boolean hasIdenticalIds(OpenNotification n, OpenNotification n2) {
        return n == n2 || n != null && n.hasIdenticalIds(n2);
    }
}
