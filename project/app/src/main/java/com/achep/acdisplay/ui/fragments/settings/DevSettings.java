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
package com.achep.acdisplay.ui.fragments.settings;

import android.os.Bundle;
import android.preference.Preference;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;

/**
 * Development settings fragment.
 *
 * @author Artem Chepurnoy
 */
public class DevSettings extends BaseSettings implements
        Preference.OnPreferenceClickListener {

    private Preference mSensorsDumpSendPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_dev_fragment);
        syncPreference(Config.KEY_DEV_SENSORS_DUMP);

        mSensorsDumpSendPreference = findPreference("dev_sensors_dump_send");
        mSensorsDumpSendPreference.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mSensorsDumpSendPreference) {

        } else
            return false;
        return true;
    }

}
