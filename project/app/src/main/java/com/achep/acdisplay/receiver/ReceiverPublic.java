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
import com.achep.base.utils.ToastUtils;

/**
 * Created by Artem on 11.03.14.
 */
public class ReceiverPublic extends BroadcastReceiver {

    private static final String TAG = "PublicReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Config config = Config.getInstance();
        String action = intent.getAction();
        switch (action) {
            case App.ACTION_ENABLE:
                Log.i(TAG, "Enabling AcDisplay by intent. " + intent);
                setAcDisplayEnabled(context, config, true);
                break;
            case App.ACTION_DISABLE:
                Log.i(TAG, "Disabling AcDisplay by intent. " + intent);
                setAcDisplayEnabled(context, config, false);
                break;
            case App.ACTION_TOGGLE:
                Log.i(TAG, "Toggling AcDisplay by intent. " + intent);
                setAcDisplayEnabled(context, config, !config.isEnabled());
                break;
            // Active mode
            case App.ACTION_ACTIVE_MODE_ENABLE:
                Log.i(TAG, "Enabling Active mode by intent. " + intent);
                setActiveModeEnabled(context, config, true);
                break;
            case App.ACTION_ACTIVE_MODE_DISABLE:
                Log.i(TAG, "Disabling Active mode by intent. " + intent);
                setActiveModeEnabled(context, config, false);
                break;
            case App.ACTION_ACTIVE_MODE_TOGGLE:
                Log.i(TAG, "Toggling Active mode by intent. " + intent);
                setActiveModeEnabled(context, config, !config.isEnabled());
                break;
        }
    }

    /**
     * Tries to {@link com.achep.acdisplay.Config#setEnabled(android.content.Context, boolean, com.achep.acdisplay.Config.OnConfigChangedListener) enable / disable }
     * AcDisplay and shows toast message about the result.
     *
     * @param enable {@code true} to enable AcDisplay, {@code false} to disable.
     */
    private void setAcDisplayEnabled(Context context, Config config, boolean enable) {
        enable &= App.getAccessManager().getMasterPermissions().isGranted();
        config.setEnabled(context, enable, null);
        ToastUtils.showLong(context, enable
                ? R.string.remote_enable_acdisplay
                : R.string.remote_disable_acdisplay);
    }

    private void setActiveModeEnabled(Context context, Config config, boolean enable) {
        config.getMap().get(Config.KEY_ACTIVE_MODE).write(config, context, enable, null);
        ToastUtils.showLong(context, enable
                ? R.string.remote_enable_active_mode
                : R.string.remote_disable_active_mode);
    }

}