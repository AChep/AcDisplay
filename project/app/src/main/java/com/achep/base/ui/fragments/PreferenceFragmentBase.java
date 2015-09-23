/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.achep.base.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.achep.acdisplay.R;
import com.achep.base.async.WeakHandler;
import com.achep.base.ui.fragments.leakcanary.LeakWatchFragment;
import com.achep.base.utils.PreferenceManagerUtils;

import java.lang.reflect.Method;

/**
 * Shows a hierarchy of {@link android.preference.Preference} objects as
 * lists. These preferences will
 * automatically save to {@link android.content.SharedPreferences} as the user interacts with
 * them. To retrieve an instance of {@link android.content.SharedPreferences} that the
 * preference hierarchy in this fragment will use, call
 * {@link android.preference.PreferenceManager#getDefaultSharedPreferences(android.content.Context)}
 * with a context in the same package as this fragment.
 * <p>
 * Furthermore, the preferences shown will follow the visual style of system
 * preferences. It is easy to create a hierarchy of preferences (that can be
 * shown on multiple screens) via XML. For these reasons, it is recommended to
 * use this fragment (as a superclass) to deal with preferences in applications.
 * <p>
 * A {@link android.preference.PreferenceScreen} object should be at the top of the preference
 * hierarchy. Furthermore, subsequent {@link android.preference.PreferenceScreen} in the hierarchy
 * denote a screen break--that is the preferences contained within subsequent
 * {@link android.preference.PreferenceScreen} should be shown on another screen. The preference
 * framework handles showing these other screens from the preference hierarchy.
 * <p>
 * The preference hierarchy can be formed in multiple ways:
 * <li> From an XML file specifying the hierarchy
 * <li> From different {@link android.app.Activity Activities} that each specify its own
 * preferences in an XML file via {@link android.app.Activity} meta-data
 * <li> From an object hierarchy rooted with {@link android.preference.PreferenceScreen}
 * <p>
 * To inflate from XML, use the {@link #addPreferencesFromResource(int)}. The
 * root element should be a {@link android.preference.PreferenceScreen}. Subsequent elements can point
 * to actual {@link android.preference.Preference} subclasses. As mentioned above, subsequent
 * {@link android.preference.PreferenceScreen} in the hierarchy will result in the screen break.
 * <p>
 * To specify an {@link android.content.Intent} to query {@link android.app.Activity Activities} that each
 * have preferences, use {@link #addPreferencesFromIntent}. Each
 * {@link android.app.Activity} can specify meta-data in the manifest (via the key
 * {@link android.preference.PreferenceManager#METADATA_KEY_PREFERENCES}) that points to an XML
 * resource. These XML resources will be inflated into a single preference
 * hierarchy and shown by this fragment.
 * <p>
 * To specify an object hierarchy rooted with {@link android.preference.PreferenceScreen}, use
 * {@link #setPreferenceScreen(android.preference.PreferenceScreen)}.
 * <p>
 * As a convenience, this fragment implements a click listener for any
 * preference in the current hierarchy, see
 * {@link #onPreferenceTreeClick(android.preference.PreferenceScreen, android.preference.Preference)}.
 * <p/>
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For information about using {@code PreferenceFragment},
 * read the <a href="{@docRoot}guide/topics/ui/settings.html">Settings</a>
 * guide.</p>
 * </div>
 * <p/>
 * <a name="SampleCode"></a>
 * <h3>Sample Code</h3>
 * <p/>
 * <p>The following sample code shows a simple preference fragment that is
 * populated from a resource.  The resource it loads is:</p>
 * <p/>
 * {@sample development/samples/ApiDemos/res/xml/preferences.xml preferences}
 * <p/>
 * <p>The fragment implementation itself simply populates the preferences
 * when created.  Note that the preferences framework takes care of loading
 * the current values out of the app preferences and writing them when changed:</p>
 * <p/>
 * {@sample development/samples/ApiDemos/src/com/example/android/apis/preference/FragmentPreferences.java
 * fragment}
 *
 * @see android.preference.Preference
 * @see android.preference.PreferenceScreen
 */
public abstract class PreferenceFragmentBase extends LeakWatchFragment implements
        PreferenceManagerUtils.OnPreferenceTreeClickListener {

    private static final String PREFERENCES_TAG = "android:preferences";

    private PreferenceManager mPreferenceManager;
    private ListView mList;
    private boolean mHavePrefs;
    private boolean mInitDone;

    /**
     * The starting request code given out to preference framework.
     */
    private static final int FIRST_REQUEST_CODE = 100;

    private static final int MSG_BIND_PREFERENCES = 1;
    private final H mHandler = new H(this);

    private static class H extends WeakHandler<PreferenceFragmentBase> {

        public H(@NonNull PreferenceFragmentBase object) {
            super(object);
        }

        @Override
        protected void onHandleMassage(@NonNull PreferenceFragmentBase pfb, Message msg) {
            switch (msg.what) {
                case MSG_BIND_PREFERENCES:
                    pfb.bindPreferences();
                    break;
            }
        }
    }

    final private Runnable mRequestFocus = new Runnable() {
        public void run() {
            mList.focusableViewAvailable(mList);
        }
    };

    /**
     * Interface that PreferenceFragment's containing activity should
     * implement to be able to process preference items that wish to
     * switch to a new fragment.
     */
    public interface OnPreferenceStartFragmentCallback {

        /**
         * Called when the user has clicked on a Preference that has
         * a fragment class name associated with it.  The implementation
         * to should instantiate and switch to an instance of the given
         * fragment.
         */
        boolean onPreferenceStartFragment(PreferenceFragmentBase caller, Preference pref);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferenceManager = PreferenceManagerUtils.newInstance(getActivity(), FIRST_REQUEST_CODE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.preference_list_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mHavePrefs) {
            bindPreferences();
        }

        mInitDone = true;

        if (savedInstanceState != null) {
            Bundle container = savedInstanceState.getBundle(PREFERENCES_TAG);
            if (container != null) {
                final PreferenceScreen preferenceScreen = getPreferenceScreen();
                if (preferenceScreen != null) {
                    preferenceScreen.restoreHierarchyState(container);
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        PreferenceManagerUtils.setOnPreferenceTreeClickListener(mPreferenceManager, this);
    }

    @Override
    public void onStop() {
        super.onStop();
        PreferenceManagerUtils.dispatchActivityStop(mPreferenceManager);
        PreferenceManagerUtils.setOnPreferenceTreeClickListener(mPreferenceManager, null);
    }

    @Override
    public void onDestroyView() {
        mList = null;
        mHandler.removeCallbacks(mRequestFocus);
        mHandler.removeMessages(MSG_BIND_PREFERENCES);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PreferenceManagerUtils.dispatchActivityDestroy(mPreferenceManager);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            Bundle container = new Bundle();
            preferenceScreen.saveHierarchyState(container);
            outState.putBundle(PREFERENCES_TAG, container);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        PreferenceManagerUtils.dispatchActivityResult(mPreferenceManager, requestCode, resultCode, data);
    }

    /**
     * Returns the {@link android.preference.PreferenceManager} used by this fragment.
     *
     * @return The {@link android.preference.PreferenceManager}.
     */
    public PreferenceManager getPreferenceManager() {
        return mPreferenceManager;
    }

    /**
     * Sets the root of the preference hierarchy that this fragment is showing.
     *
     * @param preferenceScreen The root {@link android.preference.PreferenceScreen} of the preference hierarchy.
     */
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        boolean r = PreferenceManagerUtils.setPreferences(mPreferenceManager, preferenceScreen);
        if (r && preferenceScreen != null) {
            onUnbindPreferences();
            mHavePrefs = true;
            if (mInitDone) {
                postBindPreferences();
            }
        }
    }

    /**
     * Gets the root of the preference hierarchy that this fragment is showing.
     *
     * @return The {@link android.preference.PreferenceScreen} that is the root of the preference
     * hierarchy.
     */
    public PreferenceScreen getPreferenceScreen() {
        return PreferenceManagerUtils.getPreferenceScreen(mPreferenceManager);
    }

    /**
     * Adds preferences from activities that match the given {@link android.content.Intent}.
     *
     * @param intent The {@link android.content.Intent} to query activities.
     */
    public void addPreferencesFromIntent(Intent intent) {
        requirePreferenceManager();
        setPreferenceScreen(PreferenceManagerUtils.inflateFromIntent(
                mPreferenceManager, intent, getPreferenceScreen()));
    }

    /**
     * Inflates the given XML resource and adds the preference hierarchy to the current
     * preference hierarchy.
     *
     * @param preferencesResId The XML resource ID to inflate.
     */
    public void addPreferencesFromResource(int preferencesResId) {
        requirePreferenceManager();
        setPreferenceScreen(PreferenceManagerUtils.inflateFromResource(
                mPreferenceManager, getActivity(), preferencesResId, getPreferenceScreen()));
    }

    /**
     * {@inheritDoc}
     */
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {
        return preference.getFragment() != null
                && getActivity() instanceof PreferenceFragmentBase.OnPreferenceStartFragmentCallback
                && ((PreferenceFragmentBase.OnPreferenceStartFragmentCallback) getActivity())
                .onPreferenceStartFragment(this, preference);
    }

    /**
     * Finds a {@link android.preference.Preference} based on its key.
     *
     * @param key The key of the preference to retrieve.
     * @return The {@link android.preference.Preference} with the key, or null.
     * @see android.preference.PreferenceGroup#findPreference(CharSequence)
     */
    public Preference findPreference(CharSequence key) {
        if (mPreferenceManager == null) {
            return null;
        }
        return mPreferenceManager.findPreference(key);
    }

    private void requirePreferenceManager() {
        if (mPreferenceManager == null) {
            throw new RuntimeException("This should be called after super.onCreate.");
        }
    }

    private void postBindPreferences() {
        if (mHandler.hasMessages(MSG_BIND_PREFERENCES)) return;
        mHandler.obtainMessage(MSG_BIND_PREFERENCES).sendToTarget();
    }

    private void bindPreferences() {
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            preferenceScreen.bind(getListView());
        }
        onBindPreferences();
    }

    protected void onBindPreferences() {
    }

    protected void onUnbindPreferences() {
    }

    public ListView getListView() {
        ensureList();
        return mList;
    }

    private void ensureList() {
        if (mList != null) {
            return;
        }
        View root = getView();
        if (root == null) {
            throw new IllegalStateException("Content view not yet created");
        }
        View rawListView = root.findViewById(android.R.id.list);
        if (!(rawListView instanceof ListView)) {
            throw new RuntimeException(
                    "Content has view with id attribute 'android.R.id.list' "
                            + "that is not a ListView class");
        }
        mList = (ListView) rawListView;
        mList.setOnKeyListener(mListOnKeyListener);
        mHandler.post(mRequestFocus);
    }

    private final View.OnKeyListener mListOnKeyListener = new View.OnKeyListener() {

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            Object selectedItem = mList.getSelectedItem();
            if (selectedItem instanceof Preference) {
                View selectedView = mList.getSelectedView();
                Preference pref = (Preference) selectedItem;

                try {
                    Method method = pref.getClass().getDeclaredMethod("onKey",
                            View.class, int.class, KeyEvent.class);
                    method.setAccessible(true);
                    return (boolean) method.invoke(pref, selectedView, keyCode, event);
                } catch (Exception ignored) { /* unused*/ }
            }
            return false;
        }

    };
}
