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
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.service.notification.StatusBarNotification;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.services.MediaService;
import com.achep.acdisplay.utils.PendingIntentUtils;
import com.achep.base.Device;
import com.achep.base.utils.Operator;
import com.achep.base.utils.ResUtils;

/**
 * Created by Artem on 30.12.13.
 */
public class NotificationUtils {

    private static final String TAG = "NotificationUtils";

    /**
     * Return whether the keyguard requires a password to unlock and may
     * have any privacy restrictions.
     */
    public static boolean isSecure(@NonNull Context context) {
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return km.isKeyguardSecure() && km.isKeyguardLocked();
    }

    public static boolean isSecret(@NonNull Context context, @NonNull OpenNotification n,
                                   int minVisibility, int privacyMask) {
        final int privacyMode = Config.getInstance().getPrivacyMode();
        return n.getVisibility() <= minVisibility
                && Operator.bitAnd(privacyMode, privacyMask)
                && isSecure(context);
    }

    /**
     * Imitates a click on given notification: launches content intent and
     * dismisses notification from app and system.
     *
     * @return {@code true} if launched successfully, {@code false} otherwise.
     */
    public static boolean startContentIntent(@NonNull OpenNotification n) {
        PendingIntent pi = n.getNotification().contentIntent;
        if (pi == null) {
            pi = n.getNotification().fullScreenIntent; // nullable
            if (pi != null) Log.i(TAG, "Sending full screen intent, instead of content one!");
        }
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
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static void dismissNotification(@NonNull OpenNotification n) {
        NotificationPresenter.getInstance().removeNotification(n, 0);
        StatusBarNotification sbn = n.getStatusBarNotification();
        if (sbn != null && Device.hasJellyBeanMR2Api()) {
            MediaService service = MediaService.sService;
            if (service != null) {
                // FIXME: Should I call the #deleteIntent?
                PendingIntentUtils.sendPendingIntent(sbn.getNotification().deleteIntent);
                try {
                    if (Device.hasLollipopApi()) {
                        service.cancelNotification(sbn.getKey());
                    } else {
                        service.cancelNotification(
                                sbn.getPackageName(),
                                sbn.getTag(),
                                sbn.getId());
                    }
                } catch (SecurityException e) {
                    // FIXME: Fix disallowed call from unknown listener exception.
                    // java.lang.SecurityException: Disallowed call from unknown listener: android.service.notification.INotificationListener$Stub$Proxy@42339520
                    // at android.os.Parcel.readException(Parcel.java:1465)
                    // at android.os.Parcel.readException(Parcel.java:1419)
                    // at android.app.INotificationManager$Stub$Proxy.cancelNotificationFromListener(INotificationManager.java:469)
                    // at android.service.notification.NotificationListenerService.cancelNotification(NotificationListenerService.java:116)
                    Log.e(TAG, "Failed to dismiss notification.");
                    e.printStackTrace();
                }
            } else {
                Log.e(TAG, "Failed to dismiss notification because notification service is offline.");
            }
        }
    }

    @SuppressLint("NewApi")
    public static void dismissAllNotifications() {
        NotificationPresenter.getInstance().clear(true);
        if (Device.hasJellyBeanMR2Api()) {
            MediaService service = MediaService.sService;
            if (service != null) {
                try {
                    service.cancelAllNotifications();
                } catch (SecurityException e) {
                    // FIXME: Fix disallowed call from unknown listener exception.
                    // java.lang.SecurityException: Disallowed call from unknown listener: android.service.notification.INotificationListener$Stub$Proxy@42339520
                    // at android.os.Parcel.readException(Parcel.java:1465)
                    // at android.os.Parcel.readException(Parcel.java:1419)
                    // at android.app.INotificationManager$Stub$Proxy.cancelNotificationFromListener(INotificationManager.java:469)
                    // at android.service.notification.NotificationListenerService.cancelNotification(NotificationListenerService.java:116)
                    Log.e(TAG, "Failed to dismiss notification.");
                    e.printStackTrace();
                }
            } else {
                Log.e(TAG, "Failed to dismiss notification because notification service is offline.");
            }
        }
    }

    @Nullable
    public static Drawable getDrawable(@NonNull Context context,
                                       @NonNull OpenNotification n,
                                       @DrawableRes int iconRes) {
        Context pkgContext = createContext(context, n);
        if (pkgContext != null)
            try {
                return ResUtils.getDrawable(pkgContext, iconRes);
            } catch (Resources.NotFoundException nfe) { /* unused */ }
        return null;
    }

    @Nullable
    public static Context createContext(@NonNull Context context, @NonNull OpenNotification n) {
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
    public static boolean hasIdenticalIds(@Nullable OpenNotification n,
                                          @Nullable OpenNotification n2) {
        return n == n2 || n != null && n.hasIdenticalIds(n2);
    }

}
