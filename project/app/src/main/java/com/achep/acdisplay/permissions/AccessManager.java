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
package com.achep.acdisplay.permissions;

import android.content.Context;
import android.support.annotation.NonNull;

import com.achep.base.Device;
import com.achep.base.interfaces.IOnLowMemory;
import com.achep.base.permissions.Permission;
import com.achep.base.permissions.PermissionGroup;

/**
 * @author Artem Chepurnoy
 */
public class AccessManager implements IOnLowMemory {

    @NonNull
    private final PermissionGroup mMasterPermissions;
    @NonNull
    private final PermissionGroup mKeyguardPermissions;

    public AccessManager(@NonNull Context context) {
        context = context.getApplicationContext();
        PermissionGroup.Builder builder;

        builder = new PermissionGroup.Builder(context);
        builder.add(Permission.PERMISSION_DEVICE_ADMIN);
        builder.add(Device.hasJellyBeanMR2Api()
                ? Permission.PERMISSION_NOTIFICATION_LISTENER
                : Permission.PERMISSION_ACCESSIBILITY);
        mMasterPermissions = builder.build();

        builder = new PermissionGroup.Builder(context);
        if (Device.hasLollipopApi()) builder.add(Permission.PERMISSION_USAGE_STATS);
        mKeyguardPermissions = builder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLowMemory() {
        mMasterPermissions.onLowMemory();
        mKeyguardPermissions.onLowMemory();
    }

    /**
     * @return The group of permissions which is required for basic
     * app's functionality.
     */
    @NonNull
    public PermissionGroup getMasterPermissions() {
        return mMasterPermissions;
    }

    /**
     * @return The group of permissions which is required for
     * keyguard's functionality.
     */
    @NonNull
    public PermissionGroup getKeyguardPermissions() {
        return mKeyguardPermissions;
    }

}
