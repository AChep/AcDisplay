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
package com.achep.base.utils.power;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Display;

import com.achep.base.Device;
import com.achep.base.utils.Operator;

import java.lang.reflect.Method;

import timber.log.Timber;

/**
 * Helper class with utils related to power.
 *
 * @author Artem Chepurnoy
 */
public class PowerUtils {

    private static final String TAG = "PowerUtils";

    /**
     * @return true is device is plugged at this moment, false otherwise.
     * @see #isPlugged(android.content.Intent)
     */
    public static boolean isPlugged(@NonNull Context context) {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        return isPlugged(context.getApplicationContext().registerReceiver(null, intentFilter));
    }

    /**
     * @return true is device is plugged at this moment, false otherwise.
     * @see #isPlugged(android.content.Context)
     */
    @SuppressLint("InlinedApi")
    public static boolean isPlugged(@Nullable Intent intent) {
        if (intent == null) {
            return false;
        }

        final int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return plugged == BatteryManager.BATTERY_PLUGGED_AC
                || plugged == BatteryManager.BATTERY_PLUGGED_USB
                || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;
    }

    @SuppressLint("NewApi")
    public static boolean isScreenOn(@NonNull Context context) {
        display_api:
        if (Device.hasKitKatWatchApi()) {
            DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            Display[] displays = dm.getDisplays(null);
            Display display = null;
            if (displays == null || displays.length == 0) {
                break display_api;
            } else if (displays.length > 1) {
                Timber.tag(TAG).i("The number of logical displays is " + displays.length);
            }

            for (Display d : displays) {
                final boolean virtual = Operator.bitAnd(d.getFlags(), Display.FLAG_PRESENTATION);
                if (d.isValid() && !virtual) {
                    display = d;

                    final int type;
                    try {
                        Method method = Display.class.getDeclaredMethod("getType");
                        method.setAccessible(true);
                        type = (int) method.invoke(d);
                    } catch (Exception e) {
                        continue;
                    }

                    if (type == 1 /* built-in display */) {
                        break;
                    }
                }
            }

            if (display == null) {
                return false;
            }

            Timber.tag(TAG).i("Display state=" + display.getState());
            return display.getState() == Display.STATE_ON;
        }
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return isInteractive(pm);
    }

    /**
     * Returns {@code true} if the device is in an interactive state.
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static boolean isInteractive(@NonNull PowerManager pm) {
        return Device.hasLollipopApi()
                ? pm.isInteractive()
                : pm.isScreenOn();
    }

    /**
     * @see #isPlugged(android.content.Context)
     * @see #isPlugged(android.content.Intent)
     */
    @SuppressLint("InlinedApi")
    public static int getBatteryLevel(@Nullable Intent intent) {
        if (intent == null) {
            return 100;
        }

        final int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        final int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return level * 100 / scale;
    }

}
