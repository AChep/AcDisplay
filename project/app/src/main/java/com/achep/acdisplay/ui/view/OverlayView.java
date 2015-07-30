/*
 * Copyright (C) 2015 AChep@xda <artemchep@gmail.com>
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
import android.graphics.PixelFormat;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import static com.achep.base.Build.DEBUG;

/**
 * @author Artem Chepurnoy
 */
public class OverlayView extends FrameLayout {

    private static final String TAG = "OverlayView";

    private static final int SYSTEM_UI_BASIC_FLAGS =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

    public OverlayView(Context context) {
        super(context);
    }

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setWindowSystemUiVisibilityFlags();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        setWindowSystemUiVisibilityFlags();
    }

    @Override
    public void onWindowSystemUiVisibilityChanged(int visible) {
        super.onWindowSystemUiVisibilityChanged(visible);
        postDelayed(new Runnable() {
            @Override
            public void run() {
                setWindowSystemUiVisibilityFlags();
            }
        }, 100);
    }

    /**
     *
     */
    protected void setWindowSystemUiVisibilityFlags() {
        final int flags = SYSTEM_UI_BASIC_FLAGS | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        setSystemUiVisibility(flags);
    }

    /**
     * Clears and then sets back immersive mode flags, so status-bar/nav-bar
     * definitely `hides`.
     */
    protected void setWindowSystemUiVisibilityFlagsRefresh() {
        final int flags = SYSTEM_UI_BASIC_FLAGS | View.SYSTEM_UI_FLAG_IMMERSIVE;
        setSystemUiVisibility(flags);
        postDelayed(new Runnable() {
            @Override
            public void run() {
                setWindowSystemUiVisibilityFlags();
            }
        }, 40);
    }

    /**
     * Safely adds this view to the window manager.
     */
    public void addOverlayView() {
        final Context context = getContext();
        final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        try {
            wm.addView(this, createLayoutParams());
        } catch (Exception e) {
            if (DEBUG) Log.d(TAG, "Failed to add overlay view: message=" + e.getMessage());
        }
    }

    /**
     * Safely removes this view from the window manager.
     */
    public void removeOverlayView() {
        final Context context = getContext();
        final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        try {
            wm.removeView(this);
        } catch (Exception e) {
            if (DEBUG) Log.d(TAG, "Failed to add overlay view: message=" + e.getMessage());
        }
    }

    @NonNull
    private WindowManager.LayoutParams createLayoutParams() {
        int type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        int flags = WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type, flags,
                PixelFormat.RGB_565);
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN;
        return lp;
    }
}
