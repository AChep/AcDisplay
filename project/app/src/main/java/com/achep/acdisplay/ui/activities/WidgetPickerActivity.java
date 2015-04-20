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
package com.achep.acdisplay.ui.activities;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.transition.TransitionManager;
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
import com.achep.acdisplay.ui.components.HostWidget;
import com.achep.base.Device;
import com.achep.base.content.ConfigBase;
import com.achep.base.tests.Check;
import com.achep.base.ui.SwitchBarPermissible;
import com.achep.base.ui.activities.ActivityBase;
import com.achep.base.ui.preferences.Enabler;
import com.achep.base.ui.widgets.SwitchBar;
import com.achep.base.utils.MathUtils;
import com.achep.base.utils.ViewUtils;
import com.melnykov.fab.FloatingActionButton;

/**
 * An activity for setting the custom App Widget, tweaking it
 * and licking.
 *
 * @author Artem Chepurnoy
 */
public class WidgetPickerActivity extends ActivityBase implements
        Config.OnConfigChangedListener,
        SeekBar.OnSeekBarChangeListener {

    private static final String KEY_PENDING_APPWIDGET_ID = "achep::pending_app_widget_key";

    private static final int REQUEST_APPWIDGET_DISCOVER = 1;
    private static final int REQUEST_APPWIDGET_CONFIGURE = 2;

    private final Config mConfig = Config.getInstance();

    private AppWidgetManager mAppWidgetManager;
    private AppWidgetHostView mHostView;
    private AppWidgetHost mHost;
    private ViewGroup mHostContainer;
    private int mPendingAppWidgetId = -1;

    private SwitchBarPermissible mSwitchPermissible;
    private MenuItem mConfigureMenuItem;
    private MenuItem mClearMenuItem;
    private Enabler mEnabler;
    private ViewGroup mContent;

    private View mEmptyView;

    // Adjust the width & height
    private SeekBar mWidthSeekBar;
    private View mWidthMessageView;
    private SeekBar mHeightSeekBar;
    private View mHeightMessageView;
    private int mMinWidth;
    private int mMinHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (mConfig.isWallpaperShown()) setTheme(R.style.MaterialTheme_WidgetPicker_Wallpaper);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_picker);

        mContent = (ViewGroup) findViewById(android.R.id.content);
        mEmptyView = findViewById(R.id.empty);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAppWidgetDiscover();
            }
        });

        mAppWidgetManager = AppWidgetManager.getInstance(this);
        mHostContainer = (ViewGroup) findViewById(R.id.appwidget_container);
        mHost = new MyAppWidgetHost(this, HostWidget.HOST_ID);

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

    private void onAppWidgetRemoved() {
        final int id = mConfig.getCustomWidgetId();
        Check.getInstance().isFalse(id >= 0);
        // Remove current app widget.
        deleteAppWidgetSafely();

        if (Device.hasKitKatApi() && !isPowerSaveMode() && mContent.isLaidOut()) {
            TransitionManager.beginDelayedTransition(mContent);
        }

        // Update views
        mEmptyView.setVisibility(View.VISIBLE);
        mWidthSeekBar.setVisibility(View.GONE);
        mWidthMessageView.setVisibility(View.GONE);
        mHeightSeekBar.setVisibility(View.GONE);
        mHeightMessageView.setVisibility(View.GONE);
        // Update menu
        updateConfigureMenuItem();
        updateClearMenuItem();
    }

    private void onAppWidgetUpdated(int id) {
        Check.getInstance().isTrue(id >= 0);

        // Create the App Widget and get its remote
        // views.
        AppWidgetProviderInfo appWidget = mAppWidgetManager.getAppWidgetInfo(id);
        deleteAppWidgetSafely();
        mHostView = mHost.createView(this, id, appWidget);
        mHostView.setBackgroundResource(R.drawable.bg_appwidget_preview);

        // Add it to the container.
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL);
        mHostContainer.addView(mHostView, lp);
        updateAppWidgetFrameSize();

        // Update views
        mEmptyView.setVisibility(View.GONE);
        mWidthSeekBar.setVisibility(View.VISIBLE);
        mWidthMessageView.setVisibility(View.VISIBLE);
        mHeightSeekBar.setVisibility(View.VISIBLE);
        mHeightMessageView.setVisibility(View.VISIBLE);
        // Update menu
        updateConfigureMenuItem();
        updateClearMenuItem();
    }

    private void updateUi() {
        final int id = mConfig.getCustomWidgetId();
        if (mHostView != null && mHostView.getAppWidgetId() == id) return; // do nothing
        if (id < 0) {
            onAppWidgetRemoved();
        } else {
            onAppWidgetUpdated(id);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mHost.startListening();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSwitchPermissible.resume();
        mEnabler.start();
        mConfig.registerListener(this);

        updateUi();
    }

    @Override
    protected void onPause() {
        mConfig.unregisterListener(this);
        mEnabler.stop();
        mSwitchPermissible.pause();
        super.onPause();
    }

    @Override
    public void onStop() {
        mHost.stopListening();
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
        updateConfigureMenuItem();
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
        boolean visible = mHostView != null && mHostView.getAppWidgetInfo().configure != null;
        mConfigureMenuItem.setVisible(visible);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_action:
                deleteAppWidget();
                // Save the current change.
                applyAppWidget(-1);
                break;
            case R.id.configure_action:
                Check.getInstance().isNonNull(mHostView.getAppWidgetInfo().configure);
                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                intent.setComponent(mHostView.getAppWidgetInfo().configure);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mHostView.getAppWidgetId());
                startActivity(intent);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        int id;
        switch (requestCode) {
            case REQUEST_APPWIDGET_DISCOVER:
                id = data.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
                if (resultCode == RESULT_OK && id >= 0) {
                    AppWidgetProviderInfo appWidget = mAppWidgetManager.getAppWidgetInfo(id);
                    if (appWidget.configure != null) {
                        startAppWidgetConfigure(appWidget, id);
                    } else {
                        // Just apply the widget.
                        applyAppWidget(id);
                    }
                } else {
                    // Clean-up allocated id.
                    mHost.deleteAppWidgetId(id);
                }
                break;
            case REQUEST_APPWIDGET_CONFIGURE:
                id = mPendingAppWidgetId;
                mPendingAppWidgetId = -1;
                if (id < 0) break;
                if (resultCode == RESULT_OK) {
                    applyAppWidget(id);
                } else {
                    // Clean-up allocated id.
                    mHost.deleteAppWidgetId(id);
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
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mHost.allocateAppWidgetId());
        startActivityForResult(intent, REQUEST_APPWIDGET_DISCOVER);
    }

    private void startAppWidgetConfigure(@NonNull AppWidgetProviderInfo appWidget, int id) {
        mPendingAppWidgetId = id;
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
        intent.setComponent(appWidget.configure);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        startActivityForResult(intent, REQUEST_APPWIDGET_CONFIGURE);
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
                updateUi();
                break;
            case Config.KEY_UI_CUSTOM_WIDGET_WIDTH_DP:
            case Config.KEY_UI_CUSTOM_WIDGET_HEIGHT_DP:
                if (mHostView != null) updateAppWidgetFrameSize();
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
    }

    /**
     * Writes the current widget to settings.
     *
     * @param id the id of app widget to add.
     */
    private void applyAppWidget(int id) {
        mConfig.getOption(Config.KEY_UI_CUSTOM_WIDGET_ID).write(mConfig, this, id, null);
    }

    private void deleteAppWidgetSafely() {
        if (mHostView != null) deleteAppWidget();
    }

    private void deleteAppWidget() {
        mHostContainer.removeView(mHostView);
        mHost.deleteAppWidgetId(mHostView.getAppWidgetId());
        mHostView = null;
    }

}
