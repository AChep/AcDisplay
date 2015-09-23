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

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.achep.acdisplay.R;
import com.achep.acdisplay.receiver.AdminReceiver;
import com.achep.base.permissions.Permission;

/**
 * The Device admin permission.
 *
 * @author Artem Chepurnoy
 * @see android.app.admin.DevicePolicyManager
 */
public final class PermissionDeviceAdmin extends Permission {

    @NonNull
    private final ComponentName mComponent;

    public PermissionDeviceAdmin(@NonNull Context context) {
        super(context);
        mComponent = new ComponentName(mContext, AdminReceiver.class);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isGranted() {
        return getDpmService().isAdminActive(mComponent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(@NonNull Context context) {
        return true;
    }

    @NonNull
    private DevicePolicyManager getDpmService() {
        return (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Intent getIntentSettings() {
        return new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mComponent);
    }

    //-- UI -------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @DrawableRes
    public int getIconResource() {
        return R.drawable.ic_dialog_timeout_white;
    }

    /**
     * {@inheritDoc}
     */
    @StringRes
    public int getTitleResource() {
        return R.string.permissions_device_admin;
    }

    /**
     * {@inheritDoc}
     */
    @StringRes
    public int getSummaryResource() {
        return R.string.permissions_device_admin_description;
    }

    /**
     * {@inheritDoc}
     */
    @StringRes
    public int getErrorResource() {
        return R.string.permissions_device_admin_grant_manually;
    }

}
