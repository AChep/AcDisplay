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

import java.util.ArrayList;

/**
 * Created by Artem on 21.01.14.
 */
public class Config {

    private static final String TAG = "Config";

    private static final String PREFERENCES_FILE_NAME = "config";
    public static final String KEY_ENABLED = "a";
    public static final String KEY_ONLY_WHILE_CHARGING = "b";
    public static final String KEY_LOW_PRIORITY_NOTIFICATIONS = "c";

    // timeouts
    public static final String KEY_TIMEOUT_NORMAL = "timeout_normal";
    public static final String KEY_TIMEOUT_SHORT = "timeout_short";
    public static final String KEY_TIMEOUT_INSTANT = "timeout_instant";

    private static Config sConfigSoft;

    private boolean mActiveDisplayEnabled;
    private boolean mEnabledOnlyWhileCharging;
    private boolean mLowPriorityNotificationsAllowed;
    private int mTimeoutNormal;
    private int mTimeoutShort;
    private int mTimeoutInstant;
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
        if (sConfigSoft == null)
            sConfigSoft = new Config(context);
        return sConfigSoft;
    }

    private Config(Context context) {
        mListeners = new ArrayList<>(6);

        SharedPreferences prefs = getSharedPreferences(context);
        mActiveDisplayEnabled =
                prefs.getBoolean(KEY_ENABLED, false);
        mEnabledOnlyWhileCharging =
                prefs.getBoolean(KEY_ONLY_WHILE_CHARGING, false);
        mLowPriorityNotificationsAllowed =
                prefs.getBoolean(KEY_LOW_PRIORITY_NOTIFICATIONS, false);
        mTimeoutNormal = prefs.getInt(KEY_TIMEOUT_NORMAL, 15000);
        mTimeoutShort = prefs.getInt(KEY_TIMEOUT_SHORT, 6000);
        mTimeoutInstant = prefs.getInt(KEY_TIMEOUT_INSTANT, 3500);
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

    private void putBooleanAndNotify(Context context, String key,
                                     boolean enabled, OnConfigChangedListener listener) {
        getSharedPreferences(context).edit().putBoolean(key, enabled).apply();
        notifyConfigChanged(key, enabled, listener);
    }

    private void putIntAndNotify(Context context, String key,
                                     int value, OnConfigChangedListener listener) {
        getSharedPreferences(context).edit().putInt(key, value).apply();
        notifyConfigChanged(key, value, listener);
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
        final String key = KEY_ENABLED;

        getSharedPreferences(context).edit().putBoolean(key, enabled).apply();
        notifyConfigChanged(key, enabled, listener);
        return true;
    }

    public void setActiveDisplayEnabledOnlyWhenCharging(Context context, boolean enabled,
                                                        OnConfigChangedListener listener) {
        mEnabledOnlyWhileCharging = enabled;
        putBooleanAndNotify(context, KEY_ONLY_WHILE_CHARGING, enabled, listener);
    }

    public void setLowPriorityNotificationsAllowed(Context context, boolean enabled,
                                                   OnConfigChangedListener listener) {
        mLowPriorityNotificationsAllowed = enabled;
        putBooleanAndNotify(context, KEY_LOW_PRIORITY_NOTIFICATIONS, enabled, listener);
    }

    public boolean isActiveDisplayEnabled() {
        return mActiveDisplayEnabled;
    }

    public boolean isEnabledOnlyWhileCharging() {
        return mEnabledOnlyWhileCharging;
    }

    public boolean isLowPriorityNotificationsAllowed() {
        return mLowPriorityNotificationsAllowed;
    }

    // //////////////////////////////////////////
    // //////////// -- TIMEOUT -- ///////////////
    // //////////////////////////////////////////

    public void setTimeoutNormal(Context context, int delayMillis, OnConfigChangedListener listener) {
        mTimeoutNormal = delayMillis;
        putIntAndNotify(context, KEY_TIMEOUT_NORMAL, delayMillis, listener);
    }

    public void setTimeoutShort(Context context, int delayMillis, OnConfigChangedListener listener) {
        mTimeoutShort = delayMillis;
        putIntAndNotify(context, KEY_TIMEOUT_SHORT, delayMillis, listener);
    }

    public void setTimeoutInstant(Context context, int delayMillis, OnConfigChangedListener listener) {
        mTimeoutInstant = delayMillis;
        putIntAndNotify(context, KEY_TIMEOUT_INSTANT, delayMillis, listener);
    }

    public int getTimeoutNormal() {
        return mTimeoutNormal;
    }

    public int getTimeoutShort() {
        return mTimeoutShort;
    }

    public int getTimeoutInstant() {
        return mTimeoutInstant;
    }
}
