/*
 * Copyright (C) 2013-2014 AChep@xda <artemchep@gmail.com>
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
package com.achep.activedisplay.notifications;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/**
 * Created by Artem on 23.01.14.
 */
class Parser {

    private static final String TAG = "Parser";

    public static NotificationData parse(StatusBarNotification notification) {
        NotificationData nd = new NotificationData();
        try {
            Bundle extras = notification.getNotification().extras;
            nd.titleText = extras.getCharSequence(Notification.EXTRA_TITLE_BIG);
            if (nd.titleText == null)
                nd.titleText = extras.getCharSequence(Notification.EXTRA_TITLE);
            nd.infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT);
            nd.subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
            nd.summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT);
            nd.number = notification.getNotification().number;

            // Large message text
            CharSequence[] textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
            if (textLines != null) {
                StringBuilder sb = new StringBuilder();
                for (CharSequence line : textLines) {
                    sb.append(line);
                    sb.append('\n');
                }
                nd.messageTextLarge = removeSpaces(sb.toString());
            }

            // Small message text
            nd.messageText = extras.getCharSequence(Notification.EXTRA_TEXT);
            if (nd.messageText != null) {
                nd.messageText = removeSpaces(nd.messageText.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.wtf(TAG, "Notification parsing failed.");
        }
        return nd;
    }

    private static String removeSpaces(String string) {
        if (string == null) return null;
        return string
                .replaceAll("(\\s+$|^\\s+)", "")
                .replaceAll("\n+", "\n");
    }

}
