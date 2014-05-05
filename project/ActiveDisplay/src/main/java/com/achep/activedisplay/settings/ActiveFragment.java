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

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Switch;

import com.achep.activedisplay.Config;
import com.achep.activedisplay.R;
import com.achep.activedisplay.settings.enablers.ActiveModeEnabler;

/**
 * Created by Artem on 09.02.14.
 */
public class ActiveFragment extends PreferenceFragment implements
        Config.OnConfigChangedListener, Preference.OnPreferenceChangeListener {

    private ActiveModeEnabler mActiveModeEnabler;
    private CheckBoxPreference mActiveModeWithoutNotifiesPreference;

    private boolean mBroadcasting;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.active_settings);

        Activity activity = getActivity();
        ActionBar actionBar = activity.getActionBar();

        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.layout_ab_switch);
        Switch switch_ = (Switch) actionBar.getCustomView().findViewById(R.id.switch_);
        mActiveModeEnabler = new ActiveModeEnabler(activity, switch_);

        mActiveModeWithoutNotifiesPreference = (CheckBoxPreference) findPreference(
                Config.KEY_ACTIVE_MODE_WITHOUT_NOTIFICATIONS);

        mActiveModeWithoutNotifiesPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mActiveModeEnabler.resume();
        Config config = Config.getInstance();
        config.addOnConfigChangedListener(this);

        updateActiveModeWithoutNotifiesPreference(config);
    }

    @Override
    public void onPause() {
        super.onPause();
        mActiveModeEnabler.pause();
        Config config = Config.getInstance();
        config.removeOnConfigChangedListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mBroadcasting) {
            return true;
        }

        Config config = Config.getInstance();
        if (preference == mActiveModeWithoutNotifiesPreference) {
            config.setActiveModeWithoutNotificationsEnabled(getActivity(), (Boolean) newValue, this);
        } else
            return false;
        return true;
    }

    @Override
    public void onConfigChanged(Config config, String key, Object value) {
        switch (key) {
            case Config.KEY_ACTIVE_MODE_WITHOUT_NOTIFICATIONS:
                updateActiveModeWithoutNotifiesPreference(config);
                break;
        }
    }

    private void updateActiveModeWithoutNotifiesPreference(Config config) {
        updatePreference(mActiveModeWithoutNotifiesPreference, config.isActiveModeWithoutNotifiesEnabled());
    }

    private void updatePreference(CheckBoxPreference preference, boolean checked) {
        mBroadcasting = true;
        preference.setChecked(checked);
        mBroadcasting = false;
    }
}
