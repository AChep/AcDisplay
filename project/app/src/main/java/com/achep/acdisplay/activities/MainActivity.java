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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;

import com.achep.acdisplay.App;
import com.achep.acdisplay.Build;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.Device;
import com.achep.acdisplay.DialogHelper;
import com.achep.acdisplay.R;
import com.achep.acdisplay.acdisplay.AcDisplayActivity;
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

    private static final boolean DEBUG_DIALOGS = Build.DEBUG && false;
    private static final boolean DEBUG_COMPAT_TOAST = Build.DEBUG && false;
    private static final boolean DEBUG_HEADS_UP = Build.DEBUG && false;

    private Switch mSwitch;
    private ImageView mSwitchAlertView;

    private Config mConfig;
    private boolean mBroadcasting;

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
        getActionBar().setCustomView(R.layout.layout_ab_switch2);
        mSwitchAlertView = (ImageView) getActionBar().getCustomView().findViewById(R.id.icon);
        mSwitchAlertView.setImageResource(R.drawable.ic_action_warning);
        mSwitch = (Switch) getActionBar().getCustomView().findViewById(R.id.switch_);
        mSwitch.setChecked(mConfig.isEnabled());
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                updateSendTestNotificationMenuItem();
                if (mBroadcasting) {
                    return;
                }

                Activity activity = MainActivity.this;

                if (b && !AccessUtils.hasAllRights(activity)) {
                    // Reset compound button and update
                    // testing menu item.
                    compoundButton.setChecked(false);
                    updateSendTestNotificationMenuItem();

                    // Show permission dialog.
                    DialogHelper.showSetupPermissionsDialog(activity);
                } else {
                    boolean successfully = mConfig.setEnabled(activity, b, MainActivity.this);
                    if (!successfully) {

                        // Setting option failed, so we need to
                        // sync switch with config.
                        compoundButton.setChecked(mConfig.isEnabled());
                        updateSendTestNotificationMenuItem();
                    } else if (b && (!Device.hasKitKatApi() || DEBUG_COMPAT_TOAST)) {
                        String formatter = getString(R.string.compat_formatter);

                        SpannableStringBuilder builder = new SpannableStringBuilder();
                        builder.append(getString(R.string.compat_title));
                        builder.setSpan(new StyleSpan(Typeface.BOLD), 0, builder.length(), 0);
                        builder.append('\n');

                        if (!Device.hasJellyBeanMR2Api() || DEBUG_COMPAT_TOAST) {
                            builder.append(String.format(formatter,
                                    getString(R.string.compat_notifications)));
                        }

                        if (!Device.hasKitKatApi() || DEBUG_COMPAT_TOAST) {
                            builder.append(String.format(formatter,
                                    getString(R.string.compat_immersive_mode)));
                            builder.append(String.format(formatter,
                                    getString(R.string.compat_music_widget)));
                        }

                        builder.delete(builder.length() - 1, builder.length());
                        ToastUtils.showLong(activity, builder);
                    }
                }
            }
        });

        try {
            PackageInfo pi = getPackageManager().getPackageInfo(PackageUtils.getName(this), 0);
            Config.Triggers triggers = mConfig.getTriggers();

            int oldVersionCode = triggers.getPreviousVersion();
            if (oldVersionCode < pi.versionCode || DEBUG_DIALOGS) {
                triggers.setPreviousVersion(this, pi.versionCode, null);

                if (oldVersionCode < 15 /* v2.0- */ || DEBUG_DIALOGS) {
                    showAlertSpeech();
                }

                if (oldVersionCode < 20 /* v2.2.1- */ || DEBUG_DIALOGS) {
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

        boolean hasAllRights = AccessUtils.hasAllRights(this);
        mSwitch.setChecked(mSwitch.isChecked() && hasAllRights);
        ViewUtils.setVisible(mSwitchAlertView, !hasAllRights);
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
            if (!DEBUG_HEADS_UP) {
                startActivity(new Intent(this, AcDisplayActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        .putExtra(KeyguardActivity.EXTRA_FINISH_ON_SCREEN_OFF,
                                !mConfig.isKeyguardEnabled()));

            }
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
                .setPriority(Notification.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setStyle(new Notification.BigTextStyle()
                        .bigText(getString(R.string.test_notification_message_large)))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(notificationId, builder.build());
    }

}
