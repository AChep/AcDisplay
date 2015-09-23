/*
 * Copyright (C) 2015 AChep@xda <artemchep@gmail.com>
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
package com.achep.base.utils.logs;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import timber.log.Timber;

/**
 * A log that includes fancy stack traces in addition to
 * given message. As everything here, it uses {@link Timber}
 * to print logs.
 */
public class TracingLog {

    /**
     * Logs a verbose message.
     */
    public static void v(@NonNull String tag, @Nullable String msg, int depth) {
        if (msg == null) {
            msg = "";
        }
        Timber.tag(tag).v(getLocation(depth) + msg);
    }

    @NonNull
    private static String getLocation(int depth) {
        final String className = TracingLog.class.getName();
        final StackTraceElement[] traces = Thread.currentThread().getStackTrace();
        boolean found = false;

        StringBuilder sb = new StringBuilder();
        for (StackTraceElement trace : traces) {
            try {
                if (found) {
                    if (trace.getClassName().startsWith(className)) continue;
                    Class<?> clazz = Class.forName(trace.getClassName());
                    sb.append("[");
                    sb.append(getClassName(clazz));
                    sb.append(":");
                    sb.append(trace.getMethodName());
                    sb.append(":");
                    sb.append(trace.getLineNumber());
                    sb.append("]");
                    if (--depth == 0) break;
                } else if (trace.getClassName().startsWith(className)) {
                    found = true;
                }
            } catch (ClassNotFoundException ignored) { /* ignore*/ }
        }

        return sb.toString() + ": ";
    }

    @NonNull
    private static String getClassName(@Nullable Class<?> clazz) {
        if (clazz != null) {
            if (!TextUtils.isEmpty(clazz.getSimpleName())) {
                return clazz.getSimpleName();
            }

            return getClassName(clazz.getEnclosingClass());
        }

        return "";
    }

}
