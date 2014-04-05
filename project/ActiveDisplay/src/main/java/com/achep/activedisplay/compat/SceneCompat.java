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

package com.achep.activedisplay.compat;

import android.annotation.TargetApi;
import android.os.Build;
import android.transition.Scene;
import android.view.ViewGroup;

import com.achep.activedisplay.Device;

/**
 * Created by Artem on 26.03.2014.
 */
public class SceneCompat {

    private final ViewGroup mViewGroup;
    private final ViewGroup mView;
    public Scene scene;

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public SceneCompat(ViewGroup viewGroup, ViewGroup view) {
        if (Device.hasKitKatApi()) {
            scene = new Scene(viewGroup, view);
        }
        mViewGroup = viewGroup;
        mView = view;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void enter() {
        if (Device.hasKitKatApi()) {
            scene.enter();
        } else {
            mViewGroup.removeAllViews();
            mViewGroup.addView(mView);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public ViewGroup getView() {
        return mView;
    }
}
