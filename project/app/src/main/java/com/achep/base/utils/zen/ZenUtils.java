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
package com.achep.base.utils.zen;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.support.annotation.NonNull;

import com.achep.base.Device;

/**
 * @author Artem Chepurnoy
 */
public class ZenUtils {

    public static final int ZEN_MODE_DEFAULT = ZenConsts.ZEN_MODE_OFF;

    @SuppressLint("NewApi")
    public static int getValue(@NonNull Context context) {
        if (!Device.hasLollipopApi()) {
            return ZenConsts.ZEN_MODE_OFF;
        }

        ContentResolver cr = context.getContentResolver();
        return Settings.Global.getInt(cr, ZenConsts.ZEN_MODE, ZEN_MODE_DEFAULT);
    }

    /**
     * @return the same name as its constant.
     */
    @NonNull
    public static String zenModeToString(int zenMode) {
        switch (zenMode) {
            case ZenConsts.ZEN_MODE_OFF:
                return "ZEN_MODE_OFF";
            case ZenConsts.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
                return "ZEN_MODE_IMPORTANT_INTERRUPTIONS";
            case ZenConsts.ZEN_MODE_NO_INTERRUPTIONS:
                return "ZEN_MODE_NO_INTERRUPTIONS";
            default:
                return "UNKNOWN_VALUE";
        }
    }

}
