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

import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;

import com.achep.acdisplay.R;
import com.achep.base.content.ConfigBase;
import com.achep.base.interfaces.ICheckable;
import com.achep.base.permissions.Permission;
import com.achep.base.ui.SwitchBarPermissible;
import com.achep.base.ui.activities.ActivityBase;
import com.achep.base.ui.preferences.Enabler;
import com.achep.base.ui.widgets.SwitchBar;
import com.achep.base.utils.Operator;
import com.achep.base.utils.ResUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.achep.base.Build.DEBUG;

public abstract class PreferenceFragment extends PreferenceFragmentBase {

    private static final String TAG = "PreferenceFragment";

    private SwitchBar mSwitch;
    private SwitchBarPermissible mSwitchPermissible;
    private Permission[] mEnablerPermissions;
    private String mEnablerKey;
    private Enabler mEnabler;
    private ConfigBase mConfig;
    private ConfigBase.Syncer mSyncer;

    private TwoStatePreferenceSetter mTwoStatePreferenceSetter;

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
        requestMasterSwitch(key, null);
    }

    public void requestMasterSwitch(@NonNull String key, @Nullable Permission[] permissions) {
        mEnablerKey = key;
        mEnablerPermissions = permissions;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mConfig = getConfig();
        mSyncer = new ConfigBase.Syncer(context, mConfig);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mEnablerKey != null) {
            if (DEBUG) Log.d(TAG, "Creating the enabler for #" + mEnablerKey + " key.");

            // Setup enabler to switch bar.
            mEnabler = new Enabler(getActivity(), mConfig, mEnablerKey, createPermissionSwitch());
        }
    }

    @NonNull
    private ICheckable createPermissionSwitch() {
        ActivityBase activity = (ActivityBase) getActivity();
        SwitchBar switchBar = getSwitchBar();
        assert switchBar != null;
        mSwitchPermissible = new SwitchBarPermissible(activity, switchBar, mEnablerPermissions);
        return mSwitchPermissible;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mEnabler != null) {
            mSwitchPermissible.resume();
            mEnabler.start();
        }
        mSyncer.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mEnabler != null) {
            mEnabler.stop();
            mSwitchPermissible.pause();
        }
        mSyncer.stop();
    }

    /**
     * Synchronizes simple checkbox preference with the config.
     *
     * @param key the key of preference & config's parameter.
     * @see com.achep.acdisplay.Config#getMap()
     */
    protected void syncPreference(@NonNull String key) {
        if (mTwoStatePreferenceSetter == null)
            mTwoStatePreferenceSetter = new TwoStatePreferenceSetter();
        syncPreference(key, mTwoStatePreferenceSetter);
    }

    /**
     * Synchronizes any preference with the config.
     *
     * @param key    the key of preference & config's parameter.
     * @param setter preference's setter
     * @see com.achep.acdisplay.Config#getMap()
     * @see ListPreferenceSetter
     * @see TwoStatePreferenceSetter
     */
    protected void syncPreference(@NonNull String key, @NonNull ConfigBase.Syncer.Setter setter) {
        Preference preference = findPreference(key);
        PreferenceScreen preferenceScreen = getPreferenceScreen();

        if (preference == null) {
            if (DEBUG) Log.d(TAG, "Tried to sync non-existent preference with config.");
            return;
        }

        mSyncer.syncPreference(preferenceScreen, preference, setter);
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

    /**
     * The setter for a {@link TwoStatePreference}.
     *
     * @author Artem Chepurnoy
     */
    protected static class TwoStatePreferenceSetter implements ConfigBase.Syncer.Setter {

        /**
         * {@inheritDoc}
         */
        @Override
        public final void updateSummary(@NonNull Preference preference,
                                        @NonNull ConfigBase.Option option,
                                        @NonNull Object value) {
            // This is unneeded, because you should always use
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
            TwoStatePreference cbp = (TwoStatePreference) preference;
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

    /**
     * The setter for a {@link ListPreference}.
     *
     * @author Artem Chepurnoy
     */
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

    /**
     * The setter for a {@link MultiSelectListPreference}. To work currently,
     * its data must be in a bit-mask (bit per selectable item) format.
     *
     * @author Artem Chepurnoy
     */
    protected static class MultiSelectListPreferenceSetter implements ConfigBase.Syncer.Setter {

        @NonNull
        private final Context mContext;
        private final int mStrResSummary;
        private final int mStrResDisabled;

        public MultiSelectListPreferenceSetter(@NonNull Context context,
                                               @StringRes int summaryStrRes,
                                               @StringRes int disabledStrRes) {
            mContext = context;
            mStrResSummary = summaryStrRes;
            mStrResDisabled = disabledStrRes;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void updateSummary(@NonNull Preference preference,
                                  @NonNull ConfigBase.Option option,
                                  @NonNull Object value) {
            MultiSelectListPreference mslp = (MultiSelectListPreference) preference;
            int mode = (int) value;

            CharSequence summary;
            if (mode != 0) {
                CharSequence[] entries = mslp.getEntries();
                CharSequence[] values = mslp.getEntryValues();

                String divider = mContext.getString(R.string.settings_multi_list_divider);
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
                summary = mStrResSummary != 0
                        ? ResUtils.getString(mContext, mStrResSummary, itemsText)
                        : sb.charAt(0) + itemsText.substring(1, itemsText.length());
            } else {
                summary = mContext.getString(mStrResDisabled);
            }

            mslp.setSummary(summary);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setValue(@NonNull Preference preference,
                             @NonNull ConfigBase.Option option,
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

        /**
         * {@inheritDoc}
         */
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

    }

}

