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
package com.achep.base;

import android.os.Build;

/**
 * Contains params of current device. This is nice because we can override
 * some here to test compatibility with old API.
 *
 * @author Artem Chepurnoy
 */
public class Device {

    /**
     * The user-visible SDK version of the framework;
     * its possible values are defined in Build.VERSION_CODES.
     */
    public static final int API_VERSION = Build.VERSION.SDK_INT;

    /**
     * The name of the Android version represented by two or three
     * letters. Example: KK -> KitKat, KKW -> KitKat Watches etc.
     */
    public static final String API_VERSION_NAME_SHORT;

    static {
        switch (API_VERSION) {
            case Build.VERSION_CODES.JELLY_BEAN:
                API_VERSION_NAME_SHORT = "JB";
                break;
            case Build.VERSION_CODES.JELLY_BEAN_MR1:
                API_VERSION_NAME_SHORT = "JB1";
                break;
            case Build.VERSION_CODES.JELLY_BEAN_MR2:
                API_VERSION_NAME_SHORT = "JB2";
                break;
            case Build.VERSION_CODES.KITKAT:
                API_VERSION_NAME_SHORT = "KK";
                break;
            case Build.VERSION_CODES.KITKAT_WATCH:
                API_VERSION_NAME_SHORT = "KKW";
                break;
            case Build.VERSION_CODES.LOLLIPOP:
                API_VERSION_NAME_SHORT = "LP";
                break;
            case Build.VERSION_CODES.LOLLIPOP_MR1:
                API_VERSION_NAME_SHORT = "LP1";
                break;
            case Build.VERSION_CODES.M:
                API_VERSION_NAME_SHORT = "M";
                break;
            default:
                API_VERSION_NAME_SHORT = hasMarshmallowApi() ? "X" : "WTF";
        }
    }

    /**
     * @return {@code true} if device is device supports given API version,
     * {@code false} otherwise.
     */
    public static boolean hasTargetApi(int api) {
        return API_VERSION >= api;
    }

    /**
     * @return {@code true} if device is running
     * {@link Build.VERSION_CODES#M Marshmallow 6} or higher, {@code false} otherwise.
     */
    public static boolean hasMarshmallowApi() {
        return hasTargetApi(Build.VERSION_CODES.M);
    }

    /**
     * @return {@code true} if device is running
     * {@link Build.VERSION_CODES#LOLLIPOP_MR1 Lollipop 5.1} or higher, {@code false} otherwise.
     */
    public static boolean hasLollipopMR1Api() {
        return hasTargetApi(Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    /**
     * @return {@code true} if device is running
     * {@link Build.VERSION_CODES#LOLLIPOP Lollipop} or higher, {@code false} otherwise.
     */
    public static boolean hasLollipopApi() {
        return hasTargetApi(Build.VERSION_CODES.LOLLIPOP);
    }

    /**
     * @return {@code true} if device is running
     * {@link Build.VERSION_CODES#KITKAT_WATCH KitKat watch} or higher, {@code false} otherwise.
     */
    public static boolean hasKitKatWatchApi() {
        return hasTargetApi(Build.VERSION_CODES.KITKAT_WATCH);
    }

    /**
     * @return {@code true} if device is running
     * {@link Build.VERSION_CODES#KITKAT KitKat} or higher, {@code false} otherwise.
     */
    public static boolean hasKitKatApi() {
        return hasTargetApi(Build.VERSION_CODES.KITKAT);
    }

    /**
     * @return {@code true} if device is running
     * {@link Build.VERSION_CODES#JELLY_BEAN_MR2 Jelly Bean 4.3} or higher, {@code false} otherwise.
     */
    public static boolean hasJellyBeanMR2Api() {
        return hasTargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2);
    }

    /**
     * @return {@code true} if device is running
     * {@link Build.VERSION_CODES#JELLY_BEAN_MR1 Jelly Bean 4.2} or higher, {@code false} otherwise.
     */
    public static boolean hasJellyBeanMR1Api() {
        return hasTargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1);
    }

    /**
     * @return {@code true} if device is produced by LGE and probably has
     * buggy software.
     */
    public static boolean isLge() {
        return Build.BRAND.equalsIgnoreCase("lge");
    }

}
