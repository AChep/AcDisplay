/*
 * Copyright (C) 2013 AChep@xda <artemchep@gmail.com>
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
package com.achep.activedisplay.blacklist.options;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.CompoundButton;

import com.achep.activedisplay.blacklist.AppConfig;
import com.achep.activedisplay.blacklist.Blacklist;
import com.achep.activedisplay.blacklist.BlacklistEnabler;

/**
 * Created by Artem on 27.02.14.
 */
public abstract class Option extends Blacklist.OnBlacklistChangedListener implements
        CompoundButton.OnCheckedChangeListener {

    private final Context mContext;
    private final Blacklist mBlacklist;
    private final BlacklistEnabler mBlacklistEnabler;

    private CompoundButton mCompoundButton;
    private boolean mBroadcasting;
    private boolean mResumed;

    // ui
    public Drawable icon;
    public CharSequence title;
    public CharSequence summary;

    public Option(Context context, CompoundButton cb, BlacklistEnabler enabler,
                  Drawable icon,
                  CharSequence title,
                  CharSequence summary) {
        this.icon = icon;
        this.title = title;
        this.summary = summary;

        mContext = context;
        mCompoundButton = cb;
        mBlacklist = Blacklist.getInstance(context);
        mBlacklistEnabler = enabler;
    }

    public abstract boolean[] extractVariable(AppConfig config);

    public abstract boolean isChanged(int diff);

    /**
     * Sets up listeners and updates the current state.
     * Make sure that you'll call {@link #pause()} later!
     */
    public void resume() {
        if (mResumed) {
            return;
        }

        mResumed = true;
        mBlacklistEnabler.addOnAppConfigChangedListener(this);
        mCompoundButton.setOnCheckedChangeListener(this);
        onBlacklistChanged(mBlacklistEnabler.getAppConfig(), null, -1);
    }

    public void pause() {
        if (!mResumed) {
            return;
        }

        mResumed = false;
        mBlacklistEnabler.removeOnAppConfigChangedListener(this);
        mCompoundButton.setOnCheckedChangeListener(null);
    }

    public void setCompoundButton(CompoundButton cb) {
        if (mCompoundButton == cb) {
            return;
        }

        boolean checked = cb.isChecked();

        mCompoundButton.setOnCheckedChangeListener(null);
        mCompoundButton = cb;
        mCompoundButton.setOnCheckedChangeListener(this);
        setChecked(checked);
    }

    private void setChecked(boolean checked) {
        mBroadcasting = true;
        mCompoundButton.setChecked(checked);
        mBroadcasting = false;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mBroadcasting) {
            return;
        }

        AppConfig config = mBlacklistEnabler.getAppConfig();
        extractVariable(config)[0] = isChecked;

        // Clone current state cause it can be changed after.
        mBlacklist.saveAppConfig(mContext, config, mBlacklistEnabler);
    }

    // //////////////////////////////////////////
    // ///////// -- BLACKLIST CORE -- ///////////
    // //////////////////////////////////////////

    @Override
    public void onBlacklistChanged(AppConfig configNew, AppConfig configOld, int diff) {
        if (diff == -1 || isChanged(diff)) {
            setChecked(extractVariable(configNew)[0]);
        }
    }
}
