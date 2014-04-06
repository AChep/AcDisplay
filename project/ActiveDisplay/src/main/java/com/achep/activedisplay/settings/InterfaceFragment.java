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
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.achep.activedisplay.Config;
import com.achep.activedisplay.Keys;
import com.achep.activedisplay.Operator;
import com.achep.activedisplay.R;
import com.achep.activedisplay.utils.ToastUtils;

import java.util.Locale;
import java.util.Set;

/**
 * Created by Artem on 09.02.14.
 */
public class InterfaceFragment extends PreferenceFragment implements
        Preference.OnPreferenceChangeListener,
        Config.OnConfigChangedListener {

    private CheckBoxPreference mShowWallpaper;
    private CheckBoxPreference mShadowToggle;
    private CheckBoxPreference mMirroredTimeoutToggle;
    private MultiSelectListPreference mDynamicBackground;

    private boolean mBroadcasting;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.interface_settings);

        mShowWallpaper = (CheckBoxPreference) findPreference(
                Keys.Settings.INTERFACE_IS_WALLPAPER_SHOWN);
        mShowWallpaper.setOnPreferenceChangeListener(this);
        mShadowToggle = (CheckBoxPreference) findPreference("shadow_toggle");
        mShadowToggle.setOnPreferenceChangeListener(this);
        mDynamicBackground = (MultiSelectListPreference) findPreference("dynamic_background_mode");
        mDynamicBackground.setOnPreferenceChangeListener(this);
        mMirroredTimeoutToggle = (CheckBoxPreference) findPreference("mirrored_timeout_progress_bar");
        mMirroredTimeoutToggle.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Config config = Config.getInstance(getActivity());
        config.addOnConfigChangedListener(this);

        updateShowWallpaperPreference(config);
        updateShowShadowPreference(config);
        updateMirroredTimeoutPreference(config);
        updateDynamicBackgroundSummary(config);
    }

    @Override
    public void onPause() {
        super.onPause();
        Config config = Config.getInstance(getActivity());
        config.removeOnConfigChangedListener(this);
    }

    private void updateShowWallpaperPreference(Config config) {
        updateCheckBox(mShowWallpaper, config.isWallpaperShown());
    }

    private void updateShowShadowPreference(Config config) {
        updateCheckBox(mShadowToggle, config.isWallpaperShown());
    }

    private void updateMirroredTimeoutPreference(Config config) {
        updateCheckBox(mMirroredTimeoutToggle, config.isMirroredTimeoutProgressBarEnabled());
    }

    private void updateCheckBox(CheckBoxPreference preference, boolean checked) {
        mBroadcasting = true;
        preference.setChecked(checked);
        mBroadcasting = false;
    }

    private void updateDynamicBackgroundSummary(Config config) {
        if (config.getDynamicBackgroundMode() != 0) {
            CharSequence[] entries = mDynamicBackground.getEntries();
            CharSequence[] values = mDynamicBackground.getEntryValues();
            int mode = config.getDynamicBackgroundMode();

            StringBuilder sb = new StringBuilder();
            boolean empty = true;

            for (int i = 0; i < values.length; i++) {
                int a = Integer.parseInt(values[i].toString());
                if (Operator.bitandCompare(mode, a)) {
                    if (!empty) {
                        sb.append(", ");
                    }
                    sb.append(entries[i]);
                    empty = false;
                }
            }

            String text = sb.toString().toLowerCase(Locale.getDefault());

            mDynamicBackground.setSummary(getString(R.string.settings_dynamic_background_summary, text));
        } else {
            mDynamicBackground.setSummary(getString(R.string.settings_dynamic_background_disabled));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mBroadcasting) {
            return true;
        }

        Config config = Config.getInstance(getActivity());
        if (preference == mShowWallpaper) {
            config.setWallpaperShown(getActivity(), (Boolean) newValue, this);
        } else if (preference == mShadowToggle) {
            config.setShadowEnabled(getActivity(), (Boolean) newValue, this);
        } else if (preference == mMirroredTimeoutToggle) {
            config.setMirroredTimeoutProgressBarEnabled(getActivity(), (Boolean) newValue, this);
        } else if (preference == mDynamicBackground) {
            int mode = 0;

            Set<String> values = (Set<String>) newValue;
            for (String v : values) {
                mode |= Integer.parseInt(v);
            }

            config.setDynamicBackgroundMode(getActivity(), mode, null);
        } else
            return false;
        return true;
    }

    @Override
    public void onConfigChanged(Config config, String key, Object value) {
        switch (key) {
            case Config.KEY_INTERFACE_WALLPAPER_SHOWN:
                updateShowWallpaperPreference(config);
                break;
            case Config.KEY_INTERFACE_SHADOW_TOGGLE:
                updateShowWallpaperPreference(config);
                break;
            case Config.KEY_INTERFACE_MIRRORED_TIMEOUT_PROGRESS_BAR:
                updateMirroredTimeoutPreference(config);
                break;
            case Config.KEY_INTERFACE_DYNAMIC_BACKGROUND_MODE:
                updateDynamicBackgroundSummary(config);
                break;
        }
    }
}
