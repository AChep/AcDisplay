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
package com.achep.activedisplay.activities;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
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
import com.achep.activedisplay.Project;
import com.achep.activedisplay.R;
import com.achep.activedisplay.admin.AdminReceiver;
import com.achep.activedisplay.blacklist.activities.BlacklistActivity;
import com.achep.activedisplay.services.SendNotificationService;
import com.achep.activedisplay.settings.Settings;
import com.achep.activedisplay.utils.AccessUtils;
import com.achep.activedisplay.utils.ViewUtils;

/**
 * Created by Artem on 21.01.14.
 */
public class MainActivity extends Activity implements Config.OnConfigChangedListener {

    private static final String TAG = "MainActivity";

    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS =
            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    private static final int SLEEP_SEND_NOTIFICATION_DELAY = 3000;

    private Switch mSwitch;
    private Config mConfig;
    private boolean mBroadcasting;

    private ViewGroup mAccessWarningPanel;
    private View mAccessAllowNotification;
    private View mAccessAllowDeviceAdmin;

    private MenuItem mSendTestNotificationMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mConfig = Config.getInstance();
        mConfig.addOnConfigChangedListener(this);

        getActionBar().setDisplayShowCustomEnabled(true);
        getActionBar().setCustomView(R.layout.layout_ab_switch);
        mSwitch = (Switch) getActionBar().getCustomView().findViewById(R.id.switch_);
        mSwitch.setChecked(mConfig.isEnabled());
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                updateSendTestNotificationMenuItem();
                if (mBroadcasting) {
                    return;
                }

                boolean successfully = mConfig.setEnabled(
                        MainActivity.this, b, MainActivity.this);

                if (!successfully) {
                    mBroadcasting = true;
                    compoundButton.setChecked(!b);
                    mBroadcasting = false;
                }
            }
        });


        try {
            PackageInfo pi = getPackageManager().getPackageInfo(Project.getPackageName(this), 0);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

            int oldVersionCode = prefs.getInt("previous_version_code", 0);
            if (oldVersionCode < pi.versionCode) {
                prefs.edit().putInt("previous_version_code", pi.versionCode).apply();

                // Show the warning message for Paranoid Android users.
                if (Build.DISPLAY.startsWith("pa_") && oldVersionCode == 0) {
                    CharSequence messageText = Html.fromHtml(getString(R.string.pa_message));
                    new DialogHelper.Builder(this)
                            .setTitle(R.string.pa_title)
                            .setMessage(messageText)
                            .wrap()
                            .setPositiveButton(android.R.string.ok, null)
                            .create()
                            .show();
                }

                if (oldVersionCode < 15 /* v2.0- */) {
                    CharSequence messageText = Html.fromHtml(getString(R.string.speech_message));
                    new DialogHelper.Builder(this)
                            .setTitle(R.string.speech_title)
                            .setMessage(messageText)
                            .wrap()
                            .setPositiveButton(android.R.string.ok, null)
                            .create()
                            .show();
                }

                if (oldVersionCode < 20 /* v2.2.1- */) {
                    DialogHelper.showNewsDialog(this);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.wtf(TAG, "Failed to find my PackageInfo.");
        }
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
        updateSendTestNotificationMenuItem();
    }

    private void updateSendTestNotificationMenuItem() {
        if (mSendTestNotificationMenuItem == null) {
            return;
        }

        mSendTestNotificationMenuItem.setVisible(mSwitch.isEnabled() && mSwitch.isChecked());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mConfig.removeOnConfigChangedListener(this);
    }

    @Override
    public void onConfigChanged(Config config, String key, Object value) {
        if (key.equals(Config.KEY_ENABLED)) {
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

        mSendTestNotificationMenuItem = menu.findItem(R.id.action_test);
        updateSendTestNotificationMenuItem();
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, Settings.class));
                break;
            case R.id.action_blacklist:
                startActivity(new Intent(this, BlacklistActivity.class));
                break;
            case R.id.action_test:
                // startActivity(new Intent(this, AcDisplayActivity.class));
                Intent contentIntent = new Intent(this, MainActivity.class);
                Intent notificationIntent = SendNotificationService
                        .createNotificationIntent(this, getString(R.string.app_name),
                                getString(R.string.test_notification_message),
                                NotificationIds.TEST_NOTIFICATION,
                                R.drawable.stat_test,
                                R.mipmap.ic_launcher,
                                Notification.PRIORITY_DEFAULT,
                                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                                PendingIntent.getService(this, 0, contentIntent,
                                        PendingIntent.FLAG_UPDATE_CURRENT)
                        );
                PendingIntent pi = SendNotificationService.notify(this,
                        notificationIntent, SLEEP_SEND_NOTIFICATION_DELAY);

                try {
                    // Go sleep
                    DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                    dpm.lockNow();
                } catch (SecurityException e) {
                    SendNotificationService.cancel(this, pi);
                    Log.e(TAG, "Failed to turn screen off");
                }
                break;
            case R.id.action_donate:
                DialogHelper.showDonateDialog(this);
                break;
            case R.id.action_feedback:
                DialogHelper.showFeedbackDialog(this);
                break;
            case R.id.action_about:
                DialogHelper.showAboutDialog(this);
                break;
            case R.id.action_help:
                DialogHelper.showHelpDialog(this);
                break;
            default:
                return super.onMenuItemSelected(featureId, item);
        }
        return true;
    }
}
