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
package com.achep.acdisplay.receiver;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;

/**
 * Created by Artem on 03.01.14.
 */
public class AdminReceiver extends android.app.admin.DeviceAdminReceiver {

    public static final String ACTION_DISABLED = "device_admin_disabled";
    public static final String ACTION_ENABLED = "device_admin_enabled";

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_DISABLED));

        Config config = Config.getInstance();
        if (config.isEnabled()) {

            Toast.makeText(context, R.string.permissions_auto_disabled, Toast.LENGTH_LONG).show();
            config.setEnabled(context, false, null); // auto disabling :/
        }
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_ENABLED));
    }

    public static ComponentName newComponentName(Context context) {
        return new ComponentName(context, AdminReceiver.class);
    }

}
