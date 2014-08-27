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
package com.achep.acdisplay;

import android.app.Application;
import android.content.Context;

import com.achep.acdisplay.blacklist.Blacklist;
import com.achep.acdisplay.services.KeyguardService;
import com.achep.acdisplay.services.SensorsDumpService;
import com.achep.acdisplay.services.activemode.ActiveModeService;
import com.achep.acdisplay.utils.ToastUtils;

/**
 * Created by Artem on 22.02.14.
 */
public class App extends Application {

    public static final int ID_NOTIFY_INIT = 30;
    public static final int ID_NOTIFY_TEST = 40;
    public static final int ID_NOTIFY_BATH = 50;

    public static final String ACTION_BIND_MEDIA_CONTROL_SERVICE = "com.achep.acdisplay.BIND_MEDIA_CONTROL_SERVICE";

    public static final String ACTION_ENABLE = "com.achep.acdisplay.ENABLE";
    public static final String ACTION_DISABLE = "com.achep.acdisplay.DISABLE";
    public static final String ACTION_TOGGLE = "com.achep.acdisplay.TOGGLE";

    public static final String ACTION_EAT_HOME_PRESS_START = "com.achep.acdisplay.EAT_HOME_PRESS_START";
    public static final String ACTION_EAT_HOME_PRESS_STOP = "com.achep.acdisplay.EAT_HOME_PRESS_STOP";

    public static final String ACTION_INTERNAL_TIMEOUT = "TIMEOUT";
    public static final String ACTION_INTERNAL_PING_SENSORS = "PING_SENSORS";

    @Override
    public void onCreate() {
        Config.getInstance().init(this);
        Blacklist.getInstance().init(this);

        super.onCreate();

        // Launch keyguard and (or) active mode on
        // app launch.
        KeyguardService.handleState(this);
        ActiveModeService.handleState(this);
        SensorsDumpService.handleState(this);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Blacklist.getInstance().onLowMemory();
    }

    /**
     * Starts Easter Eggs' activity.
     */
    // TODO: Put an Easter egg here.
    public static void startEasterEggs(Context context) {
        if (Build.DEBUG) ToastUtils.showShort(context, "There will be an Easter Egg.");
    }

}
