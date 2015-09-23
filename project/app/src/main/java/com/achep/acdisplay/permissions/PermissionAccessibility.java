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

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.Log;

import com.achep.acdisplay.R;
import com.achep.acdisplay.services.AccessibilityService;
import com.achep.base.permissions.Permission;

/**
 * The Accessibility notification permission.
 *
 * @author Artem Chepurnoy
 * @see com.achep.acdisplay.services.AccessibilityService
 */
public final class PermissionAccessibility extends Permission {

    private static final String TAG = "PermissionAcbNotify";

    @NonNull
    private final String mComponentString;

    public PermissionAccessibility(@NonNull Context context) {
        super(context);
        mComponentString = new ComponentName(mContext, AccessibilityService.class).flattenToString();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isGranted() {
        final ContentResolver cr = mContext.getContentResolver();

        try {
            int r = Settings.Secure.getInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED);
            if (r != 1) return false;
        } catch (Settings.SettingNotFoundException e) {
            Log.w(TAG, "Accessibility enabled setting not found!");
        }

        final String flat = Settings.Secure.getString(cr,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return flat != null && flat.contains(mComponentString);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Intent getIntentSettings() {
        return new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
    }

    //-- UI -------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @DrawableRes
    public int getIconResource() {
        return R.drawable.stat_notify;
    }

    /**
     * {@inheritDoc}
     */
    @StringRes
    public int getTitleResource() {
        return R.string.permissions_notifications;
    }

    /**
     * {@inheritDoc}
     */
    @StringRes
    public int getSummaryResource() {
        return R.string.permissions_notifications_description;
    }

    /**
     * {@inheritDoc}
     */
    @StringRes
    public int getErrorResource() {
        return 0; // Should never be needed
    }

}
