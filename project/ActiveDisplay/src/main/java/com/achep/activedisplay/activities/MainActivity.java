/*
 * Copyright (C) 2013-2014 AChep@xda <artemchep@gmail.com>
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
package com.achep.activedisplay.activities;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.achep.activedisplay.Config;
import com.achep.activedisplay.DialogHelper;
import com.achep.activedisplay.NotificationIds;
import com.achep.activedisplay.R;
import com.achep.activedisplay.admin.AdminReceiver;
import com.achep.activedisplay.utils.AccessUtils;
import com.achep.activedisplay.utils.ViewUtils;

/**
 * Created by Artem on 21.01.14.
 */
public class MainActivity extends Activity implements Config.OnConfigChangedListener {

    private static final String TAG = "MainActivity";

    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS =
            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    private static final int SLEEP_SEND_NOTIFICATION_DELAY = 2000;

    private Switch mSwitch;
    private Config mConfig;
    private boolean mBroadcasting;

    private ViewGroup mAccessWarningPanel;
    private View mAccessAllowNotification;
    private View mAccessAllowDeviceAdmin;

    private MenuItem mTestMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mConfig = Config.getInstance(this);
        mConfig.addOnConfigChangedListener(this);

        getActionBar().setDisplayShowCustomEnabled(true);
        getActionBar().setCustomView(R.layout.layout_ab_switch);
        mSwitch = (Switch) getActionBar().getCustomView().findViewById(R.id.swatch);
        mSwitch.setChecked(mConfig.getActiveDisplayEnabled());
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                updateTestMenuItem();
                if (mBroadcasting) {
                    return;
                }

                boolean successfully = mConfig.setActiveDisplayEnabled(
                        MainActivity.this, b, MainActivity.this);

                if (!successfully) {
                    mBroadcasting = true;
                    compoundButton.setChecked(!b);
                    mBroadcasting = false;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAccessPanel();
    }

    private void initAccessPanel() {
        mAccessWarningPanel = (ViewGroup) ((ViewStub) findViewById(R.id.access)).inflate();
        mAccessAllowNotification = mAccessWarningPanel.findViewById(R.id.access_notification);
        mAccessAllowNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS);
                startActivity(intent);
            }
        });
        mAccessAllowDeviceAdmin = mAccessWarningPanel.findViewById(R.id.access_device_admin);
        mAccessAllowDeviceAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ComponentName admin = new ComponentName(MainActivity.this, AdminReceiver.class);
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                        .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
                startActivity(intent);
            }
        });
    }

    private void updateAccessPanel() {
        boolean showDeviceAdminBtn = !AccessUtils.isDeviceAdminEnabled(this);
        boolean showNotifiesBtn = !AccessUtils.isNotificationAccessEnabled(this);

        if (showDeviceAdminBtn || showNotifiesBtn || mAccessWarningPanel != null) {
            if (mAccessWarningPanel == null) initAccessPanel();

            ViewUtils.setVisible(mAccessAllowDeviceAdmin, showDeviceAdminBtn);
            ViewUtils.setVisible(mAccessAllowNotification, showNotifiesBtn);
            ViewUtils.setVisible(mAccessWarningPanel, showDeviceAdminBtn || showNotifiesBtn);
        }

        mSwitch.setEnabled(!showDeviceAdminBtn && !showNotifiesBtn);
        updateTestMenuItem();
    }

    private void updateTestMenuItem() {
        if (mTestMenuItem == null) {
            return;
        }

        mTestMenuItem.setVisible(mSwitch.isEnabled() && mSwitch.isChecked());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mConfig.removeOnConfigChangedListener(this);
    }

    @Override
    public void onConfigChanged(Config config, String key, Object value) {
        if (key.equals(Config.KEY_AD_ENABLED)) {
            if (!mBroadcasting) {
                mBroadcasting = true;
                mSwitch.setChecked((Boolean) value);
                mBroadcasting = false;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        mTestMenuItem = menu.findItem(R.id.action_test);
        updateTestMenuItem();
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.action_test:
                try {
                    // Go sleep
                    DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                    dpm.lockNow();

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Notification n = new Notification.Builder(MainActivity.this)
                                    .setContentTitle(getString(R.string.test_notification_title))
                                    .setContentText(getString(R.string.test_notification_message))
                                    .setSmallIcon(R.drawable.stat_test)
                                    .build();

                            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                            nm.notify(NotificationIds.TEST_NOTIFICATION, n);
                        }
                    }, SLEEP_SEND_NOTIFICATION_DELAY);
                } catch (SecurityException e) {
                    Log.e(TAG, "Failed to turn screen off");
                }
                break;
            case R.id.action_donate:
                DialogHelper.showDonateDialog(this);
                break;
            case R.id.action_about:
                DialogHelper.showAboutDialog(this);
                break;
            default:
                return super.onMenuItemSelected(featureId, item);
        }
        return true;
    }
}
