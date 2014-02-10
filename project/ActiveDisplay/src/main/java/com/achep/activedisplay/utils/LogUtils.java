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
package com.achep.activedisplay.utils;

import android.text.TextUtils;
import android.util.Log;

/**
 * Created by Artem on 28.01.14.
 */
public class LogUtils {

    private static final String TAG = "achep";

    public static void v(String tag, String msg) {
        Log.v(tag, getLocation() + msg);
    }

    public static void d(String tag, String msg) {
        Log.d(tag, getLocation() + msg);
    }

    public static void i(String tag, String msg) {
        Log.i(tag, getLocation() + msg);
    }

    public static void e(String tag, String msg) {
        Log.e(tag, getLocation() + msg);
    }

    public static void w(String tag, String msg) {
        Log.w(tag, getLocation() + msg);
    }

    public static void track() {
        Log.d(TAG, getLocation() + " <-- tracking");
    }

    private static String getLocation() {
        final String className = LogUtils.class.getName();
        final StackTraceElement[] traces = Thread.currentThread().getStackTrace();

        String deeper = null;
        boolean found = false;
        int i = 0;
        for (StackTraceElement trace : traces) {
            i++;
            try {
                if (found) {
                    if (!trace.getClassName().startsWith(className)) {
                        Class<?> clazz = Class.forName(trace.getClassName());
                        return "[" + getClassName(clazz) + ":" + trace.getMethodName() + "(" + deeper + "):" + trace.getLineNumber() + "]: ";
                    }
                } else if (trace.getClassName().startsWith(className)) {
                    found = true;

                    StringBuilder sb = new StringBuilder();
                    for (int j = i + 2; j < i + 5 && j < traces.length; j++) {
                        sb.append(traces[j].getMethodName());
                        sb.append(" / ");
                    }
                    deeper = sb.toString();
                }
            } catch (ClassNotFoundException e) {
            }
        }

        return Thread.currentThread().getName() + ":[]: ";
    }

    private static String getClassName(Class<?> clazz) {
        if (clazz != null) {
            if (!TextUtils.isEmpty(clazz.getSimpleName())) {
                return clazz.getSimpleName();
            }

            return getClassName(clazz.getEnclosingClass());
        }

        return "";
    }
}
