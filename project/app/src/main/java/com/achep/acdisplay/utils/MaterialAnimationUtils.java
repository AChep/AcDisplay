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
package com.achep.acdisplay.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;
import android.view.ViewAnimationUtils;

/**
 * Created by Artem Chepurnoy on 07.11.2014.
 */
public class MaterialAnimationUtils {

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Animator createCircularReveal(final View view, boolean show) {
        int x = view.getMeasuredWidth() / 2;
        int y = view.getMeasuredHeight() / 2;
        return createCircularReveal(view, x, y, show);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Animator createCircularReveal(final View view, int x, int y, boolean show) {
        Animator anim;

        int distance = (int) Math.floor(Math.hypot(
                view.getMeasuredWidth(),
                view.getMeasuredHeight()) / 2);
        if (show) {
            anim = ViewAnimationUtils.createCircularReveal(view, x, y, 0, distance);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    view.setVisibility(View.VISIBLE);
                }
            });
        } else {
            anim = ViewAnimationUtils.createCircularReveal(view, x, y, distance, 0);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setVisibility(View.INVISIBLE);
                }
            });
        }
        return anim;
    }

}
