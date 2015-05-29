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
package com.achep.base.ui.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.annotation.XmlRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.ViewGroup;

import com.achep.acdisplay.R;
import com.achep.acdisplay.ui.activities.settings.Settings2;
import com.achep.base.Device;
import com.achep.base.dashboard.DashboardCategory;
import com.achep.base.dashboard.DashboardTile;
import com.achep.base.ui.fragments.DashboardFragment;
import com.achep.base.utils.xml.XmlUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class SettingsActivity extends ActivityBase implements
        PreferenceFragment.OnPreferenceStartFragmentCallback,
        FragmentManager.OnBackStackChangedListener {

    private static final String LOG_TAG = "Settings";

    // Constants for loading dashboard from xml resource.
    private static final String RESOURCE_TAG_DASHBOARD = "dashboard";
    private static final String RESOURCE_TAG_DASHBOARD_CATEGORY = "dashboard-category";
    private static final String RESOURCE_TAG_DASHBOARD_TILE = "dashboard-tile";
    private static final String RESOURCE_TAG_DASHBOARD_TILE_EXTRA = "extra";
    private static final String RESOURCE_TAG_DASHBOARD_TILE_INTENT = "intent";

    // Constants for state save/restore
    private static final String SAVE_KEY_CATEGORIES = ":settings:categories";
    private static final String SAVE_KEY_SHOW_HOME_AS_UP = ":settings:show_home_as_up";

    /**
     * When starting this activity, the invoking Intent can contain this extra
     * string to specify which fragment should be initially displayed.
     * <p/>Starting from Key Lime Pie, when this argument is passed in, the activity
     * will call isValidFragment() to confirm that the fragment class name is valid for this
     * activity.
     */
    public static final String EXTRA_SHOW_FRAGMENT = ":settings:show_fragment";

    /**
     * When starting this activity and using {@link #EXTRA_SHOW_FRAGMENT},
     * this extra can also be specified to supply a Bundle of arguments to pass
     * to that fragment when it is instantiated during the initial creation
     * of the activity.
     */
    public static final String EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args";

    /**
     * Fragment "key" argument passed thru {@link #EXTRA_SHOW_FRAGMENT_ARGUMENTS}
     */
    public static final String EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key";

    public static final String BACK_STACK_PREFS = ":settings:prefs";

    /**
     * When starting this activity and using {@link #EXTRA_SHOW_FRAGMENT},
     * those extra can also be specify to supply the title or title res id to be shown for
     * that fragment.
     */
    public static final String EXTRA_SHOW_FRAGMENT_TITLE =
            ":settings:show_fragment_title";

    public static final String EXTRA_SHOW_FRAGMENT_TITLE_RESID =
            ":settings:show_fragment_title_resid";

    public static final String EXTRA_SHOW_FRAGMENT_AS_SHORTCUT =
            ":settings:show_fragment_as_shortcut";

    public static final String EXTRA_SHOW_FRAGMENT_AS_SUBSETTING =
            ":settings:show_fragment_as_subsetting";

    private static final String META_DATA_KEY_FRAGMENT_CLASS =
            "com.android.settings.FRAGMENT_CLASS";

    private static final String EXTRA_UI_OPTIONS =
            "settings:ui_options";

    private String mFragmentClass;

    private CharSequence mInitialTitle;
    private int mInitialTitleResId;


    private static final String[] LIKE_SHORTCUT_INTENT_ACTION_ARRAY = {
            "android.settings.APPLICATION_DETAILS_SETTINGS"
    };

    private ViewGroup mContent;

    private boolean mDisplayHomeAsUpEnabled;
    private boolean mIsShortcut;

    private ArrayList<DashboardCategory> mCategories = new ArrayList<>();

    public List<DashboardCategory> getDashboardCategories(boolean forceRefresh) {
        if (forceRefresh || mCategories.size() == 0) {
            buildDashboard(mCategories);
        }
        return mCategories;
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference p) {
        startPreferencePanel(p.getFragment(), p.getExtras(), p.getTitleRes(), p.getTitle(), null, 0);
        return true;
    }

    private static boolean isShortCutIntent(final Intent intent) {
        Set<String> categories = intent.getCategories();
        return (categories != null) && categories.contains("com.android.settings.SHORTCUT");
    }

    private static boolean isLikeShortCutIntent(final Intent intent) {
        String action = intent.getAction();
        if (action == null) return false;

        for (String item : LIKE_SHORTCUT_INTENT_ACTION_ARRAY)
            if (item.equals(action))
                return true;
        return false;
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        // Should happen before any call to getIntent()
        getMetaData();

        final Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_UI_OPTIONS)) {
            getWindow().setUiOptions(intent.getIntExtra(EXTRA_UI_OPTIONS, 0));
        }

        // Getting Intent properties can only be done after the super.onCreate(...)
        final String initialFragmentName = intent.getStringExtra(EXTRA_SHOW_FRAGMENT);

        mIsShortcut = isShortCutIntent(intent) || isLikeShortCutIntent(intent) ||
                intent.getBooleanExtra(EXTRA_SHOW_FRAGMENT_AS_SHORTCUT, false);

        final ComponentName cn = intent.getComponent();
        final String className = cn.getClassName();

        boolean isShowingDashboard = className.equals(Settings2.class.getName());

        // This is a "Sub Settings" when:
        // - this is a real SubSettings
        // - or :settings:show_fragment_as_subsetting is passed to the Intent
        final boolean isSubSettings = className.equals(SubSettings.class.getName()) ||
                intent.getBooleanExtra(EXTRA_SHOW_FRAGMENT_AS_SUBSETTING, false);

        // If this is a sub settings, then apply the SubSettings Theme for the ActionBar content insets
        if (isSubSettings) {
            // Check also that we are not a Theme Dialog as we don't want to override them
            /*
            final int themeResId = getTheme(). getThemeResId();
            if (themeResId != R.style.Theme_DialogWhenLarge &&
                    themeResId != R.style.Theme_SubSettingsDialogWhenLarge) {
                setTheme(R.style.Theme_SubSettings);
            }
            */
        }

        setContentView(R.layout.settings_main_dashboard);

        mContent = (ViewGroup) findViewById(android.R.id.content);

        getSupportFragmentManager().addOnBackStackChangedListener(this);

        if (savedState != null) {
            // We are restarting from a previous saved state; used that to initialize, instead
            // of starting fresh.

            setTitleFromIntent(intent);

            ArrayList<DashboardCategory> categories =
                    savedState.getParcelableArrayList(SAVE_KEY_CATEGORIES);
            if (categories != null) {
                mCategories.clear();
                mCategories.addAll(categories);
                setTitleFromBackStack();
            }

            mDisplayHomeAsUpEnabled = savedState.getBoolean(SAVE_KEY_SHOW_HOME_AS_UP);
        } else {
            if (!isShowingDashboard) {
                mDisplayHomeAsUpEnabled = isSubSettings;
                setTitleFromIntent(intent);

                Bundle initialArguments = intent.getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
                switchToFragment(initialFragmentName, initialArguments, true, false,
                        mInitialTitleResId, mInitialTitle, false);
            } else {
                mDisplayHomeAsUpEnabled = false;
                mInitialTitleResId = R.string.app_name;
                switchToFragment(DashboardFragment.class.getName(), null, false, false,
                        mInitialTitleResId, mInitialTitle, false);
            }
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(mDisplayHomeAsUpEnabled);
            actionBar.setHomeButtonEnabled(mDisplayHomeAsUpEnabled);
        }
    }

    /*
    @Override
    public boolean onNavigateUp() {
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        return true;
    }
    */

    private void setTitleFromIntent(Intent intent) {
        final int initialTitleResId = intent.getIntExtra(EXTRA_SHOW_FRAGMENT_TITLE_RESID, -1);
        if (initialTitleResId > 0) {
            mInitialTitle = null;
            mInitialTitleResId = initialTitleResId;
            setTitle(mInitialTitleResId);
        } else {
            mInitialTitleResId = -1;
            final String initialTitle = intent.getStringExtra(EXTRA_SHOW_FRAGMENT_TITLE);
            mInitialTitle = (initialTitle != null) ? initialTitle : getTitle();
            setTitle(mInitialTitle);
        }
    }

    @Override
    public void onBackStackChanged() {
        setTitleFromBackStack();
    }

    private int setTitleFromBackStack() {
        final int count = getFragmentManager().getBackStackEntryCount();

        if (count == 0) {
            if (mInitialTitleResId > 0) {
                setTitle(mInitialTitleResId);
            } else {
                setTitle(mInitialTitle);
            }
            return 0;
        }

        FragmentManager.BackStackEntry bse = getSupportFragmentManager().getBackStackEntryAt(count - 1);
        setTitleFromBackStackEntry(bse);

        return count;
    }

    private void setTitleFromBackStackEntry(FragmentManager.BackStackEntry bse) {
        final CharSequence title;
        final int titleRes = bse.getBreadCrumbTitleRes();
        if (titleRes > 0) {
            title = getText(titleRes);
        } else {
            title = bse.getBreadCrumbTitle();
        }
        if (title != null) {
            setTitle(title);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mCategories.size() > 0) {
            outState.putParcelableArrayList(SAVE_KEY_CATEGORIES, mCategories);
        }

        outState.putBoolean(SAVE_KEY_SHOW_HOME_AS_UP, mDisplayHomeAsUpEnabled);
    }

    protected boolean isValidFragment(@NonNull String fragmentName) {
        return false;
    }

    @XmlRes
    protected int getDashboardResource() {
        return 0;
    }

    protected abstract boolean isTileSupported(@NonNull DashboardTile tile);

    @Override
    public Intent getIntent() {
        Intent superIntent = super.getIntent();
        String startingFragment = getStartingFragmentClass(superIntent);
        // This is called from super.onCreate, isMultiPane() is not yet reliable
        // Do not use onIsHidingHeaders either, which relies itself on this method
        if (startingFragment != null) {
            Intent modIntent = new Intent(superIntent);
            modIntent.putExtra(EXTRA_SHOW_FRAGMENT, startingFragment);
            Bundle args = superIntent.getExtras();
            if (args != null) {
                args = new Bundle(args);
            } else {
                args = new Bundle();
            }
            args.putParcelable("intent", superIntent);
            modIntent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
            return modIntent;
        }
        return superIntent;
    }

    /**
     * Checks if the component name in the intent is different from the Settings class and
     * returns the class name to load as a fragment.
     */
    private String getStartingFragmentClass(Intent intent) {
        if (mFragmentClass != null) return mFragmentClass;

        String intentClass = intent.getComponent().getClassName();
        return intentClass.equals(getClass().getName()) ? null : intentClass;
    }

    /**
     * Start a new fragment containing a preference panel.  If the preferences
     * are being displayed in multi-pane mode, the given fragment class will
     * be instantiated and placed in the appropriate pane.  If running in
     * single-pane mode, a new activity will be launched in which to show the
     * fragment.
     *
     * @param fragmentClass     Full name of the class implementing the fragment.
     * @param args              Any desired arguments to supply to the fragment.
     * @param titleRes          Optional resource identifier of the title of this
     *                          fragment.
     * @param titleText         Optional text of the title of this fragment.
     * @param resultTo          Optional fragment that result data should be sent to.
     *                          If non-null, resultTo.onActivityResult() will be called when this
     *                          preference panel is done.  The launched panel must use
     *                          {@link #finishPreferencePanel(Fragment, int, Intent)} when done.
     * @param resultRequestCode If resultTo is non-null, this is the caller's
     *                          request code to be received with the result.
     */
    public void startPreferencePanel(String fragmentClass, Bundle args, int titleRes,
                                     CharSequence titleText, Fragment resultTo, int resultRequestCode) {
        String title = null;
        if (titleRes < 0) {
            if (titleText != null) {
                title = titleText.toString();
            } else {
                // There not much we can do in that case
                title = "";
            }
        }

        Utils.startWithFragment(this, fragmentClass, args,
                resultTo, resultRequestCode,
                titleRes, title, mIsShortcut);
    }

    /**
     * Switch to a specific Fragment with taking care of validation, Title and BackStack
     */
    private Fragment switchToFragment(String fragmentName, Bundle args, boolean validate,
                                      boolean addToBackStack, int titleResId, CharSequence title,
                                      boolean withTransition) {
        if (validate && !isValidFragment(fragmentName)) {
            String message = "Invalid fragment for this activity: " + fragmentName;
            throw new IllegalArgumentException(message);
        }

        Fragment f = Fragment.instantiate(this, fragmentName, args);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(android.R.id.content, f);

        if (withTransition && Device.hasKitKatApi())
            TransitionManager.beginDelayedTransition(mContent);
        if (addToBackStack) transaction.addToBackStack(SettingsActivity.BACK_STACK_PREFS);
        if (titleResId > 0) {
            transaction.setBreadCrumbTitle(titleResId);
        } else if (title != null) {
            transaction.setBreadCrumbTitle(title);
        }

        transaction.commitAllowingStateLoss();
        getFragmentManager().executePendingTransactions();
        return f;
    }

    /**
     * Called when the activity needs its list of categories/tiles built.
     *
     * @param categories The list in which to place the tiles categories.
     */
    private void buildDashboard(@NonNull List<DashboardCategory> categories) {
        categories.clear();
        loadDashboardFromResource(getDashboardResource(), categories);
        updateDashboardTilesList(categories);
    }

    /**
     * Parse the given XML file as a categories description, adding each
     * parsed categories and tiles into the target list.
     *
     * @param resourceId The XML resource to load and parse.
     * @param target     The list in which the parsed categories and tiles should be placed.
     */
    protected final void loadDashboardFromResource(@XmlRes int resourceId,
                                                   @NonNull List<DashboardCategory> target) {
        XmlResourceParser parser = null;
        try {
            parser = getResources().getXml(resourceId);
            AttributeSet attrs = Xml.asAttributeSet(parser);

            int type;

            for (type = parser.next();
                 type != XmlPullParser.END_DOCUMENT;
                 type = parser.next()) {
                if (type == XmlPullParser.START_TAG) break;
            }

            String nodeName = parser.getName();
            if (!RESOURCE_TAG_DASHBOARD.equals(nodeName)) throw new RuntimeException(
                    String.format("XML document must start with <%s> tag; found %s at %s",
                            RESOURCE_TAG_DASHBOARD, nodeName, parser.getPositionDescription()));

            for (type = parser.next();
                 type != XmlPullParser.END_DOCUMENT;
                 type = parser.next()) {
                if (type == XmlPullParser.END_TAG && parser.getDepth() <= 1 /* root tag */) break;
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }

                switch (parser.getName()) {
                    case RESOURCE_TAG_DASHBOARD_CATEGORY:
                        DashboardCategory category = new DashboardCategory(this, attrs);

                        final int categoryDepth = parser.getDepth();
                        for (type = parser.next();
                             type != XmlPullParser.END_DOCUMENT;
                             type = parser.next()) {
                            if (type == XmlPullParser.END_TAG && parser.getDepth() <= categoryDepth)
                                break;
                            if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                                continue;
                            }

                            switch (parser.getName()) {
                                case RESOURCE_TAG_DASHBOARD_TILE:
                                    DashboardTile tile = new DashboardTile(this, attrs);
                                    Bundle bundle = null;

                                    final int tileDepth = parser.getDepth();
                                    for (type = parser.next();
                                         type != XmlPullParser.END_DOCUMENT;
                                         type = parser.next()) {
                                        if (type == XmlPullParser.END_TAG && parser.getDepth() <= tileDepth)
                                            break;
                                        if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                                            continue;
                                        }

                                        switch (parser.getName()) {
                                            case RESOURCE_TAG_DASHBOARD_TILE_EXTRA:
                                                if (bundle == null) {
                                                    bundle = new Bundle();
                                                }

                                                getResources().parseBundleExtra(RESOURCE_TAG_DASHBOARD_TILE_EXTRA, attrs, bundle);
                                                XmlUtils.skipCurrentTag(parser);
                                                break;
                                            case RESOURCE_TAG_DASHBOARD_TILE_INTENT:
                                                tile.intent = Intent.parseIntent(getResources(), parser, attrs);
                                                break;
                                            default:
                                                XmlUtils.skipCurrentTag(parser);
                                        }
                                    }

                                    tile.fragmentArguments = bundle;
                                    category.add(tile);
                                    break;
                                default:
                                    XmlUtils.skipCurrentTag(parser);
                            }
                        }

                        target.add(category);
                        break;
                    default:
                        XmlUtils.skipCurrentTag(parser);
                }
            }
        } catch (XmlPullParserException | IOException e) {
            throw new RuntimeException("Error parsing categories", e);
        } finally {
            if (parser != null) parser.close();
        }
    }

    private void updateDashboardTilesList(List<DashboardCategory> target) {
        for (int i = target.size() - 1; i >= 0; i--) {
            final DashboardCategory category = target.get(i);

            for (int j = category.size() - 1; j >= 0; j--) {
                final DashboardTile tile = category.get(j);
                final boolean removeTile = !isTileSupported(tile);
                if (removeTile) {
                    category.remove(j);
                }
            }

            if (category.isEmpty()) {
                target.remove(i);
            }
        }
    }

    private void getMetaData() {
        try {
            ActivityInfo ai = getPackageManager().getActivityInfo(getComponentName(),
                    PackageManager.GET_META_DATA);
            if (ai == null || ai.metaData == null) return;
            mFragmentClass = ai.metaData.getString(META_DATA_KEY_FRAGMENT_CLASS);
        } catch (NameNotFoundException nnfe) {
            // No recovery
            Log.d(LOG_TAG, "Cannot get Metadata for: " + getComponentName().toString());
        }
    }

    @Override
    public boolean shouldUpRecreateTask(@NonNull Intent unused) {
        return super.shouldUpRecreateTask(new Intent(this, SettingsActivity.class));
    }

    public static final class Utils {

        /**
         * Start a new instance of the activity, showing only the given fragment.
         * When launched in this mode, the given preference fragment will be instantiated and fill the
         * entire activity.
         *
         * @param context           The context.
         * @param fragmentName      The name of the fragment to display.
         * @param args              Optional arguments to supply to the fragment.
         * @param resultTo          Option fragment that should receive the result of the activity launch.
         * @param resultRequestCode If resultTo is non-null, this is the request code in which
         *                          to report the result.
         * @param titleResId        resource id for the String to display for the title of this set
         *                          of preferences.
         * @param title             String to display for the title of this set of preferences.
         */
        public static void startWithFragment(Context context, String fragmentName, Bundle args,
                                             Fragment resultTo, int resultRequestCode, int titleResId, CharSequence title) {
            startWithFragment(context, fragmentName, args, resultTo, resultRequestCode,
                    titleResId, title, false /* not a shortcut */);
        }

        public static void startWithFragment(Context context, String fragmentName, Bundle args,
                                             Fragment resultTo, int resultRequestCode, int titleResId, CharSequence title,
                                             boolean isShortcut) {
            Intent intent = onBuildStartFragmentIntent(context, fragmentName, args, titleResId,
                    title, isShortcut);
            if (resultTo == null) {
                context.startActivity(intent);
            } else {
                resultTo.startActivityForResult(intent, resultRequestCode);
            }
        }

        /**
         * Build an Intent to launch a new activity showing the selected fragment.
         * The implementation constructs an Intent that re-launches the current activity with the
         * appropriate arguments to display the fragment.
         *
         * @param context      The Context.
         * @param fragmentName The name of the fragment to display.
         * @param args         Optional arguments to supply to the fragment.
         * @param titleResId   Optional title resource id to show for this item.
         * @param title        Optional title to show for this item.
         * @param isShortcut   tell if this is a Launcher Shortcut or not
         * @return Returns an Intent that can be launched to display the given
         * fragment.
         */
        public static Intent onBuildStartFragmentIntent(Context context, String fragmentName,
                                                        Bundle args, int titleResId, CharSequence title, boolean isShortcut) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClass(context, SubSettings.class);
            intent.putExtra(EXTRA_SHOW_FRAGMENT, fragmentName);
            intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
            intent.putExtra(EXTRA_SHOW_FRAGMENT_TITLE_RESID, titleResId);
            intent.putExtra(EXTRA_SHOW_FRAGMENT_TITLE, title);
            intent.putExtra(EXTRA_SHOW_FRAGMENT_AS_SHORTCUT, isShortcut);
            return intent;
        }

    }
}
