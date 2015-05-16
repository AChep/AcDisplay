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

import com.achep.acdisplay.blacklist.AppConfig;
import com.achep.acdisplay.blacklist.Blacklist;
import com.achep.base.utils.Operator;

/**
 * Created by Artem on 27.02.14.
 */
public abstract class Option extends Blacklist.OnBlacklistChangedListener implements
        CompoundButton.OnCheckedChangeListener {

    private final Context mContext;
    private final Blacklist mBlacklist;
    private AppConfig mAppConfig;

    private CompoundButton mCompoundButton;
    private boolean mBroadcasting;

    // ui
    public final Drawable icon;
    public final CharSequence title;
    public final CharSequence summary;

    public Option(Context context, CompoundButton cb,
                  Blacklist blacklist, String packageName,
                  Drawable icon,
                  CharSequence title,
                  CharSequence summary) {
        this.icon = icon;
        this.title = title;
        this.summary = summary;

        mContext = context;
        mCompoundButton = cb;
        mBlacklist = blacklist;
        mAppConfig = mBlacklist.getAppConfig(packageName);
    }

    public abstract boolean[] getValue(AppConfig config);

    /**
     * @return diff mask of current option.
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
        mBlacklist.registerListener(this);
        mCompoundButton.setOnCheckedChangeListener(this);

        setChecked(getValue(mAppConfig)[0]);
    }

    public void pause() {
        mBlacklist.unregisterListener(this);
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

        getValue(mAppConfig)[0] = isChecked;

        // Clone current state cause it can be changed after.
        mBlacklist.saveAppConfig(mContext, mAppConfig, this);
    }

    // //////////////////////////////////////////
    // ///////// -- BLACKLIST CORE -- ///////////
    // //////////////////////////////////////////

    @Override
    public void onBlacklistChanged(
            @NonNull AppConfig configNew,
            @NonNull AppConfig configOld, int diff) {
        if (configOld.equals(mAppConfig)) {
            mAppConfig = configNew;
            if (Operator.bitAnd(diff, getDiffMask())) {
                setChecked(getValue(configNew)[0]);
            }
        }
    }
}
