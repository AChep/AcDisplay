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
package com.achep.activedisplay.settings;

import android.content.Context;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.achep.activedisplay.Config;

/**
 * Created by Artem on 21.02.14.
 */
public final class ActiveModeEnabler extends Enabler {

    private boolean mBroadcasting;

    public ActiveModeEnabler(Context context, Switch switch_) {
        super(context, switch_);
    }

    @Override
    protected void updateState() {
        mSwitch.setEnabled(mConfig.isActiveDisplayEnabled());
        mBroadcasting = true;
        mSwitch.setChecked(mConfig.isActiveModeEnabled());
        mBroadcasting = false;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mBroadcasting) {
            return;
        }

        mConfig.setActiveModeEnabled(mContext, isChecked, this);
    }

    @Override
    public void onConfigChanged(Config config, String key, Object value) {
        switch (key) {
            case Config.KEY_ENABLED:
            case Config.KEY_ACTIVE_MODE:
                updateState();
                break;
        }
    }
}