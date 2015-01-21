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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.achep.acdisplay.App;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.DialogHelper;
import com.achep.acdisplay.R;
import com.achep.acdisplay.utils.AccessUtils;
import com.achep.base.content.ConfigBase;
import com.achep.base.ui.activities.ActivityBase;
import com.achep.base.ui.widgets.SwitchBar;
import com.achep.base.utils.PackageUtils;
import com.achep.base.utils.ViewUtils;

/**
 * Created by Artem on 21.01.14.
 */
public class MainActivity extends ActivityBase implements ConfigBase.OnConfigChangedListener {

    private static final String TAG = "MainActivity";

    private static void sendTestNotification(@NonNull Context context) {
        final int id = App.ID_NOTIFY_TEST;
        final Resources res = context.getResources();

        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                id, new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.BigTextStyle bts = new Notification.BigTextStyle()
                .bigText(res.getString(R.string.notification_test_message_large));
        Notification.Builder builder = new Notification.Builder(context)
                .setContentTitle(res.getString(R.string.app_name))
                .setContentText(res.getString(R.string.notification_test_message))
                .setContentIntent(pendingIntent)
                .setLargeIcon(BitmapFactory.decodeResource(res, R.mipmap.ic_launcher))
                .setSmallIcon(R.drawable.stat_acdisplay)
                .setAutoCancel(true)
                .setStyle(bts)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(id, builder.build());
    }

    private SwitchBar mSwitchBar;
    private ImageView mSwitchAlertView;
    private MenuItem mSendTestNotificationMenuItem;
    private Config mConfig;

    private boolean mBroadcasting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestCheckout();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mConfig = Config.getInstance();
        mConfig.registerListener(this);

        mSwitchBar = (SwitchBar) findViewById(R.id.switch_bar);
        mSwitchAlertView = (ImageView) mSwitchBar.findViewById(R.id.icon);
        mSwitchAlertView.setImageResource(R.drawable.ic_action_warning_amber);
        mSwitchBar.setChecked(mConfig.isEnabled());
        mSwitchBar.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                updateSendTestNotificationMenuItem();
                if (mBroadcasting) {
                    return;
                }

                ActionBarActivity context = MainActivity.this;
                if (checked && !AccessUtils.hasAllRights(context)) {
                    // Reset compound button and update
                    // testing menu item.
                    compoundButton.setChecked(false);
                    updateSendTestNotificationMenuItem();

                    // Show permission dialog.
                    DialogHelper.showSetupPermissionsDialog(context);
                } else if (!mConfig.setEnabled(context, checked, MainActivity.this)) {

                    // Setting option failed, so we need to
                    // sync switch with config.
                    compoundButton.setChecked(mConfig.isEnabled());
                    updateSendTestNotificationMenuItem();
                }
            }
        });

        Config.Triggers triggers = mConfig.getTriggers();
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

        Config.Triggers triggers = mConfig.getTriggers();

        final int versionCode = pi.versionCode;
        final int versionCodeOld = triggers.getPreviousVersion();

        if (versionCodeOld < versionCode) {
            triggers.setPreviousVersion(this, pi.versionCode, null);

            if (versionCodeOld <= 34 /* version 3.0.2 */) {
                DialogHelper.showCompatDialog(MainActivity.this);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean hasAllRights = AccessUtils.hasAllRights(this);
        mSwitchBar.setChecked(mSwitchBar.isChecked() && hasAllRights);
        ViewUtils.setVisible(mSwitchAlertView, !hasAllRights);
    }

    private void updateSendTestNotificationMenuItem() {
        if (mSendTestNotificationMenuItem != null) {
            mSendTestNotificationMenuItem.setVisible(mSwitchBar.isChecked());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mConfig.unregisterListener(this);
    }

    @Override
    public void onConfigChanged(@NonNull ConfigBase config,
                                @NonNull String key,
                                @NonNull Object value) {
        switch (key) {
            case Config.KEY_ENABLED:
                mBroadcasting = true;
                mSwitchBar.setChecked((Boolean) value);
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
            sendTestNotification(this);
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
                    sendTestNotification(context);
                }
            }, delay);
        } catch (SecurityException e) {
            Log.wtf(TAG, "Failed to turn screen off!");
            wakeLock.release();
        }
    }

}
