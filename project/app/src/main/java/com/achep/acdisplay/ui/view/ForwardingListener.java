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

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;

import com.achep.base.utils.ViewUtils;

/**
 * Abstract class that forwards touch events to a {@link ForwardingLayout}.
 */
public class ForwardingListener implements
        View.OnTouchListener, View.OnAttachStateChangeListener {
    /**
     * Scaled touch slop, used for detecting movement outside bounds.
     */
    private final float mScaledTouchSlop;

    /**
     * Timeout before disallowing intercept on the source's parent.
     */
    private final int mTapTimeout;

    /**
     * Source view from which events are forwarded.
     */
    private final View mSrc;
    private final ForwardingLayout mDst;

    /**
     * Runnable used to prevent conflicts with scrolling parents.
     */
    private Runnable mDisallowIntercept;

    /**
     * Whether this listener is currently forwarding touch events.
     */
    private boolean mForwarding;

    /**
     * The id of the first pointer down in the current event stream.
     */
    private int mActivePointerId;

    private final boolean mImmediately;

    public ForwardingListener(View src) {
        this(src, false);
    }

    public ForwardingListener(View src, boolean immediately) {
        this(src, immediately, null);
    }

    public ForwardingListener(View src, boolean immediately, ForwardingLayout dst) {
        mSrc = src;
        mDst = dst;
        mImmediately = immediately;
        mScaledTouchSlop = ViewConfiguration.get(src.getContext()).getScaledTouchSlop();
        mTapTimeout = ViewConfiguration.getTapTimeout();

        src.addOnAttachStateChangeListener(this);
    }

    /**
     * Returns the layout to which this listener is forwarding events.
     * <p>
     * Override this to return the correct layout. If the layout is displayed
     * asynchronously, you may also need to override
     * {@link #onForwardingStopped} to prevent premature cancelation of
     * forwarding.
     *
     * @return the layout to which this listener is forwarding events
     */
    public ForwardingLayout getForwardingLayout() {
        return null;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        final boolean wasForwarding = mForwarding;
        final boolean forwarding;
        if (wasForwarding) {
            forwarding = onTouchForwarded(event) || !onForwardingStopped();
        } else {
            forwarding = onTouchObserved(event) && onForwardingStarted();

            if (mImmediately && forwarding) {
                mForwarding = true;
                onTouch(v, event);
            }
        }

        mForwarding = forwarding;
        return forwarding || wasForwarding;
    }

    @Override
    public void onViewAttachedToWindow(View v) {
    }

    @Override
    public void onViewDetachedFromWindow(View v) {
        mForwarding = false;
        mActivePointerId = MotionEvent.INVALID_POINTER_ID;

        if (mDisallowIntercept != null) {
            mSrc.removeCallbacks(mDisallowIntercept);
        }
    }

    /**
     * Called when forwarding would like to start.
     *
     * @return true to start forwarding, false otherwise
     */
    protected boolean onForwardingStarted() {
        return true;
    }

    /**
     * Called when forwarding would like to stop.
     *
     * @return true to stop forwarding, false otherwise
     */
    protected boolean onForwardingStopped() {
        return true;
    }

    /**
     * Observes motion events and determines when to start forwarding.
     *
     * @param srcEvent motion event in source view coordinates
     * @return true to start forwarding motion events, false otherwise
     */
    private boolean onTouchObserved(MotionEvent srcEvent) {
        final View src = mSrc;
        if (!src.isEnabled()) {
            return false;
        }

        final int actionMasked = srcEvent.getActionMasked();
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = srcEvent.getPointerId(0);
                if (!mImmediately) {
                    if (mDisallowIntercept == null) {
                        mDisallowIntercept = new DisallowIntercept();
                    }
                    src.postDelayed(mDisallowIntercept, mTapTimeout);
                    break;
                }
            case MotionEvent.ACTION_MOVE:
                final int activePointerIndex = srcEvent.findPointerIndex(mActivePointerId);
                if (activePointerIndex >= 0) {
                    final float x = srcEvent.getX(activePointerIndex);
                    final float y = srcEvent.getY(activePointerIndex);
                    if (!ViewUtils.pointInView(src, x, y, mScaledTouchSlop) || mImmediately) {
                        // The pointer has moved outside of the view.
                        if (mDisallowIntercept != null) {
                            src.removeCallbacks(mDisallowIntercept);
                        }
                        src.getParent().requestDisallowInterceptTouchEvent(true);
                        return true;
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mDisallowIntercept != null) {
                    src.removeCallbacks(mDisallowIntercept);
                }
                break;
        }

        return false;
    }

    /**
     * Handled forwarded motion events and determines when to stop
     * forwarding.
     *
     * @param srcEvent motion event in source view coordinates
     * @return true to continue forwarding motion events, false to cancel
     */
    private boolean onTouchForwarded(MotionEvent srcEvent) {
        final View src = mSrc;
        final ForwardingLayout dst = mDst != null ? mDst : getForwardingLayout();
        if (dst == null || !dst.isShown()) {
            return false;
        }

        // Convert event to destination-local coordinates.
        final MotionEvent dstEvent = MotionEvent.obtainNoHistory(srcEvent);
        assert dstEvent != null;

        ViewUtils.toGlobalMotionEvent(src, dstEvent);
        ViewUtils.toLocalMotionEvent(dst, dstEvent);

        // Forward converted event to destination view, then recycle it.
        final boolean handled = dst.onForwardedEvent(dstEvent, mActivePointerId);
        dstEvent.recycle();
        return handled;
    }

    private class DisallowIntercept implements Runnable {
        @Override
        public void run() {
            final ViewParent parent = mSrc.getParent();
            parent.requestDisallowInterceptTouchEvent(true);
        }
    }
}
