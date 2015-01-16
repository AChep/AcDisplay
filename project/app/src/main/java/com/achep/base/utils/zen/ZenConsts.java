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

import android.support.annotation.NonNull;

/**
 * @author Artem Chepurnoy
 */
// Copy-pasted from the Settings.Global
public class ZenConsts {

    /**
     * Defines global zen mode.
     *
     * @see #ZEN_MODE_OFF
     * @see #ZEN_MODE_IMPORTANT_INTERRUPTIONS
     * @see #ZEN_MODE_NO_INTERRUPTIONS
     */
    @NonNull
    public static final String ZEN_MODE = "zen_mode";

    public static final int ZEN_MODE_OFF = 0;
    public static final int ZEN_MODE_IMPORTANT_INTERRUPTIONS = 1;
    public static final int ZEN_MODE_NO_INTERRUPTIONS = 2;

}
