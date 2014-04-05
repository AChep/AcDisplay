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
package com.achep.activedisplay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Artem on 09.01.14.
 */
public class DebugLayerView extends View {

    @SuppressWarnings("PointlessBooleanExpression")
    private static final boolean DEBUG = Project.DEBUG && false;

    private float mTouchX, mTouchY;
    private float mHyperbolaX, mHyperbolaY;

    private Paint mPaint;

    public DebugLayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mPaint = new Paint();
        mPaint.setColor(Color.WHITE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //noinspection PointlessBooleanExpression
        if (!DEBUG) return;

        canvas.drawLine(mHyperbolaX, mHyperbolaY, mTouchX, mTouchY, paint(Color.WHITE));
        canvas.drawCircle(mTouchX, mTouchY, 10, paint(Color.WHITE));
        canvas.drawCircle(mHyperbolaX, mHyperbolaY, 10, paint(Color.YELLOW));
    }

    private Paint paint(int color) {
        mPaint.setColor(color);
        return mPaint;
    }

    public void setHyperbolaPoint(float x, float y) {
        mHyperbolaX = x;
        mHyperbolaY = y;
    }

    public void setTouchPoint(float x, float y) {
        mTouchX = x;
        mTouchY = y;
    }
}
