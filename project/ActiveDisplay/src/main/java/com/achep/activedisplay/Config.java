/*
 * Copyright (C) 2013-2014 AChep@xda <artemchep@gmail.com>
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
package com.achep.activedisplay;

import android.content.Context;
import android.content.SharedPreferences;

import com.achep.activedisplay.utils.AccessUtils;
import com.achep.activedisplay.utils.LogUtils;

import java.lang.ref.SoftReference;
import java.util.ArrayList;

/**
 * Created by Artem on 21.01.14.
 */
public class Config {

    private static final String TAG = "Config";

    private static final String PREFERENCES_FILE_NAME = "config";
    public static final String KEY_AD_ENABLED = "a";
    public static final String KEY_AD_ENABLED_ONLY_WHEN_CHARGING = "b";

    private static SoftReference<Config> mConfigSoft;

    private boolean mActiveDisplayEnabled;
    private boolean mActiveDisplayEnabledOnlyWhenCharging;
    private ArrayList<OnConfigChangedListener> mListeners;

    // //////////////////////////////////////////
    // /////////// -- LISTENERS -- //////////////
    // //////////////////////////////////////////

    public interface OnConfigChangedListener {
        public void onConfigChanged(Config config, String key, Object value);
    }

    public void addOnConfigChangedListener(OnConfigChangedListener listener) {
        mListeners.add(listener);
    }

    public void removeOnConfigChangedListener(OnConfigChangedListener listener) {
        mListeners.remove(listener);
    }

    // //////////////////////////////////////////
    // ///////////// -- INIT -- /////////////////
    // //////////////////////////////////////////

    public static synchronized Config getInstance(Context context) {
        Config instance;
        if (mConfigSoft == null || (instance = mConfigSoft.get()) == null) {
            if (Project.DEBUG) LogUtils.track();

            instance = new Config(context);
            mConfigSoft = new SoftReference<>(instance);
            return instance;
        }
        return instance;
    }

    private Config(Context context) {
        mListeners = new ArrayList<>(6);

        SharedPreferences prefs = getSharedPreferences(context);
        mActiveDisplayEnabled =
                prefs.getBoolean(KEY_AD_ENABLED, false);
        mActiveDisplayEnabledOnlyWhenCharging =
                prefs.getBoolean(KEY_AD_ENABLED_ONLY_WHEN_CHARGING, false);
    }

    private SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
    }

    private void notifyConfigChanged(String key, Object value, OnConfigChangedListener listener) {
        for (OnConfigChangedListener l : mListeners) {
            if (l == listener) continue;
            l.onConfigChanged(this, key, value);
        }
    }

    // //////////////////////////////////////////
    // ////////// -- PREFERENCES -- /////////////
    // //////////////////////////////////////////

    public boolean setActiveDisplayEnabled(Context context, boolean enabled,
                                           OnConfigChangedListener listener) {
        if (enabled && !(AccessUtils.isNotificationAccessEnabled(context)
                && AccessUtils.isDeviceAdminEnabled(context))) {
            return false;
        }

        mActiveDisplayEnabled = enabled;
        final String key = KEY_AD_ENABLED;

        getSharedPreferences(context).edit().putBoolean(key, enabled).apply();
        notifyConfigChanged(key, enabled, listener);
        return true;
    }

    public boolean getActiveDisplayEnabled() {
        return mActiveDisplayEnabled;
    }

    public void setActiveDisplayEnabledOnlyWhenCharging(Context context, boolean enabled,
                                                        OnConfigChangedListener listener) {
        mActiveDisplayEnabledOnlyWhenCharging = enabled;
        final String key = KEY_AD_ENABLED_ONLY_WHEN_CHARGING;

        getSharedPreferences(context).edit().putBoolean(key, enabled).apply();
        notifyConfigChanged(key, enabled, listener);
    }

    public boolean getActiveDisplayEnabledOnlyWhenCharging() {
        return mActiveDisplayEnabledOnlyWhenCharging;
    }
}
