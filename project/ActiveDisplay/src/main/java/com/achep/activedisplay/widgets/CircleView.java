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
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.achep.activedisplay.R;
import com.achep.activedisplay.animations.CircleViewAnimation;

/**
 * Created by achep on 19.04.14.
 */
public class CircleView extends View {

    public static final int ACTION_START = 0;
    public static final int ACTION_UNLOCK = 1;
    public static final int ACTION_CANCELED = 2;

    private float[] mPoint = new float[2];

    private boolean mCanceled;
    private boolean mRadiusAimed;
    private float mRadiusDecreaseThreshold;
    private float mRadiusTarget;
    private float mRadiusMax;
    private float mRadius;
    private Paint mPaint;

    private Drawable mDrawable;

    private CircleViewAnimation mAnimationOut;
    private Callback mCallback;

    private Handler mHandler;
    private Runnable mDelayedCancel;

    public interface Callback {

        public void onCircleEvent(float radius, float ratio, int event);
    }

    public CircleView(Context context) {
        super(context);
        init();
    }

    public CircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        Resources res = getContext().getResources();

        mHandler = new Handler();
        mDelayedCancel = new Runnable() {
            @Override
            public void run() {
                cancelCircle(false);
            }
        };

        mAnimationOut = new CircleViewAnimation(this, 0, 0);
        mAnimationOut.setInterpolator(new AccelerateDecelerateInterpolator());
        mAnimationOut.setDuration(res.getInteger(android.R.integer.config_mediumAnimTime));

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(2);

        mRadiusTarget = res.getDimension(R.dimen.circle_radius_target);
        mRadiusDecreaseThreshold = res.getDimension(R.dimen.circle_radius_decrease_threshold);

        mDrawable = res.getDrawable(R.drawable.ic_unlock);
        mDrawable.setBounds(0, 0,
                mDrawable.getIntrinsicWidth(),
                mDrawable.getIntrinsicHeight());

        setRadius(0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final float ratio = calculateRatio();
        float radius;

        // Darkening background
        canvas.drawColor(Color.argb((int) (200 * calculateRatio()), 0, 0, 0));

        if (mRadiusAimed) {

            // Indicate that target radius is aimed.
            mPaint.setAlpha(35);
            radius = (float) Math.sqrt(mRadius / 70) * 70;
            canvas.drawCircle(mPoint[0], mPoint[1], radius, mPaint);
        }

        // Draw unlock circle
        mPaint.setAlpha((int) (255 * Math.pow(ratio, 0.33f)));
        radius = (float) Math.sqrt(mRadius / 50) * 50;
        canvas.drawCircle(mPoint[0], mPoint[1], radius, mPaint);

        // Draw unlock icon at the center of circle
        float scale = 0.5f + 0.5f * ratio;
        canvas.save();
        canvas.translate(
                mPoint[0] - mDrawable.getMinimumWidth() / 2 * scale,
                mPoint[1] - mDrawable.getMinimumHeight() / 2 * scale);
        canvas.scale(scale, scale);
        mDrawable.draw(canvas);
        canvas.restore();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeCallbacks(mDelayedCancel);
    }

    public boolean onTouchEvent2(MotionEvent event) {
        // Cancel current circle on two-fingers touch (or more.)
        if (event.getPointerCount() > 1) {
            cancelCircle(false);
            return false;
        }

        // If current circle is canceled then
        // ignore all actions except of touch down (to reset state.)
        if (mCanceled && event.getActionMasked() != MotionEvent.ACTION_DOWN) return false;

        final float x = event.getX();
        final float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                clearAnimation();

                // Initialize circle
                mRadiusMax = 0;
                mPoint[0] = x;
                mPoint[1] = y;
                mCanceled = false;

                mHandler.postDelayed(mDelayedCancel, 1000);
                mCallback.onCircleEvent(mRadius, calculateRatio(), ACTION_START);
            case MotionEvent.ACTION_MOVE:
                setRadius((float) Math.hypot(x - mPoint[0], y - mPoint[1]));

                // Cancel the circle if it's decreasing.
                if (mRadiusMax - mRadius > mRadiusDecreaseThreshold) {
                    mRadiusAimed = false;
                    cancelCircle(false);
                    break;
                }

                if (calculateRatio() == 1) {
                    if (!mRadiusAimed) {
                        mRadiusAimed = true;
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    }
                } else if (mRadiusAimed) {
                    mRadiusAimed = false;
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mRadiusAimed) {
                    mCallback.onCircleEvent(mRadius, calculateRatio(), ACTION_UNLOCK);
                }
            case MotionEvent.ACTION_CANCEL:
                mHandler.removeCallbacks(mDelayedCancel);
                cancelCircle(mRadiusAimed);
                break;
            default:
                return super.onTouchEvent(event);
        }
        return false;
    }

    private void cancelCircle(boolean unlockAnimation) {
        if (mCanceled) {
            return;
        }

        mCanceled = true;
        mCallback.onCircleEvent(mRadius, calculateRatio(), ACTION_CANCELED);

        mAnimationOut.setStartOffset(unlockAnimation ? 150 : 0);
        mAnimationOut.setRange(mRadius, 0f);
        startAnimation(mAnimationOut);
    }

    private float calculateRatio() {
        return Math.min(mRadius / mRadiusTarget, 1);
    }

    public void setRadius(float radius) {
        mRadius = radius;
        mRadiusMax = Math.max(mRadiusMax, mRadius);

        float ratio = calculateRatio();
        mDrawable.setAlpha((int) (255 * Math.pow(ratio, 3)));

        postInvalidateOnAnimation();
    }

    public void setRadiusTarget(float radiusTarget) {
        mRadiusTarget = radiusTarget;
        setRadius(mRadius);
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

}
