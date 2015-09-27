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
package com.achep.acdisplay.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.achep.base.tests.Check;

import java.lang.reflect.Method;

/**
 * Created by Artem on 02.01.14.
 */
public class PendingIntentUtils {

    /**
     * Perform the operation associated with this PendingIntent.
     */
    public static boolean sendPendingIntent(@Nullable PendingIntent pi) {
        return sendPendingIntent(pi, null, null);
    }

    /**
     * Perform the operation associated with this PendingIntent.
     */
    public static boolean sendPendingIntent(@Nullable PendingIntent pi, Context context, Intent intent) {
        if (pi != null)
            try {
                // The Context of the caller may be null if
                // <var>intent</var> is also null.
                Check.getInstance().isTrue(context != null || intent == null);
                //noinspection ConstantConditions
                pi.send(context, 0, intent);
                return true;
            } catch (PendingIntent.CanceledException e) { /* unused */ }
        return false;
    }

    /**
     * Check whether this PendingIntent will launch an Activity.
     */
    public static boolean isActivity(@NonNull PendingIntent pi) {
        Method method;
        try {
            method = PendingIntent.class.getDeclaredMethod("isActivity");
            method.setAccessible(true);
            return (boolean) method.invoke(pi);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true; // We must use `true` ss the fallback value
    }

}
