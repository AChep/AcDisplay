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
package com.achep.snake.snake;

import android.util.Log;

import com.achep.snake.Logic;

/**
 * Created by Artem Chepurnoy on 18.11.2014.
 */
public class Animal extends GameObject {

    private static final String TAG = "Animal";

    public static final byte DIRECTION_NONE = -1;
    public static final byte DIRECTION_LEFT = 0;
    public static final byte DIRECTION_RIGHT = 1;
    public static final byte DIRECTION_DOWN = 2;
    public static final byte DIRECTION_UP = 3;

    private static final int[][] DIRECTION_RULE = {
            {-1, 0},
            {1, 0},
            {0, 1},
            {0, -1},
    };

    private byte mDirection = DIRECTION_NONE;

    public Animal(Logic logic) {
        super(logic);
    }

    private int applyDirectionX(byte direction, int x) {
        x = x + DIRECTION_RULE[direction][0];
        x = x < 0
                ? mLogic.getColumnsNumber() - 1
                : x >= mLogic.getColumnsNumber() ? 0 : x;
        return x;
    }

    private int applyDirectionY(byte direction, int y) {
        y = y + DIRECTION_RULE[direction][1];
        y = y < 0
                ? mLogic.getRowsNumber() - 1
                : y >= mLogic.getRowsNumber() ? 0 : y;
        return y;
    }

    public void setDirection(byte direction) {
        if (getHead() == null) return;

        switch (direction) {
            case DIRECTION_DOWN:
            case DIRECTION_UP:
            case DIRECTION_LEFT:
            case DIRECTION_RIGHT:
                Node child = getHead().child;
                if (child != null) {
                    int x = applyDirectionX(direction, getHead().xp);
                    int y = applyDirectionY(direction, getHead().yp);
                    if (x == child.xp && y == child.yp) {
                        Log.d(TAG, "Ignoring reverting direction.");
                        return;
                    }
                }
            case DIRECTION_NONE:
                break;
            default:
                throw new IllegalArgumentException("Trying to set an unknown direction.");
        }

        mDirection = direction;
        Log.d(TAG, "Setting direction to " + direction);
    }

    public void tick() {
        if (getHead() == null) return;
        if (mDirection != DIRECTION_NONE) {
            int x = applyDirectionX(mDirection, getHead().xp);
            int y = applyDirectionY(mDirection, getHead().yp);
            getHead().move(x, y);
            // Log.d(TAG, "Tick x=" + getHead().x + " y=" + getHead().y);
        }
    }

}
