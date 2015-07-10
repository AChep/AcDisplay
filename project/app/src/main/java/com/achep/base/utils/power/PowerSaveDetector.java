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
package com.achep.base.utils.power;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.NonNull;

import com.achep.base.Device;

/**
 * Created by Artem Chepurnoy on 22.11.2014.
 */
public abstract class PowerSaveDetector {

    @NonNull
    public static PowerSaveDetector newInstance(@NonNull Context context) {
        return Device.hasLollipopApi()
                ? new PowerSaveLollipop(context)
                : new PowerSaveCompat(context);
    }

    @NonNull
    protected final Context mContext;
    protected boolean mPowerSaveMode;

    private PowerSaveDetector(@NonNull Context context) {
        mContext = context;
    }

    public abstract void start();

    public abstract void stop();

    /**
     * Returns {@code true} if the device is currently in power save mode.
     * When in this mode, applications should reduce their functionality
     * in order to conserve battery as much as possible.
     *
     * @return {@code true} if the device is currently in power save mode, {@code false} otherwise.
     */
    public boolean isPowerSaveMode() {
        return mPowerSaveMode;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static class PowerSaveLollipop extends PowerSaveDetector {

        private final PowerManager mPowerManager;
        private final BroadcastReceiver mReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        switch (intent.getAction()) {
                            case PowerManager.ACTION_POWER_SAVE_MODE_CHANGED:
                                mPowerSaveMode = mPowerManager.isPowerSaveMode();
                                break;
                        }
                    }
                };

        public PowerSaveLollipop(@NonNull Context context) {
            super(context);
            mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        }

        @Override
        public void start() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
            mContext.registerReceiver(mReceiver, intentFilter);
            mPowerSaveMode = mPowerManager.isPowerSaveMode();
        }

        @Override
        public void stop() {
            mContext.unregisterReceiver(mReceiver);
        }

    }

    // TODO: Support some other vendor's pre-Lollipop software.
    private static class PowerSaveCompat extends PowerSaveDetector {

        public PowerSaveCompat(@NonNull Context context) {
            super(context);
        }

        @Override
        public void start() {

        }

        @Override
        public void stop() {

        }

    }

}
