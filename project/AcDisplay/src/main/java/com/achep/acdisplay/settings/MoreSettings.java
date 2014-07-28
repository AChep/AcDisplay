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
import android.preference.Preference;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.acdisplay.utils.DateUtils;
import com.achep.acdisplay.utils.MathUtils;

/**
 * Created by Artem on 09.02.14.
 */
public class MoreSettings extends PreferenceFragment implements
        Config.OnConfigChangedListener {

    private Preference mInactiveHoursPreference;
    private Preference mTimeoutPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.more_settings);
        syncPreference(Config.KEY_ONLY_WHILE_CHARGING);
        syncPreference(Config.KEY_FEEL_SCREEN_OFF_AFTER_LAST_NOTIFY);
        syncPreference(Config.KEY_FEEL_WIDGET_PINNABLE);
        syncPreference(Config.KEY_FEEL_WIDGET_READABLE);

        mInactiveHoursPreference = findPreference("inactive_hours");
        mTimeoutPreference = findPreference("timeout");
    }

    @Override
    public void onResume() {
        super.onResume();
        Config config = getConfig();
        config.registerListener(this);

        updateInactiveHoursSummary(config);
        updateTimeoutSummary(config);
    }

    @Override
    public void onPause() {
        super.onPause();
        Config config = getConfig();
        config.unregisterListener(this);
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
                updateTimeoutSummary(config);
                break;
        }
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
        mTimeoutPreference.setSummary(getString(R.string.settings_timeout_summary,
                Float.toString(config.getTimeoutNormal() / 1000f),
                Float.toString(config.getTimeoutShort() / 1000f)));
    }
}
