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

import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.achep.activedisplay.Config;
import com.achep.activedisplay.Operator;
import com.achep.activedisplay.R;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Interface settings fragment.
 *
 * @author Artem Chepurnoy
 */
public class InterfaceFragment extends PreferenceFragment implements
        Preference.OnPreferenceChangeListener,
        Config.OnConfigChangedListener {

    private CheckBoxPreference mShowWallpaper;
    private CheckBoxPreference mShadowToggle;
    private CheckBoxPreference mMirroredTimeoutToggle;
    private CheckBoxPreference mCircledIconToggle;
    private MultiSelectListPreference mDynamicBackground;

    private boolean mBroadcasting;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.interface_settings);

        mShowWallpaper = (CheckBoxPreference) findPreference(
                Config.KEY_UI_WALLPAPER_SHOWN);
        mShadowToggle = (CheckBoxPreference) findPreference(
                Config.KEY_UI_SHADOW_TOGGLE);
        mDynamicBackground = (MultiSelectListPreference) findPreference(
                Config.KEY_UI_DYNAMIC_BACKGROUND_MODE);
        mMirroredTimeoutToggle = (CheckBoxPreference) findPreference(
                Config.KEY_UI_MIRRORED_TIMEOUT_BAR);
        mCircledIconToggle = (CheckBoxPreference) findPreference(
                Config.KEY_UI_NOTIFY_CIRCLED_ICON);

        mShowWallpaper.setOnPreferenceChangeListener(this);
        mShadowToggle.setOnPreferenceChangeListener(this);
        mDynamicBackground.setOnPreferenceChangeListener(this);
        mMirroredTimeoutToggle.setOnPreferenceChangeListener(this);
        mCircledIconToggle.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Config config = Config.getInstance();
        config.addOnConfigChangedListener(this);

        updateShowWallpaperPreference(config);
        updateShowShadowPreference(config);
        updateMirroredTimeoutPreference(config);
        updateCircledIconPreference(config);
        updateDynamicBackgroundPreference(config);
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

        Context context = getActivity();
        Config config = Config.getInstance();
        if (preference == mShowWallpaper) {
            config.setWallpaperShown(context, (Boolean) newValue, this);
        } else if (preference == mShadowToggle) {
            config.setShadowEnabled(context, (Boolean) newValue, this);
        } else if (preference == mMirroredTimeoutToggle) {
            config.setMirroredTimeoutProgressBarEnabled(context, (Boolean) newValue, this);
        } else if (preference == mCircledIconToggle) {
            config.setCircledLargeIconEnabled(context, (Boolean) newValue, this);
        } else if (preference == mDynamicBackground) {
            int mode = 0;

            Set<String> values = (Set<String>) newValue;
            for (String v : values) {
                mode |= Integer.parseInt(v);
            }

            config.setDynamicBackgroundMode(context, mode, this);
            updateDynamicBackgroundPreferenceSummary(config);
        } else
            return false;
        return true;
    }

    @Override
    public void onConfigChanged(Config config, String key, Object value) {
        switch (key) {
            case Config.KEY_UI_WALLPAPER_SHOWN:
                updateShowWallpaperPreference(config);
                break;
            case Config.KEY_UI_SHADOW_TOGGLE:
                updateShowShadowPreference(config);
                break;
            case Config.KEY_UI_MIRRORED_TIMEOUT_BAR:
                updateMirroredTimeoutPreference(config);
                break;
            case Config.KEY_UI_NOTIFY_CIRCLED_ICON:
                updateCircledIconPreference(config);
                break;
            case Config.KEY_UI_DYNAMIC_BACKGROUND_MODE:
                updateDynamicBackgroundPreference(config);
                break;
        }
    }

    private void updateShowWallpaperPreference(Config config) {
        updatePreference(mShowWallpaper, config.isWallpaperShown());
    }

    private void updateShowShadowPreference(Config config) {
        updatePreference(mShadowToggle, config.isShadowEnabled());
    }

    private void updateMirroredTimeoutPreference(Config config) {
        updatePreference(mMirroredTimeoutToggle, config.isMirroredTimeoutProgressBarEnabled());
    }

    private void updateCircledIconPreference(Config config) {
        updatePreference(mCircledIconToggle, config.isCircledLargeIconEnabled());
    }

    private void updatePreference(CheckBoxPreference preference, boolean checked) {
        mBroadcasting = true;
        preference.setChecked(checked);
        mBroadcasting = false;
    }

    private void updateDynamicBackgroundPreference(Config config) {
        mBroadcasting = true;

        int mode = config.getDynamicBackgroundMode();
        String[] values = new String[Integer.bitCount(mode)];
        for (int i = 1, j = 0; j < values.length; i <<= 1) {
            if (Operator.bitAnd(mode, i)) {
                values[j++] = Integer.toString(i);
            }
        }

        Set<String> valuesSet = new HashSet<>();
        Collections.addAll(valuesSet, values);
        mDynamicBackground.setValues(valuesSet);

        mBroadcasting = false;
        updateDynamicBackgroundPreferenceSummary(config);
    }

    private void updateDynamicBackgroundPreferenceSummary(Config config) {
        CharSequence summary;
        if (config.getDynamicBackgroundMode() != 0) {
            CharSequence[] entries = mDynamicBackground.getEntries();
            CharSequence[] values = mDynamicBackground.getEntryValues();
            int mode = config.getDynamicBackgroundMode();

            String divider = getString(R.string.settings_multi_list_divider);
            StringBuilder sb = new StringBuilder();
            boolean empty = true;

            assert entries != null;
            assert values != null;

            // Append selected items.
            for (int i = 0; i < values.length; i++) {
                int a = Integer.parseInt(values[i].toString());
                if (Operator.bitAnd(mode, a)) {
                    if (!empty) {
                        sb.append(divider);
                    }
                    sb.append(entries[i]);
                    empty = false;
                }
            }

            String itemsText = sb.toString().toLowerCase(Locale.getDefault());
            summary = getString(R.string.settings_dynamic_background_summary, itemsText);
        } else {
            summary = getString(R.string.settings_dynamic_background_disabled);
        }
        mDynamicBackground.setSummary(summary);
    }
}
