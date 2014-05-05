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
package com.achep.activedisplay.settings;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.achep.activedisplay.Config;
import com.achep.activedisplay.R;
import com.achep.activedisplay.utils.DateUtils;
import com.achep.activedisplay.utils.MathUtils;

/**
 * Created by Artem on 09.02.14.
 */
public class MoreFragment extends PreferenceFragment implements
        Config.OnConfigChangedListener,
        Preference.OnPreferenceChangeListener {

    private CheckBoxPreference mOnlyWhileChargingPreference;
    private Preference mInactiveHoursPreference;
    private Preference mTimeoutPreference;

    private boolean mBroadcasting;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.more_settings);

        mInactiveHoursPreference = findPreference("inactive_hours");
        mTimeoutPreference = findPreference("timeout");

        mOnlyWhileChargingPreference = (CheckBoxPreference) findPreference(
                Config.KEY_ONLY_WHILE_CHARGING);

        mOnlyWhileChargingPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Config config = Config.getInstance();
        config.addOnConfigChangedListener(this);

        updateOnlyWhileChargingPreference(config);

        updateInactiveHoursSummary(config);
        updateTimeoutSummary(config);
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
        if (preference == mOnlyWhileChargingPreference) {
            config.setActiveDisplayEnabledOnlyWhileCharging(getActivity(), (Boolean) newValue, this);
        } else
            return false;
        return true;
    }

    @Override
    public void onConfigChanged(Config config, String key, Object value) {
        switch (key) {
            case Config.KEY_INACTIVE_TIME_ENABLED:
            case Config.KEY_INACTIVE_TIME_FROM:
            case Config.KEY_INACTIVE_TIME_TO:
                updateInactiveHoursSummary(config);
                break;
            case Config.KEY_TIMEOUT_NORMAL:
            case Config.KEY_TIMEOUT_SHORT:
            case Config.KEY_TIMEOUT_ENABLED:
                updateTimeoutSummary(config);
                break;
            case Config.KEY_ONLY_WHILE_CHARGING:
                updateOnlyWhileChargingPreference(config);
                break;
        }
    }

    private void updateOnlyWhileChargingPreference(Config config) {
        updatePreference(mOnlyWhileChargingPreference, config.isEnabledOnlyWhileCharging());
    }

    private void updatePreference(CheckBoxPreference preference, boolean checked) {
        mBroadcasting = true;
        preference.setChecked(checked);
        mBroadcasting = false;
    }

    private void updatePreference(ListPreference preference, int item) {
        mBroadcasting = true;
        preference.setValue(Integer.toString(item));
        mBroadcasting = false;
    }

    private void updatePreferenceListSummary(ListPreference preference) {
        preference.setSummary(preference.getEntry());
    }

    private void updateInactiveHoursSummary(Config config) {
        if (config.isInactiveTimeEnabled()) {
            int from = config.getInactiveTimeFrom();
            int to = config.getInactiveTimeTo();
            mInactiveHoursPreference.setSummary(getString(R.string.settings_inactive_hours_enabled,
                    DateUtils.formatTime(getActivity(), MathUtils.div(from, 60), from % 60),
                    DateUtils.formatTime(getActivity(), MathUtils.div(to, 60), to % 60)));
        } else {
            mInactiveHoursPreference.setSummary(getString(R.string.settings_inactive_hours_disabled));
        }
    }

    private void updateTimeoutSummary(Config config) {
        mTimeoutPreference.setSummary(config.isTimeoutEnabled()
                ? getString(R.string.settings_timeout_summary,
                Float.toString(config.getTimeoutNormal() / 1000f),
                Float.toString(config.getTimeoutShort() / 1000f))
                : getString(R.string.settings_timeout_forever));
    }
}
