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

package com.painless.pc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public abstract class PowerTogglesPlugin extends BroadcastReceiver {

    // Intent send by PowerToggles asking for a state change
    public static final String ACTION_SET_STATE = "com.painless.pc.ACTION_SET_STATE";

    // Intent send to PowerToggles notifying it of state changes
    public static final String ACTION_STATE_CHANGED = "com.painless.pc.ACTION_STATE_CHANGED";

    // Intent extra denoting the state. Value must be a boolean.
    public static final String EXTRA_STATE = "state";

    // Must be set to the class name of the receiver, when informing PowerToggles
    // of a state change.
    public static final String EXTRA_VARID = "varID";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_SET_STATE.equals(intent.getAction())) {
            changeState(context, intent.getBooleanExtra(EXTRA_STATE, false));
        }
    }

    /**
     * Called when the PowerToggles widget requests a state change because
     * the user clicked the corresponding toggle.
     */
    protected abstract void changeState(Context context, boolean newState);

    /**
     * Updates the plugin state in PowerToggles widget.
     */
    public final void sendStateUpdate(Context context, boolean newState) {
        sendStateUpdate(this.getClass(), newState, context);
    }

    /**
     * Updates the plugin state in PowerToggles widget.
     *
     * @param pluginClass The receiver implementing the plugin. An application can
     *                    define multiple plugins, with one receiver per plugin.
     * @param newState
     * @param context
     */
    public static void sendStateUpdate(Class<? extends PowerTogglesPlugin> pluginClass,
                                       boolean newState, Context context) {
        context.sendBroadcast(new Intent(ACTION_STATE_CHANGED)
                .putExtra(EXTRA_VARID, pluginClass.getName())
                .putExtra(EXTRA_STATE, newState));
    }
}
