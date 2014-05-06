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
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;

import com.achep.activedisplay.Project;

/**
 * Created by Artem on 23.02.14.
 */
public abstract class KeyguardActivity extends Activity {

    private static final String TAG = "KeyguardActivity";

    public static final String EXTRA_TURN_SCREEN_ON = "turn_screen_on";
    public static final String EXTRA_FINISH_ON_SCREEN_OFF = "finish_on_screen_off";

    public static final String INTENT_EAT_HOME_PRESS_START = "com.achep.acdisplay.EAT_HOME_PRESS_START";
    public static final String INTENT_EAT_HOME_PRESS_STOP = "com.achep.acdisplay.EAT_HOME_PRESS_STOP";

    private BroadcastReceiver mScreenOffReceiver;

    private boolean mLocking;
    private boolean mUnlocking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean finishOnScreenOff = false;
        int windowFlags = 0;

        Intent intent = getIntent();
        if (intent != null) {
            finishOnScreenOff = intent.getBooleanExtra(EXTRA_FINISH_ON_SCREEN_OFF, false);
            if (intent.getBooleanExtra(EXTRA_TURN_SCREEN_ON, false)) {
                windowFlags |= WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
            }
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES
                | windowFlags);

        mLocking = false;
        mUnlocking = false;

        if (finishOnScreenOff) {
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
            registerReceiver((mScreenOffReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(final Context context, Intent intent) {
                    finish();
                }
            }), intentFilter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        mUnlocking = false;
        mLocking = false;

        // Notifies Xposed module to start ignoring
        // home button press.
        Intent intent = new Intent(INTENT_EAT_HOME_PRESS_START);
        sendBroadcast(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Notifies Xposed module to stop ignoring
        // home button press.
        Intent intent = new Intent(INTENT_EAT_HOME_PRESS_STOP);
        sendBroadcast(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mScreenOffReceiver != null) {
            unregisterReceiver(mScreenOffReceiver);
        }
    }

    /**
     * Turns screen off.
     *
     * @return True if successful, False otherwise.
     */
    public boolean lock() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        try {
            dpm.lockNow();
            mLocking = true;
        } catch (SecurityException e) {
            mLocking = false;
        }
        return mLocking;
    }

    /**
     * Unlocks device and runs {@link Runnable runnable} when unlocked.
     *
     * @param runnable may be null
     */
    public void unlock(final Runnable runnable, final boolean finish) {
        if (Project.DEBUG) Log.d(TAG, "Unlocking with params: finish=" + finish);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        // If keyguard is disabled no need to make
        // a delay between calling this method and
        // unlocking.
        // Otherwise we need this delay to get new
        // flags applied.
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        int delay = km.isKeyguardLocked() ? 120 : 0;

        mUnlocking = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (runnable != null) runnable.run();
                if (finish) {
                    finish();
                    overridePendingTransition(0, 0);
                }
            }
        }, delay);
    }

    public final boolean isLocking() {
        return mLocking;
    }

    public final boolean isUnlocking() {
        return mUnlocking;
    }

    @Override
    public void onBackPressed() { /* override back button */ }

}
