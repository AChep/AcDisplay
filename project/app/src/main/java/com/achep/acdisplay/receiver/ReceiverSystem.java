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
import android.os.Handler;

import com.achep.acdisplay.services.KeyguardService;
import com.achep.acdisplay.services.SensorsDumpService;
import com.achep.acdisplay.services.activemode.ActiveModeService;

/**
 * Created by Artem on 11.03.14.
 */
public class ReceiverSystem extends BroadcastReceiver {

    private static final String TAG = "Receiver";

    private Handler mHandler = new Handler();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String action = intent.getAction();
                switch (action) {
                    case Intent.ACTION_BOOT_COMPLETED:
                    case Intent.ACTION_POWER_CONNECTED:
                    case Intent.ACTION_POWER_DISCONNECTED:
                        ActiveModeService.handleState(context);
                        KeyguardService.handleState(context);
                        SensorsDumpService.handleState(context);
                        break;
                }
            }
        });
    }

}