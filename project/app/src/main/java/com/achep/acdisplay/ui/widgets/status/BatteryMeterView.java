/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Modified 2014 AChep@xda <artemchep@gmail.com>
package com.achep.acdisplay.ui.widgets.status;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.achep.acdisplay.R;
import com.achep.base.utils.ResUtils;

// TODO: Bring RTL support
public class BatteryMeterView extends TextView {
    public static final String TAG = BatteryMeterView.class.getSimpleName();

    public static final int EVENT_LEVEL = 1;
    public static final int EVENT_CHARGING = 2;

    public enum BatteryMeterMode {
        BATTERY_METER_ICON_PORTRAIT,
    }

    protected class BatteryTracker extends BroadcastReceiver {

        public static final int UNKNOWN_LEVEL = -1;

        // current battery status
        boolean present = true;
        boolean plugged;
        int plugType;
        int status;
        int level = UNKNOWN_LEVEL;

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_BATTERY_CHANGED:
                    final boolean chargingOld = indicateCharging();
                    final int levelOld = level;

                    // Get battery level
                    level = 100
                            * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                            / intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

                    present = intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true);
                    plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                    plugged = plugType != 0;
                    status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                            BatteryManager.BATTERY_STATUS_UNKNOWN);

                    // Update view
                    setText(String.format(mBatteryFormat, this.level));
                    setContentDescription(ResUtils.getString(context, R.string.accessibility_battery_level, level));

                    // Notify listener
                    if (mOnBatteryChangedListener != null) {
                        int event = 0;
                        event |= chargingOld != indicateCharging() ? EVENT_CHARGING : 0;
                        event |= levelOld != level ? EVENT_LEVEL : 0;

                        if (event != 0) {
                            mOnBatteryChangedListener.onBatteryChanged(BatteryMeterView.this, event);
                        }
                    }
                    break;
            }
        }

        /**
         * @return {@code true} if device is charging, {@code false} otherwise.
         */
        protected boolean indicateCharging() {
            return status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL && plugged;
        }
    }

    /**
     * Interface definition for a callback to be invoked
     * when battery status/level/other changed.
     *
     * @see #setOnBatteryChangedListener(OnBatteryChangedListener)
     */
    public interface OnBatteryChangedListener {

        /**
         * Invoked when battery status/level/other changed.
         *
         * @param event bit-set of events: {@link #EVENT_LEVEL}, {@link #EVENT_CHARGING} or other.
         */
        void onBatteryChanged(BatteryMeterView view, int event);

    }

    protected BatteryMeterMode mBatteryMeterMode;

    final int[] mColors;

    private String mBatteryFormat;
    private String mWarningString;
    private final int mChargeColor;
    private final int mBatteryHeight;
    private final int mBatteryWidth;
    private final int mBatteryPadding;

    private OnBatteryChangedListener mOnBatteryChangedListener;

    private boolean mAttached;
    private Context mContext;

    protected BatteryTracker mTracker = new BatteryTracker();
    private BatteryMeterDrawable mBatteryDrawable;
    private final Object mLock = new Object();

    private int mPaddingLeft;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttached = true;

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent sticky = getContext().registerReceiver(mTracker, filter);

        // Pre-load the battery level
        if (sticky != null) {
            mTracker.onReceive(getContext(), sticky);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttached = false;
        getContext().unregisterReceiver(mTracker);
    }

    public BatteryMeterView(Context context) {
        this(context, null, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;

        TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.BatteryMeterView, defStyle, 0);
        mBatteryHeight = arr.getDimensionPixelSize(R.styleable.BatteryMeterView_battery_height, 0);
        mBatteryWidth = arr.getDimensionPixelSize(R.styleable.BatteryMeterView_battery_width, 0);
        mBatteryPadding = arr.getDimensionPixelSize(R.styleable.BatteryMeterView_battery_padding, 0);
        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom());
        arr.recycle();

        final Resources res = context.getResources();
        if (!isInEditMode()) {
            TypedArray levels = res.obtainTypedArray(R.array.batterymeter_color_levels);
            TypedArray colors = res.obtainTypedArray(R.array.batterymeter_color_values);

            final int n = levels.length();
            mColors = new int[2 * n];
            for (int i = 0; i < n; i++) {
                mColors[2 * i] = levels.getInt(i, 0);
                mColors[2 * i + 1] = colors.getColor(i, 0);
            }
            levels.recycle();
            colors.recycle();
        } else {
            mColors = new int[]{
                    4, res.getColor(R.color.batterymeter_critical),
                    15, res.getColor(R.color.batterymeter_low),
                    100, res.getColor(R.color.batterymeter_full),
            };
        }

        mChargeColor = getResources().getColor(R.color.batterymeter_charge_color);
        mBatteryFormat = getResources().getString(R.string.batterymeter_precise);
        mWarningString = context.getString(R.string.batterymeter_very_low_overlay_symbol);

        setMode(BatteryMeterMode.BATTERY_METER_ICON_PORTRAIT);
        mBatteryDrawable.onSizeChanged(mBatteryWidth, mBatteryHeight, 0, 0);

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        // Apply additional padding to leave free space
        // for battery meter drawing.
        final int leftExtended = (left += mBatteryPadding - mPaddingLeft) + mBatteryWidth + mBatteryPadding;
        super.setPadding(leftExtended, top, right, bottom);
        mPaddingLeft = left;
    }

    @Override
    public int getPaddingLeft() {
        return mPaddingLeft;
    }

    private BatteryMeterDrawable createBatteryMeterDrawable(BatteryMeterMode mode) {
        Resources res = mContext.getResources();
        return new NormalBatteryMeterDrawable(res);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int heightMin = mBatteryHeight + getTotalPaddingTop() + getTotalPaddingBottom();
        int height = Math.max(getMeasuredHeight(), heightMin);
        setMeasuredDimension(getMeasuredWidth(), height);
    }

    public void setOnBatteryChangedListener(OnBatteryChangedListener listener) {
        mOnBatteryChangedListener = listener;
    }

    /**
     * @return battery charge level, from {@code [0...100]}
     */
    public int getBatteryLevel() {
        return mTracker.level;
    }

    /**
     * @return {@code true} if charging, {@code false} otherwise
     */
    public boolean getBatteryCharging() {
        return mTracker.indicateCharging();
    }

    public int getColorForLevel(int percent) {
        int thresh, color;
        for (int i = 0; i < mColors.length; i += 2) {
            thresh = mColors[i];
            color = mColors[i + 1];
            if (percent <= thresh) {
                return color;
            }
        }

        throw new RuntimeException("Broken color levels!");
    }

    public void setMode(BatteryMeterMode mode) {
        synchronized (mLock) {
            if (mBatteryMeterMode == mode) {
                return;
            }

            mBatteryMeterMode = mode;

            if (mBatteryDrawable != null)
                mBatteryDrawable.onDispose();
            mBatteryDrawable = createBatteryMeterDrawable(mode);

            if (mBatteryMeterMode == BatteryMeterMode.BATTERY_METER_ICON_PORTRAIT) {
                NormalBatteryMeterDrawable drawable = (NormalBatteryMeterDrawable) mBatteryDrawable;
                drawable.loadBoltPoints(mContext.getResources());
            }

            if (mAttached) {
                postInvalidate();
            }
        }
    }

    @Override
    public void onDraw(@NonNull Canvas c) {
        super.onDraw(c);

        synchronized (mLock) {
            if (mBatteryDrawable != null) {
                mBatteryDrawable.onDraw(c, mTracker);
            }
        }
    }

    protected interface BatteryMeterDrawable {
        void onDraw(Canvas c, BatteryTracker tracker);

        void onSizeChanged(int w, int h, int oldw, int oldh);

        void onDispose();
    }

    protected class NormalBatteryMeterDrawable implements BatteryMeterDrawable {

        public static final int FULL = 96;
        public static final int EMPTY = 4;

        public static final float SUBPIXEL = 0.4f;  // inset rects for softer edges

        private boolean mDisposed;

        private Paint mFramePaint, mBatteryPaint, mWarningTextPaint, mBoltPaint;
        private int mButtonHeight;
        private float mWarningTextHeight;

        private final float[] mBoltPoints;
        private final Path mBoltPath = new Path();

        private final RectF mFrame = new RectF();
        private final RectF mButtonFrame = new RectF();
        private final RectF mClipFrame = new RectF();
        private final RectF mBoltFrame = new RectF();

        public NormalBatteryMeterDrawable(Resources res) {
            super();
            mDisposed = false;

            mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mFramePaint.setColor(res.getColor(R.color.batterymeter_frame_color));
            mFramePaint.setDither(true);
            mFramePaint.setStrokeWidth(0);
            mFramePaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mFramePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));

            mBatteryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mBatteryPaint.setDither(true);
            mBatteryPaint.setStrokeWidth(0);
            mBatteryPaint.setStyle(Paint.Style.FILL_AND_STROKE);

            mWarningTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mWarningTextPaint.setColor(mColors[1]);
            Typeface font = Typeface.create("sans-serif", Typeface.BOLD);
            mWarningTextPaint.setTypeface(font);
            mWarningTextPaint.setTextAlign(Paint.Align.CENTER);

            mBoltPaint = new Paint();
            mBoltPaint.setAntiAlias(true);
            mBoltPaint.setColor(res.getColor(R.color.batterymeter_bolt_color));
            mBoltPoints = loadBoltPoints(res);
        }

        @Override
        public void onDraw(Canvas c, BatteryTracker tracker) {
            if (mDisposed) return;

            final int level = tracker.level;

            if (level == BatteryTracker.UNKNOWN_LEVEL) return;

            float drawFrac = (float) level / 100f;
            final int pt = getTotalPaddingTop() + (getHeight() - getTotalPaddingTop() - getTotalPaddingBottom() - mBatteryHeight) / 2;
            final int pl = getPaddingLeft();
            int height = mBatteryHeight;
            int width = mBatteryWidth;

            mButtonHeight = (int) (height * 0.12f);

            mFrame.set(0, 0, width, height);
            mFrame.offset(pl, pt);

            mButtonFrame.set(
                    mFrame.left + width * 0.25f,
                    mFrame.top,
                    mFrame.right - width * 0.25f,
                    mFrame.top + mButtonHeight + 5 /*cover frame border of intersecting area*/);

            mButtonFrame.top += SUBPIXEL;
            mButtonFrame.left += SUBPIXEL;
            mButtonFrame.right -= SUBPIXEL;

            mFrame.top += mButtonHeight;
            mFrame.left += SUBPIXEL;
            mFrame.top += SUBPIXEL;
            mFrame.right -= SUBPIXEL;
            mFrame.bottom -= SUBPIXEL;

            // first, draw the battery shape
            c.drawRect(mFrame, mFramePaint);

            // fill 'er up
            final int color = tracker.plugged ? mChargeColor : getColorForLevel(level);
            mBatteryPaint.setColor(color);

            if (level >= FULL) {
                drawFrac = 1f;
            } else if (level <= EMPTY) {
                drawFrac = 0f;
            }

            c.drawRect(mButtonFrame, drawFrac == 1f ? mBatteryPaint : mFramePaint);

            mClipFrame.set(mFrame);
            mClipFrame.top += (mFrame.height() * (1f - drawFrac));

            c.save(Canvas.CLIP_SAVE_FLAG);
            c.clipRect(mClipFrame);
            c.drawRect(mFrame, mBatteryPaint);
            c.restore();

            if (tracker.indicateCharging()) {
                // draw the bolt
                final float bl = (int) (mFrame.left + mFrame.width() / 4.5f);
                final float bt = (int) (mFrame.top + mFrame.height() / 6f);
                final float br = (int) (mFrame.right - mFrame.width() / 7f);
                final float bb = (int) (mFrame.bottom - mFrame.height() / 10f);
                if (mBoltFrame.left != bl || mBoltFrame.top != bt
                        || mBoltFrame.right != br || mBoltFrame.bottom != bb) {
                    mBoltFrame.set(bl, bt, br, bb);
                    mBoltPath.reset();
                    mBoltPath.moveTo(
                            mBoltFrame.left + mBoltPoints[0] * mBoltFrame.width(),
                            mBoltFrame.top + mBoltPoints[1] * mBoltFrame.height());
                    for (int i = 2; i < mBoltPoints.length; i += 2) {
                        mBoltPath.lineTo(
                                mBoltFrame.left + mBoltPoints[i] * mBoltFrame.width(),
                                mBoltFrame.top + mBoltPoints[i + 1] * mBoltFrame.height());
                    }
                    mBoltPath.lineTo(
                            mBoltFrame.left + mBoltPoints[0] * mBoltFrame.width(),
                            mBoltFrame.top + mBoltPoints[1] * mBoltFrame.height());
                }
                c.drawPath(mBoltPath, mBoltPaint);
            } else if (level <= EMPTY) {
                final float x = pl + mBatteryWidth * 0.5f;
                final float y = pt + (mBatteryHeight + mWarningTextHeight) * 0.48f;
                c.drawText(mWarningString, x, y, mWarningTextPaint);
            }
        }

        @Override
        public void onDispose() {
            mDisposed = true;
        }

        @Override
        public void onSizeChanged(int w, int h, int oldw, int oldh) {
            mWarningTextPaint.setTextSize(h * 0.75f);
            mWarningTextHeight = -mWarningTextPaint.getFontMetrics().ascent;
        }

        private float[] loadBoltPoints(Resources res) {
            if (!isInEditMode()) {
                final int[] pts = res.getIntArray(getBoltPointsArrayResource());
                int maxX = 0, maxY = 0;
                for (int i = 0; i < pts.length; i += 2) {
                    maxX = Math.max(maxX, pts[i]);
                    maxY = Math.max(maxY, pts[i + 1]);
                }
                final float[] ptsF = new float[pts.length];
                for (int i = 0; i < pts.length; i += 2) {
                    ptsF[i] = (float) pts[i] / maxX;
                    ptsF[i + 1] = (float) pts[i + 1] / maxY;
                }
                return ptsF;
            } else {
                return new float[]{0, 0, 1, 1};
            }
        }

        protected int getBoltPointsArrayResource() {
            return R.array.batterymeter_bolt_points;
        }
    }
}