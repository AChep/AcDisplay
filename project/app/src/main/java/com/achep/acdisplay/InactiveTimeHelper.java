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
package com.achep.acdisplay;

import android.os.Build;
import android.text.format.Time;

import com.achep.base.utils.DateUtils;

import java.util.GregorianCalendar;

/**
 * Created by Artem on 10.03.14.
 */
public class InactiveTimeHelper {

    public static boolean isInactiveTime(Config config) {
        int now=0;
        if(Build.VERSION.SDK_INT< Build.VERSION_CODES.LOLLIPOP_MR1)
        {
            Time time = new Time();
            time.setToNow();
            now = time.hour * 60 + time.minute;
        }
        else {
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            now = gregorianCalendar.get(GregorianCalendar.HOUR) * 60 + gregorianCalendar.get(GregorianCalendar.MINUTE);
        }

        int from = config.getInactiveTimeFrom();
        int to = config.getInactiveTimeTo();
        return from < to ? now >= from && now <= to : now >= from || now <= to;
    }

}
