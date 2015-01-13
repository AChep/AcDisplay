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

/**
 * Created by Artem Chepurnoy on 10.01.2015.
 */
public class NullUtils {

    /**
     * @return first non-null {@link CharSequence} from the attrs.
     */
    public static CharSequence whileNotNull(@NonNull CharSequence... objects) {
        return (CharSequence) whileNotNull((Object[]) objects);
    }

    /**
     * @return first non-null {@link java.lang.Object object} from the attrs.
     */
    public static Object whileNotNull(@NonNull Object... objects) {
        for (Object object : objects)
            if (object != null)
                return object;
        return null;
    }

}
