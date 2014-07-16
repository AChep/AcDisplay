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
package com.achep.acdisplay.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.achep.acdisplay.App;
import com.achep.acdisplay.Build;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.DialogHelper;
import com.achep.acdisplay.R;
import com.achep.acdisplay.acdisplay.AcDisplayActivity;
import com.achep.acdisplay.admin.AdminReceiver;
import com.achep.acdisplay.blacklist.activities.BlacklistActivity;
import com.achep.acdisplay.fragments.AboutDialog;
import com.achep.acdisplay.iab.DonationFragment;
import com.achep.acdisplay.settings.Settings;
import com.achep.acdisplay.utils.AccessUtils;
import com.achep.acdisplay.utils.PackageUtils;
import com.achep.acdisplay.utils.ToastUtils;
import com.achep.acdisplay.utils.ViewUtils;

/**
 * Created by Artem on 21.01.14.
 */
public class MainActivity extends Activity implements Config.OnConfigChangedListener {

    private static final String TAG = "MainActivity";

    private Switch mSwitch;
    private Config mConfig;
    private boolean mBroadcasting;

    private ViewGroup mAccessWarningPanel;
    private Button mAccessAllowNotification;
    private Button mAccessAllowDeviceAdmin;

    private MenuItem mSendTestNotificationMenuItem;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Transfer in-app-billing service's events to
        // its fragment.
        if (requestCode == DonationFragment.RC_REQUEST) {
            FragmentManager fm = getFragmentManager();
            Fragment fragment = fm.findFragmentByTag(DialogHelper.TAG_FRAGMENT_DONATION);
            if (fragment instanceof DonationFragment) {
                fragment.onActivityResult(requestCode, resultCode, data);
                return;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mConfig = Config.getInstance();
        mConfig.registerListener(this);

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
            PackageInfo pi = getPackageManager().getPackageInfo(PackageUtils.getName(this), 0);
            Config.Triggers triggers = mConfig.getTriggers();

            int oldVersionCode = triggers.getPreviousVersion();
            if (oldVersionCode < pi.versionCode) {
                triggers.setPreviousVersion(this, pi.versionCode, null);

                if (oldVersionCode < 15 /* v2.0- */) {
                    showAlertSpeech();
                }

                if (oldVersionCode < 20 /* v2.2.1- */) {
                    showAlertWelcome();
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.wtf(TAG, "Failed to find my PackageInfo.");
        }
    }

    private void showAlertSpeech() {
        showSimpleDialog(
                R.drawable.ic_dialog_me,
                getString(R.string.speech_title),
                Html.fromHtml(getString(R.string.speech_message)));
    }

    private void showAlertWelcome() {
        showSimpleDialog(
                R.mipmap.ic_launcher,
                AboutDialog.getVersionName(this),
                Html.fromHtml(getString(R.string.news_message)));
    }

    private void showSimpleDialog(int iconRes, CharSequence title, CharSequence message) {
        new DialogHelper.Builder(this)
                .setIcon(iconRes)
                .setTitle(title)
                .setMessage(message)
                .wrap()
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAccessPanel();
    }

    /**
     * Inflates access panel's layout and sets it up.
     *
     * @see #updateAccessPanel()
     */
    private void initAccessPanel() {
        mAccessWarningPanel = (ViewGroup) ((ViewStub) findViewById(R.id.access)).inflate();
        mAccessAllowNotification = (Button) mAccessWarningPanel.findViewById(R.id.access_notification);
        mAccessAllowNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = MainActivity.this;
                Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    ToastUtils.showLong(context, R.string.access_notifications_grant_manually);
                    Log.e(TAG, "Notification listeners activity not found.");
                }
            }
        });
        mAccessAllowDeviceAdmin = (Button) mAccessWarningPanel.findViewById(R.id.access_device_admin);
        mAccessAllowDeviceAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = MainActivity.this;
                ComponentName admin = new ComponentName(context, AdminReceiver.class);
                Intent intent = new Intent()
                        .setAction(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                        .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);

                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    ToastUtils.showLong(context, R.string.access_device_admin_grant_manually);
                    Log.e(TAG, "Device admins activity not found.");
                }
            }
        });
    }

    private void updateAccessPanel() {
        boolean showDeviceAdminBtn = !AccessUtils.isDeviceAdminAccessGranted(this);
        boolean showNotifiesBtn = !AccessUtils.isNotificationAccessGranted(this);

        if (showDeviceAdminBtn || showNotifiesBtn || mAccessWarningPanel != null) {
            if (mAccessWarningPanel == null) initAccessPanel();

            ViewUtils.setVisible(mAccessAllowDeviceAdmin, showDeviceAdminBtn);
            ViewUtils.setVisible(mAccessAllowNotification, showNotifiesBtn);
            ViewUtils.setVisible(mAccessWarningPanel, showDeviceAdminBtn || showNotifiesBtn);
        }

        mSwitch.setEnabled(!showDeviceAdminBtn && !showNotifiesBtn);
        mSwitch.setChecked(mSwitch.isEnabled() && mSwitch.isChecked());
        updateSendTestNotificationMenuItem();
    }

    private void updateSendTestNotificationMenuItem() {
        if (mSendTestNotificationMenuItem != null) {
            mSendTestNotificationMenuItem.setVisible(mSwitch.isChecked());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mConfig.unregisterListener(this);
    }

    @Override
    public void onConfigChanged(Config config, String key, Object value) {
        if (mBroadcasting) {
            return;
        }

        switch (key) {
            case Config.KEY_ENABLED:
                mBroadcasting = true;
                mSwitch.setChecked((Boolean) value);
                mBroadcasting = false;
                break;
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
                startAcDisplayTest(Build.DEBUG);
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

    /**
     * Turns screen off and sends a test notification.
     *
     * @param fake {@code true} if it simply starts {@link AcDisplayActivity},
     *             {@code false} if it uses notification
     */
    private void startAcDisplayTest(boolean fake) {
        if (fake) {
            sendTestNotification();
            startActivity(new Intent(this, AcDisplayActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_NO_ANIMATION));
            return;
        }

        int delay = getResources().getInteger(R.integer.config_test_notification_delay);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Test notification.");
        wakeLock.acquire(delay);

        try {
            // Go sleep
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            dpm.lockNow();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendTestNotification();
                }
            }, delay);
        } catch (SecurityException e) {
            Log.wtf(TAG, "Failed to turn screen off");

            wakeLock.release();
        }
    }

    private void sendTestNotification() {
        int notificationId = App.ID_NOTIFY_TEST;
        PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this,
                notificationId, new Intent(MainActivity.this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(MainActivity.this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.test_notification_message))
                .setContentIntent(pendingIntent)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setSmallIcon(R.drawable.stat_test)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        Notification.BigTextStyle builderBigText = new Notification.BigTextStyle(builder)
                .bigText(getString(R.string.test_notification_message_large));

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(notificationId, builderBigText.build());
    }

}
