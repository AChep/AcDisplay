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
package com.achep.activedisplay;

import android.app.Application;
import android.content.Intent;

import com.achep.activedisplay.services.LockscreenService;

/**
 * Created by Artem on 22.02.14.
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // If lockscreen is enabled launch it on start.
        Config config = Config.getInstance(this);
        if (config.isLockscreenEnabled()) {
            Intent intent = new Intent(this, LockscreenService.class);
            startService(intent);
        }
    }
}
