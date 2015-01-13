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
package com.achep.base.ui.activities;

import android.support.annotation.NonNull;
import android.util.Log;

import com.achep.base.dashboard.DashboardTile;

/**
 * Stub class for showing sub-settings; we can't use the main Settings class
 * since for our app it is a special singleTask class.
 */
public class SubSettings extends SettingsActivity {

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected boolean isValidFragment(@NonNull String fragmentName) {
        Log.d("SubSettings", "Launching fragment " + fragmentName);
        return true;
    }

    @Override
    protected boolean isTileSupported(@NonNull DashboardTile tile) {
        return false;
    }

}
