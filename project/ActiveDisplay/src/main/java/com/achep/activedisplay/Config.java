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
package com.achep.activedisplay;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;

import com.achep.activedisplay.activemode.ActiveModeService;
import com.achep.activedisplay.services.LockscreenService;
import com.achep.activedisplay.utils.AccessUtils;

import java.util.ArrayList;

/**
 * @author Artem
 * @since 21.01.14
 * Saves all the configurations for the app
 */
public class Config {

    private static final String TAG = "Config";
    private static final String PREFERENCES_FILE_NAME = "config";

    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_ONLY_WHILE_CHARGING = "only_while_charging";
    public static final String KEY_LOW_PRIORITY_NOTIFICATIONS = "low_priority_notifications";

    // inactive time
    public static final String KEY_INACTIVE_TIME_FROM = "inactive_time_from";
    public static final String KEY_INACTIVE_TIME_TO = "inactive_time_to";
    public static final String KEY_INACTIVE_TIME_ENABLED = "inactive_time_enabled";

    // timeouts
    public static final String KEY_TIMEOUT_ACTIVE = "timeout_active";
    public static final String KEY_TIMEOUT_NORMAL = "timeout_normal";
    public static final String KEY_TIMEOUT_SHORT = "timeout_short";
    //public static final String KEY_TIMEOUT_INSTANT = "timeout_instant";   Unused Variable

    // lockscreen
    public static final String KEY_LOCK_SCREEN = "lock_screen";

    // active mode
    public static final String KEY_ACTIVE_MODE = "active_mode";

    // interface
    public static final String KEY_INTERFACE_WALLPAPER_SHOWN = "wallpaper_shown";
    public static final String KEY_INTERFACE_SHADOW_TOGGLE = "shadow_toggle";
    public static final String KEY_INTERFACE_DYNAMIC_BACKGROUND_MODE = "dynamic_background_mode";
    public static final int DYNAMIC_BG_ARTWORK_MASK = 1;
    public static final int DYNAMIC_BG_NOTIFICATION_MASK = 2;
    public static final String KEY_INTERFACE_MIRRORED_TIMEOUT_PROGRESS_BAR = "mirrored_timeout_progress_bar";
    public static final String KEY_CLOCK_FONT ="clock_font";
    public static final String KEY_CLOCK_COLOR ="clock_color";
    public static final String KEY_CLOCK_SIZE ="clock_size";

    // swipe actions
    public static final String KEY_SWIPE_LEFT_ACTION = "swipe_left_action";
    public static final String KEY_SWIPE_RIGHT_ACTION = "swipe_right_action";
    private static Config sConfigSoft;

    private boolean mActiveDisplayEnabled;
    private boolean mEnabledOnlyWhileCharging;
    private boolean mLowPriorityNotificationsAllowed;
    private boolean mCanTimeOut;
    private int mTimeoutNormal;
    private int mTimeoutShort;
    private int mInactiveTimeFrom;
    private int mInactiveTimeTo;
    private int mSwipeLeftAction;
    private int mSwipeRightAction;
    private int mDynamicBackgroundMode;
    private boolean mInactiveTimeEnabled;
    private boolean mLockscreenEnabled;
    private Boolean mActiveMode;
    private ArrayList<OnConfigChangedListener> mListeners;
    private boolean mWallpaperShown;
    private boolean mShadowShown;
    private boolean mMirroredTimeoutProgressBarEnabled;
    private String mClockFont;
    private int mClockColor;
    private int mClockSize;

    /**
     * Config constructor, sets all the variables to the value using sharedPreference or uses a preset key
     *
     * @param context
     */
    private Config(Context context) {
        mListeners = new ArrayList<>(6);

        SharedPreferences prefs = getSharedPreferences(context);
        mActiveDisplayEnabled = prefs.getBoolean(KEY_ENABLED, false);
        mEnabledOnlyWhileCharging = prefs.getBoolean(KEY_ONLY_WHILE_CHARGING, false);
        mLowPriorityNotificationsAllowed = prefs.getBoolean(KEY_LOW_PRIORITY_NOTIFICATIONS, false);
        mLockscreenEnabled = prefs.getBoolean(KEY_LOCK_SCREEN, false);
        mActiveMode = prefs.getBoolean(KEY_ACTIVE_MODE, false);
        mWallpaperShown = prefs.getBoolean(KEY_INTERFACE_WALLPAPER_SHOWN, false);
        mShadowShown = prefs.getBoolean(KEY_INTERFACE_SHADOW_TOGGLE, true);
        mMirroredTimeoutProgressBarEnabled = prefs.getBoolean(KEY_INTERFACE_MIRRORED_TIMEOUT_PROGRESS_BAR, true);
        mCanTimeOut = prefs.getBoolean(KEY_TIMEOUT_ACTIVE, false);
        mTimeoutNormal = prefs.getInt(KEY_TIMEOUT_NORMAL, 12000);
        mTimeoutShort = prefs.getInt(KEY_TIMEOUT_SHORT, 6000);
        mInactiveTimeFrom = prefs.getInt(KEY_INACTIVE_TIME_FROM, 0);
        mInactiveTimeTo = prefs.getInt(KEY_INACTIVE_TIME_TO, 0);
        mInactiveTimeEnabled = prefs.getBoolean(KEY_INACTIVE_TIME_ENABLED, false);
        mSwipeLeftAction = prefs.getInt(KEY_SWIPE_LEFT_ACTION, 2);
        mSwipeRightAction = prefs.getInt(KEY_SWIPE_RIGHT_ACTION, 2);
        mDynamicBackgroundMode = prefs.getInt(KEY_INTERFACE_DYNAMIC_BACKGROUND_MODE,
                DYNAMIC_BG_ARTWORK_MASK | DYNAMIC_BG_NOTIFICATION_MASK);
        mClockFont = prefs.getString(KEY_CLOCK_FONT, "fonts/Roboto-Light.ttf");
        mClockColor = prefs.getInt(KEY_CLOCK_COLOR, Color.parseColor("#ffffff"));
        mClockSize=prefs.getInt(KEY_CLOCK_SIZE, 85);
    }

    /**
     * Get's an instance of the config
     *
     * @param context
     * @return a config instance
     */

    public static synchronized Config getInstance(Context context) {
        if (sConfigSoft == null)
            sConfigSoft = new Config(context);
        return sConfigSoft;
    }

    static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Listeners modifiers
     * TODO: add better doc
     */

    public interface OnConfigChangedListener {
        public void onConfigChanged(Config config, String key, Object value);
    }

    public void addOnConfigChangedListener(OnConfigChangedListener listener) {
        if (Project.DEBUG) Log.d(TAG, "add_l=" + listener);
        mListeners.add(listener);
    }

    public void removeOnConfigChangedListener(OnConfigChangedListener listener) {
        if (Project.DEBUG) Log.d(TAG, "remove_l=" + listener);
        mListeners.remove(listener);
    }


    /**
     * This is for debugging, writes to the log if a config changes.
     *
     * @param key
     * @param value
     * @param listener
     */
    private void notifyConfigChanged(String key, Object value, OnConfigChangedListener listener) {
        if (Project.DEBUG) Log.d(TAG, "Notifying listeners: \"" + key + "\" = \"" + value + "\"");
        for (OnConfigChangedListener l : mListeners) {
            if (l == listener) continue;
            l.onConfigChanged(this, key, value);
        }
    }

    private void saveOption(Context context, String key, Object value,
                            OnConfigChangedListener listener, boolean changed) {
        if (!changed) return;

        if (Project.DEBUG) Log.d(TAG, "Saving \"" + key + "\" to config as \"" + value + "\"");

        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        }else if(value instanceof String) {
        	editor.putString(key, (String)value);
        }else throw new IllegalArgumentException("Unknown option type.");
        editor.apply();

        notifyConfigChanged(key, value, listener);
    }

    /**
     * Setter for the entire app enabler
     *
     * @param context
     * @param enabled
     * @param listener
     * @return
     */

    public boolean setActiveDisplayEnabled(Context context, boolean enabled,
                                           OnConfigChangedListener listener) {
        if (enabled && !(AccessUtils.isNotificationAccessEnabled(context)
                && AccessUtils.isDeviceAdminEnabled(context))) {
            return false;
        }

        boolean changed = mActiveDisplayEnabled != (mActiveDisplayEnabled = enabled);
        saveOption(context, KEY_ENABLED, enabled, listener, changed);

        if (changed) {
            ActiveModeService.handleState(context);
            LockscreenService.handleState(context);
        }
        return true;
    }

    /**
     * Setter to only have the app running while charging
     *
     * @param context
     * @param enabled
     * @param listener
     */
    public void setActiveDisplayEnabledOnlyWhileCharging(Context context, boolean enabled,
                                                         OnConfigChangedListener listener) {
        saveOption(context, KEY_ONLY_WHILE_CHARGING, enabled, listener,
                mEnabledOnlyWhileCharging != (mEnabledOnlyWhileCharging = enabled));
    }

    /**
     * Setter to allow notifications with a lower priority like Google Now
     *
     * @param context
     * @param enabled
     * @param listener
     */
    public void setLowPriorityNotificationsAllowed(Context context, boolean enabled,
                                                   OnConfigChangedListener listener) {
        saveOption(context, KEY_LOW_PRIORITY_NOTIFICATIONS, enabled, listener,
                mLowPriorityNotificationsAllowed != (mLowPriorityNotificationsAllowed = enabled));
    }

    /**
     * Setter to allow the screen to time out or not
     *
     * @param context
     * @param enabled
     * @param listener
     */
    public void setTimeOutAvailable(Context context, boolean enabled, OnConfigChangedListener listener) {
        saveOption(context, KEY_TIMEOUT_ACTIVE, enabled, listener,
                mCanTimeOut != (mCanTimeOut = enabled));
    }

    /**
     * Setter to set the timeout in a normal situation
     * used via reflections!
     *
     * @param context
     * @param delayMillis
     * @param listener
     */
    public void setTimeoutNormal(Context context, int delayMillis, OnConfigChangedListener listener) {
        saveOption(context, KEY_TIMEOUT_NORMAL, delayMillis, listener,
                mTimeoutNormal != (mTimeoutNormal = delayMillis));
    }

    /**
     * Setter for short timeout time
     * used via reflections!
     *
     * @param context
     * @param delayMillis
     * @param listener
     */
    public void setTimeoutShort(Context context, int delayMillis, OnConfigChangedListener listener) {
        saveOption(context, KEY_TIMEOUT_SHORT, delayMillis, listener,
                mTimeoutShort != (mTimeoutShort = delayMillis));
    }

    /**
     * Setter to enable "night mode"
     *
     * @param context
     * @param enabled
     * @param listener
     */
    public void setInactiveTimeEnabled(Context context, boolean enabled, OnConfigChangedListener listener) {
        saveOption(context, KEY_INACTIVE_TIME_ENABLED, enabled, listener,
                mInactiveTimeEnabled != (mInactiveTimeEnabled = enabled));
    }

    /**
     * Setter for the time "night mode" should start
     *
     * @param context
     * @param minutes
     * @param listener
     */
    public void setInactiveTimeFrom(Context context, int minutes, OnConfigChangedListener listener) {
        saveOption(context, KEY_INACTIVE_TIME_FROM, minutes, listener,
                mInactiveTimeFrom != (mInactiveTimeFrom = minutes));
    }

    /**
     * Setter for the time "night mode" should end
     *
     * @param context
     * @param minutes
     * @param listener
     */
    public void setInactiveTimeTo(Context context, int minutes, OnConfigChangedListener listener) {
        saveOption(context, KEY_INACTIVE_TIME_TO, minutes, listener,
                mInactiveTimeTo != (mInactiveTimeTo = minutes));
    }

    /**
     * TODO: write doc here
     *
     * @param context
     * @param action
     * @param listener
     */
    public void setSwipeLeftAction(Context context, int action, OnConfigChangedListener listener) {
        saveOption(context, KEY_SWIPE_LEFT_ACTION, action, listener,
                mSwipeLeftAction != (mSwipeLeftAction = action));
    }

    /**
     * TODO: write doc here
     *
     * @param context
     * @param action
     * @param listener
     */
    public void setSwipeRightAction(Context context, int action, OnConfigChangedListener listener) {
        saveOption(context, KEY_SWIPE_RIGHT_ACTION, action, listener,
                mSwipeRightAction != (mSwipeRightAction = action));
    }

    /**
     * Setter to enable the lockscreen mode
     *
     * @param context
     * @param enabled
     * @param listener
     */
    public void setLockscreenEnabled(Context context, boolean enabled, OnConfigChangedListener listener) {
        boolean changed = mLockscreenEnabled != (mLockscreenEnabled = enabled);

        saveOption(context, KEY_LOCK_SCREEN, enabled, listener, changed);

        // Launch / stop lockscreen service
        if (changed) LockscreenService.handleState(context);
    }

    /**
     * Setter to enable active mode
     *
     * @param context
     * @param enabled
     * @param listener
     */
    public void setActiveModeEnabled(Context context, boolean enabled, OnConfigChangedListener listener) {
        boolean changed = mActiveMode != (mActiveMode = enabled);
        saveOption(context, KEY_ACTIVE_MODE, enabled, listener, changed);

        // Launch / stop sensor monitor service
        if (changed) ActiveModeService.handleState(context);
    }

    /**
     * Setter to allow the wallpaper to be shown instead of black
     *
     * @param context
     * @param shown
     * @param listener
     */
    public void setWallpaperShown(Context context, boolean shown, OnConfigChangedListener listener) {
        saveOption(context, KEY_INTERFACE_WALLPAPER_SHOWN, shown, listener,
                mWallpaperShown != (mWallpaperShown = shown));
    }

    /**
     * TODO: write doc
     *
     * @param context
     * @param shown
     * @param listener
     */
    public void setShadowEnabled(Context context, boolean shown, OnConfigChangedListener listener) {
        saveOption(context, KEY_INTERFACE_SHADOW_TOGGLE, shown, listener,
                mShadowShown != (mShadowShown = shown));
    }

    /**
     * Allow the background to change based on the notification
     *
     * @param context
     * @param mode
     * @param listener
     */
    public void setDynamicBackgroundMode(Context context, int mode, OnConfigChangedListener listener) {
        saveOption(context, KEY_INTERFACE_DYNAMIC_BACKGROUND_MODE, mode, listener,
                mDynamicBackgroundMode != (mDynamicBackgroundMode = mode));
    }

    /**
     * Allow the dots to move in from both sides
     *
     * @param context
     * @param enabled
     * @param listener
     */
    public void setMirroredTimeoutProgressBarEnabled(Context context, boolean enabled, OnConfigChangedListener listener) {
        saveOption(context, KEY_INTERFACE_MIRRORED_TIMEOUT_PROGRESS_BAR, enabled, listener,
                mMirroredTimeoutProgressBarEnabled != (mMirroredTimeoutProgressBarEnabled = enabled));
    }

    public void setClockFont(Context context, String value, OnConfigChangedListener listener) {
    	saveOption(context, KEY_CLOCK_FONT, value, listener, mClockFont != (mClockFont = value));
    }
    
    public void setClockColor(Context context, int value, OnConfigChangedListener listener) {
    	saveOption(context, KEY_CLOCK_COLOR, value, listener, mClockColor != (mClockColor = value));
    }
    
    public void setClockSize(Context context, int value, OnConfigChangedListener listener) {
    	saveOption(context, KEY_CLOCK_SIZE, value, listener, mClockSize != (mClockSize = value));
    }
    
    public boolean isTimeOutAvailable() {
        return mCanTimeOut;
    }

    public int getTimeoutNormal() {
        return mTimeoutNormal;
    }

    public int getTimeoutShort() {
        return mTimeoutShort;
    }

    public int getInactiveTimeFrom() {
        return mInactiveTimeFrom;
    }

    public int getInactiveTimeTo() {
        return mInactiveTimeTo;
    }

    public int getSwipeLeftAction() {
        return mSwipeLeftAction;
    }

    public int getSwipeRightAction() {
        return mSwipeRightAction;
    }

    public int getDynamicBackgroundMode() {
        return mDynamicBackgroundMode;
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

    public boolean isLockscreenEnabled() {
        return mLockscreenEnabled;
    }

    public boolean isActiveModeEnabled() {
        return mActiveMode;
    }

    public boolean isWallpaperShown() {
        return mWallpaperShown;
    }

    public boolean isShadowEnabled() {
        return mShadowShown;
    }

    public boolean isMirroredTimeoutProgressBarEnabled() {
        return mMirroredTimeoutProgressBarEnabled;
    }

    public boolean isInactiveTimeEnabled() {
        return mInactiveTimeEnabled;
    }
    
    public String getClockFont() {
    	return mClockFont;
    }
    
    public int getClockColor() {
    	return mClockColor;
    }
    
    public int getClockSize() {
    	return mClockSize;
    }

}
