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

package com.achep.activedisplay.activemode.handlers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.achep.activedisplay.activemode.ActiveModeHandler;
import com.achep.activedisplay.utils.PowerUtils;

/**
 * Prevents {@link com.achep.activedisplay.activemode.ActiveModeService} from listening to
 * sensors while screen is turned on.
 *
 * @author Artem Chepurnoy
 */
public final class ScreenHandler extends ActiveModeHandler {

    private Receiver mReceiver = new Receiver();

    private class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_ON:
                    requestInactive();
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    requestActive();
                    break;
            }
        }

    }

    public ScreenHandler(Context context, Callback callback) {
        super(context, callback);
    }

    @Override
    public void onCreate() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
        getContext().registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        getContext().unregisterReceiver(mReceiver);
    }

    @Override
    public boolean isActive() {
        return !PowerUtils.isScreenOn(getContext());
    }

}
