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
package com.achep.acdisplay.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.Toast;

/**
 * Helper class with utils related to toasts (no bacon.)
 *
 * @author Artem Chepurnoy
 */
public class ToastUtils {

    /**
     * Shows toast message with given message shortly.
     *
     * @param text message to show
     * @see #showLong(android.content.Context, CharSequence)
     */
    @NonNull
    public static Toast showShort(@NonNull Context context, @NonNull CharSequence text) {
        return show(context, text, Toast.LENGTH_SHORT);
    }

    @NonNull
    public static Toast showShort(@NonNull Context context, int stringRes) {
        return showShort(context, context.getString(stringRes));
    }

    /**
     * Shows toast message with given message for a long time.
     *
     * @param text message to show
     * @see #showShort(android.content.Context, CharSequence)
     */
    @NonNull
    public static Toast showLong(@NonNull Context context, @NonNull CharSequence text) {
        return show(context, text, Toast.LENGTH_LONG);
    }

    @NonNull
    public static Toast showLong(@NonNull Context context, int stringRes) {
        return showLong(context, context.getString(stringRes));
    }

    @NonNull
    private static Toast show(@NonNull Context context, CharSequence text, int duration) {
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
        return toast;
    }

}
