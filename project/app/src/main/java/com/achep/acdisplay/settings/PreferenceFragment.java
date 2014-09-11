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

package com.achep.acdisplay.settings;

import android.app.Activity;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.achep.acdisplay.Build;
import com.achep.acdisplay.Config;

/**
 * Created by achep on 04.06.14.
 */
public class PreferenceFragment extends android.preference.PreferenceFragment {

    private static final String TAG = "PreferenceFragment";

    private Config.Syncer mSyncer;
    private Config mConfig;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mConfig = Config.getInstance();
        mSyncer = new Config.Syncer(activity, mConfig);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSyncer.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSyncer.stop();
    }

    protected Config getConfig() {
        return mConfig;
    }

    protected void syncPreference(String key) {
        Preference preference = findPreference(key);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preference != null) {
            mSyncer.addPreference(preferenceScreen, preference);
        } else if (Build.DEBUG) {
            Log.d(TAG, "Tried to sync non-existent preference with config.");
        }
    }
}
