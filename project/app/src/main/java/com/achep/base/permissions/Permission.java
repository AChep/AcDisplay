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

import android.content.Context;
import android.content.Intent;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.achep.acdisplay.permissions.PermissionAccessibility;
import com.achep.acdisplay.permissions.PermissionDeviceAdmin;
import com.achep.acdisplay.permissions.PermissionNotificationListener;
import com.achep.acdisplay.permissions.PermissionUsageStats;
import com.achep.base.interfaces.IPermission;
import com.achep.base.interfaces.ISubscriptable;
import com.achep.base.utils.IntentUtils;

import java.util.ArrayList;

/**
 * @author Artem Chepurnoy
 */
public abstract class Permission implements
        ISubscriptable<Permission.OnPermissionStateChanged>,
        IPermission {

    /**
     * @see com.achep.acdisplay.permissions.PermissionAccessibility
     */
    @NonNull
    public static final String PERMISSION_ACCESSIBILITY =
            PermissionAccessibility.class.getSimpleName();

    /**
     * @see com.achep.acdisplay.permissions.PermissionDeviceAdmin
     */
    @NonNull
    public static final String PERMISSION_DEVICE_ADMIN =
            PermissionDeviceAdmin.class.getSimpleName();

    /**
     * @see com.achep.acdisplay.permissions.PermissionNotificationListener
     */
    @NonNull
    public static final String PERMISSION_NOTIFICATION_LISTENER =
            PermissionNotificationListener.class.getSimpleName();

    /**
     * @see com.achep.acdisplay.permissions.PermissionUsageStats
     */
    @NonNull
    public static final String PERMISSION_USAGE_STATS =
            PermissionUsageStats.class.getSimpleName();

    /**
     * @see com.achep.base.permissions.Permission#PERMISSION_ACCESSIBILITY
     * @see com.achep.base.permissions.Permission#PERMISSION_DEVICE_ADMIN
     * @see com.achep.base.permissions.Permission#PERMISSION_NOTIFICATION_LISTENER
     * @see com.achep.base.permissions.Permission#PERMISSION_USAGE_STATS
     */
    @NonNull
    public static Permission newInstance(@NonNull Context context, @NonNull String name) {
        if (PERMISSION_ACCESSIBILITY.equals(name)) {
            return new PermissionAccessibility(context);
        } else if (PERMISSION_DEVICE_ADMIN.equals(name)) {
            return new PermissionDeviceAdmin(context);
        } else if (PERMISSION_NOTIFICATION_LISTENER.equals(name)) {
            return new PermissionNotificationListener(context);
        } else if (PERMISSION_USAGE_STATS.equals(name)) {
            return new PermissionUsageStats(context);
        }

        throw new IllegalArgumentException();
    }

    //-- MAIN -----------------------------------------------------------------

    @NonNull
    protected final Context mContext;
    @NonNull
    protected final ArrayList<OnPermissionStateChanged> mListeners = new ArrayList<>();

    public interface OnPermissionStateChanged {
    }

    public Permission(@NonNull Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerListener(@NonNull OnPermissionStateChanged listener) {
        synchronized (this) {
            mListeners.add(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterListener(@NonNull OnPermissionStateChanged listener) {
        synchronized (this) {
            mListeners.remove(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLowMemory() { /* empty */ }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(@NonNull Context context) {
        return IntentUtils.hasActivityForThat(context, getIntentSettings());
    }

    /**
     * @return An intent to <i>enable the permission</i> settings' screen.
     */
    @NonNull
    public abstract Intent getIntentSettings();

    //-- UI -------------------------------------------------------------------

    @DrawableRes
    public int getIconResource() {
        return 0;
    }

    @StringRes
    public int getTitleResource() {
        return 0;
    }

    @StringRes
    public int getSummaryResource() {
        return 0;
    }

    @StringRes
    public int getErrorResource() {
        return 0;
    }

}
