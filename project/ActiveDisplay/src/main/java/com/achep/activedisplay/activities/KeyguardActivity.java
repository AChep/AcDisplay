/*
 * Copyright (C) 2013 AChep@xda <artemchep@gmail.com>
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
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;

import com.achep.activedisplay.Timeout;

/**
 * Created by Artem on 23.02.14.
 */
public abstract class KeyguardActivity extends Activity implements Timeout.OnTimeoutEventListener {

    private boolean mLocking;
    private boolean mUnlocking;

    private Timeout mTimeout = new Timeout();

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        mLocking = false;
        mUnlocking = false;
        mTimeout.addListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTimeout.release();

        mUnlocking = false;
        mLocking = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mTimeout.lock();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTimeout.removeListener(this);
    }

    @Override
    public void onTimeoutEvent(int event) {
        switch (event) {
            case Timeout.EVENT_TIMEOUT:
                lock();
                break;
        }
    }

    public void lock() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        try {
            dpm.lockNow();

            mTimeout.lock();
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
    public void unlock(final Runnable runnable) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        mTimeout.lock();
        mUnlocking = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (runnable != null) runnable.run();

                finish();
                overridePendingTransition(0, 0);
            }
        }, 100 /* We need this delay to get new flags applied */);
    }

    public final boolean isLocking() {
        return mLocking;
    }

    public final boolean isUnlocking() {
        return mUnlocking;
    }

    public final Timeout getTimeout() {
        return mTimeout;
    }

    @Override
    public void onBackPressed() { /* override back button */ }

}
