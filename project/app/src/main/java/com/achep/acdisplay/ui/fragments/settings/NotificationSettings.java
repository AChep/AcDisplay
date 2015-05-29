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
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;

/**
 * Created by Artem on 09.02.14.
 */
public class NotificationSettings extends BaseSettings {

    private final ListPreferenceSetter mListPreferenceNotifyPrioritySetter =
            new ListPreferenceSetter() {

                @Override
                public void updateSummary(@NonNull Preference preference,
                                          @NonNull Config.Option option,
                                          @NonNull Object value) {
                    int pos = -(int) value + 2;
                    ListPreference cbp = (ListPreference) preference;
                    cbp.setSummary(cbp.getEntries()[pos]);
                }

            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_notification_fragment);
        syncPreference(Config.KEY_NOTIFY_MIN_PRIORITY, mListPreferenceNotifyPrioritySetter);
        syncPreference(Config.KEY_NOTIFY_MAX_PRIORITY, mListPreferenceNotifyPrioritySetter);
        syncPreference(Config.KEY_NOTIFY_WAKE_UP_ON);
        syncPreference(Config.KEY_NOTIFY_GLANCE);
    }

}
