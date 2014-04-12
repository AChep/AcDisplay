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

package com.achep.activedisplay.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Created by Artem on 24.03.2014.
 */
public class BitmapUtils {

    public static boolean hasTransparentCorners(Bitmap bitmap) {
        int width = bitmap.getWidth() - 1;
        int height = bitmap.getHeight() - 1;
        return bitmap.getPixel(0, 0) == Color.TRANSPARENT
                || bitmap.getPixel(width, 0) == Color.TRANSPARENT
                || bitmap.getPixel(0, height) == Color.TRANSPARENT
                || bitmap.getPixel(width, height) == Color.TRANSPARENT;
    }

}
