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
package com.achep.acdisplay.notifications;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.RemoteInput;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Artem Chepurnoy
 */
public class RemoteInputUtils {

    private static final String TAG = "RemoteInputUtils";

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    @Nullable
    public static RemoteInput[] toCompat(@Nullable android.app.RemoteInput[] srcArray) {
        if (srcArray == null) return null;
        RemoteInput[] result = new RemoteInput[srcArray.length];
        try {
            Constructor constructor = RemoteInput.class.getDeclaredConstructor(
                    String.class, CharSequence.class, CharSequence[].class,
                    boolean.class, Bundle.class);
            constructor.setAccessible(true);
            for (int i = 0; i < srcArray.length; i++) {
                android.app.RemoteInput src = srcArray[i];
                result[i] = (RemoteInput) constructor.newInstance(
                        src.getResultKey(), src.getLabel(), src.getChoices(),
                        src.getAllowFreeFormInput(), src.getExtras());
            }
        } catch (NoSuchMethodException
                | InvocationTargetException
                | InstantiationException
                | IllegalAccessException e) {
            Log.e(TAG, "Failed to create the remote inputs!");
            return null;
        }
        return result;
    }

}
