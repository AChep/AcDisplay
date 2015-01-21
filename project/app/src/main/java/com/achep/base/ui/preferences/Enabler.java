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
package com.achep.base.ui.preferences;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.CompoundButton;

import com.achep.base.content.ConfigBase;
import com.achep.base.interfaces.ICheckable;

/**
 * @author Artem Chepurnoy
 */
public class Enabler {

    private final String mKey;
    private final Context mContext;
    private final ConfigBase mConfig;
    private final ConfigBase.Option mOption;
    private final ConfigBase.OnConfigChangedListener mConfigListener =
            new ConfigBase.OnConfigChangedListener() {

                @Override
                public void onConfigChanged(@NonNull ConfigBase config,
                                            @NonNull String key,
                                            @NonNull Object value) {
                    if (mKey.equals(key)) updateCheckableState();
                }
            };

    private final CompoundButton.OnCheckedChangeListener mCheckableListener
            = new CompoundButton.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (mBroadcasting) {
                return;
            }

            mOption.write(mConfig, mContext, isChecked, mConfigListener);
        }
    };

    private ICheckable mCheckable;
    private boolean mBroadcasting;

    public Enabler(@NonNull Context context,
                   @NonNull ConfigBase config,
                   @NonNull String key,
                   @NonNull ICheckable checkable) {
        mKey = key;
        mConfig = config;
        mContext = context;
        mCheckable = checkable;

        mOption = mConfig.getOption(mKey);
    }

    /**
     * Starts listening to the config's changes and updates corresponding
     * {@link com.achep.base.interfaces.ICheckable compound button}.
     * You must call {@link #stop() stop} method later!
     */
    public void start() {
        mConfig.registerListener(mConfigListener);
        mCheckable.setOnCheckedChangeListener(mCheckableListener);
        updateCheckableState();
    }

    /**
     * Stops listening to the config's changes.
     */
    public void stop() {
        mConfig.unregisterListener(mConfigListener);
        mCheckable.setOnCheckedChangeListener(null);
    }

    public void setCheckable(@NonNull ICheckable checkable) {
        if (mCheckable == checkable) {
            return;
        }

        mCheckable.setOnCheckedChangeListener(null);
        mCheckable = checkable;
        mCheckable.setOnCheckedChangeListener(mCheckableListener);
        updateCheckableState();
    }

    private void updateCheckableState() {
        mBroadcasting = true;
        mCheckable.setChecked((boolean) mOption.read(mConfig));
        mBroadcasting = false;
    }

}
