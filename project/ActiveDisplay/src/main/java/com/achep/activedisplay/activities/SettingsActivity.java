/*
 * Copyright (C) 2013-2014 AChep@xda <artemchep@gmail.com>
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
package com.achep.activedisplay.activities;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.achep.activedisplay.Config;
import com.achep.activedisplay.Keys;
import com.achep.activedisplay.R;
import com.achep.activedisplay.preferences.TimeoutPreference;

/**
 * Created by Artem on 21.01.14.
 */
@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener,
        Config.OnConfigChangedListener {

    private static final String TAG = "SettingsActivity";

    private CheckBoxPreference mOnlyWhileCharging;
    private CheckBoxPreference mLowPriorityNotifications;

    private TimeoutPreference mTimeoutPreference;

    private Config mConfig;
    private boolean mBroadcasting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);

        mConfig = Config.getInstance(this);
        mConfig.addOnConfigChangedListener(this);

        mOnlyWhileCharging = (CheckBoxPreference)
                findPreference(Keys.Settings.ONLY_WHILE_CHARGING);
        mOnlyWhileCharging.setOnPreferenceChangeListener(this);
        mLowPriorityNotifications = (CheckBoxPreference)
                findPreference(Keys.Settings.LOW_PRIORITY_NOTIFICATIONS);
        mLowPriorityNotifications.setOnPreferenceChangeListener(this);

        mTimeoutPreference = (TimeoutPreference)
                findPreference(Keys.Settings.CONFIGURE_TIMEOUT);
        updateTimeoutPreferenceSummary();

        mBroadcasting = true;
        mOnlyWhileCharging.setChecked(mConfig.isEnabledOnlyWhileCharging());
        mLowPriorityNotifications.setChecked(mConfig.isLowPriorityNotificationsAllowed());
        mBroadcasting = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mConfig.removeOnConfigChangedListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        //noinspection StatementWithEmptyBody
        if (mBroadcasting) {
            /* fall down */
        } else if (mOnlyWhileCharging == preference) {
            mConfig.setActiveDisplayEnabledOnlyWhenCharging(this, (Boolean) o, null);
        } else if (mLowPriorityNotifications == preference) {
            mConfig.setLowPriorityNotificationsAllowed(this, (Boolean) o, null);
        } else
            return false;
        return true;
    }

    @Override
    public void onConfigChanged(Config config, String key, Object value) {
        mBroadcasting = true;
        switch (key) {
            case Config.KEY_ONLY_WHILE_CHARGING:
                mOnlyWhileCharging.setChecked((Boolean) value);
                break;
            case Config.KEY_LOW_PRIORITY_NOTIFICATIONS:
                mLowPriorityNotifications.setChecked((Boolean) value);
                break;
            case Config.KEY_TIMEOUT_NORMAL:
            case Config.KEY_TIMEOUT_SHORT:
            case Config.KEY_TIMEOUT_INSTANT:
                updateTimeoutPreferenceSummary();
                break;
        }
        mBroadcasting = false;
    }

    private void updateTimeoutPreferenceSummary() {
        Config config = Config.getInstance(this);
        mTimeoutPreference.setSummary(getString(R.string.settings_timeout_summary,
                Float.toString(config.getTimeoutNormal() / 1000f),
                Float.toString(config.getTimeoutShort() / 1000f),
                Float.toString(config.getTimeoutInstant() / 1000f)));
    }
}
