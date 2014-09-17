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
import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.widget.Switch;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;

/**
 * Created by Artem on 09.02.14.
 */
public class HeadsUpSettings extends PreferenceFragment implements
        Preference.OnPreferenceChangeListener,
        Config.OnConfigChangedListener {

    private Enabler mHeadsUpEnabler;

    private ListPreference mStylePreference;

    private boolean mBroadcasting;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.headsup_settings);

        Activity activity = getActivity();
        ActionBar actionBar = activity.getActionBar();
        assert actionBar != null;

        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.layout_ab_switch);
        Switch switch_ = (Switch) actionBar.getCustomView().findViewById(R.id.switch_);
        mHeadsUpEnabler = new Enabler(activity, switch_, Config.KEY_HEADS_UP);

        mStylePreference = (ListPreference) findPreference(
                Config.KEY_HEADS_UP_STYLE);
        mStylePreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mHeadsUpEnabler.resume();
        Config config = getConfig();
        config.registerListener(this);

        updateStylePreferenceSummary(config);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHeadsUpEnabler.pause();
        Config config = getConfig();
        config.unregisterListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mBroadcasting) {
            return true;
        }

        Context context = getActivity();
        Config config = Config.getInstance();
        if (preference == mStylePreference) {
            String style = (String) newValue;

            config.setHeadsUpStyle(context, style, this);
            updateStylePreferenceSummary(config);
        } else
            return false;
        return true;
    }

    @Override
    public void onConfigChanged(Config config, String key, Object value) {
        switch (key) {
            case Config.KEY_HEADS_UP_STYLE:
                updateStylePreference(config);
                break;
        }
    }

    private void updateStylePreference(Config config) {
        mBroadcasting = true;

        mStylePreference.setValue(config.getHeadsUpStyle());

        mBroadcasting = false;
        updateStylePreferenceSummary(config);
    }

    private void updateStylePreferenceSummary(Config config) {
        int index = -1, i = 0;

        String style = config.getHeadsUpStyle();
        for (CharSequence cs : mStylePreference.getEntryValues()) {
            if (cs.equals(style)) {
                index = i;
            } else {
                i++;
            }
        }

        mStylePreference.setSummary(getString(R.string.settings_heads_up_style_summary, index != -1
                ? mStylePreference.getEntries()[index]
                : getString(R.string.settings_unknown_value)));
    }
}