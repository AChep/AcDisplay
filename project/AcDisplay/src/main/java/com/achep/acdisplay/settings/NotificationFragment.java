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

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;

/**
 * Created by Artem on 09.02.14.
 */
public class NotificationFragment extends PreferenceFragment implements
        Preference.OnPreferenceChangeListener,
        Config.OnConfigChangedListener {

    private CheckBoxPreference mLowPriorityPreference;
    private CheckBoxPreference mWakeUpOnPreference;
    private CheckBoxPreference mPrivacyPreference;

    private boolean mBroadcasting;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.notification_settings);

        mLowPriorityPreference = (CheckBoxPreference) findPreference(
                Config.KEY_NOTIFY_LOW_PRIORITY);
        mWakeUpOnPreference = (CheckBoxPreference) findPreference(
                Config.KEY_NOTIFY_WAKE_UP_ON);
        
        mPrivacyPreference = (CheckBoxPreference) findPreference(
                Config.KEY_NOTIFY_PRIVACY);

        mLowPriorityPreference.setOnPreferenceChangeListener(this);
        mWakeUpOnPreference.setOnPreferenceChangeListener(this);
        mPrivacyPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Config config = Config.getInstance();
        config.addOnConfigChangedListener(this);

        updateLowPriorityPreference(config);
        updateWakeUpOnPreference(config);
        updatePrivacyPreference(config);
    }

    @Override
    public void onPause() {
        super.onPause();
        Config config = Config.getInstance();
        config.removeOnConfigChangedListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mBroadcasting) {
            return true;
        }

        Config config = Config.getInstance();
        if (preference == mLowPriorityPreference) {
            config.setLowPriorityNotificationsAllowed(getActivity(), (Boolean) newValue, this);
        } else if (preference == mWakeUpOnPreference) {
            config.setWakeUpOnNotifyEnabled(getActivity(), (Boolean) newValue, this);
        } else if(preference == mPrivacyPreference) {
           config.setNotificationPrivacy(getActivity(), (Boolean) newValue, this);
        } else return false;
        return true;
    }

    @Override
    public void onConfigChanged(Config config, String key, Object value) {
        switch (key) {
            case Config.KEY_NOTIFY_LOW_PRIORITY:
                updateLowPriorityPreference(config);
                break;
            case Config.KEY_NOTIFY_WAKE_UP_ON:
                updateLowPriorityPreference(config);
                break;
            case Config.KEY_NOTIFY_PRIVACY:
                updatePrivacyPreference(config);
                break;
        }
    }

    private void updateLowPriorityPreference(Config config) {
        updatePreference(mLowPriorityPreference, config.isLowPriorityNotificationsAllowed());
    }

    private void updateWakeUpOnPreference(Config config) {
        updatePreference(mWakeUpOnPreference, config.isNotifyWakingUp());
    }
    
    private void updatePrivacyPreference(Config config) {
        updatePreference(mPrivacyPreference, config.isPrivacyEnabled());
    }

    private void updatePreference(CheckBoxPreference preference, boolean checked) {
        mBroadcasting = true;
        preference.setChecked(checked);
        mBroadcasting = false;
    }
}
