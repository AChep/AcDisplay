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

package com.achep.acdisplay.animations;

import android.view.animation.Animation;
import android.view.animation.Transformation;

import com.achep.acdisplay.widgets.CircleView;

/**
 * Created by achep on 19.04.14.
 */
public class CircleDarkeningAnimation extends Animation {

    private final CircleView mCircleView;
    private float from;
    private float to;

    public CircleDarkeningAnimation(CircleView circleView, float from, float to) {
        super();
        mCircleView = circleView;
        this.from = from;
        this.to = to;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);
        float value = (from + (to - from) * interpolatedTime);

        mCircleView.setDarkening(value);
    }

    public void setRange(float from, float to) {
        this.from = from;
        this.to = to;
    }

}
