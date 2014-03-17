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
package com.achep.activedisplay.notifications.parser;

import android.content.Context;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import com.achep.activedisplay.Device;
import com.achep.activedisplay.notifications.NotificationData;

/**
 * Created by Artem on 23.01.14.
 */
public class Parser {

    private static final NotificationParser NATIVE = new NativeParser();
    private static final NotificationParser VIEW_PARSER = new ViewParser();

    public static NotificationData parse(Context context, StatusBarNotification notification) {
        NotificationData data = new NotificationData();
        boolean useViewParser = !Device.hasKitKatApi();

        if (!useViewParser) {
            NATIVE.parse(context, notification, data);
            useViewParser = true
                    && TextUtils.isEmpty(data.titleText)
                    && TextUtils.isEmpty(data.getLargeMessage());
        }
        if (useViewParser) VIEW_PARSER.parse(context, notification, data);
        return data;
    }

    /**
     * Removes all kinds of multiple spaces from given string.
     */
    static String removeSpaces(String string) {
        if (string == null) return null;
        return string
                .replaceAll("(\\s+$|^\\s+)", "")
                .replaceAll("\n+", "\n");
    }

    interface NotificationParser {
        public NotificationData parse(Context context, StatusBarNotification notification, NotificationData nd);
    }

}
