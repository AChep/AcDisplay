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
package com.achep.acdisplay.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.util.Log;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;

/**
 * Created by Artem Chepurnoy on 04.05.2015.
 */
public class CornerHelper {

    private static final String TAG = "CornerHelper";

    private static final int[] ICON_IDS = {
            R.drawable.ic_corner_unlock_white,
            R.drawable.ic_corner_launch_photo_camera_white,
            R.drawable.ic_corner_launch_dialer_white,
    };

    /**
     * @return the icon's resource of the specific action.
     */
    @DrawableRes
    public static int getIconResource(int actionId) {
        return ICON_IDS[actionId];
    }

    /**
     * Performs the specific corner-action.
     */
    public static void perform(@NonNull Context context, int actionId) {
        switch (actionId) {
            case Config.CORNER_UNLOCK:
                // Do nothing special.
                break;
            case Config.CORNER_LAUNCH_CAMERA:
                try {
                    Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
                    context.startActivity(intent);
                } catch (Exception e) {
                    Log.i(TAG, "Unable to launch a camera.");
                    e.printStackTrace();
                }
                break;
            case Config.CORNER_LAUNCH_DIALER:
                try {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:")); // TODO: Check if works on all devices.
                    context.startActivity(intent);
                } catch (Exception e) {
                    Log.i(TAG, "Unable to launch a dialer.");
                    e.printStackTrace();
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

}
