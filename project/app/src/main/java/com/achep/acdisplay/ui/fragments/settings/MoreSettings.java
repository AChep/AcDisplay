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
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.support.annotation.NonNull;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.base.content.ConfigBase;
import com.achep.base.utils.DateUtils;
import com.achep.base.utils.ResUtils;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import static com.achep.base.Build.DEBUG;

/**
 * Created by Artem on 09.02.14.
 */
public class MoreSettings extends BaseSettings implements
        ConfigBase.OnConfigChangedListener,
        Preference.OnPreferenceClickListener {

    private final ListPreferenceSetter mListPreferenceCornerActionSetter =
            new ListPreferenceSetter() {

                @Override
                public void updateSummary(@NonNull Preference preference,
                                          @NonNull Config.Option option,
                                          @NonNull Object value) {
                    int pos = (int) value;
                    ListPreference cbp = (ListPreference) preference;
                    cbp.setSummary(cbp.getEntries()[pos]);
                }

            };

    private Preference mInactiveHoursPreference;
    private Preference mTimeoutPreference;
    private Preference mDataRestoreDefaultsPreference;
    private Preference mDataBackupPreference;
    private Preference mDataRestorePreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_more_fragment);
        syncPreference(Config.KEY_PRIVACY,
                new MultiSelectListPreferenceSetter(getActivity(), 0,
                        R.string.settings_privacy_mode_disabled));
        syncPreference(Config.KEY_ONLY_WHILE_CHARGING);
        syncPreference(Config.KEY_FEEL_SCREEN_OFF_AFTER_LAST_NOTIFY);
        syncPreference(Config.KEY_DOUBLE_TAP_TO_SLEEP);
        syncPreference(Config.KEY_MEDIA_WIDGET);
        syncPreference(Config.KEY_FEEL_WIDGET_PINNABLE);
        syncPreference(Config.KEY_FEEL_WIDGET_READABLE);
        syncPreference(Config.KEY_CORNER_ACTION_LEFT_TOP, mListPreferenceCornerActionSetter);
        syncPreference(Config.KEY_CORNER_ACTION_LEFT_BOTTOM, mListPreferenceCornerActionSetter);
        syncPreference(Config.KEY_CORNER_ACTION_RIGHT_TOP, mListPreferenceCornerActionSetter);
        syncPreference(Config.KEY_CORNER_ACTION_RIGHT_BOTTOM, mListPreferenceCornerActionSetter);

        mInactiveHoursPreference = findPreference("inactive_hours");
        mTimeoutPreference = findPreference("timeout");
        // Back-up & restore
        mDataRestoreDefaultsPreference = findPreference("data_restore_defaults");
        mDataBackupPreference = findPreference("data_backup");
        mDataRestorePreference = findPreference("data_restore");

        mDataRestoreDefaultsPreference.setOnPreferenceClickListener(this);
        mDataBackupPreference.setOnPreferenceClickListener(this);
        mDataRestorePreference.setOnPreferenceClickListener(this);

        if (!DEBUG) {
            PreferenceGroup pg = (PreferenceGroup) findPreference("data_category");
            pg.removePreference(mDataBackupPreference);
            pg.removePreference(mDataRestorePreference);
        }
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
    public void onConfigChanged(@NonNull ConfigBase configBase,
                                @NonNull String key,
                                @NonNull Object value) {
        Config config = (Config) configBase;
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
            mInactiveHoursPreference.setSummary(ResUtils.getString(getResources(),
                    R.string.settings_inactive_hours_enabled,
                    DateUtils.formatTime(getActivity(), from / 60, from % 60),
                    DateUtils.formatTime(getActivity(), to / 60, to % 60)));
        } else {
            mInactiveHoursPreference.setSummary(getString(R.string.settings_inactive_hours_disabled));
        }
    }

    private void updateTimeoutSummary(Config config) {
        mTimeoutPreference.setSummary(ResUtils.getString(getResources(),
                R.string.settings_timeout_summary,
                Float.toString(config.getTimeoutNormal() / 1000f),
                Float.toString(config.getTimeoutShort() / 1000f)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mDataRestoreDefaultsPreference) {
            new MaterialDialog.Builder(getActivity())
                    .title(R.string.settings_restore_defaults_warning_title)
                    .content(R.string.settings_restore_defaults_warning_message)
                    .positiveText(android.R.string.ok)
                    .negativeText(android.R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog materialDialog,
                                            @NonNull DialogAction dialogAction) {
                            Config.getInstance().reset(getActivity());
                        }
                    })
                    .build()
                    .show();
        } else if (preference == mDataBackupPreference) {
            // TODO: Back up all the things.
        } else if (preference == mDataRestorePreference) {
            // TODO: Restore all the things.
        } else
            return false;
        return true;
    }

}
