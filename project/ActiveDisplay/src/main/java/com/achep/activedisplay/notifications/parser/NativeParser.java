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

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.achep.activedisplay.Project;
import com.achep.activedisplay.notifications.NotificationData;

/**
 * Created by Artem on 04.03.14.
 */
final class NativeParser implements Parser.NotificationParser {

    private static final String TAG = "NativeParser";

    @Override
    public NotificationData parse(Context context, StatusBarNotification statusBarNotification, NotificationData data) {
        if (Project.DEBUG) Log.d(TAG, "Extracting notification data via native API.");

        Notification notification = statusBarNotification.getNotification();
        Bundle extras = notification.extras;

        if (extras == null) {
            Log.i(TAG, "");
            return data;
        }

        try {
            data.titleText = extras.getCharSequence(Notification.EXTRA_TITLE_BIG);
            if (data.titleText == null)
                data.titleText = extras.getCharSequence(Notification.EXTRA_TITLE);
            data.infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT);
            data.subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
            data.summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT);
            data.number = notification.number;

            // Large message text
            CharSequence[] textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
            if (textLines != null) {
                StringBuilder sb = new StringBuilder();
                for (CharSequence line : textLines) {
                    sb.append(line);
                    sb.append('\n');
                }
                data.messageTextLarge = Parser.removeSpaces(sb.toString());
            }

            // Small message text
            data.messageText = extras.getCharSequence(Notification.EXTRA_TEXT);
            if (data.messageText != null) {
                data.messageText = Parser.removeSpaces(data.messageText.toString());
            }
        } catch (Exception e) {
            Log.wtf(TAG, "Native notification parsing failed.");
            e.printStackTrace();
        }
        return data;
    }

}
