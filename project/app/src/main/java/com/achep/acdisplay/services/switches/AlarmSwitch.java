/*
 * Copyright (C) 2015 AChep@xda <artemchep@gmail.com>
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
package com.achep.acdisplay.services.switches;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.achep.acdisplay.services.Switch;

/**
 * Prevents {@link com.achep.acdisplay.services.SwitchService} from working
 * while an alarm app is alarming.
 *
 * @author Artem Chepurnoy
 */
public final class AlarmSwitch extends Switch {

    private static final String TAG = "AlarmSwitch";

    public static final String ALARM_ALERT = "ALARM_ALERT";
    public static final String ALARM_DISMISS = "ALARM_DISMISS";
    public static final String ALARM_SNOOZE = "ALARM_SNOOZE";
    public static final String ALARM_DONE = "ALARM_DONE";

    private boolean mActive;
    private long mAlarmingTimestamp;

    @NonNull
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.contains(ALARM_ALERT)) {
                // Hide the keyguard
                mActive = false;
                mAlarmingTimestamp = SystemClock.elapsedRealtime();
                requestInactive();
            } else if (action.contains(ALARM_DISMISS)
                    || action.contains(ALARM_SNOOZE)
                    || action.contains(ALARM_DONE)) {
                // Show the keyguard
                mActive = true;
                requestActive();
            } else if (mActive) {
                // Get mad
                Log.w(TAG, "Received an unknown intent=" + intent.getAction()
                        + " re-enabling the switch.");
                // Show the keyguard
                mActive = true;
                requestActive();
            }
        }
    };

    public AlarmSwitch(@NonNull Context context, @NonNull Callback callback) {
        super(context, callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        mActive = true;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Alarms.STANDARD_ALARM_ALERT_ACTION);
        intentFilter.addAction(Alarms.STANDARD_ALARM_DISMISS_ACTION);
        intentFilter.addAction(Alarms.STANDARD_ALARM_SNOOZE_ACTION);
        intentFilter.addAction(Alarms.STANDARD_ALARM_DONE_ACTION);
        for (String alarm : Alarms.ALARMS) {
            intentFilter.addAction(alarm + "." + ALARM_ALERT);
            intentFilter.addAction(alarm + "." + ALARM_DISMISS);
            intentFilter.addAction(alarm + "." + ALARM_SNOOZE);
            intentFilter.addAction(alarm + "." + ALARM_DONE);
        }
        intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
        getContext().registerReceiver(mReceiver, intentFilter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        getContext().unregisterReceiver(mReceiver);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isActive() {
        // Check how old the alarm intent is. This is needed because
        // we can't be sure that alarm app will broadcast any of
        // DISMISS, SNOOZE, DONE intents.
        final long now = SystemClock.elapsedRealtime();
        final boolean timedOut = now - mAlarmingTimestamp > 1000 * 60 * 5;
        Log.i(TAG, "Checking if AlarmSwitch is active: "
                + "active=" + mActive + ", "
                + "timed_out=" + timedOut + ", ");
        return mActive || timedOut;
    }

    /**
     * @author Artem Chepurnoy
     */
    private static class Alarms {

        // Modern Android app
        public static final String STANDARD_ALARM_PACKAGE = "com.android.deskclock";
        public static final String STANDARD_ALARM_ALERT_ACTION =
                "com.android.deskclock.ALARM_ALERT";
        public static final String STANDARD_ALARM_SNOOZE_ACTION =
                "com.android.deskclock.ALARM_SNOOZE";
        public static final String STANDARD_ALARM_DISMISS_ACTION =
                "com.android.deskclock.ALARM_DISMISS";
        public static final String STANDARD_ALARM_DONE_ACTION =
                "com.android.deskclock.ALARM_DONE";

        // Deprecated Android app
        public static final String STANDARD_OLD_ALARM_PACKAGE = "com.android.alarmclock";

        //-- MANUFACTURERS --------------------------------------------------------

        // Samsung
        public static final String SAMSUNG_ALARM_PACKAGE = "com.samsung.sec.android.clockpackage.alarm";
        public static final String SAMSUNG_ALARM_PACKAGE_2 = "com.sec.android.app.clockpackage.alarm";
        // HTC
        public static final String HTC_ALARM_ALERT_PACKAGE = "com.htc.android.worldclock";
        public static final String HTC_ONE_ALARM_ALERT_PACKAGE = "com.htc.android";
        // Sony
        public static final String SONY_ALARM_PACKAGE = "com.sonyericsson.alarm";
        // ZTE
        public static final String ZTE_ALARM_PACKAGE = "zte.com.cn.alarmclock";
        // Motorola
        public static final String MOTO_ALARM_PACKAGE = "com.motorola.blur.alarmclock";
        // LG
        public static final String LG_ALARM_PACKAGE = "com.lge.alarm.alarmclocknew";

        //-- THIRD PARTY ----------------------------------------------------------

        // Gentle Alarm
        public static final String GENTLE_ALARM_PACKAGE = "com.mobitobi.android.gentlealarm";
        // Sleep As Android
        public static final String SLEEPASDROID_ALARM_PACKAGE = "com.urbandroid.sleep.alarmclock";
        // Alarmdroid (1.13.2)
        public static final String ALARMDROID_ALARM_PACKAGE = "com.splunchy.android.alarmclock";
        // Timely
        public static final String TIMELY_ALARM_PACKAGE = "ch.bitspin.timely";

        //-- ALL-IN-ONE -----------------------------------------------------------

        @NonNull
        public static final String ALARMS[] = {
                STANDARD_ALARM_PACKAGE,
                STANDARD_OLD_ALARM_PACKAGE,
                SAMSUNG_ALARM_PACKAGE,
                SAMSUNG_ALARM_PACKAGE_2,
                HTC_ALARM_ALERT_PACKAGE,
                HTC_ONE_ALARM_ALERT_PACKAGE,
                SONY_ALARM_PACKAGE,
                ZTE_ALARM_PACKAGE,
                MOTO_ALARM_PACKAGE,
                LG_ALARM_PACKAGE,
                GENTLE_ALARM_PACKAGE,
                SLEEPASDROID_ALARM_PACKAGE,
                ALARMDROID_ALARM_PACKAGE,
                TIMELY_ALARM_PACKAGE
        };

    }

}
