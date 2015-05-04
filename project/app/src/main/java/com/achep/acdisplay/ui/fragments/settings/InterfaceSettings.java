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
import android.preference.Preference;
import android.support.annotation.NonNull;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.base.content.ConfigBase;
import com.achep.base.utils.ResUtils;

/**
 * Interface settings fragment.
 *
 * @author Artem Chepurnoy
 */
public class InterfaceSettings extends BaseSettings implements
        ConfigBase.OnConfigChangedListener {

    private Preference mIconSizePreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_interface_fragment);
        syncPreference(Config.KEY_UI_DYNAMIC_BACKGROUND_MODE,
                new MultiSelectListPreferenceSetter(getActivity(),
                        R.string.settings_dynamic_background_summary,
                        R.string.settings_dynamic_background_disabled));
        syncPreference(Config.KEY_UI_WALLPAPER_SHOWN);
        syncPreference(Config.KEY_UI_STATUS_BATTERY_STICKY);
        syncPreference(Config.KEY_UI_FULLSCREEN);
        syncPreference(Config.KEY_UI_OVERRIDE_FONTS);
        syncPreference(Config.KEY_UI_EMOTICONS);
        syncPreference(Config.KEY_UI_UNLOCK_ANIMATION);

        mIconSizePreference = findPreference("icon_size");
    }

    @Override
    public void onResume() {
        super.onResume();
        Config config = getConfig();
        config.registerListener(this);

        updateIconSizeSummary(config);
    }

    @Override
    public void onPause() {
        super.onPause();
        Config config = getConfig();
        config.unregisterListener(this);
    }

    @Override
    public void onConfigChanged(@NonNull ConfigBase configBase,
                                @NonNull String key,
                                @NonNull Object value) {
        Config config = (Config) configBase;
        switch (key) {
            case Config.KEY_UI_ICON_SIZE:
                updateIconSizeSummary(config);
                break;
        }
    }

    private void updateIconSizeSummary(Config config) {
        String summary = ResUtils.getString(getResources(),
                R.string.settings_icon_size_summary,
                Integer.toString(config.getIconSize(Config.ICON_SIZE_DP)));
        mIconSizePreference.setSummary(summary);
    }
}
