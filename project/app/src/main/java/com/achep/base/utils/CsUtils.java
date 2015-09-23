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
package com.achep.base.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by Artem on 16.03.14.
 */
public class CsUtils {

    /**
     * Python-like joining strings!
     */
    @Nullable
    public static CharSequence join(@Nullable CharSequence divider, @NonNull CharSequence... array) {
        StringBuilder sb = new StringBuilder();

        boolean divide = false;
        for (CharSequence cs : array) {
            if (cs != null) {
                if (divide && divider != null) {
                    sb.append(divider);
                } else {
                    divide = true;
                }
                sb.append(cs);
            }
        }

        return sb.length() == 0 ? null : sb;
    }

}
