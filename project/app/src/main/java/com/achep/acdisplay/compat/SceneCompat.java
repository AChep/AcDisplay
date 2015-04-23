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

import android.transition.Scene;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.achep.base.Device;

import static com.achep.base.Build.DEBUG;

/**
 * This is a restricted {@link android.transition.Scene} compatibility
 * class for supporting Android 4.3 or below.
 *
 * @author Artem Chepurnoy
 */
public class SceneCompat {

    private static final String TAG = "SceneCompat";

    private final ViewGroup mView;
    private final ViewGroup mParent;
    private Scene mScene;

    public SceneCompat(ViewGroup parent, ViewGroup view) {
        if (Device.hasKitKatApi()) {
            mScene = new Scene(parent, view);
        }
        mParent = parent;
        mView = view;
    }

    public Scene getScene() {
        return mScene;
    }

    public ViewGroup getView() {
        return mView;
    }

    public void enter() {
        if (Device.hasKitKatApi()) {
            // FIXME: This is a workaround that more research.
            ViewParent vp = mView.getParent();
            if (vp != null) {
                if (DEBUG) Log.d(TAG, "Removing the view[" + mView + "] from [" + vp + "]!");
                ((ViewGroup) vp).removeView(mView);
            }

            mScene.enter();
        } else {
            mParent.removeAllViews();
            mParent.addView(mView);
        }
    }

}
