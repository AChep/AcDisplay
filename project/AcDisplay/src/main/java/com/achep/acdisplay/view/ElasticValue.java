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

package com.achep.acdisplay.view;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;

import com.achep.acdisplay.utils.MathUtils;

/**
 * Created by achep on 14.05.14 for AcDisplay.
 *
 * @author Artem Chepurnoy
 */
public abstract class ElasticValue {

    private static final int PERIOD = 16; // ~60 fps.

    private float mMove;
    private float mStrength;
    private float mCenter;

    private long mCurTime;

    private boolean mBroadcasting;
    private Listener mListener;

    private final View mView;
    private final float mDensity;
    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            float force = mStrength;
            float value = getValue() - mCenter + mMove + (mMove = 0);
            if (value != 0f) {
                if (Math.abs(value) < force) {
                    setValue(0f);
                    return;
                }

                int charge = MathUtils.charge(value);
                setValue((Math.abs(value) - force) * charge);
                sendEmptyMessageAtTime(0, (mCurTime += PERIOD));
            }
        }

        private void setValue(float value) {
            ElasticValue.this.setValue(value + mCenter);
        }

        private float getValue() {
            return ElasticValue.this.getValue() - mCenter;
        }

    };

    private void start() {
        if (!mHandler.hasMessages(0)) {
            mCurTime = SystemClock.uptimeMillis();
            mHandler.sendEmptyMessage(0);
        }
    }

    public void stop() {
        mHandler.removeMessages(0);
    }

    /**
     * Interface definition for a callback to be invoked
     * when a view's param is changed.
     */
    public interface Listener {

        /**
         * Called on {@link #setValue(float) setting value}.
         *
         * @param view Changed view
         */
        public void onValueChanged(View view, float value);
    }

    public ElasticValue(View view, float strengthDpi) {
        mDensity = view.getResources().getDisplayMetrics().density;
        mView = view;

        setStrength(strengthDpi);
    }

    /**
     * Sets a callback to be invoked when a view's param is changed.
     *
     * @see Listener#onValueChanged(android.view.View, float)
     */
    public void setListener(Listener listener) {
        mListener = listener;
    }

    /**
     * @return the view set on creating instance.
     */
    public View getView() {
        return mView;
    }

    /**
     * Returns how powerful is the desire to reach {@link #setCenter(float) center}.
     *
     * @return dpi-per-second in normal mode.
     * @see #setStrength(float)
     */
    public float getStrength() {
        return mStrength / mDensity / PERIOD * 1000f;
    }

    public void reset() {
        stop();
        setValue(mCenter);
    }

    public void move(float value) {
        mMove += value;
        start();
    }

    /**
     * Defines the neutral value.
     *
     * @see #setStrength(float)
     */
    public void setCenter(float center) {
        mCenter = center;
    }

    /**
     * Defines how powerful is the desire to reach {@link #setCenter(float) center}.
     *
     * @param strengthDpi dpi-per-second in normal mode.
     * @see #getStrength()
     * @see #setCenter(float)
     */
    public void setStrength(float strengthDpi) {
        mStrength = strengthDpi * mDensity / 1000f * PERIOD;
    }

    public void setValue(float value) {

        // Notify listeners about the change.
        // Broadcasting check is here to prevent looping
        // if setValue() is calling in listener.
        if (!mBroadcasting && mListener != null) {
            mBroadcasting = true;
            mListener.onValueChanged(mView, value);
            mBroadcasting = false;
        }
    }

    public abstract float getValue();

    /**
     * Elastic setter of view's translation.
     *
     * @author Artem Chepurnoy
     */
    public static class TranslationX extends ElasticValue {

        public TranslationX(View view, float strengthDpi) {
            super(view, strengthDpi);
        }

        @Override
        public void setValue(float value) {
            getView().setTranslationX(value);
            super.setValue(value);
        }

        @Override
        public float getValue() {
            return getView().getTranslationX();
        }

    }

}
