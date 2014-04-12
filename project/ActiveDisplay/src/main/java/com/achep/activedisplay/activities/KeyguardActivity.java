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
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;

/**
 * Created by Artem on 23.02.14.
 */
public abstract class KeyguardActivity extends Activity {

    public static final String EXTRA_TURN_SCREEN_ON = "turn_screen_on";
    public static final String EXTRA_FINISH_ON_SCREEN_OFF = "finish_on_screen_off";

    private BroadcastReceiver mScreenOffReceiver;

    private boolean mLocking;
    private boolean mUnlocking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean finishOnScreenOff = false;
        int windowExtraFlags = 0;

        Intent intent = getIntent();
        if (intent != null) {
            finishOnScreenOff = intent.getBooleanExtra(EXTRA_FINISH_ON_SCREEN_OFF, false);
            if (intent.getBooleanExtra(EXTRA_TURN_SCREEN_ON, false)) {
                windowExtraFlags |= WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
            }
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES
                | windowExtraFlags);

        mLocking = false;
        mUnlocking = false;

        if (finishOnScreenOff) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            registerReceiver((mScreenOffReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(final Context context, Intent intent) {
                    finish();
                }
            }), filter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        mUnlocking = false;
        mLocking = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mScreenOffReceiver != null) {
            unregisterReceiver(mScreenOffReceiver);
        }
    }

    public void lock() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        try {
            dpm.lockNow();
            mLocking = true;
        } catch (SecurityException e) {
            mLocking = false;
        }
    }

    /**
     * Unlocks device and runs {@link Runnable runnable} when unlocked.
     *
     * @param runnable may be null
     */
    public void unlock(final Runnable runnable, final boolean finish) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        mUnlocking = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (runnable != null) runnable.run();
                if (finish) {
                    overridePendingTransition(0, 0);
                    finish();
                }
            }
        }, 120 /* We need this delay to get new flags applied. */);
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
