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

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Message;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Property;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.acdisplay.ui.CornerHelper;
import com.achep.acdisplay.ui.drawables.CornerIconDrawable;
import com.achep.base.async.WeakHandler;
import com.achep.base.tests.Check;
import com.achep.base.utils.FloatProperty;
import com.achep.base.utils.MathUtils;
import com.achep.base.utils.RefCacheBase;
import com.achep.base.utils.ResUtils;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import static com.achep.acdisplay.ui.preferences.ColorPickerPreference.getColor;
import static com.achep.base.Build.DEBUG;

/**
 * Created by achep on 19.04.14.
 */
public class CircleView extends View {

    private static final String TAG = "CircleView";

    public static final int ACTION_START = 0;
    public static final int ACTION_UNLOCK = 1;
    public static final int ACTION_UNLOCK_START = 2;
    public static final int ACTION_UNLOCK_CANCEL = 3;
    public static final int ACTION_CANCELED = 4;

    private static final int MSG_CANCEL = -1;

    @NonNull
    private static final Property<CircleView, Float> RADIUS_PROPERTY =
            new FloatProperty<CircleView>("setRadius") {
                @Override
                public void setValue(CircleView cv, float value) {
                    cv.setRadius(value);
                }

                @Override
                public Float get(CircleView cv) {
                    return cv.mRadius;
                }
            };

    /**
     * The current touch point.
     */
    private float[] mPoint = new float[2];

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
    // Target
    private float mRadiusTarget;
    private boolean mRadiusTargetAimed;
    // Decreasing detection
    private float mRadiusMaxPeak;
    private float mRadiusDecreaseThreshold;

    private boolean mCanceled;
    private float mDarkening;

    private float mCornerMargin;
    @DrawableRes
    private int mDrawableResourceId = -1;
    private ColorFilter mInverseColorFilter;
    private CornerIconDrawable mDrawableLeftTopCorner;
    private CornerIconDrawable mDrawableRightTopCorner;
    private CornerIconDrawable mDrawableLeftBottomCorner;
    private CornerIconDrawable mDrawableRightBottomCorner;
    private Drawable mDrawable;
    private Paint mPaint;
    @NonNull
    private RefCacheBase<Drawable> mDrawableCache = new RefCacheBase<Drawable>() {
        @NonNull
        @Override
        protected Reference<Drawable> onCreateReference(@NonNull Drawable object) {
            return new WeakReference<>(object);
        }
    };

    // animation
    private ObjectAnimator mAnimator;
    private int mShortAnimTime;
    private int mMediumAnimTime;

    private Callback mCallback;
    private Supervisor mSupervisor;

    private H mHandler = new H(this);

    private int mInnerColor;
    private int mOuterColor;
    private int mCornerActionId;

    public interface Callback {

        void onCircleEvent(float radius, float ratio, int event, int actionId);
    }

    public interface Supervisor {

        boolean isAnimationEnabled();

        boolean isAnimationUnlockEnabled();

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
        Resources res = getResources();
        mCornerMargin = res.getDimension(R.dimen.circle_corner_margin);
        mRadiusTarget = res.getDimension(R.dimen.circle_radius_target);
        mRadiusDecreaseThreshold = res.getDimension(R.dimen.circle_radius_decrease_threshold);
        mShortAnimTime = res.getInteger(android.R.integer.config_shortAnimTime);
        mMediumAnimTime = res.getInteger(android.R.integer.config_mediumAnimTime);

        mDrawableLeftTopCorner = new CornerIconDrawable(Config.KEY_CORNER_ACTION_LEFT_TOP);
        mDrawableRightTopCorner = new CornerIconDrawable(Config.KEY_CORNER_ACTION_RIGHT_TOP);
        mDrawableLeftBottomCorner = new CornerIconDrawable(Config.KEY_CORNER_ACTION_LEFT_BOTTOM);
        mDrawableRightBottomCorner = new CornerIconDrawable(Config.KEY_CORNER_ACTION_RIGHT_BOTTOM);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        initInverseColorFilter();

        setRadius(0);
    }

    private void initInverseColorFilter() {
        final float v = -1;
        final float[] matrix = {
                v, 0, 0, 0, 0,
                0, v, 0, 0, 0,
                0, 0, v, 0, 0,
                0, 0, 0, 1, 0,
        };

        mInverseColorFilter = new ColorMatrixColorFilter(matrix);
    }

    public void setSupervisor(Supervisor supervisor) {
        mSupervisor = supervisor;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Start tracking the corners' icons.
        Context context = getContext();
        mDrawableLeftTopCorner.start(context);
        mDrawableRightTopCorner.start(context);
        mDrawableLeftBottomCorner.start(context);
        mDrawableRightBottomCorner.start(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final float ratio = calculateRatio();

        // Draw all corners
        drawCornerIcon(canvas, mDrawableLeftTopCorner, 0, 0 /* left top */);
        drawCornerIcon(canvas, mDrawableRightTopCorner, 1, 0 /* right top */);
        drawCornerIcon(canvas, mDrawableLeftBottomCorner, 0, 1 /* left bottom */);
        drawCornerIcon(canvas, mDrawableRightBottomCorner, 1, 1 /* right bottom */);

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

    private void drawCornerIcon(@NonNull Canvas canvas, @NonNull Drawable drawable, int xm, int ym) {
        int width = getMeasuredWidth() - drawable.getBounds().width();
        int height = getMeasuredHeight() - drawable.getBounds().height();
        float margin = (1 - 2 * xm) * mCornerMargin;
        // Draw
        canvas.save();
        canvas.translate(xm * width + margin, ym * height + margin);
        drawable.draw(canvas);
        canvas.restore();
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelAndClearAnimator();
        mHandler.removeCallbacksAndMessages(null);
        mDrawableCache.clear();

        mDrawableLeftTopCorner.stop();
        mDrawableRightTopCorner.stop();
        mDrawableLeftBottomCorner.stop();
        mDrawableRightBottomCorner.stop();
        super.onDetachedFromWindow();
    }

    private void setInnerColor(int color, boolean needsColorReset) {
        if (mInnerColor == (mInnerColor = color) && !needsColorReset) return;

        // Inverse the drawable if needed
        boolean isBright = ColorUtils.calculateLuminance(color) > 0.5;
        mDrawable.setColorFilter(isBright ? mInverseColorFilter : null);
    }

    private void setOuterColor(int color) {
        mOuterColor = color;
    }

    /**
     * Updates the icon in center of the circle, to the once corresponding
     * with the current action.
     *
     * @see CornerHelper
     */
    private boolean updateIcon() {
        final int res = CornerHelper.getIconResource(mCornerActionId);
        if (res == mDrawableResourceId) return false; // No need to update
        mDrawableResourceId = res;

        label:
        {
            // Try to get from the cache.
            final CharSequence key = Integer.toString(res);
            mDrawable = mDrawableCache.get(key);
            if (mDrawable != null) {
                if (DEBUG) Log.d(TAG, "Got an icon<" + key + "> from the cache.");
                break label;
            }

            // Load from resources.
            mDrawable = ResUtils.getDrawable(getContext(), res);
            assert mDrawable != null;
            mDrawable.setBounds(0, 0,
                    mDrawable.getIntrinsicWidth(),
                    mDrawable.getIntrinsicHeight());
            mDrawable = mDrawable.mutate(); // don't affect the original drawable
            mDrawableCache.put(key, mDrawable);
        }
        // Update alpha
        float ratio = calculateRatio();
        mDrawable.setAlpha((int) (255 * Math.pow(ratio, 3)));
        return true;
    }

    public boolean sendTouchEvent(@NonNull MotionEvent event) {
        final int action = event.getActionMasked();

        // If current circle is canceled then
        // ignore all actions except of touch down (to reset state.)
        if (mCanceled && action != MotionEvent.ACTION_DOWN) return false;

        // Cancel the current circle on two-or-more-fingers touch.
        if (event.getPointerCount() > 1) {
            cancelCircle();
            return false;
        }

        final float x = event.getX();
        final float y = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                clearAnimation();
                Config config = Config.getInstance();

                // Corner actions
                int width = getWidth();
                int height = getHeight();
                int radius = Math.min(width, height) / 3;
                if (MathUtils.isInCircle(x, y, 0, 0, radius)) { // Top left
                    mCornerActionId = config.getCornerActionLeftTop();
                } else if (MathUtils.isInCircle(x, y, -width, 0, radius)) { // Top right
                    mCornerActionId = config.getCornerActionRightTop();
                } else if (MathUtils.isInCircle(x, y, 0, -height, radius)) { // Bottom left
                    mCornerActionId = config.getCornerActionLeftBottom();
                } else if (MathUtils.isInCircle(x, y, -width, -height, radius)) { // Bottom right
                    mCornerActionId = config.getCornerActionRightBottom();
                } else {
                    // The default action is unlocking.
                    mCornerActionId = Config.CORNER_UNLOCK;
                }

                // Update colors and icon drawable.
                boolean needsColorReset = updateIcon();
                setInnerColor(getColor(config.getCircleInnerColor()), needsColorReset);
                setOuterColor(getColor(config.getCircleOuterColor()));

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
                break;
            case MotionEvent.ACTION_MOVE:
                setRadius(x, y);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (!mRadiusTargetAimed || action == MotionEvent.ACTION_CANCEL) {
                    cancelCircle();
                    break;
                }

                startUnlock();
                break;
        }
        return true;
    }

    private void cancelCircle() {
        cancelCircle(mSupervisor.isAnimationUnlockEnabled());
    }

    private void cancelCircle(boolean animate) {
        Check.getInstance().isFalse(mCanceled);

        mCanceled = true;
        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendEmptyMessage(ACTION_CANCELED);

        if (animate) {
            startAnimatorBy(mRadius, 0f, mMediumAnimTime);
        } else {
            setRadius(0f);
        }
    }

    private void startUnlock() {
        startUnlock(mSupervisor.isAnimationUnlockEnabled());
    }

    private void startUnlock(boolean animate) {
        if (animate) {
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

            distance = (float) (Math.pow(distance / 50f, 2) * 50f);
            startAnimatorBy(mRadius, distance, mShortAnimTime);
        }

        final int delayUnlock = animate ? mShortAnimTime - 10 : 0;
        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendEmptyMessage(ACTION_UNLOCK_START);
        mHandler.sendEmptyMessageDelayed(ACTION_UNLOCK, delayUnlock);
    }

    private void startAnimatorBy(float from, float to, int duration) {
        cancelAndClearAnimator();
        // Animate the circle
        mAnimator = ObjectAnimator.ofFloat(this, RADIUS_PROPERTY, from, to);
        mAnimator.setInterpolator(new FastOutLinearInInterpolator());
        mAnimator.setDuration(duration);
        mAnimator.start();
    }

    private void cancelAndClearAnimator() {
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
    }

    private float calculateRatio() {
        return Math.min(mRadius / mRadiusTarget, 1f);
    }

    private void setRadius(float x, float y) {
        double radius = Math.hypot(x - mPoint[0], y - mPoint[1]);
        setRadius((float) radius);
    }

    /**
     * Sets the radius of fake circle.
     *
     * @param radius radius to set
     */
    private void setRadius(float radius) {
        mRadius = radius;

        if (!mCanceled) {
            // Save maximum radius for detecting
            // decreasing of the circle's size.
            if (mRadius > mRadiusMaxPeak) {
                mRadiusMaxPeak = mRadius;
            } else if (mRadiusMaxPeak - mRadius > mRadiusDecreaseThreshold) {
                cancelCircle();
                return; // Cancelling circle will recall #setRadius
            }

            boolean aimed = mRadius >= mRadiusTarget;
            if (mRadiusTargetAimed != aimed) {
                mRadiusTargetAimed = aimed;
                // Vibrate if the user is interacting with the device.
                if (isInTouchMode()) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            }
        }
        final float ratio = calculateRatio();
        int alpha;

        // Update unlock icon's transparency.
        if (mDrawable != null) {
            alpha = (int) (255 * Math.pow(ratio, 3));
            mDrawable.setAlpha(alpha);
        }

        // Update corners' icons transparency.
        alpha = (int) (50f * Math.pow(1f - ratio, 0.3f));
        mDrawableLeftTopCorner.setAlpha(alpha);
        mDrawableRightTopCorner.setAlpha(alpha);
        mDrawableLeftBottomCorner.setAlpha(alpha);
        mDrawableRightBottomCorner.setAlpha(alpha);

        // Update the size of the unlock circle.
        radius = (float) Math.sqrt(mRadius / 50f) * 50f;
        setRadiusDrawn(radius);
    }

    private void setRadiusDrawn(float radius) {
        mRadiusDrawn = radius;
        postInvalidateOnAnimation();
    }

    private static class H extends WeakHandler<CircleView> {

        public H(@NonNull CircleView cv) {
            super(cv);
        }

        @Override
        protected void onHandleMassage(@NonNull CircleView cv, Message msg) {
            switch (msg.what) {
                case MSG_CANCEL:
                    cv.cancelCircle();
                    break;
                case ACTION_START:
                case ACTION_UNLOCK:
                case ACTION_UNLOCK_START:
                case ACTION_UNLOCK_CANCEL:
                case ACTION_CANCELED:
                    if (cv.mCallback != null) {
                        final float ratio = cv.calculateRatio();
                        final int actionId = cv.mCornerActionId;
                        cv.mCallback.onCircleEvent(cv.mRadius, ratio, msg.what, actionId);
                    }
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }

    }

}
