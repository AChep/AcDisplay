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
package com.achep.activedisplay.blacklist;

import android.content.Context;
import android.widget.CompoundButton;

import com.achep.activedisplay.Operator;

import java.util.ArrayList;

/**
 * Created by Artem on 21.02.14.
 */
public class BlacklistEnabler extends Blacklist.OnBlacklistChangedListener
        implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "BlacklistEnabler";

    private final Context mContext;
    private final Blacklist mBlacklist;
    private AppConfig mAppConfig;
    private CompoundButton mCompoundButton;

    private boolean mResumed;
    private boolean mBroadcasting;

    private ArrayList<Blacklist.OnBlacklistChangedListener> mListeners;

    public void addOnAppConfigChangedListener(Blacklist.OnBlacklistChangedListener listener) {
        mListeners.add(listener);
    }

    public void removeOnAppConfigChangedListener(Blacklist.OnBlacklistChangedListener listener) {
        mListeners.remove(listener);
    }

    public BlacklistEnabler(Context context, CompoundButton cb, String packageName) {
        mContext = context;
        mCompoundButton = cb;
        mBlacklist = Blacklist.getInstance(mContext);
        mAppConfig = new AppConfig(packageName);

        mListeners = new ArrayList<>(6);
    }

    /**
     * Sets up listeners and updates the current state.
     * Make sure that you call {@link #pause()} after!
     */
    public void resume() {
        if (mResumed) {
            return;
        }

        mResumed = true;
        mBlacklist.registerListener(this);
        mCompoundButton.setOnCheckedChangeListener(this);
        reloadAppConfig();
    }

    public void pause() {
        if (!mResumed) {
            return;
        }

        mResumed = false;
        mBlacklist.unregisterListener(this);
        mCompoundButton.setOnCheckedChangeListener(null);
    }

    // //////////////////////////////////////////
    // ////////////// -- MAIN -- ////////////////
    // //////////////////////////////////////////

    public void setSwitch(CompoundButton cb) {
        if (mCompoundButton == cb) {
            return;
        }

        boolean checked = mCompoundButton.isChecked();

        mCompoundButton.setOnCheckedChangeListener(null);
        mCompoundButton = cb;
        mCompoundButton.setOnCheckedChangeListener(this);
        setChecked(checked);
    }

    /**
     * Changes tracking app to new one.
     *
     * @param packageName a package name of new app
     */
    public void setPackageName(String packageName) {
        mAppConfig.packageName = packageName;
        reloadAppConfig();
    }

    public AppConfig getAppConfig() {
        return mAppConfig;
    }

    private void reloadAppConfig() {
        AppConfig configOld = AppConfig.copy(mAppConfig, new AppConfig(null));
        mBlacklist.fill(mAppConfig);
        onBlacklistChanged(mAppConfig, configOld,
                mBlacklist.getComparator().compare(mAppConfig, configOld));
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

        mAppConfig.enabled = isChecked;
        mBlacklist.saveAppConfig(mContext, mAppConfig, this);
    }

    // //////////////////////////////////////////
    // ///////// -- BLACKLIST CORE -- ///////////
    // //////////////////////////////////////////

    @Override
    public void onBlacklistChanged(AppConfig configNew, AppConfig configOld, int diff) {
        if (configNew.equals(mAppConfig)) {
            AppConfig.copy(configNew, mAppConfig);

            if (Operator.bitAnd(diff, AppConfig.DIFF_ENABLED))
                setChecked(configNew.enabled);

            for (Blacklist.OnBlacklistChangedListener listener : mListeners)
                listener.onBlacklistChanged(mAppConfig, configOld, diff);
        }
    }
}
