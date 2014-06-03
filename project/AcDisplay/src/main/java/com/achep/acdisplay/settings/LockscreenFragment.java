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

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Switch;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.acdisplay.settings.enablers.LockscreenEnabler;

/**
 * Created by Artem on 09.02.14.
 */
public class LockscreenFragment extends PreferenceFragment implements
        Config.OnConfigChangedListener, Preference.OnPreferenceChangeListener {

    private LockscreenEnabler mLockscreenEnabler;
    private CheckBoxPreference mKeyguardWithoutNotifiesPreference;

    private boolean mBroadcasting;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.lockscreen_settings);

        Activity activity = getActivity();
        ActionBar actionBar = activity.getActionBar();

        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.layout_ab_switch);
        Switch switch_ = (Switch) actionBar.getCustomView().findViewById(R.id.switch_);
        mLockscreenEnabler = new LockscreenEnabler(activity, switch_);

        mKeyguardWithoutNotifiesPreference = (CheckBoxPreference) findPreference(
                Config.KEY_KEYGUARD_WITHOUT_NOTIFICATIONS);
        mKeyguardWithoutNotifiesPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mLockscreenEnabler.resume();
        Config config = Config.getInstance();
        config.addOnConfigChangedListener(this);

        updateKeyguardWithoutNotifiesPreference(config);
    }

    @Override
    public void onPause() {
        super.onPause();
        mLockscreenEnabler.pause();
        Config config = Config.getInstance();
        config.removeOnConfigChangedListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mBroadcasting) {
            return true;
        }

        Config config = Config.getInstance();
        if (preference == mKeyguardWithoutNotifiesPreference) {
            config.setKeyguardWithoutNotificationsEnabled(getActivity(), (Boolean) newValue, this);
        } else
            return false;
        return true;
    }

    @Override
    public void onConfigChanged(Config config, String key, Object value) {
        switch (key) {
            case Config.KEY_KEYGUARD_WITHOUT_NOTIFICATIONS:
                updateKeyguardWithoutNotifiesPreference(config);
                break;
        }
    }

    private void updateKeyguardWithoutNotifiesPreference(Config config) {
        updatePreference(mKeyguardWithoutNotifiesPreference, config.isKeyguardWithoutNotifiesEnabled());
    }

    private void updatePreference(CheckBoxPreference preference, boolean checked) {
        mBroadcasting = true;
        preference.setChecked(checked);
        mBroadcasting = false;
    }
}
