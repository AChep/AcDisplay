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
package com.achep.acdisplay.widgets;

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

import com.achep.acdisplay.R;
import com.achep.acdisplay.animations.CircleRadiusAnimation;
import com.achep.acdisplay.utils.MathUtils;

/**
 * Created by achep on 19.04.14.
 */
public class CircleView extends View {

    public static final int ACTION_START = 0;
    public static final int ACTION_UNLOCK = 1;
    public static final int ACTION_CANCELED = 2;

    private float[] mPoint = new float[2];

    // Target
    private boolean mRadiusTargetAimed;
    private float mRadiusTarget;

    // Decreasing detection
    private float mRadiusDecreaseThreshold;
    private float mRadiusMaxPeak;

    /**
     * Real radius of the circle, measured by touch.
     */
    private float mRadius;

    /**
     * Radius of the drawn circle.
     *
     * @see #setRadiusDrawn(float)
     */
    private float mRadiusDrawn;


    private boolean mCanceled;
    private float mDarkening;

    private Drawable mDrawable;
    private Paint mPaint;

    private int mAnimationOverDuration;
    private CircleRadiusAnimation mAnimationOver;
    private CircleRadiusAnimation mAnimationOut;
    private Callback mCallback;

    private Handler mHandler = new Handler();
    private Runnable mDelayedCancel = new Runnable() {
        @Override
        public void run() {
            cancelCircle(false);
        }
    };
    private Runnable mDelayedUnlock = new Runnable() {
        @Override
        public void run() {
            mCallback.onCircleEvent(mRadius, calculateRatio(), ACTION_UNLOCK);
        }
    };

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

        mAnimationOverDuration = res.getInteger(android.R.integer.config_shortAnimTime);
        mAnimationOver = new CircleRadiusAnimation(this, 0, 0, true);
        mAnimationOver.setInterpolator(new AccelerateDecelerateInterpolator());
        mAnimationOver.setDuration(mAnimationOverDuration);

        mAnimationOut = new CircleRadiusAnimation(this, 0, 0, false);
        mAnimationOut.setInterpolator(new AccelerateDecelerateInterpolator());
        mAnimationOut.setDuration(res.getInteger(android.R.integer.config_mediumAnimTime));

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.WHITE);

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

        // Darkening background
        int alpha = (int) (mDarkening * 255);
        alpha += (int) ((255 - alpha) * ratio * 0.7f); // Change alpha dynamically
        canvas.drawColor(Color.argb(alpha, 0, 0, 0));

        // Draw unlock circle
        mPaint.setAlpha((int) (255 * Math.pow(ratio, 0.33f)));
        canvas.drawCircle(mPoint[0], mPoint[1], mRadiusDrawn, mPaint);

        if (mRadiusTargetAimed) { // We're ready to unlock

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
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeCallbacks(mDelayedUnlock);
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
                mRadiusMaxPeak = 0;
                mPoint[0] = x;
                mPoint[1] = y;
                mCanceled = false;

                mHandler.removeCallbacks(mDelayedCancel);
                mHandler.postDelayed(mDelayedCancel, 1000);
                mCallback.onCircleEvent(mRadius, calculateRatio(), ACTION_START);
            case MotionEvent.ACTION_MOVE:
                setRadius((float) Math.hypot(x - mPoint[0], y - mPoint[1]));

                // Cancel the circle if it's decreasing.
                if (mRadiusMaxPeak - mRadius > mRadiusDecreaseThreshold) {
                    mRadiusTargetAimed = false;
                    cancelCircle(false);
                    break;
                }

                if (calculateRatio() == 1) {
                    if (!mRadiusTargetAimed) {
                        mRadiusTargetAimed = true;
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    }
                } else if (mRadiusTargetAimed) {
                    mRadiusTargetAimed = false;
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mRadiusTargetAimed) {
                    // Calculate biggest distance between center of
                    // the circle and view's corners.
                    float distance = 0f;
                    int[] corners = new int[]{
                            0, 0, // top left
                            0, getHeight(), // bottom left
                            getWidth(), getHeight(), // bottom right
                            getWidth(), 0 // top right
                    };
                    for (int i = 0; i < corners.length; i += 2) {
                        double c = Math.hypot(
                                mPoint[0] - corners[i],
                                mPoint[1] - corners[i + 1]);
                        if (c > distance) distance = (float) c;
                    }
                    mAnimationOver.setRange(mRadiusDrawn, distance);

                    // Start Android L-like ripple animation.
                    startAnimation(mAnimationOver);

                    mHandler.postDelayed(mDelayedUnlock, mAnimationOverDuration);
                    mHandler.removeCallbacks(mDelayedCancel);
                    break;
                }
            case MotionEvent.ACTION_CANCEL:
                mHandler.removeCallbacks(mDelayedCancel);
                cancelCircle(mRadiusTargetAimed);
                break;
            default:
                return super.onTouchEvent(event);
        }
        return true;
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

    /**
     * Sets the radius of fake circle.
     *
     * @param radius radius to set
     */
    public void setRadius(float radius) {
        mRadius = radius;

        // Save maximum radius for detecting
        // decreasing of the circle's size.
        if (mRadius > mRadiusMaxPeak) {
            mRadiusMaxPeak = mRadius;
        }

        // Update unlock icon's transparency.
        float ratio = calculateRatio();
        mDrawable.setAlpha((int) (255 * Math.pow(ratio, 3)));

        // Update the size of the unlock circle.
        radius = (float) Math.sqrt(mRadius / 50f) * 50f;
        setRadiusDrawn(radius);
    }

    public void setRadiusDrawn(float radius) {
        mRadiusDrawn = radius;
        postInvalidateOnAnimation();
    }

    public void setRadiusTarget(float radiusTarget) {
        mRadiusTarget = radiusTarget;
        setRadius(mRadius);
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void setDarkening(float darkening) {
        mDarkening = MathUtils.range(darkening, 0f, 1f);
        invalidate();
    }

    public float getDarkening() {
        return mDarkening;
    }

}
