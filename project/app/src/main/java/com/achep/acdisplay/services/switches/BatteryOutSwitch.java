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
import android.support.annotation.NonNull;

import com.achep.acdisplay.services.Switch;
import com.achep.base.content.ConfigBase;
import com.achep.base.utils.power.PowerUtils;

/**
 * Prevents {@link com.achep.acdisplay.services.SwitchService} from working
 * while the battery level is {@link #BATTERY_LOW_LEVEL low}.
 *
 * @author Artem Chepurnoy
 */
public final class BatteryOutSwitch extends Switch.Optional {

    private static final int BATTERY_LOW_LEVEL = 15; // 15 %

    private boolean mBatteryPlugged;
    private int mBatteryLevel;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_BATTERY_CHANGED:
                    mBatteryLevel = PowerUtils.getBatteryLevel(intent);
                    mBatteryPlugged = PowerUtils.isPlugged(intent);

                    // Update the state
                    if (isActiveInternal()) {
                        requestActiveInternal();
                    } else {
                        requestInactiveInternal();
                    }
                    break;
            }
        }

    };

    public BatteryOutSwitch(
            @NonNull Context context,
            @NonNull Callback callback,
            @NonNull ConfigBase.Option option, boolean isOptionInverted) {
        super(context, callback, option, isOptionInverted);
    }

    @Override
    public void onCreate() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = getContext().registerReceiver(mReceiver, intentFilter);
        mBatteryLevel = PowerUtils.getBatteryLevel(intent);
        mBatteryPlugged = PowerUtils.isPlugged(intent);
    }

    @Override
    public void onDestroy() {
        getContext().unregisterReceiver(mReceiver);
    }

    @Override
    public boolean isActiveInternal() {
        return mBatteryPlugged || mBatteryLevel > BATTERY_LOW_LEVEL;
    }

}
