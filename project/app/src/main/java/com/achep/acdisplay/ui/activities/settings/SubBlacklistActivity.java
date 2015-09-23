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
package com.achep.acdisplay.ui.activities.settings;

import android.app.FragmentManager;
import android.util.Log;
import android.view.Menu;

/**
 * Top-level settings activity to handle single pane and double pane UI layout.
 */
public class SubBlacklistActivity extends BlacklistActivity {

    private static final String TAG = "SubBlacklist";

    @Override
    public boolean onNavigateUp() {
        if (!popFragment()) {
            finish();
        }
        return true;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        Log.d(TAG, "Launching fragment " + fragmentName);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    private boolean popFragment() {
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
            return true;
        }
        return false;
    }

}
