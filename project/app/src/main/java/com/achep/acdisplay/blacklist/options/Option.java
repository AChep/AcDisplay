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
package com.achep.acdisplay.blacklist.options;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.widget.CompoundButton;

import com.achep.acdisplay.Operator;
import com.achep.acdisplay.blacklist.AppConfig;
import com.achep.acdisplay.blacklist.Blacklist;
import com.achep.acdisplay.blacklist.BlacklistEnabler;

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

    // ui
    public Drawable icon;
    public CharSequence title;
    public CharSequence summary;

    public Option(Context context, CompoundButton cb,
                  Blacklist blacklist, BlacklistEnabler enabler,
                  Drawable icon,
                  CharSequence title,
                  CharSequence summary) {
        this.icon = icon;
        this.title = title;
        this.summary = summary;

        mContext = context;
        mCompoundButton = cb;
        mBlacklist = blacklist;
        mBlacklistEnabler = enabler;
    }

    public abstract boolean[] getValue(AppConfig config);

    /**
     * @return diff mask of current option.
     * @see com.achep.acdisplay.blacklist.AppConfig#DIFF_ENABLED
     * @see com.achep.acdisplay.blacklist.AppConfig#DIFF_HIDDEN
     * @see com.achep.acdisplay.blacklist.AppConfig#DIFF_RESTRICTED
     * @see com.achep.acdisplay.blacklist.AppConfig#DIFF_NON_CLEARABLE
     */
    public abstract int getDiffMask();

    /**
     * Sets up listeners and updates the current state.
     * Make sure that you'll call {@link #pause()} later!
     */
    public void resume() {
        mBlacklistEnabler.addOnAppConfigChangedListener(this);
        mCompoundButton.setOnCheckedChangeListener(this);

        AppConfig config = mBlacklistEnabler.getAppConfig();
        setChecked(getValue(config)[0]);
    }

    public void pause() {
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
        getValue(config)[0] = isChecked;

        // Clone current state cause it can be changed after.
        mBlacklist.saveAppConfig(mContext, config, mBlacklistEnabler);
    }

    // //////////////////////////////////////////
    // ///////// -- BLACKLIST CORE -- ///////////
    // //////////////////////////////////////////

    @Override
    public void onBlacklistChanged(
            @NonNull AppConfig configNew,
            @NonNull AppConfig configOld, int diff) {
        if (Operator.bitAnd(diff, getDiffMask())) {
            setChecked(getValue(configNew)[0]);
        }
    }
}
