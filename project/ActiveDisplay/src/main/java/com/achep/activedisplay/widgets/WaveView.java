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
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.achep.activedisplay.Project;

/**
 * Created by Artem on 23.12.13.
 */
public class WaveView extends View {

    private static final String TAG = "WaveView";

    private static final int DURATION = 300; // ms.
    @SuppressWarnings("PointlessBooleanExpression")
    private static final boolean DEBUG = Project.DEBUG && false;

    private float mProgress;
    private Paint mPaint;
    private Paint mDebugPaint;

    private int mTop;
    private int mCenter;
    private int mBottom;
    private int mPadding;

    private float mPointRadius;
    private int mPointNumber;

    // Expand animation
    private final Handler mHandler = new Handler();
    private final R mRunnable = new R();

    private class R implements Runnable {

        private long end;

        @Override
        public void run() {
            final long now = SystemClock.uptimeMillis();
            if (now > end) {
                mProgress = 1f;
            } else {
                mProgress = 1f - (float) (end - now) / DURATION;
                mHandler.postDelayed(mRunnable, 16);
            }

            // Refresh the view
            postInvalidateOnAnimation();
        }

        public void prepare() {
            end = SystemClock.uptimeMillis() + DURATION;
        }

        public void cancel() {
            mProgress = 0f;
            mHandler.removeCallbacks(this);
            postInvalidateOnAnimation();
        }
    }


    public WaveView(final Context context) {
        this(context, null);
    }

    public WaveView(final Context context, final AttributeSet attrs) {
        this(context, attrs, com.achep.activedisplay.R.attr.waveViewStyle);
    }

    public WaveView(Context context, AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);

        mPaint = new Paint();
        mPaint.setColor(Color.WHITE);

        if (DEBUG) {
            mDebugPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mDebugPaint.setColor(Color.GREEN);
            mDebugPaint.setStrokeWidth(4);
        }

        final TypedArray attributes = context.obtainStyledAttributes(attrs,
                com.achep.activedisplay.R.styleable.WaveView, defStyle, 0);

        assert attributes != null;
        setPointRadius(attributes.getDimension(com.achep.activedisplay.R.styleable.WaveView_point_radius, 3));
        setPointNumber(attributes.getInt(com.achep.activedisplay.R.styleable.WaveView_point_number, 7));

        attributes.recycle();
    }

    /**
     * <pre> Visualisation:
     *     ---  top
     *      *
     *      *
     *      *
     *     --- padding
     *      -  center
     *     --- padding
     *      *
     *     --- bottom
     */
    public void init(int top, int center, int bottom, int padding) {
        mTop = top;
        mCenter = center;
        mBottom = bottom;
        mPadding = padding;
    }

    public void setPointRadius(float radius) {
        mPointRadius = radius;
    }

    public void setPointNumber(int number) {
        mPointNumber = number;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelExpand();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mPointRadius == 0) return;

        int centerTop = mCenter - mPadding;
        int centerBottom = mCenter + mPadding;

        drawGlow(canvas, mTop, centerTop, mProgress, false);
        drawGlow(canvas, centerBottom, mBottom, mProgress, true);

        if (DEBUG) {
            mDebugPaint.setColor(Color.GREEN);
            canvas.drawLine(0, mTop, 200, mTop, mDebugPaint);
            mDebugPaint.setColor(Color.RED);
            canvas.drawLine(0, centerTop, 200, centerTop, mDebugPaint);
            mDebugPaint.setColor(Color.BLUE);
            canvas.drawLine(0, centerBottom, 200, centerBottom, mDebugPaint);
            mDebugPaint.setColor(Color.YELLOW);
            canvas.drawLine(0, mBottom, 200, mBottom, mDebugPaint);
        }
    }

    private void drawGlow(Canvas canvas, int top, int bottom, float progress, boolean inverse) {
        if (top > bottom) {
            if (Project.DEBUG) Log.d(TAG, "Skipped wave draw: top=" + top + " bottom=" + bottom);
            return;
        }

        // TODO: Refactor that shit.
        final int x = canvas.getWidth() / 2;
        final int height = bottom - top;
        final float hprogress = progress * 1.6f * height;
        float alpha = 1 - Math.abs(mProgress * 2 - 1);
        if (alpha > 1) alpha = 1;
        for (int i = 0; i < mPointNumber; i++) {
            float y = height / mPointNumber * i;
            float a = 0.7f - Math.abs(y - hprogress) / height;
            if (y > hprogress) a /= 4;
            if (a < 0) a = 0;
            mPaint.setAlpha((int) (255f * alpha * a));
            canvas.drawCircle(x, inverse ? top + y : bottom - y, mPointRadius, mPaint);
        }

    }

    /**
     * Starts expand animation.
     */
    public void animateExpand() {
        mRunnable.prepare();
        mHandler.post(mRunnable);
    }

    public void cancelExpand() {
        mRunnable.cancel();
    }
}
