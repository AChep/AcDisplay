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

package com.achep.acdisplay.compat;

import android.annotation.SuppressLint;
import android.transition.Scene;
import android.view.ViewGroup;

import com.achep.acdisplay.Device;

/**
 * This is a restricted {@link android.transition.Scene} compatibility
 * class for supporting Android 4.3 or below.
 *
 * @author Artem Chepurnoy
 */
@SuppressLint("NewApi")
public class SceneCompat {

    private final ViewGroup mViewGroup;
    private final ViewGroup mView;
    public Scene scene;

    public SceneCompat(ViewGroup viewGroup, ViewGroup view) {
        if (Device.hasKitKatApi()) {
            scene = new Scene(viewGroup, view);
        }
        mViewGroup = viewGroup;
        mView = view;
    }

    public void enter() {
        if (Device.hasKitKatApi()) {
            scene.enter();
        } else {
            mViewGroup.removeAllViews();
            mViewGroup.addView(mView);
        }
    }

    public ViewGroup getView() {
        return mView;
    }
}
