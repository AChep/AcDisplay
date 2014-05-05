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

package com.achep.activedisplay.receiver;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.achep.activedisplay.Config;
import com.achep.activedisplay.R;
import com.achep.activedisplay.utils.ToastUtils;

/**
 * Created by Artem on 26.03.2014.
 */
public class ReceiverActivity extends Activity {

    private static final String TAG = "PublicReceiverActivity";

    public static final String HOST_ENABLE = "enable";
    public static final String HOST_DISABLE = "disable";
    public static final String HOST_TOGGLE = "toggle";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null) {
            handleIntent(intent);
        } else {
            Log.i(TAG, "Got an empty launch intent.");
        }

        finish();
    }

    private void handleIntent(Intent intent) {
        String host = LocalReceiverActivity.extractHost(intent);
        if (host == null) {
            Log.i(TAG, "Got an empty launch intent.");
            return;
        }

        Config config;
        switch (host) {
            case HOST_ENABLE:
                Log.i(TAG, "Enabling active display by remote intent.");
                ToastUtils.showLong(this, getString(R.string.remote_enable_acdisplay));

                Config.getInstance().setEnabled(this, true, null);
                break;

            case HOST_DISABLE:
                Log.i(TAG, "Disabling active display by remote intent.");
                ToastUtils.showLong(this, getString(R.string.remote_disable_acdisplay));

                Config.getInstance().setEnabled(this, false, null);
                break;

            case HOST_TOGGLE:
                Log.i(TAG, "Toggling active display by remote intent.");
                config = Config.getInstance();
                config.setEnabled(this, !config.isEnabled(), null);

                ToastUtils.showLong(this, getString(config.isEnabled()
                        ? R.string.remote_enable_acdisplay
                        : R.string.remote_disable_acdisplay));
                break;

            default:
                Log.i(TAG, "Got an unknown intent: " + host);
                break;
        }
    }

}
