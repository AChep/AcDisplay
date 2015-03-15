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
package com.achep.acdisplay.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by achep on 28.04.14 for AcDisplay.
 *
 * @author Artem Chepurnoy
 */
public class NotifyingLayout extends FrameLayout {

    private OnPressStateChangedListener mOnPressStateChangedListener;

    public interface OnPressStateChangedListener {

        void onPressStateChanged(NotifyingLayout view, boolean pressed);

    }

    public NotifyingLayout(Context context) {
        super(context);
    }

    public NotifyingLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NotifyingLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnPressStateChangedListener(OnPressStateChangedListener listener) {
        mOnPressStateChangedListener = listener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPressed(boolean pressed) {
        boolean notify = pressed != isPressed();
        super.setPressed(pressed);
        if (mOnPressStateChangedListener != null && notify) {
            mOnPressStateChangedListener.onPressStateChanged(this, pressed);
        }
    }
}
