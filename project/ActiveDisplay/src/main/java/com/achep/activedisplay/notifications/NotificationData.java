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
    public int number;

    public CharSequence getLargeMessage() {
        return messageTextLarge == null ? messageText : messageTextLarge;
    }

}
