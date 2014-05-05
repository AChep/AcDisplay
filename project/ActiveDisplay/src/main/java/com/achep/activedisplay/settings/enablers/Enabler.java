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
package com.achep.activedisplay.settings.enablers;

import android.content.Context;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.achep.activedisplay.Config;

/**
 * Created by Artem on 21.02.14.
 */
public abstract class Enabler implements
        Config.OnConfigChangedListener,
        CompoundButton.OnCheckedChangeListener {

    protected final Context mContext;
    protected final Config mConfig;
    protected Switch mSwitch;

    public Enabler(Context context, Switch switch_) {
        mContext = context;
        mConfig = Config.getInstance();
        mSwitch = switch_;
    }

    abstract protected void updateState();

    public void resume() {
        mConfig.addOnConfigChangedListener(this);
        mSwitch.setOnCheckedChangeListener(this);
        updateState();
    }

    public void pause() {
        mConfig.removeOnConfigChangedListener(this);
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
}
