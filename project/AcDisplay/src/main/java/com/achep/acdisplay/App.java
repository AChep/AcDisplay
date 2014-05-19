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

import com.achep.acdisplay.activemode.ActiveModeService;
import com.achep.acdisplay.blacklist.Blacklist;
import com.achep.acdisplay.services.KeyguardService;

/**
 * Created by Artem on 22.02.14.
 */
public class App extends Application {

    public static final int ID_NOTIFY_KEYGUARD = 10;
    public static final int ID_NOTIFY_ACTIVE_MODE = 20;
    public static final int ID_NOTIFY_INIT = 30;
    public static final int ID_NOTIFY_TEST = 40;

    @Override
    public void onCreate() {
        Config.getInstance().init(this);
        Blacklist.getInstance().init(this);

        super.onCreate();

        // Launch keyguard and (or) active mode on
        // app launch.
        KeyguardService.handleState(this);
        ActiveModeService.handleState(this);
    }
}
