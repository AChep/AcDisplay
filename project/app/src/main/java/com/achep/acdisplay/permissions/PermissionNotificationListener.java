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

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.achep.acdisplay.R;
import com.achep.acdisplay.services.MediaService;
import com.achep.base.Device;
import com.achep.base.permissions.Permission;
import com.achep.base.tests.Check;

/**
 * The Notification listener service permission.
 *
 * @author Artem Chepurnoy
 * @see android.service.notification.NotificationListenerService
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public final class PermissionNotificationListener extends Permission {

    private static final String KEY = "enabled_notification_listeners";
    private static final String ACTION = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    @NonNull
    private final String mComponentString;

    public PermissionNotificationListener(@NonNull Context context) {
        super(context);
        Check.getInstance().isTrue(Device.hasJellyBeanMR2Api());

        mComponentString = new ComponentName(mContext, MediaService.class).flattenToString();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isGranted() {
        final ContentResolver cr = mContext.getContentResolver();
        final String flat = Settings.Secure.getString(cr, KEY);
        return flat != null && flat.contains(mComponentString);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Intent getIntentSettings() {
        return new Intent(ACTION);
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
        return R.string.permissions_notifications_grant_manually;
    }

}
