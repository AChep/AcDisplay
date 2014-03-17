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
package com.achep.activedisplay.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by Artem on 15.03.14.
 */
public class ToastUtils {

    public static void showShort(Context context, CharSequence text) {
        show(context, text, Toast.LENGTH_SHORT);
    }

    public static void showLong(Context context, CharSequence text) {
        show(context, text, Toast.LENGTH_LONG);
    }

    private static void show(Context context, CharSequence text, int duration) {
        Toast.makeText(context, text, duration).show();
    }

}
