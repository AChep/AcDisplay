/*
 * Copyright (C) 2013-2014 AChep@xda <artemchep@gmail.com>
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
package com.achep.activedisplay.utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

/**
 * Created by Artem on 28.01.14.
 */
public class PowerUtils {

    public static boolean isConnected(Context context) {
        Intent intent = context.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        // TODO: I'm not sure if we need that.
        if (intent == null) {
            return false;
        }

        final int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return plugged == BatteryManager.BATTERY_PLUGGED_AC
                || plugged == BatteryManager.BATTERY_PLUGGED_USB;
    }

}
