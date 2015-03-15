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
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.achep.base.utils.ViewUtils;

/**
 * Created by achep on 26.04.14.
 */
public class ForwardingLayout extends LinearLayout {

    private static final String TAG = "ForwardingLayout";

    private View mPressedChild;

    private OnForwardedEventListener mOnForwardedEventListener;
    private boolean mVibrateOnForwarded;
    private boolean mForwardAll;
    private int mForwardDepth;

    public ForwardingLayout(Context context) {
        super(context);
    }

    public ForwardingLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ForwardingLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public interface OnForwardedEventListener {

        void onForwardedEvent(MotionEvent event, int activePointerId);

        void onPressedView(MotionEvent event, int activePointerId, View view);
    }

    public void setOnForwardedEventListener(OnForwardedEventListener listener) {
        mOnForwardedEventListener = listener;
    }

    /**
     * If enabled, forwarding motion event will be accompanied by
     * haptic feedback on press / un-press / click.
     */
    public void setVibrateOnForwardedEventEnabled(boolean enabled) {
        mVibrateOnForwarded = enabled;
    }

    public void setAllViewsForwardable(boolean enabled, int depth) {
        mForwardAll = enabled;
        mForwardDepth = depth;
    }

    /**
     * Handles forwarded events.
     *
     * @param activePointerId id of the pointer that activated forwarding
     * @return whether the event was handled
     */
    public boolean onForwardedEvent(MotionEvent event, int activePointerId) {
        boolean handledEvent = true;
        View vibrateView = null;

        final int actionMasked = event.getActionMasked();
        switch (actionMasked) {
            case MotionEvent.ACTION_CANCEL:
                handledEvent = false;
                break;
            case MotionEvent.ACTION_UP:
                handledEvent = false;
                // $FALL-THROUGH$
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                final int activeIndex = event.findPointerIndex(activePointerId);
                if (activeIndex < 0) {
                    handledEvent = false;
                    break;
                }

                final int x = (int) event.getX(activeIndex);
                final int y = (int) event.getY(activeIndex);

                View pressedView = findViewByCoordinate(this, x, y, 0);
                if (mPressedChild != pressedView) {
                    vibrateView = pressedView != null ? pressedView : mPressedChild;
                    if (pressedView != null) pressedView.setPressed(true);
                    if (mPressedChild != null) mPressedChild.setPressed(false);
                    mPressedChild = pressedView;

                    if (mOnForwardedEventListener != null) {
                        mOnForwardedEventListener.onPressedView(event, activePointerId, mPressedChild);
                    }
                }

                if (actionMasked == MotionEvent.ACTION_UP) {
                    vibrateView = mPressedChild;
                    clickPressedItem();
                }
                break;
        }

        // Failure to handle the event cancels forwarding.
        if (!handledEvent) {
            clearPressedItem();
        }

        if (vibrateView != null && mVibrateOnForwarded) {
            vibrateView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }

        if (mOnForwardedEventListener != null) {
            mOnForwardedEventListener.onForwardedEvent(event, activePointerId);
        }

        return handledEvent;
    }

    private View findViewByCoordinate(ViewGroup viewGroup, float x, float y, int depth) {
        final int childCount = viewGroup.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            final View child = viewGroup.getChildAt(i);
            assert child != null;

            if (child.getVisibility() != View.VISIBLE || !child.isEnabled()) {
                continue;
            }

            if (child instanceof ViewGroup && (depth < mForwardDepth || !mForwardAll)) {
                View view = findViewByCoordinate((ViewGroup) child,
                        x - child.getLeft(),
                        y - child.getTop(), depth + 1);
                if (view != null) {
                    return view;
                }
            }

            if ((child.isClickable() || mForwardAll) && ViewUtils.pointInView(child, x, y, 0)) {
                return child;
            }
        }

        return null;
    }

    private void clearPressedItem() {
        if (mPressedChild == null) {
            return;
        }

        mPressedChild.setPressed(false);
        mPressedChild.refreshDrawableState();
        mPressedChild = null;
    }

    private void clickPressedItem() {
        if (mPressedChild == null) {
            return;
        }

        mPressedChild.performClick();
        clearPressedItem();
    }
}
