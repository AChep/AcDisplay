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
package com.achep.acdisplay.settings;

import android.content.Context;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.achep.acdisplay.Config;

/**
 * Created by Artem on 21.02.14.
 */
public class Enabler implements
        Config.OnConfigChangedListener,
        CompoundButton.OnCheckedChangeListener {

    private final Context mContext;

    private final String mKey;
    private final Config mConfig;
    private final Config.Option mOption;

    private Switch mSwitch;

    private boolean mBroadcasting;

    public Enabler(Context context, Switch switch_, String key) {
        mContext = context;

        mKey = key;
        mConfig = Config.getInstance();
        mOption = mConfig.getHashMap().get(mKey);

        mSwitch = switch_;
    }

    private void updateState() {
        mBroadcasting = true;
        mSwitch.setChecked((Boolean) mOption.read(mConfig));
        mBroadcasting = false;
    }

    public void resume() {
        mConfig.registerListener(this);
        mSwitch.setOnCheckedChangeListener(this);
        updateState();
    }

    public void pause() {
        mConfig.unregisterListener(this);
        mSwitch.setOnCheckedChangeListener(null);
    }

    public void setSwitch(Switch switch_) {
        if (mSwitch == switch_) {
            return;
        }

        mSwitch = switch_;
        mSwitch.setOnCheckedChangeListener(this);
        updateState();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mBroadcasting) {
            return;
        }

        mOption.write(mConfig, mContext, isChecked, this);
    }

    @Override
    public void onConfigChanged(Config config, String key, Object value) {
        if (mKey.equals(key)) {
            updateState();
        }
    }
}
