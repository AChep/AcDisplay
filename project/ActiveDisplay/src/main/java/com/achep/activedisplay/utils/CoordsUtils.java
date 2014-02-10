/*
 * Copyright (C) 2013-2014 AChep@xda <artemchep@gmail.com>
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

/**
 * Created by Artem on 11.01.14.
 */
public class CoordsUtils {

    public static void putIntoHyperbola(float[] point, float a) {
        final float originX = point[0], originY = point[1];

        float charge = MathUtils.charge(originX) == MathUtils.charge(originY) ? 1 : -1;
        float k = (float) Math.pow(a, 2) / 2;
        float b = originY - originX * charge;
        float d = (float) Math.sqrt(Math.pow(b, 2) + 4 * k);

        float x, y;
        boolean insideOf;

        x = (-b * charge + d * MathUtils.charge(originX)) / 2;
        y = (k / x * charge);

        if (originX >= 0 && originY >= 0) {
            insideOf = originX <= x || originY <= y;
        } else if (originX < 0 && originY < 0) {
            insideOf = originX >= x || originY >= y;
        } else if (originX >= 0 && originY < 0) {
            insideOf = originX <= x || originY >= y;
        } else {
            insideOf = originX >= x || originY <= y;
        }

        if (!insideOf) {
            point[0] = x;
            point[1] = y;
        }
    }

}
