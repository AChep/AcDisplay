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
package com.achep.activedisplay.notifications;

import android.app.Notification;

/**
 * Created by Artem on 13.01.14.
 */
public class NotificationData {

    public CharSequence titleText;
    public CharSequence messageText;
    public CharSequence messageTextLarge;
    public CharSequence infoText;
    public CharSequence subText;
    public CharSequence summaryText;

    /**
     * The number of events that this notification represents. For example, in a new mail
     * notification, this could be the number of unread messages.
     * <p/>
     * The system may or may not use this field to modify the appearance of the notification. For
     * example, before {@link android.os.Build.VERSION_CODES#HONEYCOMB}, this number was
     * superimposed over the icon in the status bar. Starting with
     * {@link android.os.Build.VERSION_CODES#HONEYCOMB}, the template used by
     * {@link Notification.Builder} has displayed the number in the expanded notification view.
     * <p/>
     * If the number is 0 or negative, it is never shown.
     */
    public int number;

    /**
     * @return {@link #messageTextLarge large message} if not null,
     * otherwise returns {@link #messageText short message}.
     */
    public CharSequence getLargeMessage() {
        return messageTextLarge == null ? messageText : messageTextLarge;
    }

}
