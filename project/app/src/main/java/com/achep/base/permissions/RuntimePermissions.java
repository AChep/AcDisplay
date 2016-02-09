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
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import com.achep.base.AppHeap;
import com.achep.base.Device;
import com.achep.base.content.CfgBase;
import com.achep.base.notifications.NotificationSpace;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Artem Chepurnoy
 * @since 4.0.0
 */
public class RuntimePermissions extends CfgBase {

    private static final String PREFERENCES_FILE_NAME = "__permissions";

    @Nullable
    private static RuntimePermissions sRuntimePermissions;

    /**
     * @return {@code true} if the app has this permission given,
     * {@code false} otherwise.
     * @see #allowed(Context, String)
     */
    @SuppressLint("NewApi")
    public static boolean has(@NonNull Context context, @NonNull String permission) {
        if (allowed(context, permission)) {
        } else return false;

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

    /**
     * @return {@code true} if the app may ask this permission,
     * {@code false} otherwise.
     * @see #has(Context, String)
     */
    public static boolean allowed(@NonNull Context context, @NonNull String permission) {
        return getInstance().contains(permission) && getInstance().getBoolean(permission);
    }

    public static void ask(@NonNull Context context, @NonNull String permission) {
        final boolean disturbing = context instanceof Activity;
        if (disturbing) {
        } else {
            NotificationSpace.getInstance().ensure(0);
        }
    }

    /**
     * @return a singlet object of the runtime permissions manager.
     */
    @NonNull
    public static synchronized RuntimePermissions getInstance() {
        if (sRuntimePermissions == null) {
            sRuntimePermissions = new RuntimePermissions();
        }
        return sRuntimePermissions;
    }

    //-- BEGIN ----------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    private RuntimePermissions() {
        super();
        NotificationSpace.getInstance().requestRange(100, 100);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public String getPreferencesFileName() {
        return PREFERENCES_FILE_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onConfigChanged(@NonNull Transaction transaction, @NonNull Option option) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreateMap(@NonNull Map<String, Option> map) {
        // Fulfil the set of permissions.
        Set<String> set = new HashSet<>();
        set.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        AppHeap.getInstance().getConfiguration().getPermissions().onBuildPermissions(set);
        // Create a map
        for (String permission : set) {
            putOption(map, new CfgBase.Option(
                    boolean.class, permission, true,
                    0, 0, Integer.MAX_VALUE));
        }
    }
}
