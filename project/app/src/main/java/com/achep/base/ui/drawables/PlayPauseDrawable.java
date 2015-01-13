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
package com.achep.base.ui.drawables;

/**
 * Created by Artem Chepurnoy on 18.10.2014.
 */
public class PlayPauseDrawable extends TransformationDrawable {

    /**
     * Pause icon
     */
    private static final float[][] VERTEX_PAUSE = {
            {0.3f, 0.0f, 0.0f, 0.3f, 0.3f, 0.7f, 0.7f, 1.0f, 1.0f},
            {0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f}
    };

    /**
     * Play icon
     */
    private static final float[][] VERTEX_PLAY = {
            {1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f},
            {0.5f, 0.5f, 0.0f, 1.0f, 0.5f, 0.5f, 0.0f, 1.0f, 0.5f}
    };

    /**
     * Stop icon
     */
    private static final float[][] VERTEX_STOP = {
            {0.5f, 0.0f, 0.0f, 0.5f, 0.5f, 0.5f, 0.5f, 1.0f, 1.0f},
            {0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f}
    };

    public PlayPauseDrawable() {
        super(VERTEX_PAUSE, VERTEX_PLAY, VERTEX_STOP);
    }

    public void transformToPause() {
        transformToShape(0);
    }

    public void transformToPlay() {
        transformToShape(1);
    }

    public void transformToStop() {
        transformToShape(2);
    }

}
