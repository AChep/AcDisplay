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
package com.achep.base.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.annotation.NonNull;
import android.view.View;

import com.achep.acdisplay.R;
import com.achep.base.Device;
import com.achep.base.interfaces.IActivityBase;

import dreamers.graphics.RippleDrawable;

/**
 * Created by Artem Chepurnoy on 04.01.2015.
 */
public class RippleUtils {

    /**
     * @return {@code false} if the ripple has been set, {@code true} otherwise
     */
    public static boolean makeFor(@NonNull View view, boolean parentIsScrollContainer) {
        return makeFor(parentIsScrollContainer, true, view);
    }

    /**
     * @return {@code false} if the ripple has been set, {@code true} otherwise
     */
    public static boolean makeFor(boolean parentIsScrollContainer,
                                  boolean darkTheme, @NonNull View... views) {
        ColorStateList csl = views[0].getResources().getColorStateList(
                darkTheme ? R.color.ripple_dark : R.color.ripple_light);
        return csl == null || makeFor(csl, parentIsScrollContainer, views);
    }

    public static boolean makeFor(@NonNull ColorStateList csl,
                                  boolean parentIsScrollContainer,
                                  @NonNull View... views) {
        if (!Device.hasLollipopApi()) {
            // Do not create ripple effect if in power save mode, because
            // this will drain more energy.
            Context context = views[0].getContext();
            if (context instanceof IActivityBase) {
                IActivityBase activityBase = (IActivityBase) context;
                if (activityBase.isPowerSaveMode()) {
                    return true;
                }
            }

            for (View view : views) {
                view.setBackground(null);
                RippleDrawable.makeFor(view, csl, parentIsScrollContainer);
            }
            return false;
        }
        return true;
    }

}
