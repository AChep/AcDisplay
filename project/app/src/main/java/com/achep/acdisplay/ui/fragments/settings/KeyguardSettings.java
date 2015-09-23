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

import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;

import com.achep.acdisplay.App;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.base.permissions.PermissionGroup;

/**
 * Created by Artem on 09.02.14.
 */
public class KeyguardSettings extends BaseSettings {

    @NonNull
    private final ListPreferenceSetter mDelayPreferenceSetter = new ListPreferenceSetter() {

        @Override
        public void updateSummary(@NonNull Preference preference,
                                  @NonNull Config.Option option,
                                  @NonNull Object value) {
            ListPreference cbp = (ListPreference) preference;
            final CharSequence valueStr = Integer.toString((Integer) value);
            final CharSequence[] values = cbp.getEntryValues();
            final int length = values.length;
            for (int i = 0; i < length; i++) {
                if (valueStr.equals(values[i])) {
                    cbp.setSummary(cbp.getEntries()[i]);
                    break;
                }
            }

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getActivity();

        // Request to grant the keyguard permissions if possible,
        // no need of permissions otherwise (since AcDisplay v3.8).
        PermissionGroup pg = App.getAccessManager().getKeyguardPermissions();
        if (pg.exists(context)) {
            requestMasterSwitch(Config.KEY_KEYGUARD, pg.permissions);
        } else requestMasterSwitch(Config.KEY_KEYGUARD);

        addPreferencesFromResource(R.xml.settings_keyguard_fragment);
        syncPreference(Config.KEY_KEYGUARD_RESPECT_INACTIVE_TIME);
        syncPreference(Config.KEY_KEYGUARD_WITHOUT_NOTIFICATIONS);
        syncPreference(Config.KEY_KEYGUARD_LOCK_DELAY, mDelayPreferenceSetter);
    }

}
