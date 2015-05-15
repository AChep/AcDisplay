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
package com.achep.base.utils;

/**
 * Created by Artem on 03.01.14.
 */
public class MathUtils {

    /**
     * Using a simple circle formula, returns {@code true} if the
     * point is in it, {@code false} otherwise.
     */
    public static boolean isInCircle(float x, float y, float x0, float y0, float r) {
        return (x0 += x) * x0 + (y0 += y) * y0 < r * r;
    }

    public static float range(float a, float min, float max) {
        return a < min ? min : a > max ? max : a;
    }

    public static int bool(boolean a) {
        return a ? 1 : 0;
    }

}
