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
package com.achep.base.ui.animations;

import android.animation.Animator;

/**
 * This adapter class provides empty implementations of the methods from
 * {@link android.animation.Animator.AnimatorListener}.
 * Any custom listener that cares only about a subset of the methods of this listener can
 * simply subclass this adapter class instead of implementing the interface directly.
 * </br>
 * In addition, it provides {@link #isCanceled()} method.
 *
 * @author Artem Chepurnoy
 */
public class AnimatorListenerAdapter extends android.animation.AnimatorListenerAdapter {

    private boolean mCanceled;

    @Override
    public void onAnimationStart(Animator animation) {
        mCanceled = false;
        super.onAnimationStart(animation);
    }

    @Override
    public void onAnimationCancel(Animator animation) {
        mCanceled = true;
        super.onAnimationCancel(animation);
    }

    /**
     * @return {@code true} if the animation is canceled, {@code false} otherwise.
     */
    public boolean isCanceled() {
        return mCanceled;
    }

}
