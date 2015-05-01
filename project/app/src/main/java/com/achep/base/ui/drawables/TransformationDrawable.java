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

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Property;

import com.achep.base.utils.FloatProperty;

/**
 * A class for creating simple transformation buttons. It is very simple to
 * use and perfectly fits simple Material icons' transformation.
 *
 * @author Artem Chepurnoy
 * @see com.achep.base.ui.drawables.PlayPauseDrawable
 */
public abstract class TransformationDrawable extends Drawable {

    private final Path mPath;
    private final Paint mPaint;
    private final float[][][] mVertex;
    private final float[][] mVertexFrom;

    private int mSize = Integer.MAX_VALUE;

    private float mProgress;
    private int mToShape;

    private final Animator mAnimator = ObjectAnimator.ofFloat(this, TRANSFORM, 0f, 1f);
    private final static Property<TransformationDrawable, Float> TRANSFORM =
            new FloatProperty<TransformationDrawable>("setTransformation") {
                @Override
                public void setValue(TransformationDrawable object, float value) {
                    object.setTransformation(value);
                }

                @Override
                public Float get(TransformationDrawable object) {
                    return object.getTransformation();
                }
            };

    protected TransformationDrawable(@NonNull float[][]... vertex) {
        mVertexFrom = new float[2][vertex[0][0].length];
        mVertex = vertex;
        mProgress = 1f;

        mPath = new Path();
        mPath.setFillType(Path.FillType.WINDING);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.FILL);
    }

    public void transformToShape(int i) {
        transformToShape(i, true);
    }

    public void transformToShape(int i, boolean animate) {
        if (setTargetShape(i) && animate && !mAnimator.isRunning()) {
            // If the target shape is new, then we probably should animate the
            // change.
            mAnimator.start();
        }
    }

    /**
     * Sets the size of canvas rectangle.
     *
     * @param size size in pixels.
     */
    public void setSize(int size) {
        mSize = size;
    }

    public boolean setTargetShape(int i) {
        if (mToShape == i) return false;
        updateVertexFrom();
        mToShape = i;
        return true;
    }

    public void setTransformation(float progress) {
        mProgress = progress;
        Rect rect = getBounds();

        final float size = Math.min(Math.min(
                rect.right - rect.left,
                rect.bottom - rect.top), mSize);
        final float left = rect.left + (rect.right - rect.left - size) / 2;
        final float top = rect.top + (rect.bottom - rect.top - size) / 2;

        mPath.reset();
        mPath.moveTo(
                left + calcTransformation(0, 0, progress, size),
                top + calcTransformation(1, 0, progress, size));
        for (int i = 1; i < mVertex[0][0].length; i++) {
            mPath.lineTo(
                    left + calcTransformation(0, i, progress, size),
                    top + calcTransformation(1, i, progress, size));
        }

        mPath.close();
        invalidateSelf();
    }

    public float getTransformation() {
        return mProgress;
    }

    private float calcTransformation(int type, int i, float progress, float size) {
        float v0 = mVertexFrom[type][i] * (1f - progress);
        float v1 = mVertex[mToShape][type][i] * progress;
        return (v0 + v1) * size;
    }

    /**
     * Updates the current `init` state of the icon. While animating, the icon will go
     * from this state to one of the defined {@link #mVertex by vertexes}.
     */
    private void updateVertexFrom() {
        int length = mVertexFrom[0].length;
        for (int i = 0; i < length; i++) {
            mVertexFrom[0][i] = calcTransformation(0, i, mProgress, 1f);
            mVertexFrom[1][i] = calcTransformation(1, i, mProgress, 1f);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        setTransformation(mProgress);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void draw(Canvas canvas) {
        canvas.drawPath(mPath, mPaint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
        invalidateSelf();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setColorFilter(@Nullable ColorFilter cf) {
        mPaint.setColorFilter(cf);
        invalidateSelf();
    }

    /**
     * Specify an optional color filter for the drawable. Note that the color
     * is an int containing alpha as well as r,g,b. This 32bit value is not
     * pre-multiplied, meaning that its alpha can be any value, regardless
     * of the values of r,g,b. See the {@link Color Color class} for more details.
     *
     * @param color the color to be set
     * @see #setColorFilter(ColorFilter)
     */
    public void setColor(int color) {
        mPaint.setColor(color);
        invalidateSelf();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

}
