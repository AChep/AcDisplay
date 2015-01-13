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
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.base.content.ConfigBase;
import com.achep.base.ui.fragments.PreferenceFragment;
import com.achep.base.utils.Operator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Interface settings fragment.
 *
 * @author Artem Chepurnoy
 */
public class InterfaceSettings extends PreferenceFragment implements
        ConfigBase.OnConfigChangedListener {

    private final ListPreferenceSetter mListPreferenceDynamicBackgroundSetter =
            new ListPreferenceSetter() {

                @Override
                public void updateSummary(@NonNull Preference preference,
                                          @NonNull Config.Option option,
                                          @NonNull Object value) {
                    MultiSelectListPreference mslp = (MultiSelectListPreference) preference;
                    int mode = (int) value;

                    CharSequence summary;
                    if (mode != 0) {
                        CharSequence[] entries = mslp.getEntries();
                        CharSequence[] values = mslp.getEntryValues();

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

                        String itemsText = sb.toString().toLowerCase();
                        summary = getString(R.string.settings_dynamic_background_summary, itemsText);
                    } else {
                        summary = getString(R.string.settings_dynamic_background_disabled);
                    }

                    mslp.setSummary(summary);
                }

                @Override
                public void setValue(@NonNull Preference preference,
                                     @NonNull Config.Option option,
                                     @NonNull Object value) {
                    int mode = (int) value;
                    String[] values = new String[Integer.bitCount(mode)];
                    for (int i = 1, j = 0; j < values.length; i <<= 1) {
                        if (Operator.bitAnd(mode, i)) {
                            values[j++] = Integer.toString(i);
                        }
                    }

                    Set<String> valuesSet = new HashSet<>();
                    Collections.addAll(valuesSet, values);

                    MultiSelectListPreference mslp = (MultiSelectListPreference) preference;
                    mslp.setValues(valuesSet);
                }

                @NonNull
                @Override
                public Object getValue(@NonNull Object value) {
                    int mode = 0;

                    Set<String> values = (Set<String>) value;
                    for (String v : values) {
                        mode |= Integer.parseInt(v);
                    }

                    return mode;
                }

            };

    private Preference mIconSizePreference;

    @Override
    public Config getConfig() {
        return Config.getInstance();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_interface_fragment);
        syncPreference(Config.KEY_UI_DYNAMIC_BACKGROUND_MODE, mListPreferenceDynamicBackgroundSetter);
        syncPreference(Config.KEY_UI_WALLPAPER_SHOWN);
        syncPreference(Config.KEY_UI_STATUS_BATTERY_STICKY);
        syncPreference(Config.KEY_UI_FULLSCREEN);
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
        mIconSizePreference.setSummary(getString(R.string.settings_icon_size_summary,
                Integer.toString(config.getIconSize(Config.ICON_SIZE_DP))));
    }
}
