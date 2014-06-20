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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;

import com.achep.acdisplay.R;

/**
 * {@inheritDoc}
 */
public class ProgressBar extends android.widget.ProgressBar {

    private boolean mMirrored;

    private OnProgressChangeListener mListener;

    /**
     * A callback that notifies clients when the progress/max level has been changed.
     */
    public interface OnProgressChangeListener {

        /**
         * Notification that the progress level has changed.
         *
         * @param progressBar The ProgressBar whose progress has changed
         * @param progress    The current progress level. This will be in the range 0..{@link com.achep.acdisplay.widgets.ProgressBar#getMax()}}
         */
        public void onProgressChanged(ProgressBar progressBar, int progress);

        public void onMaxChanged(ProgressBar progressBar, int max);
    }

    public ProgressBar(Context context) {
        super(context);
        init(null, 0);
    }

    public ProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        if (attrs == null) {
            return;
        }

        Context context = getContext();
        assert context != null;

        TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.ProgressBar, defStyle, 0);
        assert arr != null;

        setMirrored(arr.getBoolean(R.styleable.ProgressBar_mirrored, mMirrored));

        arr.recycle();
    }

    @Override
    public synchronized void setProgress(int progress) {
        super.setProgress(progress);

        if (mListener != null) {
            mListener.onProgressChanged(this, progress);
        }
    }

    @Override
    public synchronized void setMax(int max) {
        super.setMax(max);

        if (mListener != null) {
            mListener.onMaxChanged(this, max);
        }
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        if (mMirrored) {
            canvas.save();
            canvas.translate(getWidth() - getPaddingEnd(), getPaddingTop());
            canvas.scale(-1.0f, 1.0f);
            super.onDraw(canvas);
            canvas.restore();
        } else {
            super.onDraw(canvas);
        }
    }

    public void setMirrored(boolean mirrored) {
        mMirrored = mirrored;
    }

    /**
     * Sets a listener to receive notifications
     * of changes to the ProgressBar's progress/max level.
     */
    public void setOnProgressChangeListener(OnProgressChangeListener listener) {
        mListener = listener;
    }

}
