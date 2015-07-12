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
package com.achep.acdisplay;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.achep.acdisplay.blacklist.Blacklist;
import com.achep.acdisplay.notifications.NotificationPresenter;
import com.achep.acdisplay.notifications.OpenNotification;
import com.achep.acdisplay.services.activemode.sensors.ProximitySensor;
import com.achep.acdisplay.ui.activities.AcDisplayActivity;
import com.achep.acdisplay.ui.activities.KeyguardActivity;
import com.achep.base.utils.Operator;
import com.achep.base.utils.power.PowerUtils;
import com.achep.base.utils.zen.ZenConsts;
import com.achep.base.utils.zen.ZenUtils;

import static com.achep.base.Build.DEBUG;

/**
 * Created by Artem on 07.03.14.
 */
public class Presenter implements NotificationPresenter.OnNotificationPostedListener {

    private static final String TAG = "AcDisplayPresenter";
    private static final String WAKE_LOCK_TAG = "AcDisplay launcher.";

    public static final int STATE_CREATED = 5;
    public static final int STATE_STARTED = 4;
    public static final int STATE_RESUMED = 3;
    public static final int STATE_PAUSED = 2;
    public static final int STATE_STOPPED = 1;
    public static final int STATE_DESTROYED = 0;

    private static Presenter sPresenter;

    @Nullable
    private AcDisplayActivity mActivity;
    private int mActivityState = STATE_DESTROYED;

    public static synchronized Presenter getInstance() {
        if (sPresenter == null) {
            sPresenter = new Presenter();
        }
        return sPresenter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNotificationPosted(@NonNull Context context,
                                     @NonNull OpenNotification n, int flags) {
        boolean silent = Operator.bitAnd(flags, NotificationPresenter.FLAG_SILENCE);
        if (!silent) tryStartGuiCauseNotification(context, n);
    }

    //-- START-UP -------------------------------------------------------------

    /**
     * Checks if the current state of device is correct for waking-up cause
     * of notification, or no.
     */
    public boolean checkNotification(@NonNull Context context, @NonNull OpenNotification n) {
        NotificationPresenter np = NotificationPresenter.getInstance();

        if (np.isTestNotification(context, n)) {
            return true;  // force test notification to be shown
        }

        if (ProximitySensor.isNear()) {
            // Don't display while device is face down.
            return false;
        }

        Config config = Config.getInstance();
        if (!config.isEnabled() || !config.isNotifyWakingUp()
                // Inactive time
                || config.isInactiveTimeEnabled()
                && InactiveTimeHelper.isInactiveTime(config)
                // Only while charging
                || config.isEnabledOnlyWhileCharging()
                && !PowerUtils.isPlugged(context)) {
            // Don't turn screen on due to user settings.
            return false;
        }

        // Respect the device's zen mode.
        final int zenMode = ZenUtils.getValue(context);
        if (DEBUG) Log.d(TAG, "The current ZEN mode is " + ZenUtils.zenModeToString(zenMode));
        switch (zenMode) {
            case ZenConsts.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
                if (n.getNotification().priority >= Notification.PRIORITY_HIGH) {
                    break;
                }
            case ZenConsts.ZEN_MODE_NO_INTERRUPTIONS:
                return false;
        }

        String packageName = n.getPackageName();
        Blacklist blacklist = Blacklist.getInstance();
        return !blacklist.getAppConfig(packageName).isRestricted();
    }

    /**
     * Checks if the screen if off and call state is idle.
     */
    public boolean checkBasics(@NonNull Context context) {
        TelephonyManager ts = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return !PowerUtils.isScreenOn(context) && ts.getCallState() == TelephonyManager.CALL_STATE_IDLE;
    }

    public boolean tryStartGuiCauseNotification(
            @NonNull Context context,
            @NonNull OpenNotification n) {
        final int hash = n.hashCode();
        return checkNotification(context, n) && checkBasics(context) && start(context, hash);
    }

    public boolean tryStartGuiCauseSensor(@NonNull Context context) {
        return checkBasics(context) && start(context);
    }

    //-- START-UP -------------------------------------------------------------

    public boolean start(@NonNull Context context) {
        return start(context, 0);
    }

    public boolean start(@NonNull Context context, int notification) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        // Wake up from possible deep sleep.
        //
        //           )))
        //          (((
        //        +-----+
        //        |     |]
        //        `-----'    Good morning! ^-^
        //      ___________
        //      `---------'
        pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).acquire(1000);

        //kill();
        context.startActivity(new Intent(context, AcDisplayActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_NO_USER_ACTION
                        | Intent.FLAG_ACTIVITY_NO_ANIMATION
                        | Intent.FLAG_FROM_BACKGROUND)
                .putExtra(KeyguardActivity.EXTRA_CAUSE, notification)
                .putExtra(KeyguardActivity.EXTRA_TURN_SCREEN_ON, true));

        if (DEBUG) Log.i(TAG, "Launching AcDisplay activity; state=" + mActivityState);
        return true;
    }

    public boolean tryStartGuiCauseKeyguard(@NonNull Context context) {
        if ((mActivityState == STATE_RESUMED
                || mActivityState == STATE_PAUSED)
                && !isFinishing()) {
            Log.i(TAG, "Passed the AcDisplay keyguard launch cause it\'s showing");
            return true;
        }

        context.startActivity(new Intent(context, AcDisplayActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_NO_ANIMATION));

        if (DEBUG) Log.i(TAG, "Launching[kg] AcDisplay activity; state=" + mActivityState);
        return true;
    }

    //-- ACTIVITY -------------------------------------------------------------

    public void onCreate(@NonNull AcDisplayActivity activity) {
        mActivity = activity;
        mActivityState = STATE_CREATED;
    }

    public void onStart() {
        mActivityState = STATE_STARTED;
    }

    public void onResume() {
        mActivityState = STATE_RESUMED;
    }

    public void onPause() {
        mActivityState = STATE_PAUSED;
    }

    public void onStop() {
        mActivityState = STATE_STOPPED;
    }

    public void onDestroy() {
        mActivity = null;
        mActivityState = STATE_DESTROYED;
    }

    /**
     * @return {@code true} if the activity is present and finishing,
     * {@code false} otherwise.
     */
    private boolean isFinishing() {
        return mActivity != null && mActivity.isFinishing();
    }

    public boolean isLocked() {
        return mActivityState == STATE_RESUMED;
    }

    //-- OTHER ----------------------------------------------------------------

    public void kill() {
        if (mActivity != null) mActivity.finish();
    }

}
