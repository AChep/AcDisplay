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
package com.achep.base.ui.widgets;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

/**
 * @author Artem Chepurnoy
 */
public class FloatingActionButton extends android.support.design.widget.FloatingActionButton {
    private static final int TRANSLATE_DURATION_MILLIS = 200;

    @NonNull
    private final Interpolator mInterpolator = new AccelerateDecelerateInterpolator();

    private boolean mVisible = true;
    private ValueAnimator mAnimator;

    public FloatingActionButton(Context context) {
        super(context);
    }

    public FloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public boolean isVisible() {
        return mVisible;
    }

    public void show() {
        show(true);
    }

    public void hide() {
        hide(true);
    }

    public void show(boolean animate) {
        toggle(true, animate, false);
    }

    public void hide(boolean animate) {
        toggle(false, animate, false);
    }

    private void toggle(final boolean visible, final boolean animate, boolean force) {
        if (mVisible != visible || force) {
            mVisible = visible;
            int height = getHeight();
            if (height == 0 && !force) {
                ViewTreeObserver vto = getViewTreeObserver();
                if (vto.isAlive()) {
                    vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            ViewTreeObserver currentVto = getViewTreeObserver();
                            if (currentVto.isAlive()) {
                                currentVto.removeOnPreDrawListener(this);
                            }
                            toggle(visible, animate, true);
                            return true;
                        }
                    });
                    return;
                }
            }
            int translationY = visible ? 0 : height + getMarginBottom();
            if (mAnimator != null && mAnimator.isRunning()) mAnimator.cancel();
            if (animate) {
                mAnimator = ObjectAnimator.ofFloat(this, TRANSLATION_Y,
                        getTranslationY(),
                        translationY);
                mAnimator.setInterpolator(mInterpolator);
                mAnimator.setDuration(TRANSLATE_DURATION_MILLIS);
                mAnimator.start();
            } else {
                setTranslationY(translationY);
            }

            // On pre-Honeycomb a translated view is still clickable,
            // so we need to disable clicks manually
            // if (!Device.hasHoneycombApi()) {
            //     setClickable(visible);
            // }
        }
    }

    private int getMarginBottom() {
        final ViewGroup.LayoutParams layoutParams = getLayoutParams();
        return layoutParams instanceof ViewGroup.MarginLayoutParams
                ? ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin
                : 0;
    }

}
