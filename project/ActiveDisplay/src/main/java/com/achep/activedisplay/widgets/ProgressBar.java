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

package com.achep.activedisplay.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;

import com.achep.activedisplay.R;

/**
 * {@inheritDoc}
 */
public class ProgressBar extends android.widget.ProgressBar {

    private boolean mMirrored;

    public ProgressBar(Context context) {
        super(context);
    }

    public ProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final TypedArray attributes = context.obtainStyledAttributes(attrs,
                R.styleable.ProgressBar, defStyle, 0);

        assert attributes != null;
        setMirrored(attributes.getBoolean(R.styleable.ProgressBar_mirrored, mMirrored));

        attributes.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!mMirrored) {
            canvas.save();
            canvas.translate(getWidth() - getPaddingEnd(), getPaddingTop());
            canvas.scale(-1.0f, 1.0f);
        }
        super.onDraw(canvas);
        if (!mMirrored) {
            canvas.restore();
        }
    }

    public void setMirrored(boolean mirrored) {
        mMirrored = mirrored;
    }
}
