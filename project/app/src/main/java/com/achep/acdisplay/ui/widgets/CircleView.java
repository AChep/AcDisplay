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
package com.achep.acdisplay.ui.widgets;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.acdisplay.ui.animations.CircleRadiusAnimation;
import com.achep.base.utils.MathUtils;

/**
 * Created by achep on 19.04.14.
 */
public class CircleView extends View {

    public static final int ACTION_START = 0;
    public static final int ACTION_UNLOCK = 1;
    public static final int ACTION_UNLOCK_START = 2;
    public static final int ACTION_UNLOCK_CANCEL = 3;
    public static final int ACTION_CANCELED = 4;

    private static final int MSG_CANCEL = -1;

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

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_CANCEL:
                    cancelCircle();
                    break;
                case ACTION_START:
                case ACTION_UNLOCK:
                case ACTION_UNLOCK_START:
                case ACTION_UNLOCK_CANCEL:
                case ACTION_CANCELED:
                    if (mCallback != null) {
                        mCallback.onCircleEvent(mRadius, calculateRatio(), msg.what);
                    }
                    break;
            }
        }
    };

    private int mInnerColor;
    private int mOuterColor;

    public interface Callback {

        public void onCircleEvent(float radius, float ratio, int event);
    }

    public CircleView(Context context) {
        this(context, null);
    }

    public CircleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
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

        mRadiusTarget = res.getDimension(R.dimen.circle_radius_target);
        mRadiusDecreaseThreshold = res.getDimension(R.dimen.circle_radius_decrease_threshold);


        Config config = Config.getInstance();
        mInnerColor = config.getCircleInnerColor();
        mOuterColor = config.getCircleOuterColor();
        float[] innerHsv = new float[3];
        Color.colorToHSV(mInnerColor, innerHsv);
        float innerHsvValue = innerHsv[2];

        mDrawable = res.getDrawable(R.drawable.ic_settings_keyguard_white);
        mDrawable.setBounds(0, 0,
                mDrawable.getIntrinsicWidth(),
                mDrawable.getIntrinsicHeight());
        if (innerHsvValue > 0.5f) { // inverse the drawable
            final float[] matrix = {
                    -1, 0, 0, 0, 0,
                    0, -1, 0, 0, 0,
                    0, 0, -1, 0, 0,
                    0, 0, -0, 1, 0,
            };
            mDrawable = mDrawable.mutate(); // don't affect the original drawable
            mDrawable.setColorFilter(new ColorMatrixColorFilter(matrix));
        }

        setRadius(0);
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final float ratio = calculateRatio();

        // Darkening background
        int alpha = (int) (mDarkening * 255);
        alpha += (int) ((255 - alpha) * ratio * 0.7f); // Change alpha dynamically
        canvas.drawColor(Color.argb(alpha,
                Color.red(mOuterColor),
                Color.green(mOuterColor),
                Color.blue(mOuterColor)));

        // Draw unlock circle
        mPaint.setColor(mInnerColor);
        mPaint.setAlpha((int) (255 * Math.pow(ratio, 1f / 3f)));
        canvas.drawCircle(mPoint[0], mPoint[1], mRadiusDrawn, mPaint);

        if (ratio >= 0.5f) {
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
        mHandler.removeCallbacksAndMessages(null);
    }

    public boolean onTouchEvent2(MotionEvent event) {
        // Cancel current circle on two-fingers touch (or more.)
        if (event.getPointerCount() > 1) {
            cancelCircle();
            return false;
        }

        final int action = event.getActionMasked();

        // If current circle is canceled then
        // ignore all actions except of touch down (to reset state.)
        if (mCanceled && action != MotionEvent.ACTION_DOWN) return false;

        final float x = event.getX();
        final float y = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                clearAnimation();

                // Initialize circle
                mRadiusTargetAimed = false;
                mRadiusMaxPeak = 0;
                mPoint[0] = x;
                mPoint[1] = y;
                mCanceled = false;

                if (mHandler.hasMessages(ACTION_UNLOCK)) {
                    // Cancel unlocking process.
                    mHandler.sendEmptyMessage(ACTION_UNLOCK_CANCEL);
                }

                mHandler.removeCallbacksAndMessages(null);
                mHandler.sendEmptyMessageDelayed(MSG_CANCEL, 1000);
                mHandler.sendEmptyMessage(ACTION_START);
            case MotionEvent.ACTION_MOVE:
                setRadius((float) Math.hypot(x - mPoint[0], y - mPoint[1]));

                boolean aimed = mRadius >= mRadiusTarget;
                if (mRadiusTargetAimed != aimed) {
                    mRadiusTargetAimed = aimed;
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); // vibrate
                }

                if (mRadiusMaxPeak - mRadius > mRadiusDecreaseThreshold) {
                    cancelCircle();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mHandler.removeMessages(ACTION_CANCELED);

                if (!mRadiusTargetAimed || action == MotionEvent.ACTION_CANCEL) {
                    cancelCircle();
                    break;
                }

                boolean animateUnlocking = Config.getInstance().isUnlockAnimationEnabled();

                if (animateUnlocking) {

                    // Calculate longest distance between center of
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
                    startAnimation(mAnimationOver);
                }

                mHandler.sendEmptyMessage(ACTION_UNLOCK_START);
                mHandler.sendEmptyMessageDelayed(ACTION_UNLOCK,
                        animateUnlocking ? mAnimationOverDuration : 0);
                break;
            default:
                return super.onTouchEvent(event);
        }
        return true;
    }

    private void cancelCircle() {
        if (mCanceled) {
            return;
        }

        mCanceled = true;
        mHandler.sendEmptyMessage(ACTION_CANCELED);

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

    public void setDarkening(float darkening) {
        mDarkening = MathUtils.range(darkening, 0f, 1f);
        invalidate();
    }

    public float getDarkening() {
        return mDarkening;
    }

}
