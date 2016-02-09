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

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;

import com.achep.acdisplay.App;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.acdisplay.notifications.NotificationHelper;
import com.achep.acdisplay.ui.DialogHelper;
import com.achep.acdisplay.ui.activities.base.BaseActivity;
import com.achep.acdisplay.ui.activities.settings.Settings2;
import com.achep.base.content.ConfigBase;
import com.achep.base.permissions.Permission;
import com.achep.base.ui.SwitchBarPermissible;
import com.achep.base.ui.widgets.SwitchBar;
import com.achep.base.utils.PackageUtils;

/**
 * @author Artem Chepurnoy
 */
public class MainActivity extends BaseActivity implements ConfigBase.OnConfigChangedListener {

    private static final String TAG = "MainActivity";

    private SwitchBarPermissible mSwitchPermission;
    private MenuItem mSendTestNotificationMenuItem;

    private boolean mBroadcasting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestCheckout();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        getConfig().registerListener(this);

        Permission[] permissions = App.getAccessManager().getMasterPermissions().permissions;
        SwitchBar switchBar = (SwitchBar) findViewById(R.id.switch_bar);
        mSwitchPermission = new SwitchBarPermissible(this, switchBar, permissions);
        mSwitchPermission.setChecked(getConfig().isEnabled());
        mSwitchPermission.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                updateSendTestNotificationMenuItem();
                if (mBroadcasting) {
                    return;
                }

                AppCompatActivity context = MainActivity.this;
                getConfig().setEnabled(context, checked, MainActivity.this);
            }
        });

        Config.Triggers triggers = getConfig().getTriggers();
        if (!triggers.isDonationAsked() && triggers.getLaunchCount() >= 15) {
            triggers.setDonationAsked(this, true, this);
            DialogHelper.showCryDialog(this);
        }

        handleAppUpgrade();
    }

    private void handleAppUpgrade() {
        PackageInfo pi;
        try {
            pi = getPackageManager().getPackageInfo(PackageUtils.getName(this), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.wtf(TAG, "Failed to find my PackageInfo.");
            return;
        }

        Config.Triggers triggers = getConfig().getTriggers();

        final int versionCode = pi.versionCode;
        final int versionCodeOld = triggers.getPreviousVersion();

        if (versionCodeOld < versionCode) {
            triggers.setPreviousVersion(this, pi.versionCode, null);

            if (versionCodeOld <= 34 /* version 3.0.2 */) {
                DialogHelper.showCompatDialog(MainActivity.this);
            }

            if (versionCodeOld <= 76 /* before 4.0.0 */) {
                startAcDisplayIntro();
            }
        }
    }

    private void updateSendTestNotificationMenuItem() {
        if (mSendTestNotificationMenuItem == null) return;
        mSendTestNotificationMenuItem.setVisible(mSwitchPermission.isChecked());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSwitchPermission.resume();
    }

    @Override
    protected void onPause() {
        mSwitchPermission.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getConfig().unregisterListener(this);
    }

    @Override
    public void onConfigChanged(@NonNull ConfigBase config,
                                @NonNull String key,
                                @NonNull Object value) {
        switch (key) {
            case Config.KEY_ENABLED:
                mBroadcasting = true;
                mSwitchPermission.setChecked((Boolean) value);
                mBroadcasting = false;
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        mSendTestNotificationMenuItem = menu.findItem(R.id.test_action);
        updateSendTestNotificationMenuItem();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings_action:
                startActivity(new Intent(this, Settings2.class));
                break;
            case R.id.test_action:
                startAcDisplayTest(true);
                break;
            case R.id.intro_action:
                startAcDisplayIntro();
                break;

            //-- DIALOGS ------------------------------------------------------

            case R.id.donate_action:
                DialogHelper.showDonateDialog(this);
                break;
            case R.id.feedback_action:
                DialogHelper.showFeedbackDialog(this);
                break;
            case R.id.about_action:
                DialogHelper.showAboutDialog(this);
                break;
            case R.id.help_action:
                DialogHelper.showHelpDialog(this);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void startAcDisplayIntro() {
        startActivity(new Intent(this, IntroActivity.class));
    }

    /**
     * Turns screen off and sends a test notification.
     *
     * @param cheat {@code true} if it simply starts {@link AcDisplayActivity},
     *              {@code false} if it turns device off and then uses notification
     *              to wake it up.
     */
    private void startAcDisplayTest(boolean cheat) {
        if (cheat) {
            startActivity(new Intent(this, AcDisplayActivity.class));
            NotificationHelper.sendNotification(this, App.ID_NOTIFY_TEST);
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

                private final Context context = getApplicationContext();

                @Override
                public void run() {
                    NotificationHelper.sendNotification(context, App.ID_NOTIFY_TEST);
                }
            }, delay);
        } catch (SecurityException e) {
            Log.wtf(TAG, "Failed to turn screen off!");
            wakeLock.release();
        }
    }
}