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
package com.achep.acdisplay.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.achep.acdisplay.App;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.acdisplay.services.KeyguardService;
import com.achep.acdisplay.services.SensorsDumpService;
import com.achep.acdisplay.services.activemode.ActiveModeService;
import com.achep.acdisplay.services.headsup.HeadsUpService;
import com.achep.acdisplay.utils.ToastUtils;

/**
 * Created by Artem on 11.03.14.
 */
public class Receiver extends BroadcastReceiver {

    private static final String TAG = "Receiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED:
            case Intent.ACTION_POWER_CONNECTED:
            case Intent.ACTION_POWER_DISCONNECTED:
                ActiveModeService.handleState(context);
                KeyguardService.handleState(context);
                HeadsUpService.handleState(context);
                SensorsDumpService.handleState(context);
                break;
            case App.ACTION_ENABLE:
            case App.ACTION_DISABLE:
            case App.ACTION_TOGGLE:
                Config config = Config.getInstance();
                switch (action) {
                    case App.ACTION_ENABLE:
                        Log.i(TAG, "Enabling AcDisplay from broadcast receiver.");
                        setAcDisplayEnabled(context, config, true);
                        break;
                    case App.ACTION_DISABLE:
                        Log.i(TAG, "Disabling AcDisplay from broadcast receiver.");
                        setAcDisplayEnabled(context, config, false);
                        break;
                    default:
                        Log.i(TAG, "Toggling from broadcast receiver.");
                        setAcDisplayEnabled(context, config, !config.isEnabled());
                        break;
                }
                break;
        }
    }

    /**
     * Tries to {@link Config#setEnabled(Context, boolean, Config.OnConfigChangedListener) enable / disable }
     * AcDisplay and shows toast message about the result.
     *
     * @param enable {@code true} to enable AcDisplay, {@code false} to disable.
     */
    private void setAcDisplayEnabled(Context context, Config config, boolean enable) {
        config.setEnabled(context, enable, null);
        ToastUtils.showLong(context, config.isEnabled()
                ? R.string.remote_enable_acdisplay
                : R.string.remote_disable_acdisplay);
    }
}