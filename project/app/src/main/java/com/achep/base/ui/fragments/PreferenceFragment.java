/*
 * Copyright (C) 2015 AChep@xda <artemchep@gmail.com>
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
package com.achep.base.ui.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;

import com.achep.acdisplay.R;
import com.achep.base.content.ConfigBase;
import com.achep.base.ui.preferences.Enabler;
import com.achep.base.ui.widgets.SwitchBar;

import static com.achep.base.Build.DEBUG;

public abstract class PreferenceFragment extends PreferenceFragmentBase {

    private static final String TAG = "PreferenceFragment";

    private SwitchBar mSwitch;
    private String mEnablerKey;
    private Enabler mEnabler;
    private ConfigBase mConfig;
    private ConfigBase.Syncer mSyncer;

    private CheckBoxPreferenceSetter mCheckBoxPreferenceSetter;

    public abstract ConfigBase getConfig();

    /**
     * Requests the fragment to setup master switch from
     * the corresponding key.
     *
     * @param key the key of one of the {@link com.achep.base.content.ConfigBase}'s options
     * @see com.achep.base.content.ConfigBase
     * @see com.achep.base.ui.preferences.Enabler
     */
    public void requestMasterSwitch(@NonNull String key) {
        mEnablerKey = key;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mConfig = getConfig();
        mSyncer = new ConfigBase.Syncer(activity, mConfig);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mEnablerKey != null) {
            if (DEBUG) Log.d(TAG, "Creating the enabler for #" + mEnablerKey + " key.");

            // Setup enabler to switch bar.
            SwitchBar switchBar = getSwitchBar();
            assert switchBar != null;
            mEnabler = new Enabler(getActivity(), mConfig, mEnablerKey, switchBar);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mEnabler != null) mEnabler.start();
        mSyncer.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mEnabler != null) mEnabler.stop();
        mSyncer.stop();
    }

    /**
     * Synchronizes simple checkbox preference with the config.
     *
     * @param key the key of preference & config's parameter.
     * @see com.achep.acdisplay.Config#getHashMap()
     */
    protected void syncPreference(@NonNull String key) {
        if (mCheckBoxPreferenceSetter == null)
            mCheckBoxPreferenceSetter = new CheckBoxPreferenceSetter();
        syncPreference(key, mCheckBoxPreferenceSetter);
    }

    /**
     * Synchronizes any preference with the config.
     *
     * @param key    the key of preference & config's parameter.
     * @param setter preference's setter
     * @see com.achep.acdisplay.Config#getHashMap()
     * @see ListPreferenceSetter
     * @see CheckBoxPreferenceSetter
     */
    protected void syncPreference(@NonNull String key, @NonNull ConfigBase.Syncer.Setter setter) {
        Preference preference = findPreference(key);
        PreferenceScreen preferenceScreen = getPreferenceScreen();

        if (preference == null) {
            if (DEBUG) Log.d(TAG, "Tried to sync non-existent preference with config.");
            return;
        }

        mSyncer.addPreference(preferenceScreen, preference, setter);
    }

    @Nullable
    public SwitchBar getSwitchBar() {
        if (mSwitch == null) {
            if (getView() == null) {
                return null;
            }

            ViewStub stub = (ViewStub) getView().findViewById(R.id.switch_bar_stub);
            mSwitch = (SwitchBar) stub.inflate().findViewById(R.id.switch_bar);
        }
        return mSwitch;
    }

    protected static class CheckBoxPreferenceSetter implements ConfigBase.Syncer.Setter {

        /**
         * {@inheritDoc}
         */
        @Override
        public final void updateSummary(@NonNull Preference preference,
                                        @NonNull ConfigBase.Option option,
                                        @NonNull Object value) {
            // This is non needed, because you should always use
            //     android:summaryOn=""
            //     android:summaryOff=""
            // attributes.
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setValue(@NonNull Preference preference,
                             @NonNull ConfigBase.Option option,
                             @NonNull Object value) {
            CheckBoxPreference cbp = (CheckBoxPreference) preference;
            cbp.setChecked((Boolean) value);
        }

        /**
         * {@inheritDoc}
         */
        @NonNull
        @Override
        public Object getValue(@NonNull Object value) {
            return value;
        }

    }

    protected static class ListPreferenceSetter implements ConfigBase.Syncer.Setter {

        /**
         * {@inheritDoc}
         */
        @Override
        public void updateSummary(@NonNull Preference preference,
                                  @NonNull ConfigBase.Option option,
                                  @NonNull Object value) { /* unused */ }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setValue(@NonNull Preference preference,
                             @NonNull ConfigBase.Option option,
                             @NonNull Object value) {
            ListPreference cbp = (ListPreference) preference;
            cbp.setValue(Integer.toString((Integer) value));
        }

        /**
         * {@inheritDoc}
         */
        @NonNull
        @Override
        public Object getValue(@NonNull Object value) {
            return Integer.parseInt((String) value);
        }

    }

}

