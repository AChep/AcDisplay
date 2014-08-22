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

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import org.apache.commons.lang.builder.EqualsBuilder;

/**
 * Created by Artem on 30.12.13.
 */
public class NotificationUtils {

    private static final String TAG = "NotificationUtils";

    public static Drawable getDrawable(Context context, StatusBarNotification n, int iconRes) {
        Context pkgContext = createContext(context, n);
        if (pkgContext != null)
            try {
                return pkgContext.getResources().getDrawable(iconRes);
            } catch (Resources.NotFoundException nfe) { /* unused */ }
        return null;
    }

    public static Context createContext(Context context, StatusBarNotification n) {
        try {
            return context.createPackageContext(n.getPackageName(), Context.CONTEXT_RESTRICTED);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Failed to create notification\'s context");
            return null;
        }
    }

    public static boolean equals(StatusBarNotification n, StatusBarNotification n2) {
        return n == n2 || n != null && n2 != null && new EqualsBuilder()
                .append(n.getId(), n2.getId())
                .append(n.getPackageName(), n2.getPackageName())
                .append(n.getTag(), n2.getTag())
                .isEquals();
    }

    public static boolean equals(OpenNotification n, OpenNotification n2) {
        return n == n2 || n != null && n2 != null && equals(
                n.getStatusBarNotification(),
                n2.getStatusBarNotification());
    }
}
