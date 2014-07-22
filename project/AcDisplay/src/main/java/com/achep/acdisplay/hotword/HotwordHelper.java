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

package com.achep.acdisplay.hotword;

import android.content.Context;
import android.content.SharedPreferences;

import com.achep.acdisplay.Build;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.blacklist.SharedList;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
* Created by achep on 25.06.14.
*/
public class HotwordHelper {

    /**
     * @return {@code true} if device supports Hotword feature, {@code false} otherwise.
     */
    public static boolean isSupported(Context context) {
        return Build.DEBUG || true;
    }

}