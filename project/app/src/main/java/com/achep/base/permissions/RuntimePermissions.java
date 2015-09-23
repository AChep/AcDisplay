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
package com.achep.base.permissions;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import com.achep.base.Device;

/**
 * @author Artem Chepurnoy
 * @since 4.0.0
 */
public class RuntimePermissions {

    /**
     * @return {@code true} if the app has this permission given,
     * {@code false} otherwise.
     */
    @SuppressLint("NewApi")
    public static boolean has(@NonNull Context context, @NonNull String permission) {
        switch (permission) {
            case Manifest.permission.SYSTEM_ALERT_WINDOW:
                return !Device.hasMarshmallowApi()
                        || Settings.canDrawOverlays(context);
            case Manifest.permission.WRITE_SETTINGS:
                return !Device.hasMarshmallowApi()
                        || Settings.System.canWrite(context);
        }

        final int r = ContextCompat.checkSelfPermission(context, permission);
        return r == PackageManager.PERMISSION_GRANTED;
    }

}
