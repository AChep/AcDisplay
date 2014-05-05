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
import android.content.res.Resources;
import android.util.Log;

import com.achep.activedisplay.activemode.ActiveModeService;
import com.achep.activedisplay.services.KeyguardService;
import com.achep.activedisplay.utils.AccessUtils;

import java.util.ArrayList;

/**
 * Saves all the configurations for the app.
 *
 * @author Artem Chepurnoy
 * @since 21.01.14
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
    public static final String KEY_TIMEOUT_ENABLED = "timeout_enabled";
    public static final String KEY_TIMEOUT_NORMAL = "timeout_normal";
    public static final String KEY_TIMEOUT_SHORT = "timeout_short";

    // keyguard
    public static final String KEY_KEYGUARD = "lock_screen";

    // active mode
    public static final String KEY_ACTIVE_MODE = "active_mode";
    public static final String KEY_ACTIVE_MODE_WITHOUT_NOTIFICATIONS = "active_mode_without_notifications";

    // interface
    public static final String KEY_UI_WALLPAPER_SHOWN = "wallpaper_shown";
    public static final String KEY_UI_SHADOW_TOGGLE = "shadow_toggle";
    public static final String KEY_UI_DYNAMIC_BACKGROUND_MODE = "dynamic_background_mode";
    public static final int DYNAMIC_BG_ARTWORK_MASK = 1;
    public static final int DYNAMIC_BG_NOTIFICATION_MASK = 2;
    public static final String KEY_UI_MIRRORED_TIMEOUT_BAR = "mirrored_timeout_progress_bar";
    public static final String KEY_UI_NOTIFY_CIRCLED_ICON = "notify_circled_icon";

    private static Config sConfig;

    private boolean mAcDisplayEnabled;
    private boolean mKeyguardEnabled;
    private boolean mActiveMode;
    private boolean mActiveModeWithoutNotifies;
    private boolean mEnabledOnlyWhileCharging;
    private boolean mNotifyLowPriority;
    private boolean mTimeoutEnabled;
    private int mTimeoutNormal;
    private int mTimeoutShort;
    private int mInactiveTimeFrom;
    private int mInactiveTimeTo;
    private int mUiDynamicBackground;
    private boolean mInactiveTimeEnabled;
    private boolean mUiWallpaper;
    private boolean mUiWallpaperShadow;
    private boolean mUiMirroredTimeoutBar;
    private boolean mUiNotifyCircledIcon;

    private ArrayList<OnConfigChangedListener> mListeners;
    private Context mContext;

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

    public static synchronized Config getInstance() {
        if (sConfig == null) {
            sConfig = new Config();
        }
        return sConfig;
    }

    private Config() { /* unused */ }

    /**
     * Loads saved values from shared preferences.
     * This is called on {@link App app's} create.
     */
    void init(Context context) {
        mListeners = new ArrayList<>(6);

        Resources res = context.getResources();
        SharedPreferences prefs = getSharedPreferences(context);
        mAcDisplayEnabled = prefs.getBoolean(KEY_ENABLED,
                res.getBoolean(R.bool.config_default_enabled));
        mKeyguardEnabled = prefs.getBoolean(KEY_KEYGUARD,
                res.getBoolean(R.bool.config_default_keyguard_enabled));
        mActiveMode = prefs.getBoolean(KEY_ACTIVE_MODE,
                res.getBoolean(R.bool.config_default_active_mode_enabled));
        mActiveModeWithoutNotifies = prefs.getBoolean(KEY_ACTIVE_MODE_WITHOUT_NOTIFICATIONS,
                res.getBoolean(R.bool.config_default_active_mode_without_notifies_enabled));

        // notifications
        mNotifyLowPriority = prefs.getBoolean(KEY_LOW_PRIORITY_NOTIFICATIONS,
                res.getBoolean(R.bool.config_default_notify_low_priority));

        // timeout
        mTimeoutEnabled = prefs.getBoolean(KEY_TIMEOUT_ENABLED,
                res.getBoolean(R.bool.config_default_timeout_enabled));
        mTimeoutNormal = prefs.getInt(KEY_TIMEOUT_NORMAL,
                res.getInteger(R.integer.config_default_timeout_normal));
        mTimeoutShort = prefs.getInt(KEY_TIMEOUT_SHORT,
                res.getInteger(R.integer.config_default_timeout_short));

        // inactive time
        mInactiveTimeFrom = prefs.getInt(KEY_INACTIVE_TIME_FROM,
                res.getInteger(R.integer.config_default_inactive_time_from));
        mInactiveTimeTo = prefs.getInt(KEY_INACTIVE_TIME_TO,
                res.getInteger(R.integer.config_default_inactive_time_to));
        mInactiveTimeEnabled = prefs.getBoolean(KEY_INACTIVE_TIME_ENABLED,
                res.getBoolean(R.bool.config_default_inactive_time_enabled));

        // interface
        mUiWallpaper = prefs.getBoolean(KEY_UI_WALLPAPER_SHOWN,
                res.getBoolean(R.bool.config_default_ui_show_wallpaper));
        mUiWallpaperShadow = prefs.getBoolean(KEY_UI_SHADOW_TOGGLE,
                res.getBoolean(R.bool.config_default_ui_show_shadow));
        mUiDynamicBackground = prefs.getInt(KEY_UI_DYNAMIC_BACKGROUND_MODE,
                res.getInteger(R.integer.config_default_ui_show_shadow_dynamic_bg));
        mUiMirroredTimeoutBar = prefs.getBoolean(KEY_UI_MIRRORED_TIMEOUT_BAR,
                res.getBoolean(R.bool.config_default_ui_mirrored_timeout_bar));
        mUiNotifyCircledIcon = prefs.getBoolean(KEY_UI_NOTIFY_CIRCLED_ICON,
                res.getBoolean(R.bool.config_default_ui_notify_circled_icon));

        // other
        mEnabledOnlyWhileCharging = prefs.getBoolean(KEY_ONLY_WHILE_CHARGING,
                res.getBoolean(R.bool.config_default_enabled_only_while_charging));
    }

    static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
    }

    /**
     * You may get a context from here only on
     * {@link Config.OnConfigChangedListener#onConfigChanged(Config, String, Object) config change}.
     */
    public Context getContext() {
        return mContext;
    }

    private void notifyConfigChanged(String key, Object value, OnConfigChangedListener listener) {
        for (OnConfigChangedListener l : mListeners) {
            if (l == listener) continue;
            l.onConfigChanged(this, key, value);
        }
    }

    private void saveOption(Context context, String key, Object value,
                            OnConfigChangedListener listener, boolean changed) {
        if (!changed) {
            // Don't update preferences if this change is a lie.
            return;
        }

        if (Project.DEBUG) Log.d(TAG, "Saving \"" + key + "\" to config as \"" + value + "\"");

        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else throw new IllegalArgumentException("Unknown option type.");
        editor.apply();

        mContext = context;
        notifyConfigChanged(key, value, listener);
        mContext = null;
    }

    // //////////////////////////////////////////
    // ///////////// -- OPTIONS -- //////////////
    // //////////////////////////////////////////

    /**
     * Setter for the entire app enabler.
     */
    public boolean setEnabled(Context context, boolean enabled,
                              OnConfigChangedListener listener) {
        if (enabled
                && !(AccessUtils.isNotificationAccessEnabled(context)
                && AccessUtils.isDeviceAdminEnabled(context))) {
            return false;
        }

        boolean changed = mAcDisplayEnabled != (mAcDisplayEnabled = enabled);
        saveOption(context, KEY_ENABLED, enabled, listener, changed);

        if (changed) {
            ActiveModeService.handleState(context);
            KeyguardService.handleState(context);
        }
        return true;
    }

    /**
     * Setter to enable the lockscreen mode.
     */
    public void setKeyguardEnabled(Context context, boolean enabled, OnConfigChangedListener listener) {
        boolean changed = mKeyguardEnabled != (mKeyguardEnabled = enabled);
        saveOption(context, KEY_KEYGUARD, enabled, listener, changed);

        if (changed) {
            KeyguardService.handleState(context);
        }
    }

    /**
     * Setter to enable the active mode.
     */
    public void setActiveModeEnabled(Context context, boolean enabled, OnConfigChangedListener listener) {
        boolean changed = mActiveMode != (mActiveMode = enabled);
        saveOption(context, KEY_ACTIVE_MODE, enabled, listener, changed);

        if (changed) {
            ActiveModeService.handleState(context);
        }
    }

    public void setActiveModeWithoutNotificationsEnabled(Context context, boolean enabled, OnConfigChangedListener listener) {
        boolean changed = mActiveModeWithoutNotifies != (mActiveModeWithoutNotifies = enabled);
        saveOption(context, KEY_ACTIVE_MODE_WITHOUT_NOTIFICATIONS, enabled, listener, changed);
    }

    /**
     * Setter to only have the app running while charging.
     */
    public void setActiveDisplayEnabledOnlyWhileCharging(Context context, boolean enabled,
                                                         OnConfigChangedListener listener) {
        boolean changed = mEnabledOnlyWhileCharging != (mEnabledOnlyWhileCharging = enabled);
        saveOption(context, KEY_ONLY_WHILE_CHARGING, enabled, listener, changed);

        if (changed) {
            ActiveModeService.handleState(context);
            KeyguardService.handleState(context);
        }
    }

    /**
     * Setter to allow notifications with a lower priority like Google Now.
     */
    public void setLowPriorityNotificationsAllowed(Context context, boolean enabled,
                                                   OnConfigChangedListener listener) {
        boolean changed = mNotifyLowPriority != (mNotifyLowPriority = enabled);
        saveOption(context, KEY_LOW_PRIORITY_NOTIFICATIONS, enabled, listener, changed);
    }

    /**
     * Setter to allow the screen to time out or not.
     */
    public void setTimeoutEnabled(Context context, boolean enabled, OnConfigChangedListener listener) {
        boolean changed = mTimeoutEnabled != (mTimeoutEnabled = enabled);
        saveOption(context, KEY_TIMEOUT_ENABLED, enabled, listener, changed);
    }

    /**
     * Setter to set the timeout in a normal situation.
     */
    // used via reflections!
    public void setTimeoutNormal(Context context, int delayMillis, OnConfigChangedListener listener) {
        boolean changed = mTimeoutNormal != (mTimeoutNormal = delayMillis);
        saveOption(context, KEY_TIMEOUT_NORMAL, delayMillis, listener, changed);
    }

    /**
     * Setter for short timeout time.
     */
    // used via reflections!
    public void setTimeoutShort(Context context, int delayMillis, OnConfigChangedListener listener) {
        boolean changed = mTimeoutShort != (mTimeoutShort = delayMillis);
        saveOption(context, KEY_TIMEOUT_SHORT, delayMillis, listener, changed);
    }

    /**
     * Setter to enable "night mode".
     */
    public void setInactiveTimeEnabled(Context context, boolean enabled, OnConfigChangedListener listener) {
        boolean changed = mInactiveTimeEnabled != (mInactiveTimeEnabled = enabled);
        saveOption(context, KEY_INACTIVE_TIME_ENABLED, enabled, listener, changed);
    }

    /**
     * Setter for the time "night mode" should start
     */
    public void setInactiveTimeFrom(Context context, int minutes, OnConfigChangedListener listener) {
        boolean changed = mInactiveTimeFrom != (mInactiveTimeFrom = minutes);
        saveOption(context, KEY_INACTIVE_TIME_FROM, minutes, listener, changed);
    }

    /**
     * Setter for the time "night mode" should end.
     */
    public void setInactiveTimeTo(Context context, int minutes, OnConfigChangedListener listener) {
        boolean changed = mInactiveTimeTo != (mInactiveTimeTo = minutes);
        saveOption(context, KEY_INACTIVE_TIME_TO, minutes, listener, changed);
    }

    /**
     * Setter to allow the wallpaper to be shown instead of black
     */
    public void setWallpaperShown(Context context, boolean shown, OnConfigChangedListener listener) {
        boolean changed = mUiWallpaper != (mUiWallpaper = shown);
        saveOption(context, KEY_UI_WALLPAPER_SHOWN, shown, listener, changed);
    }

    public void setShadowEnabled(Context context, boolean shown, OnConfigChangedListener listener) {
        boolean changed = mUiWallpaperShadow != (mUiWallpaperShadow = shown);
        saveOption(context, KEY_UI_SHADOW_TOGGLE, shown, listener, changed);
    }

    /**
     * Allow the background to change based on the notification.
     */
    public void setDynamicBackgroundMode(Context context, int mode, OnConfigChangedListener listener) {
        boolean changed = mUiDynamicBackground != (mUiDynamicBackground = mode);
        saveOption(context, KEY_UI_DYNAMIC_BACKGROUND_MODE, mode, listener, changed);
    }

    /**
     * Allow the timeout bar to move in from both sides.
     */
    public void setMirroredTimeoutProgressBarEnabled(Context context, boolean enabled, OnConfigChangedListener listener) {
        boolean changed = mUiMirroredTimeoutBar != (mUiMirroredTimeoutBar = enabled);
        saveOption(context, KEY_UI_MIRRORED_TIMEOUT_BAR, enabled, listener, changed);
    }

    public void setCircledLargeIconEnabled(Context context, boolean enabled, OnConfigChangedListener listener) {
        boolean changed = mUiNotifyCircledIcon != (mUiNotifyCircledIcon = enabled);
        saveOption(context, KEY_UI_NOTIFY_CIRCLED_ICON, enabled, listener, changed);
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

    public int getDynamicBackgroundMode() {
        return mUiDynamicBackground;
    }

    public boolean isEnabled() {
        return mAcDisplayEnabled;
    }

    public boolean isKeyguardEnabled() {
        return mKeyguardEnabled;
    }

    public boolean isActiveModeEnabled() {
        return mActiveMode;
    }

    public boolean isActiveModeWithoutNotifiesEnabled() {
        return mActiveModeWithoutNotifies;
    }

    public boolean isEnabledOnlyWhileCharging() {
        return mEnabledOnlyWhileCharging;
    }

    public boolean isLowPriorityNotificationsAllowed() {
        return mNotifyLowPriority;
    }

    public boolean isWallpaperShown() {
        return mUiWallpaper;
    }

    public boolean isShadowEnabled() {
        return mUiWallpaperShadow;
    }

    public boolean isCircledLargeIconEnabled() {
        return mUiNotifyCircledIcon;
    }

    public boolean isMirroredTimeoutProgressBarEnabled() {
        return mUiMirroredTimeoutBar;
    }

    public boolean isInactiveTimeEnabled() {
        return mInactiveTimeEnabled;
    }

    public boolean isTimeoutEnabled() {
        return mTimeoutEnabled;
    }

}
