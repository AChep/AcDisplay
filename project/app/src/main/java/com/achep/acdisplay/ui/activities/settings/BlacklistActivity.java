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
package com.achep.acdisplay.ui.activities.settings;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.achep.acdisplay.R;
import com.achep.acdisplay.blacklist.AppConfig;
import com.achep.acdisplay.blacklist.Blacklist;
import com.achep.acdisplay.ui.fragments.BlacklistAppFragment;
import com.achep.base.utils.MathUtils;
import com.achep.base.utils.ResUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Top-level settings activity to handle single pane and double pane UI layout.
 */
public class BlacklistActivity extends PreferenceActivity {

    private static final String TAG = "BlacklistActivity";

    // meta data
    private static final String META_DATA_KEY_HEADER_ID =
            "com.achep.acdisplay.blacklist.TOP_LEVEL_HEADER_ID";
    private static final String META_DATA_KEY_FRAGMENT_CLASS =
            "com.achep.acdisplay.blacklist.FRAGMENT_CLASS";
    private static final String META_DATA_KEY_PARENT_TITLE =
            "com.achep.acdisplay.blacklist.PARENT_FRAGMENT_TITLE";
    private static final String META_DATA_KEY_PARENT_FRAGMENT_CLASS =
            "com.achep.acdisplay.blacklist.PARENT_FRAGMENT_CLASS";

    // save state
    private static final String SAVE_KEY_CURRENT_HEADER =
            "com.achep.acdisplay.blacklist.CURRENT_HEADER";
    private static final String SAVE_KEY_PARENT_HEADER =
            "com.achep.acdisplay.blacklist.PARENT_HEADER";

    // preferences
    private static final String PREF_KEY_SHOW_SYSTEM_APPS = "show_system_apps";

    private String mFragmentClass;
    private int mTopLevelHeaderId;
    private Header mFirstHeader;
    private Header mCurrentHeader;
    private Header mParentHeader;
    private boolean mInLocalHeaderSwitch;

    protected HashMap<Integer, Integer> mHeaderIndexMap = new HashMap<>();

    private SharedPreferences mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mPreferences = getSharedPreferences(Blacklist.PREF_NAME, Activity.MODE_PRIVATE);

        getMetaData();
        mInLocalHeaderSwitch = true;
        super.onCreate(savedInstanceState);
        mInLocalHeaderSwitch = false;

        if (!onIsHidingHeaders() && onIsMultiPane()) {
            highlightHeader(mTopLevelHeaderId);
            // Force the title so that it doesn't get overridden by a direct launch of
            // a specific settings screen.
            setTitle(R.string.settings);
        }

        // Retrieve any saved state
        if (savedInstanceState != null) {
            mCurrentHeader = savedInstanceState.getParcelable(SAVE_KEY_CURRENT_HEADER);
            mParentHeader = savedInstanceState.getParcelable(SAVE_KEY_PARENT_HEADER);
        }

        // If the current header was saved, switch to it
        if (savedInstanceState != null && mCurrentHeader != null) {
            //switchToHeaderLocal(mCurrentHeader);
            showBreadCrumbs(mCurrentHeader.title, null);
        }

        if (mParentHeader != null) {
            setParentTitle(mParentHeader.title, null, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchToParent(mParentHeader.fragment);
                }
            });
        }

        // Override up navigation for multi-pane, since we handle it in the fragment breadcrumbs
        if (onIsMultiPane()) {
            getActionBar().setDisplayHomeAsUpEnabled(false);
            getActionBar().setHomeButtonEnabled(false);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the current fragment, if it is the same as originally launched
        if (mCurrentHeader != null) {
            outState.putParcelable(SAVE_KEY_CURRENT_HEADER, mCurrentHeader);
        }
        if (mParentHeader != null) {
            outState.putParcelable(SAVE_KEY_PARENT_HEADER, mParentHeader);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        invalidateHeaders();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.blacklist, menu);
        menu.findItem(R.id.show_system_apps).setChecked(shouldShowSystemApps());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.show_system_apps:
                item.setChecked(!item.isChecked());
                mPreferences
                        .edit()
                        .putBoolean(PREF_KEY_SHOW_SYSTEM_APPS, item.isChecked())
                        .apply();
                invalidateHeaders();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private boolean shouldShowSystemApps() {
        return mPreferences.getBoolean(PREF_KEY_SHOW_SYSTEM_APPS, false);
    }

    private void updateIcons() {
        ListAdapter listAdapter = getListAdapter();
        if (listAdapter instanceof HeaderAdapter) {
            ((HeaderAdapter) listAdapter).loadIcons();
        }
    }

    /**
     * Fills header list with a list of installed apps.
     */
    private void buildHeaderList(List<Header> headers, boolean showSystemApps) {
        String fragmentName = BlacklistAppFragment.class.getCanonicalName();
        int id = 1;

        PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo app : packages) {
            int systemFlag = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
            int system = MathUtils.bool((app.flags & systemFlag) != 0);

            if (system == 1 && !showSystemApps) {
                continue;
            }

            Header header = new Header();
            header.fragment = fragmentName;
            header.title = app.loadLabel(pm); // TODO: This is a huge operation
            header.summary = app.packageName;
            header.id = system | id++ << 1; // Store if system app to id

            // Add package name for the fragment
            Bundle args = new Bundle();
            args.putString(BlacklistAppFragment.ARGS_PACKAGE_NAME, app.packageName);
            header.fragmentArguments = args;

            headers.add(header);
        }

        // Sort by app name
        Collections.sort(headers, new Comparator<Header>() {
            @Override
            public int compare(Header header1, Header header2) {
                String title1 = header1.title.toString();
                String title2 = header2.title.toString();
                return title1.compareToIgnoreCase(title2);
            }
        });
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }

    private void switchToHeaderLocal(Header header) {
        mInLocalHeaderSwitch = true;
        switchToHeader(header);
        mInLocalHeaderSwitch = false;
    }

    @Override
    public void switchToHeader(@NonNull Header header) {
        if (!mInLocalHeaderSwitch) {
            mCurrentHeader = null;
            mParentHeader = null;
        }
        super.switchToHeader(header);
    }

    /**
     * Switch to parent fragment and store the grand parent's info
     *
     * @param className name of the activity wrapper for the parent fragment.
     */
    private void switchToParent(String className) {
        final ComponentName cn = new ComponentName(this, className);
        try {
            final PackageManager pm = getPackageManager();
            final ActivityInfo parentInfo = pm.getActivityInfo(cn, PackageManager.GET_META_DATA);

            if (parentInfo != null && parentInfo.metaData != null) {
                String fragmentClass = parentInfo.metaData.getString(META_DATA_KEY_FRAGMENT_CLASS);
                CharSequence fragmentTitle = parentInfo.loadLabel(pm);
                Header parentHeader = new Header();
                parentHeader.fragment = fragmentClass;
                parentHeader.title = fragmentTitle;
                mCurrentHeader = parentHeader;

                switchToHeaderLocal(parentHeader);
                highlightHeader(mTopLevelHeaderId);

                mParentHeader = new Header();
                mParentHeader.fragment
                        = parentInfo.metaData.getString(META_DATA_KEY_PARENT_FRAGMENT_CLASS);
                mParentHeader.title = parentInfo.metaData.getString(META_DATA_KEY_PARENT_TITLE);
            }
        } catch (NameNotFoundException nnfe) {
            Log.w(TAG, "Could not find parent activity : " + className);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // If it is not launched from history, then reset to top-level
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
            if (mFirstHeader != null && !onIsHidingHeaders() && onIsMultiPane()) {
                switchToHeaderLocal(mFirstHeader);
            }
            getListView().setSelectionFromTop(0, 0);
        }
    }

    private void highlightHeader(int id) {
        if (id != 0) {
            Integer index = mHeaderIndexMap.get(id);
            if (index != null) {
                getListView().setItemChecked(index, true);
                if (isMultiPane()) {
                    getListView().smoothScrollToPosition(index);
                }
            }
        }
    }

    @Override
    public Intent getIntent() {
        Intent superIntent = super.getIntent();
        String startingFragment = getStartingFragmentClass(superIntent);
        // This is called from super.onCreate, isMultiPane() is not yet reliable
        // Do not use onIsHidingHeaders either, which relies itself on this method
        if (startingFragment != null && !onIsMultiPane()) {
            Intent modIntent = new Intent(superIntent);
            modIntent.putExtra(EXTRA_SHOW_FRAGMENT, startingFragment);
            Bundle args = superIntent.getExtras();
            if (args != null) {
                args = new Bundle(args);
            } else {
                args = new Bundle();
            }
            args.putParcelable("intent", superIntent);
            modIntent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, superIntent.getExtras());
            return modIntent;
        }
        return superIntent;
    }

    /**
     * Checks if the component name in the intent is different from the Settings class and
     * returns the class name to load as a fragment.
     */
    protected String getStartingFragmentClass(Intent intent) {
        if (mFragmentClass != null) return mFragmentClass;
        String intentClass = intent.getComponent().getClassName();
        if (intentClass.equals(getClass().getName())) return null;

        return intentClass;
    }

    /**
     * Override initial header when an activity-alias is causing Settings to be launched
     * for a specific fragment encoded in the android:name parameter.
     */
    @Override
    public Header onGetInitialHeader() {
        String fragmentClass = getStartingFragmentClass(super.getIntent());
        if (fragmentClass != null) {
            Header header = new Header();
            header.fragment = fragmentClass;
            header.title = getTitle();
            header.fragmentArguments = getIntent().getExtras();
            mCurrentHeader = header;
            return header;
        }

        return mFirstHeader;
    }

    @Override
    public Intent onBuildStartFragmentIntent(String fragmentName, Bundle args,
                                             int titleRes, int shortTitleRes) {
        return super
                .onBuildStartFragmentIntent(fragmentName, args, titleRes, shortTitleRes)
                .setClass(this, SubBlacklistActivity.class);
    }

    /**
     * Populate the activity with the top-level headers.
     */
    @Override
    public void onBuildHeaders(List<Header> headers) {
        if (!onIsHidingHeaders()) {
            buildHeaderList(headers, shouldShowSystemApps());
            updateHeaderList(headers);
            updateIcons();
        }
    }

    private void updateHeaderList(List<Header> target) {
        int i = 0;
        mHeaderIndexMap.clear();
        while (i < target.size()) {
            Header header = target.get(i);
            // Ids are integers, so downcasting
            int id = (int) header.id;

            // Increment if the current one wasn't removed by the Utils code.
            if (i < target.size() && target.get(i) == header) {
                // Hold on to the first header, when we need to reset to the top-level
                int headerType = HeaderAdapter.getHeaderType(header);
                if (mFirstHeader == null &&
                        headerType != HeaderAdapter.HEADER_TYPE_CATEGORY) {
                    mFirstHeader = header;
                }
                mHeaderIndexMap.put(id, i);
                i++;
            }
        }
    }

    private void getMetaData() {
        try {
            ActivityInfo ai = getPackageManager().getActivityInfo(getComponentName(),
                    PackageManager.GET_META_DATA);
            if (ai == null || ai.metaData == null) return;
            mTopLevelHeaderId = ai.metaData.getInt(META_DATA_KEY_HEADER_ID);
            mFragmentClass = ai.metaData.getString(META_DATA_KEY_FRAGMENT_CLASS);

            // Check if it has a parent specified and create a Header object
            final int parentHeaderTitleRes = ai.metaData.getInt(META_DATA_KEY_PARENT_TITLE);
            String parentFragmentClass = ai.metaData.getString(META_DATA_KEY_PARENT_FRAGMENT_CLASS);
            if (parentFragmentClass != null) {
                mParentHeader = new Header();
                mParentHeader.fragment = parentFragmentClass;
                if (parentHeaderTitleRes != 0) {
                    mParentHeader.title = getResources().getString(parentHeaderTitleRes);
                }
            }
        } catch (NameNotFoundException e) {
            // No recovery
        }
    }

    private static class HeaderAdapter extends ArrayAdapter<Header> {

        static final int HEADER_TYPE_CATEGORY = 0;
        static final int HEADER_TYPE_NORMAL = 1;

        private static final int HEADER_TYPE_COUNT = 2;

        private final Context mContext;
        private final LayoutInflater mInflater;

        private final PackageManager mPackageManager;
        private final Drawable mDefaultImg;
        private final List<Header> mHeaders;
        private LoadIconsTask mLoadIconsTask;

        private final ConcurrentHashMap<String, Drawable> mIcons;

        static int getHeaderType(Header header) {
            if (header.fragment == null && header.intent == null) {
                return HEADER_TYPE_CATEGORY;
            } else {
                return HEADER_TYPE_NORMAL;
            }
        }

        @Override
        public int getItemViewType(int position) {
            Header header = getItem(position);
            return getHeaderType(header);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false; // because of categories
        }

        @Override
        public boolean isEnabled(int position) {
            return getItemViewType(position) != HEADER_TYPE_CATEGORY;
        }

        @Override
        public int getViewTypeCount() {
            return HEADER_TYPE_COUNT;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public HeaderAdapter(Context context, List<Header> headers) {
            super(context, 0, headers);

            mContext = context;
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mPackageManager = context.getPackageManager();
            mHeaders = headers;

            // Set the default icon till the actual app icon is loaded in async task
            //noinspection ResourceType
            mDefaultImg = ResUtils.getDrawable(context, android.R.mipmap.sym_def_app_icon);

            mIcons = new ConcurrentHashMap<>();

            loadIcons();
        }

        private static class Holder {
            ImageView icon;
            TextView title;
            TextView summary;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Holder holder;
            final Header header = getItem(position);
            final int headerType = getHeaderType(header);
            View view = null;

            if (convertView == null) {
                holder = new Holder();
                switch (headerType) {
                    case HEADER_TYPE_CATEGORY:
                        view = new TextView(getContext(), null,
                                android.R.attr.listSeparatorTextViewStyle);
                        holder.title = (TextView) view;
                        break;

                    case HEADER_TYPE_NORMAL:
                        view = mInflater.inflate(
                                R.layout.preference_header_item, parent,
                                false);
                        holder.icon = (ImageView) view.findViewById(R.id.icon);
                        holder.title = (TextView) view.findViewById(android.R.id.title);
                        holder.summary = (TextView) view.findViewById(android.R.id.summary);
                        break;
                }
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (Holder) view.getTag();
            }

            Resources res = mContext.getResources();

            // All view fields must be updated every time, because the view may be recycled
            switch (headerType) {
                case HEADER_TYPE_CATEGORY:
                    holder.title.setText(header.getTitle(getContext().getResources()));
                    break;

                case HEADER_TYPE_NORMAL:
                    String packageName = "" + header.summary;
                    AppConfig config = Blacklist.getInstance().getAppConfig(packageName);

                    // Show checked options in summary.
                    // TODO: Find the way to optimize it.
                    if (config.isHidden() || config.isRestricted() || config.isNonClearableEnabled()) {
                        StringBuilder sb = new StringBuilder();
                        boolean empty = true;

                        int[] pairs = new int[]{
                                // Hidden
                                MathUtils.bool(config.isHidden()),
                                R.string.blacklist_app_hide_title,
                                // Restricted
                                MathUtils.bool(config.isRestricted()),
                                R.string.blacklist_app_restricted_title,
                                // Non-clearable
                                MathUtils.bool(config.isNonClearableEnabled()),
                                R.string.blacklist_app_non_clearable_title,
                        };

                        // Append checked options.
                        String divider = res.getString(R.string.settings_multi_list_divider);
                        for (int i = 0; i < pairs.length / 2; i++) {
                            int a = pairs[i * 2];
                            if (a == 1) {
                                if (!empty) {
                                    sb.append(divider);
                                }
                                sb.append(res.getString(pairs[i * 2 + 1]));
                                empty = false;
                            }
                        }

                        String summary = sb.toString();
                        if (!TextUtils.isEmpty(summary)) {
                            // Keep only first letter with upper case and
                            // force all other to lower case.
                            summary = summary.charAt(0)
                                    + summary.substring(1).toLowerCase(Locale.getDefault());

                            holder.summary.setVisibility(View.VISIBLE);
                            holder.summary.setText(summary);
                        } else {
                            holder.summary.setVisibility(View.GONE);
                        }
                    } else {
                        holder.summary.setVisibility(View.GONE);
                    }

                    Drawable icon = mIcons.get(packageName);
                    holder.icon.setImageDrawable(icon != null ? icon : mDefaultImg);
                    holder.title.setText(header.title != null ? header.title : packageName);
                    break;
            }

            return view;
        }

        /**
         * Update the list of apps' icons
         */
        public void loadIcons() {
            if (mLoadIconsTask != null && !mLoadIconsTask.getStatus()
                    .equals(AsyncTask.Status.FINISHED)) {
                mLoadIconsTask.cancel = true;
                mLoadIconsTask.cancel(false);
            }
            mLoadIconsTask = new LoadIconsTask();
            mLoadIconsTask.execute(mHeaders.toArray(new Header[mHeaders.size()]));
        }

        /**
         * An asynchronous task to load the icons & titles of the installed applications.
         */
        // TODO: Maybe use SoftReference<> to save some memory.
        private class LoadIconsTask extends AsyncTask<Header, Void, Void> {

            private volatile long time;
            private volatile boolean cancel;

            @Override
            protected Void doInBackground(Header... headers) {
                for (Header header : headers) {
                    try {
                        if (cancel) return null;

                        String packageName = "" + header.summary;
                        if (mIcons.containsKey(packageName)) {
                            continue;
                        }

                        ApplicationInfo app = mPackageManager.getApplicationInfo(packageName, 0);
                        Drawable icon = mPackageManager.getApplicationIcon(app.packageName);
                        mIcons.put(app.packageName, icon);

                        long now = SystemClock.uptimeMillis();
                        if (now - time > 500) {
                            publishProgress();
                            time = now;
                        }
                    } catch (NameNotFoundException e) {
                        // ignored; app will show up with default image & title
                    }
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Void... progress) {
                notifyDataSetChanged();
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                notifyDataSetChanged();
            }
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment caller, @NonNull Preference pref) {
        startPreferencePanel(
                pref.getFragment(),
                pref.getExtras(),
                pref.getTitleRes(),
                pref.getTitle(), null, 0);
        return true;
    }

    @Override
    public boolean shouldUpRecreateTask(@NonNull Intent targetIntent) {
        return super.shouldUpRecreateTask(new Intent(this, BlacklistActivity.class));
    }

    @Override
    public void setListAdapter(ListAdapter adapter) {
        if (adapter == null) {
            super.setListAdapter(null);
        } else {
            List<Header> headers = null;
            try {
                Method method = PreferenceActivity.class.getDeclaredMethod("getHeaders");
                method.setAccessible(true);
                headers = (List<Header>) method.invoke(this);
            } catch (Exception e) {
                e.printStackTrace();
            }

            super.setListAdapter(new HeaderAdapter(this, headers));
        }
    }
}
