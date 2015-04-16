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
package com.achep.acdisplay.plugins.powertoggles;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.achep.acdisplay.Config;
import com.achep.base.content.ConfigBase;
import com.painless.pc.PowerTogglesPlugin;

import static com.achep.base.Build.DEBUG;

/**
 * AcDisplay toggle plugin for PowerToggles <http://powertoggles.com>
 *
 * @author Artem Chepurnoy
 */
public class ToggleReceiver extends PowerTogglesPlugin implements ConfigBase.OnConfigChangedListener {
    private static final String TAG = "ToggleReceiver";

    /**
     * An object to monitor the garbage collector.
     */
    /* This is possible only because the Config wraps listeners
     * in a weak references. If it changes -> we will get a memory leak. */
    private final Object dFinilizer = new Object() {

        @Override
        protected void finalize() throws Throwable {
            try {
                Config.getInstance().unregisterListener(ToggleReceiver.this);
                if (DEBUG) Log.d(TAG, "Un-registering the toggle receiver from Config.");
            } finally {
                super.finalize();
            }
        }
    };

    public ToggleReceiver() {
        Config.getInstance().registerListener(this);
    }

    @Override
    protected void changeState(Context context, boolean newState) {
        Config.getInstance().setEnabled(context, newState, null);
    }

    @Override
    public void onConfigChanged(@NonNull ConfigBase config,
                                @NonNull String key,
                                @NonNull Object value) {
        switch (key) {
            case Config.KEY_ENABLED:
                sendStateUpdate(config.getContext(), (Boolean) value);
                break;
        }
    }
}