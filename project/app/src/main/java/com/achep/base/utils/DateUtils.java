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
package com.achep.base.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;

/**
 * Helper class with utils related to date.
 *
 * @author Artem Chepurnoy
 */
public class DateUtils {

    /**
     * Formats given hours and minutes to 24 / 12 hours format
     * (depends on current system settings.)
     *
     * @return Formatted string time
     */
    @NonNull
    public static String formatTime(@NonNull Context context, int h, int m) {
        if (!DateFormat.is24HourFormat(context)) { // 24h formatting
            if (h == 0) h = 12;
            else if (h >= 13) h -= 12;
        }
        return String.format("%02d:%02d", h, m);
    }

}
