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

/**
 * Contains params of current device. This is nice because we can override
 * some here to test compatibility with old API.
 *
 * @author Artem Chepurnoy
 */
public class Device {

    /**
     * @return {@code true} if device is running
     * {@link Build.VERSION_CODES#L Lemon Cake} or higher, {@code false} otherwise.
     */
    public static boolean hasLemonCakeApi() {
        return Build.VERSION.SDK_INT >= 20; // Build.VERSION_CODES.L;
    }

    /**
     * @return {@code true} if device is running
     * {@link Build.VERSION_CODES#KITKAT KitKat} or higher, {@code false} otherwise.
     */
    public static boolean hasKitKatApi() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    /**
     * @return {@code true} if device is running
     * {@link Build.VERSION_CODES#JELLY_BEAN_MR2 Jelly Bean 4.3} or higher, {@code false} otherwise.
     */
    public static boolean hasJellyBeanMR2Api() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

}
