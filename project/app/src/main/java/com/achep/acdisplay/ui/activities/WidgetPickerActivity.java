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

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.acdisplay.appwidget.MyAppWidgetHost;
import com.achep.acdisplay.ui.components.HostWidget;
import com.achep.acdisplay.ui.components.Widget;
import com.achep.base.Device;
import com.achep.base.content.ConfigBase;
import com.achep.base.ui.SwitchBarPermissible;
import com.achep.base.ui.activities.ActivityBase;
import com.achep.base.ui.preferences.Enabler;
import com.achep.base.ui.widgets.SwitchBar;

/**
 * An activity for setting the custom App Widget, tweaking it
 * and licking.
 *
 * @author Artem Chepurnoy
 */
public class WidgetPickerActivity extends ActivityBase implements
        Widget.Callback, Config.OnConfigChangedListener {

    private static final String KEY_PENDING_APPWIDGET_ID = "achep::pending_app_widget_key";

    private static final int REQUEST_APPWIDGET_DISCOVER = 1;
    private static final int REQUEST_APPWIDGET_CONFIGURE = 2;

    private final Config mConfig = Config.getInstance();

    private AppWidgetManager mAppWidgetManager;
    private AppWidgetHostView mHostView;
    private AppWidgetHost mHost;
    private ViewGroup mHostContainer;
    private int mPendingAppWidgetId = -1;
    private boolean mHasAppWidget;

    private SwitchBarPermissible mSwitchPermissible;
    private MenuItem mClearMenuItem;
    private Enabler mEnabler;
    private View mEmptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (mConfig.isWallpaperShown()) setTheme(R.style.MaterialTheme_WidgetPicker_Wallpaper);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_widget_picker);
        ActivityBase activity = (ActivityBase) getActivity();
        SwitchBar switchBar = (SwitchBar) findViewById(R.id.switch_bar);
        mSwitchPermissible = new SwitchBarPermissible(activity, switchBar, null);
        mEmptyView = findViewById(R.id.empty);
        mEnabler = new Enabler(this, mConfig, Config.KEY_UI_CUSTOM_WIDGET, mSwitchPermissible);
        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAppWidgetDiscover();
            }
        });

        mAppWidgetManager = AppWidgetManager.getInstance(this);
        mHostContainer = (ViewGroup) findViewById(R.id.container);
        mHost = new MyAppWidgetHost(this, HostWidget.HOST_ID);

        updateUi();
    }

    private void updateUi() {
        int id = mConfig.getCustomWidgetId();
        if (id < 0) {
            removeAppWidgetSafely();
            mHasAppWidget = false;

            if (Device.hasKitKatApi() && !isPowerSaveMode() && mHostContainer.isLaidOut()) {
                TransitionManager.beginDelayedTransition(mHostContainer);
            }

            mEmptyView.setVisibility(View.VISIBLE);
            updateClearMenuItem();
            return;
        }

        mHasAppWidget = true;
        mEmptyView.setVisibility(View.GONE);
        updateClearMenuItem();

        // Create the App Widget and get its remote
        // views.
        AppWidgetProviderInfo appWidget = mAppWidgetManager.getAppWidgetInfo(id);
        if (mHostView != null) {
            mHostContainer.removeView(mHostView);
            if (mHostView.getAppWidgetId() != id) {
                mHost.deleteAppWidgetId(mHostView.getAppWidgetId());
            }
        }
        mHostView = mHost.createView(this, id, appWidget);

        // Add it to the container.
        final int width = getResources().getDimensionPixelSize(R.dimen.scene_max_width);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL);
        mHostContainer.addView(mHostView, lp);
    }

    @Override
    public void onStart() {
        super.onStart();
        mHost.startListening();
        mConfig.registerListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSwitchPermissible.resume();
        mEnabler.start();
    }

    @Override
    protected void onPause() {
        mEnabler.stop();
        mSwitchPermissible.pause();
        super.onPause();
    }

    @Override
    public void onStop() {
        mConfig.unregisterListener(this);
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
        updateClearMenuItem();
        return true;
    }

    /**
     * Updates the visibility of {@link #mClearMenuItem}. Shows when
     * {@link #mHasAppWidget} is {@code true}, hides otherwise.
     */
    private void updateClearMenuItem() {
        if (mClearMenuItem != null) mClearMenuItem.setVisible(mHasAppWidget);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_action:
                removeAppWidget();
                // Save the current change.
                applyAppWidget(-1);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void requestBackgroundUpdate(@NonNull Widget widget) { /* do nothing */ }

    @Override
    public void requestTimeoutRestart(@NonNull Widget widget) { /* do nothing */ }

    @Override
    public void requestWidgetStick(@NonNull Widget widget) { /* do nothing */ }

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
                        applyAppWidget(id);
                    }
                } else {
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
     * Writes the current widget to settings.
     *
     * @param id the id of app widget to add.
     */
    private void applyAppWidget(int id) {
        mConfig.getOption(Config.KEY_UI_CUSTOM_WIDGET_ID).write(mConfig, this, id, null);
    }

    private void removeAppWidgetSafely() {
        if (mHostView != null) removeAppWidget();
    }

    private void removeAppWidget() {
        mHostContainer.removeView(mHostView);
        mHost.deleteAppWidgetId(mHostView.getAppWidgetId());
        mHostView = null;
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
        }
    }
}
