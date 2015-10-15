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

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.acdisplay.appwidget.MyAppWidgetHost;
import com.achep.acdisplay.appwidget.MyAppWidgetHostView;
import com.achep.acdisplay.ui.components.HostWidget;
import com.achep.base.Device;
import com.achep.base.content.ConfigBase;
import com.achep.base.tests.Check;
import com.achep.base.ui.SwitchBarPermissible;
import com.achep.base.ui.activities.ActivityBase;
import com.achep.base.ui.preferences.Enabler;
import com.achep.base.ui.widgets.SwitchBar;
import com.achep.base.utils.AppWidgetUtils;
import com.achep.base.utils.MathUtils;
import com.achep.base.utils.ToastUtils;
import com.achep.base.utils.ViewUtils;
import com.afollestad.materialdialogs.MaterialDialog;
import com.melnykov.fab.FloatingActionButton;

import java.util.ArrayList;

/**
 * An activity for setting the custom App Widget, tweaking it
 * and licking.
 *
 * @author Artem Chepurnoy
 */
public class WidgetPickerActivity extends ActivityBase implements
        Config.OnConfigChangedListener,
        SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "WidgetPickerActivity";

    private static final String KEY_PENDING_APPWIDGET_ID = "achep::pending_app_widget_key";

    /**
     * A request to open default AppWidget Picker dialog.
     */
    private static final int REQUEST_APPWIDGET_DISCOVER = 1;

    /**
     * A request to open the configure activity of an AppWidget.
     */
    private static final int REQUEST_APPWIDGET_CONFIGURE = 2;

    private static final int APPWIDGET_ID_NONE = -1;

    static {
        Check.getInstance().isFalse(AppWidgetUtils.isValidId(APPWIDGET_ID_NONE));
    }

    private final Config mConfig = Config.getInstance();

    private AppWidgetManager mAppWidgetManager;
    private MyAppWidgetHostView mHostView;
    private MyAppWidgetHost mAppWidgetHost;
    private ViewGroup mHostContainer;
    private int mPendingAppWidgetId = -1;

    private SwitchBarPermissible mSwitchPermissible;
    private MenuItem mConfigureMenuItem;
    private MenuItem mTouchableMenuItem;
    private MenuItem mClearMenuItem;
    private Enabler mEnabler;

    private View mEmptyView;

    // Adjust the width & height
    private SeekBar mWidthSeekBar;
    private View mWidthMessageView;
    private SeekBar mHeightSeekBar;
    private View mHeightMessageView;
    private int mMinWidth;
    private int mMinHeight;
    private FloatingActionButton mFab;

    private boolean mHostViewNeedsReInflate;
    private boolean mActivityResumed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (mConfig.isWallpaperShown()) setTheme(R.style.MaterialTheme_WidgetPicker_Wallpaper);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_picker);

        mEmptyView = findViewById(R.id.empty);
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAppWidgetDiscover();
            }
        });

        mAppWidgetManager = AppWidgetManager.getInstance(this);
        mHostContainer = (ViewGroup) findViewById(R.id.appwidget_container);
        mAppWidgetHost = new MyAppWidgetHost(this, HostWidget.HOST_ID);

        initSwitchBar();
        initSeekBars();
    }

    private void initSwitchBar() {
        ActivityBase activity = (ActivityBase) getActivity();
        SwitchBar switchBar = (SwitchBar) findViewById(R.id.switch_bar);
        mSwitchPermissible = new SwitchBarPermissible(activity, switchBar, null);
        mEnabler = new Enabler(this, mConfig, Config.KEY_UI_CUSTOM_WIDGET, mSwitchPermissible);
    }

    private void initSeekBars() {
        // Load the dimensions
        mMinHeight = getResources().getDimensionPixelSize(R.dimen.scene_min_height);
        int hMax = getResources().getDimensionPixelSize(R.dimen.scene_max_height);
        mMinWidth = getResources().getDimensionPixelSize(R.dimen.scene_min_width);
        int wMax = getResources().getDimensionPixelSize(R.dimen.scene_max_width);

        // Init views
        float progress, density = getResources().getDisplayMetrics().density;
        mWidthSeekBar = (SeekBar) findViewById(R.id.appwidget_width_seek_bar);
        mWidthSeekBar.setOnSeekBarChangeListener(this);
        mWidthSeekBar.setMax(wMax - mMinWidth);
        progress = MathUtils.range(mConfig.getCustomWidgetWidthDp() * density, mMinWidth, wMax);
        mWidthSeekBar.setProgress(Math.round(progress) - mMinWidth);
        mWidthMessageView = findViewById(R.id.appwidget_width_label);
        mHeightSeekBar = (SeekBar) findViewById(R.id.appwidget_height_seek_bar);
        mHeightSeekBar.setOnSeekBarChangeListener(this);
        mHeightSeekBar.setMax(hMax - mMinHeight);
        progress = MathUtils.range(mConfig.getCustomWidgetHeightDp() * density, mMinHeight, hMax);
        mHeightSeekBar.setProgress(Math.round(progress) - mMinHeight);
        mHeightMessageView = findViewById(R.id.appwidget_height_label);
    }

    @Override
    public void onStart() {
        super.onStart();
        mAppWidgetHost.startListening();
        updateAppWidgetViewIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mActivityResumed = true;

        mSwitchPermissible.resume();
        mEnabler.start();
        mConfig.registerListener(this);
    }

    @Override
    protected void onPause() {
        mConfig.unregisterListener(this);
        mEnabler.stop();
        mSwitchPermissible.pause();

        mActivityResumed = false;
        super.onPause();
    }

    @Override
    public void onStop() {
        mAppWidgetHost.stopListening();
        mHostViewNeedsReInflate = true;
        // Stopping listening removes all active views from it,
        // so we will have to re-inflate them.
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_PENDING_APPWIDGET_ID, mPendingAppWidgetId);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mPendingAppWidgetId = savedInstanceState.getInt(KEY_PENDING_APPWIDGET_ID, -1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.widget_picker, menu);
        mClearMenuItem = menu.findItem(R.id.clear_action);
        mConfigureMenuItem = menu.findItem(R.id.configure_action);
        mTouchableMenuItem = menu.findItem(R.id.touchable);
        updateConfigureMenuItem();
        updateTouchableMenuItem();
        updateClearMenuItem();
        return true;
    }

    /**
     * Updates the visibility of {@link #mClearMenuItem}.
     */
    private void updateClearMenuItem() {
        if (mClearMenuItem == null) return;
        boolean visible = mHostView != null;
        mClearMenuItem.setVisible(visible);
    }

    /**
     * Updates the visibility of {@link #mConfigureMenuItem}. Shows if
     * the current widget has configure page, hides otherwise.
     */
    private void updateConfigureMenuItem() {
        if (mClearMenuItem == null) return;
        boolean visible = mHostView != null
                && mHostView.getAppWidgetInfo() != null
                && mHostView.getAppWidgetInfo().configure != null;
        mConfigureMenuItem.setVisible(visible);
    }

    /**
     * Updates the visibility of {@link #mTouchableMenuItem}.
     */
    private void updateTouchableMenuItem() {
        if (mTouchableMenuItem == null) return;
        boolean visible = mHostView != null;
        mTouchableMenuItem.setVisible(visible);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_action:
                deleteCurrentAppWidget();
                // Save the current change.
                storeAppWidget(APPWIDGET_ID_NONE);
                break;
            case R.id.configure_action:
                Check.getInstance().isNonNull(mHostView.getAppWidgetInfo().configure);
                startAppWidgetConfigure(mHostView.getAppWidgetInfo(), mHostView.getAppWidgetId());
                break;
            case R.id.touchable:
                mTouchableMenuItem.setChecked(!mTouchableMenuItem.isChecked());
                mConfig
                        .getOption(Config.KEY_UI_CUSTOM_WIDGET_TOUCHABLE)
                        .write(mConfig, this, mTouchableMenuItem.isChecked(), null);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConfigChanged(@NonNull ConfigBase config,
                                @NonNull String key,
                                @NonNull Object value) {
        switch (key) {
            case Config.KEY_UI_CUSTOM_WIDGET_ID:
                final int id = (int) value;
                if (mActivityResumed) {
                    // Automatically turn the switch on/off, causing
                    // terror and murders.
                    mSwitchPermissible.setChecked(AppWidgetUtils.isValidId(id));
                }
                mHostViewNeedsReInflate = true;
                updateAppWidgetViewIfNeeded();
                break;
            case Config.KEY_UI_CUSTOM_WIDGET_WIDTH_DP:
            case Config.KEY_UI_CUSTOM_WIDGET_HEIGHT_DP:
                if (mHostView != null) updateAppWidgetFrameSize();
                break;
            case Config.KEY_UI_CUSTOM_WIDGET_TOUCHABLE:
                if (mHostView != null) updateAppWidgetTouchable();
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (mHostView == null) return;
        updateAppWidgetFrameSize();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) { /* unused */ }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        double density = getResources().getDisplayMetrics().density;
        int widthDp = (int) ((mMinWidth + mWidthSeekBar.getProgress()) / density);
        int heightDp = (int) ((mMinHeight + mHeightSeekBar.getProgress()) / density);
        // Save new width or height to the config. The callback will
        // be ignored.
        mConfig.getOption(Config.KEY_UI_CUSTOM_WIDGET_WIDTH_DP).write(mConfig, this, widthDp, this);
        mConfig.getOption(Config.KEY_UI_CUSTOM_WIDGET_HEIGHT_DP).write(mConfig, this, heightDp, this);
    }

    /**
     * Updates the {@link #mHostView app widget's frame} size based on the
     * current width and height, specified by {@link #mWidthSeekBar}, {@link #mHeightSeekBar}.
     */
    private void updateAppWidgetFrameSize() {
        int width = mMinWidth + mWidthSeekBar.getProgress();
        int height = mMinHeight + mHeightSeekBar.getProgress();
        ViewUtils.setSize(mHostView, width, height);
        mHostView.updateAppWidgetSize(null, width, height, width, height);
    }

    private void updateAppWidgetTouchable() {
        mHostView.setTouchable(mConfig.isCustomWidgetTouchable());
    }

    /**
     * Writes the current widget to settings.
     *
     * @param id the id of app widget to add.
     */
    private void storeAppWidget(int id) {
        mConfig.getOption(Config.KEY_UI_CUSTOM_WIDGET_ID).write(mConfig, this, id, null);
    }

    private void deleteCurrentAppWidgetSafely() {
        if (mHostView != null) deleteCurrentAppWidget();
    }

    private void deleteCurrentAppWidget() {
        mHostContainer.removeView(mHostView);
        mAppWidgetHost.deleteAppWidgetId(mHostView.getAppWidgetId());
        mHostView = null;
    }

    //-- DISCOVER and CONFIGURE -----------------------------------------------

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        int id;
        switch (requestCode) {
            case REQUEST_APPWIDGET_DISCOVER:
                mPendingAppWidgetId = APPWIDGET_ID_NONE;
                if (data == null || data.getExtras() == null) {
                    Check.getInstance().isFalse(resultCode == RESULT_OK);
                    Log.i(TAG, "The intent data is empty.");
                    break;
                }
                id = data.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
                if (resultCode == RESULT_OK && AppWidgetUtils.isValidId(id)) {
                    AppWidgetProviderInfo appWidget = mAppWidgetManager.getAppWidgetInfo(id);
                    if (appWidget == null) {
                        // Clean-up allocated id. This is probably not needed,
                        // cause we don't have an access to the widget.
                        mAppWidgetHost.deleteAppWidgetId(id);
                        // TODO: Toast a user about this incident.
                    } else if (appWidget.configure != null) {
                        mPendingAppWidgetId = id;
                        startAppWidgetConfigure(appWidget, id);
                    } else {
                        // Just apply the widget.
                        storeAppWidget(id);
                    }
                } else {
                    // Clean-up allocated id.
                    mAppWidgetHost.deleteAppWidgetId(id);
                }
                break;
            case REQUEST_APPWIDGET_CONFIGURE:
                if (!AppWidgetUtils.isValidId(id = mPendingAppWidgetId)) break;
                mPendingAppWidgetId = APPWIDGET_ID_NONE;

                if (resultCode == RESULT_OK) {
                    storeAppWidget(id);
                } else {
                    // Clean-up allocated id.
                    mAppWidgetHost.deleteAppWidgetId(id);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Launches the {@link AppWidgetManager#ACTION_APPWIDGET_PICK App Widget picker}
     * and wait for the result.
     */
    private void startAppWidgetDiscover() {
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetHost.allocateAppWidgetId());
        /*
         * This avoids a bug in the com.android.settings.AppWidgetPickActivity, which is used
         * to select widgets. This just adds empty extras to the intent, avoiding the bug. See
         * more: http://code.google.com/p/android/issues/detail?id=4272
         */
        ArrayList<Parcelable> customInfo = new ArrayList<>(0);
        intent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
        ArrayList<Parcelable> customExtras = new ArrayList<>(0);
        intent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);
        startActivityForResult(intent, REQUEST_APPWIDGET_DISCOVER);
    }

    @SuppressWarnings("NewApi")
    private void startAppWidgetConfigure(@NonNull AppWidgetProviderInfo appWidget, int id) {
        try {
            if (Device.hasLollipopApi()) {
                mAppWidgetHost.startAppWidgetConfigureActivityForResult(
                        this, id, 0, REQUEST_APPWIDGET_CONFIGURE, null);
            } else {
                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                intent.setComponent(appWidget.configure);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
                startActivityForResult(intent, REQUEST_APPWIDGET_CONFIGURE);
            }
        } catch (Exception e) {
            if (Device.isLge()) {
                CharSequence message = Html.fromHtml(getString(R.string.error_dialog_custom_widget_lg));
                new MaterialDialog.Builder(this)
                        .title(R.string.error_dialog_title)
                        .content(message)
                        .positiveText(android.R.string.ok)
                        .show();
            } else ToastUtils.showLong(this, R.string.error_dialog_title);
        }
    }

    //-- UPDATE USER INTERFACE ------------------------------------------------

    private void updateAppWidgetViewIfNeeded() {
        int id = mConfig.getCustomWidgetId();
        if (!AppWidgetUtils.isValidId(id)) {
            mHostViewNeedsReInflate = false;
            // Remove current app widget.
            deleteCurrentAppWidgetSafely();

            // Update views
            mFab.show();
            mEmptyView.setVisibility(View.VISIBLE);
            mWidthSeekBar.setVisibility(View.GONE);
            mWidthMessageView.setVisibility(View.GONE);
            mHeightSeekBar.setVisibility(View.GONE);
            mHeightMessageView.setVisibility(View.GONE);
            // Update menu
            updateConfigureMenuItem();
            updateTouchableMenuItem();
            updateClearMenuItem();
            return;
        }

        if (mHostView == null) {
            mHostView = new MyAppWidgetHostView(this);
            mHostView.setBackgroundResource(R.drawable.bg_appwidget_preview);
            updateAppWidgetTouchable();

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_HORIZONTAL);
            mHostContainer.addView(mHostView, lp);
        } else if (!mHostViewNeedsReInflate && mHostView.getAppWidgetId() == id) return;
        AppWidgetProviderInfo appWidget = mAppWidgetManager.getAppWidgetInfo(id);
        mAppWidgetHost.updateView(this, id, appWidget, mHostView);
        mHostViewNeedsReInflate = false;
        updateAppWidgetFrameSize();

        // Update views
        mFab.hide(hasWindowFocus());
        mEmptyView.setVisibility(View.GONE);
        mWidthSeekBar.setVisibility(View.VISIBLE);
        mWidthMessageView.setVisibility(View.VISIBLE);
        mHeightSeekBar.setVisibility(View.VISIBLE);
        mHeightMessageView.setVisibility(View.VISIBLE);
        // Update menu
        mHostView.post(new Runnable() {
            @Override
            public void run() {
                updateConfigureMenuItem();
                updateTouchableMenuItem();
                updateClearMenuItem();
            }
        });
    }

}
