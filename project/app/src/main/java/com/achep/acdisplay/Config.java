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
package com.achep.acdisplay;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import com.achep.acdisplay.interfaces.IOnLowMemory;
import com.achep.acdisplay.powertoggles.ToggleReceiver;
import com.achep.acdisplay.services.KeyguardService;
import com.achep.acdisplay.services.SensorsDumpService;
import com.achep.acdisplay.services.activemode.ActiveModeService;
import com.achep.acdisplay.services.headsup.HeadsUpService;
import com.achep.acdisplay.utils.AccessUtils;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Saves all the configurations for the app.
 *
 * @author Artem Chepurnoy
 * @since 21.01.14
 */
@SuppressWarnings("ConstantConditions")
public class Config implements IOnLowMemory {

    private static final String TAG = "Config";

    private static final String PREFERENCES_FILE_NAME = "config";

    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_ONLY_WHILE_CHARGING = "only_while_charging";

    // notifications
    public static final String KEY_NOTIFY_LOW_PRIORITY = "notify_low_priority";
    public static final String KEY_NOTIFY_WAKE_UP_ON = "notify_wake_up_on";

    // inactive time
    public static final String KEY_INACTIVE_TIME_FROM = "inactive_time_from";
    public static final String KEY_INACTIVE_TIME_TO = "inactive_time_to";
    public static final String KEY_INACTIVE_TIME_ENABLED = "inactive_time_enabled";

    // timeouts
    public static final String KEY_TIMEOUT_NORMAL = "timeout_normal";
    public static final String KEY_TIMEOUT_SHORT = "timeout_short";

    // keyguard
    public static final String KEY_KEYGUARD = "keyguard";

    // active mode
    public static final String KEY_ACTIVE_MODE = "active_mode";
    public static final String KEY_ACTIVE_MODE_WITHOUT_NOTIFICATIONS = "active_mode_without_notifications";

    // heads up
    public static final String KEY_HEADS_UP = "heads_up";
    public static final String KEY_HEADS_UP_STYLE = "heads_up_style";

    // interface
    public static final String KEY_UI_FULLSCREEN = "ui_fullscreen";
    public static final String KEY_UI_WALLPAPER_SHOWN = "wallpaper_shown";
    public static final String KEY_UI_SHADOW_TOGGLE = "shadow_toggle";
    public static final String KEY_UI_DYNAMIC_BACKGROUND_MODE = "dynamic_background_mode";
    public static final int DYNAMIC_BG_ARTWORK_MASK = 1;
    public static final int DYNAMIC_BG_NOTIFICATION_MASK = 2;
    public static final String KEY_UI_MIRRORED_TIMEOUT_BAR = "mirrored_timeout_progress_bar";
    public static final String KEY_UI_NOTIFY_CIRCLED_ICON = "notify_circled_icon";
    public static final String KEY_UI_STATUS_BATTERY_STICKY = "ui_status_battery_sticky";
    public static final String KEY_UI_ICON_SIZE = "ui_condensed_view_size";
    public static final String ICON_SIZE_PX = "px";
    public static final String ICON_SIZE_DP = "dp";
    public static final String KEY_UI_UNLOCK_ANIMATION = "unlock_animation";

    // behavior
    public static final String KEY_FEEL_SCREEN_OFF_AFTER_LAST_NOTIFY = "feel_widget_screen_off_after_last_notify";
    public static final String KEY_FEEL_WIDGET_PINNABLE = "feel_widget_pinnable";
    public static final String KEY_FEEL_WIDGET_READABLE = "feel_widget_readable";

    // development
    public static final String KEY_DEV_SENSORS_DUMP = "dev_sensors_dump";

    // triggers
    public static final String KEY_TRIG_PREVIOUS_VERSION = "trigger_previous_version";
    public static final String KEY_TRIG_HELP_READ = "trigger_help_read";

    private static Config sConfig;

    private boolean mAcDisplayEnabled;
    private boolean mKeyguardEnabled;
    private boolean mActiveMode;
    private boolean mActiveModeWithoutNotifies;
    private boolean mHeadsUpEnabled;
    private boolean mEnabledOnlyWhileCharging;
    private boolean mScreenOffAfterLastNotify;
    private boolean mFeelWidgetPinnable;
    private boolean mFeelWidgetReadable;
    private boolean mNotifyLowPriority;
    private boolean mNotifyWakeUpOn;
    private String mHeadsUpStyle;
    private int mTimeoutNormal;
    private int mTimeoutShort;
    private int mInactiveTimeFrom;
    private int mInactiveTimeTo;
    private int mUiDynamicBackground;
    private int mUiIconSize; // dp.
    private boolean mInactiveTimeEnabled;
    private boolean mUiFullScreen;
    private boolean mUiWallpaper;
    private boolean mUiWallpaperShadow;
    private boolean mUiMirroredTimeoutBar;
    private boolean mUiBatterySticky;
    private boolean mUiNotifyCircledIcon;
    private boolean mUiUnlockAnimation;

    private boolean mDevSensorsDump;

    private final Triggers mTriggers;
    private int mTrigPreviousVersion;
    private boolean mTrigHelpRead;

    @NonNull
    private SoftReference<HashMap<String, Option>> mHashMapRef = new SoftReference<>(null);

    public static class Option {
        private final String setterName;
        private final String getterName;
        private final Class clazz;
        private final int minSdkVersion;

        public Option(String setterName,
                      String getterName,
                      Class clazz) {
            this(setterName, getterName, clazz, 0);
        }

        public Option(String setterName,
                      String getterName,
                      Class clazz, int minSdkVersion) {
            this.setterName = setterName;
            this.getterName = getterName;
            this.clazz = clazz;
            this.minSdkVersion = minSdkVersion;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return new HashCodeBuilder(11, 31)
                    .append(setterName)
                    .append(getterName)
                    .append(clazz)
                    .toHashCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if (o == null)
                return false;
            if (o == this)
                return true;
            if (!(o instanceof Option))
                return false;

            Option option = (Option) o;
            return new EqualsBuilder()
                    .append(setterName, option.setterName)
                    .append(getterName, option.getterName)
                    .append(clazz, option.clazz)
                    .isEquals();
        }

        /**
         * Reads an option from given config instance.</br>
         * Reading is done using reflections!
         *
         * @param config a config to read from.
         * @throws java.lang.RuntimeException if failed to read given config.
         */
        @NonNull
        public Object read(@NonNull Config config) {
            Object configInstance = getConfigInstance(config);
            Class configClass = configInstance.getClass();
            try {
                Method method = configClass.getDeclaredMethod(getterName);
                method.setAccessible(true);
                return method.invoke(configInstance);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException("Failed to access " + clazz.getName() + "." + getterName + " method.");
            }
        }


        /**
         * Writes new value to the option to given config instance.</br>
         * Writing is done using reflections!
         *
         * @param config a config to write to.
         * @throws java.lang.RuntimeException if failed to read given config.
         */
        public void write(@NonNull Config config, @NonNull Context context,
                          @NonNull Object newValue, @Nullable OnConfigChangedListener listener) {
            Object configInstance = getConfigInstance(config);
            Class configClass = configInstance.getClass();
            try {
                Method method = configClass.getDeclaredMethod(setterName,
                        Context.class, clazz,
                        Config.OnConfigChangedListener.class);
                method.setAccessible(true);
                method.invoke(configInstance, context, newValue, listener);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException("Failed to access " + clazz.getName() + "." + setterName + " method.");
            }
        }

        @NonNull
        protected Object getConfigInstance(Config config) {
            return config;
        }

    }

    private ArrayList<OnConfigChangedListener> mListeners;
    private Context mContext;

    // //////////////////////////////////////////
    // /////////// -- LISTENERS -- //////////////
    // //////////////////////////////////////////

    public interface OnConfigChangedListener {
        public void onConfigChanged(
                @NonNull Config config,
                @NonNull String key,
                @NonNull Object value);
    }

    public void registerListener(@NonNull OnConfigChangedListener listener) {
        mListeners.add(listener);
    }

    public void unregisterListener(@NonNull OnConfigChangedListener listener) {
        mListeners.remove(listener);
    }

    // //////////////////////////////////////////
    // ///////////// -- INIT -- /////////////////
    // //////////////////////////////////////////

    @NonNull
    public static synchronized Config getInstance() {
        if (sConfig == null) {
            sConfig = new Config();
        }
        return sConfig;
    }

    private Config() {
        mTriggers = new Triggers();
    }

    @Override
    public void onLowMemory() {
        // Clear hash-map; it will be recreated on #getHashMap().
        mHashMapRef.clear();
    }

    /**
     * Loads saved values from shared preferences.
     * This is called on {@link App app's} create.
     */
    void init(@NonNull Context context) {
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
        mHeadsUpEnabled = prefs.getBoolean(KEY_HEADS_UP,
                res.getBoolean(R.bool.config_default_headsup_enabled));
        mHeadsUpStyle = prefs.getString(KEY_HEADS_UP_STYLE,
                res.getString(R.string.config_default_heads_up_style));

        // notifications
        mNotifyLowPriority = prefs.getBoolean(KEY_NOTIFY_LOW_PRIORITY,
                res.getBoolean(R.bool.config_default_notify_low_priority));
        mNotifyWakeUpOn = prefs.getBoolean(KEY_NOTIFY_WAKE_UP_ON,
                res.getBoolean(R.bool.config_default_notify_wake_up_on));

        // timeout
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
        mUiBatterySticky = prefs.getBoolean(KEY_UI_STATUS_BATTERY_STICKY,
                res.getBoolean(R.bool.config_default_ui_status_battery_sticky));
        mUiFullScreen = prefs.getBoolean(KEY_UI_FULLSCREEN,
                res.getBoolean(R.bool.config_default_ui_full_screen));
        mUiUnlockAnimation = prefs.getBoolean(KEY_UI_UNLOCK_ANIMATION,
                res.getBoolean(R.bool.config_default_ui_unlock_animation));
        mUiIconSize = prefs.getInt(KEY_UI_ICON_SIZE,
                res.getInteger(R.integer.config_default_ui_icon_size_dp));

        // development
        mDevSensorsDump = prefs.getBoolean(KEY_DEV_SENSORS_DUMP,
                res.getBoolean(R.bool.config_default_dev_sensors_dump));

        // other
        mEnabledOnlyWhileCharging = prefs.getBoolean(KEY_ONLY_WHILE_CHARGING,
                res.getBoolean(R.bool.config_default_enabled_only_while_charging));
        mScreenOffAfterLastNotify = prefs.getBoolean(KEY_FEEL_SCREEN_OFF_AFTER_LAST_NOTIFY,
                res.getBoolean(R.bool.config_default_feel_screen_off_after_last_notify));
        mFeelWidgetPinnable = prefs.getBoolean(KEY_FEEL_WIDGET_PINNABLE,
                res.getBoolean(R.bool.config_default_feel_widget_pinnable));
        mFeelWidgetReadable = prefs.getBoolean(KEY_FEEL_WIDGET_READABLE,
                res.getBoolean(R.bool.config_default_feel_widget_readable));

        // triggers
        mTrigHelpRead = prefs.getBoolean(KEY_TRIG_HELP_READ, false);
        mTrigPreviousVersion = prefs.getInt(KEY_TRIG_PREVIOUS_VERSION, 0);
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

    @NonNull
    public HashMap<String, Option> getHashMap() {
        HashMap<String, Option> hashMap = mHashMapRef.get();
        if (hashMap == null) {
            hashMap = new HashMap<>();
            hashMap.put(KEY_ENABLED, new Option(
                    "setEnabled", "isEnabled", boolean.class));
            hashMap.put(KEY_KEYGUARD, new Option(
                    "setKeyguardEnabled", "isKeyguardEnabled", boolean.class));
            hashMap.put(KEY_ACTIVE_MODE, new Option(
                    "setActiveModeEnabled", "isActiveModeEnabled", boolean.class));
            hashMap.put(KEY_ACTIVE_MODE_WITHOUT_NOTIFICATIONS, new Option(
                    "setActiveModeWithoutNotificationsEnabled",
                    "isActiveModeWithoutNotifiesEnabled", boolean.class));
            hashMap.put(KEY_HEADS_UP, new Option(
                    "setHeadsUpEnabled",
                    "isHeadsUpEnabled", boolean.class));
            hashMap.put(KEY_NOTIFY_LOW_PRIORITY, new Option(
                    "setLowPriorityNotificationsAllowed",
                    "isLowPriorityNotificationsAllowed", boolean.class));
            hashMap.put(KEY_NOTIFY_WAKE_UP_ON, new Option(
                    "setWakeUpOnNotifyEnabled",
                    "isNotifyWakingUp", boolean.class));
            hashMap.put(KEY_ONLY_WHILE_CHARGING, new Option(
                    "setEnabledOnlyWhileCharging",
                    "isEnabledOnlyWhileCharging", boolean.class));
            hashMap.put(KEY_UI_FULLSCREEN, new Option(
                    "setFullScreen", "isFullScreen", boolean.class));
            hashMap.put(KEY_UI_WALLPAPER_SHOWN, new Option(
                    "setWallpaperShown", "isWallpaperShown", boolean.class));
            hashMap.put(KEY_UI_SHADOW_TOGGLE, new Option(
                    "setShadowEnabled", "isShadowEnabled", boolean.class));
            hashMap.put(KEY_UI_MIRRORED_TIMEOUT_BAR, new Option(
                    "setMirroredTimeoutProgressBarEnabled",
                    "isMirroredTimeoutProgressBarEnabled", boolean.class));
            hashMap.put(KEY_UI_NOTIFY_CIRCLED_ICON, new Option(
                    "setCircledLargeIconEnabled",
                    "isCircledLargeIconEnabled", boolean.class));
            hashMap.put(KEY_UI_STATUS_BATTERY_STICKY, new Option(
                    "setStatusBatterySticky",
                    "isStatusBatterySticky", boolean.class));
            hashMap.put(KEY_UI_UNLOCK_ANIMATION, new Option(
                    "setUnlockAnimationEnabled",
                    "isUnlockAnimationEnabled", boolean.class));
            hashMap.put(KEY_FEEL_SCREEN_OFF_AFTER_LAST_NOTIFY, new Option(
                    "setScreenOffAfterLastNotify",
                    "isScreenOffAfterLastNotify", boolean.class));
            hashMap.put(KEY_FEEL_WIDGET_PINNABLE, new Option(
                    "setWidgetPinnable",
                    "isWidgetPinnable", boolean.class));
            hashMap.put(KEY_FEEL_WIDGET_READABLE, new Option(
                    "setWidgetReadable",
                    "isWidgetReadable", boolean.class));
            hashMap.put(KEY_DEV_SENSORS_DUMP, new Option(
                    "setDevSensorsDumpEnabled",
                    "isDevSensorsDumpEnabled", boolean.class));

            mHashMapRef = new SoftReference<>(hashMap);
        }
        return hashMap;
    }

    /**
     * Separated group of different internal triggers.
     */
    @NonNull
    public Triggers getTriggers() {
        return mTriggers;
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

        if (Build.DEBUG) Log.d(TAG, "Writing \"" + key + "=" + value + "\" to config.");

        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof String) {
            editor.putString(key, (String) value);
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
        boolean changed = mAcDisplayEnabled != (mAcDisplayEnabled = enabled);

        if (!changed) {
            return true;
        }
        if (enabled
                && !(AccessUtils.isNotificationAccessGranted(context)
                && AccessUtils.isDeviceAdminAccessGranted(context))) {
            return false;
        }

        saveOption(context, KEY_ENABLED, enabled, listener, changed);

        if (changed) {
            ActiveModeService.handleState(context);
            KeyguardService.handleState(context);
        }

        ToggleReceiver.sendStateUpdate(ToggleReceiver.class, enabled, context);
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

    public void setHeadsUpEnabled(Context context, boolean enabled, OnConfigChangedListener listener) {
        boolean changed = mHeadsUpEnabled != (mHeadsUpEnabled = enabled);
        saveOption(context, KEY_HEADS_UP, enabled, listener, changed);

        if (changed) {
            HeadsUpService.handleState(context);
        }
    }

    public void setHeadsUpStyle(Context context, String style, OnConfigChangedListener listener) {
        boolean changed = !mHeadsUpStyle.equals(mHeadsUpStyle = style);
        saveOption(context, KEY_HEADS_UP_STYLE, style, listener, changed);
    }

    public void setActiveModeWithoutNotificationsEnabled(Context context, boolean enabled, OnConfigChangedListener listener) {
        boolean changed = mActiveModeWithoutNotifies != (mActiveModeWithoutNotifies = enabled);
        saveOption(context, KEY_ACTIVE_MODE_WITHOUT_NOTIFICATIONS, enabled, listener, changed);
    }

    /**
     * Setter to only have the app running while charging.
     */
    public void setEnabledOnlyWhileCharging(Context context, boolean enabled,
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
        saveOption(context, KEY_NOTIFY_LOW_PRIORITY, enabled, listener, changed);
    }

    public void setWakeUpOnNotifyEnabled(Context context, boolean enabled,
                                         OnConfigChangedListener listener) {
        boolean changed = mNotifyWakeUpOn != (mNotifyWakeUpOn = enabled);
        saveOption(context, KEY_NOTIFY_WAKE_UP_ON, enabled, listener, changed);
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

    public void setStatusBatterySticky(Context context, boolean visible, OnConfigChangedListener listener) {
        boolean changed = mUiBatterySticky != (mUiBatterySticky = visible);
        saveOption(context, KEY_UI_STATUS_BATTERY_STICKY, visible, listener, changed);
    }

    public void setWidgetPinnable(Context context, boolean pinnable, OnConfigChangedListener listener) {
        boolean changed = mFeelWidgetPinnable != (mFeelWidgetPinnable = pinnable);
        saveOption(context, KEY_FEEL_WIDGET_PINNABLE, pinnable, listener, changed);
    }

    public void setWidgetReadable(Context context, boolean readable, OnConfigChangedListener listener) {
        boolean changed = mFeelWidgetReadable != (mFeelWidgetReadable = readable);
        saveOption(context, KEY_FEEL_WIDGET_READABLE, readable, listener, changed);
    }

    /**
     * Setter for Immersive Mode
     */
    public void setFullScreen(Context context, boolean enabled, OnConfigChangedListener listener) {
        boolean changed = mUiFullScreen != (mUiFullScreen = enabled);
        saveOption(context, KEY_UI_FULLSCREEN, enabled, listener, changed);
    }

    /**
     * Sets the size (or height only) of collapsed views.
     *
     * @param size preferred size in dip.
     * @see #getIconSizePx()
     * @see #getIconSize(String)
     */
    public void setIconSizeDp(Context context, int size, OnConfigChangedListener listener) {
        boolean changed = mUiIconSize != (mUiIconSize = size);
        saveOption(context, KEY_UI_ICON_SIZE, size, listener, changed);
    }

    /**
     * Setter to turn the screen off after dismissing the last notification.
     */
    public void setScreenOffAfterLastNotify(Context context, boolean enabled, OnConfigChangedListener listener) {
        boolean changed = mScreenOffAfterLastNotify != (mScreenOffAfterLastNotify = enabled);
        saveOption(context, KEY_FEEL_SCREEN_OFF_AFTER_LAST_NOTIFY, enabled, listener, changed);
    }

    /**
     * Setter to turn the screen off after dismissing the last notification.
     */
    public void setUnlockAnimationEnabled(Context context, boolean enabled, OnConfigChangedListener listener) {
        boolean changed = mUiUnlockAnimation != (mUiUnlockAnimation = enabled);
        saveOption(context, KEY_UI_UNLOCK_ANIMATION, enabled, listener, changed);
    }

    public void setDevSensorsDumpEnabled(Context context, boolean enabled, OnConfigChangedListener listener) {
        boolean changed = mDevSensorsDump != (mDevSensorsDump = enabled);
        saveOption(context, KEY_DEV_SENSORS_DUMP, enabled, listener, changed);

        if (changed) {
            SensorsDumpService.handleState(context);
        }
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

    public String getHeadsUpStyle() {
        return mHeadsUpStyle;
    }

    /**
     * @return the size (or height only) of collapsed views in pixels.
     * @see #getIconSize(String)
     */
    public int getIconSizePx() {
        return getIconSize(ICON_SIZE_PX);
    }

    /**
     * @return the size (or height only) of collapsed views.
     * @see #getIconSizePx()
     * @see #ICON_SIZE_DP
     * @see #ICON_SIZE_PX
     */
    public int getIconSize(String type) {
        switch (type) {
            case ICON_SIZE_PX:
                DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
                return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mUiIconSize, dm);
            case ICON_SIZE_DP:
                return mUiIconSize;
            default:
                throw new IllegalArgumentException(type + " is not a valid icon size type.");
        }
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

    public boolean isHeadsUpEnabled() {
        return mHeadsUpEnabled;
    }

    public boolean isActiveModeWithoutNotifiesEnabled() {
        return mActiveModeWithoutNotifies;
    }

    public boolean isEnabledOnlyWhileCharging() {
        return mEnabledOnlyWhileCharging;
    }

    public boolean isNotifyWakingUp() {
        return mNotifyWakeUpOn;
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

    public boolean isStatusBatterySticky() {
        return mUiBatterySticky;
    }

    public boolean isWidgetPinnable() {
        return mFeelWidgetPinnable;
    }

    public boolean isWidgetReadable() {
        return mFeelWidgetReadable;
    }

    public boolean isMirroredTimeoutProgressBarEnabled() {
        return mUiMirroredTimeoutBar;
    }

    public boolean isInactiveTimeEnabled() {
        return mInactiveTimeEnabled;
    }

    public boolean isFullScreen() {
        return mUiFullScreen;
    }

    public boolean isScreenOffAfterLastNotify() {
        return mScreenOffAfterLastNotify;
    }

    public boolean isUnlockAnimationEnabled() {
        return mUiUnlockAnimation;
    }

    public boolean isDevSensorsDumpEnabled() {
        return mDevSensorsDump;
    }

    /**
     * A class that syncs {@link android.preference.Preference} with its
     * value in config.
     *
     * @author Artem Chepurnoy
     */
    public static class Syncer {

        private final ArrayList<Group> mGroups;
        private final Context mContext;
        private final Config mConfig;

        private boolean mBroadcasting;
        private boolean mStarted;

        private final Preference.OnPreferenceChangeListener mPreferenceListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mBroadcasting) {
                    return true;
                }

                Group group = null;
                for (Group c : mGroups) {
                    if (preference == c.preference) {
                        group = c;
                        break;
                    }
                }

                assert group != null;

                group.option.write(mConfig, mContext, newValue, mConfigListener);
                return true;
            }
        };

        private final OnConfigChangedListener mConfigListener = new OnConfigChangedListener() {

            @Override
            public void onConfigChanged(@NonNull Config config, @NonNull String key,
                                        @NonNull Object value) {
                Group group = null;
                for (Group c : mGroups) {
                    if (key.equals(c.preference.getKey())) {
                        group = c;
                        break;
                    }
                }

                if (group == null) {
                    return;
                }

                setPreferenceValue(group, value);
            }

        };

        private void setPreferenceValue(@NonNull Group group, @NonNull Object value) {
            mBroadcasting = true;

            Option option = group.option;
            if (option.clazz.equals(boolean.class)) {
                CheckBoxPreference preference = (CheckBoxPreference) group.preference;
                preference.setChecked((boolean) value);
            }

            mBroadcasting = false;
        }

        /**
         * A class-merge of {@link android.preference.Preference}
         * and its {@link com.achep.acdisplay.Config.Option}.
         *
         * @author Artem Chepurnoy
         */
        private static class Group {
            final Preference preference;
            final Option option;

            public Group(@NonNull Config config, @NonNull Preference preference) {
                this.preference = preference;
                this.option = config.getHashMap().get(preference.getKey());
            }
        }

        public Syncer(@NonNull Context context, @NonNull Config config) {
            mGroups = new ArrayList<>(10);
            mContext = context;
            mConfig = config;
        }

        @NonNull
        public Syncer addPreference(@Nullable PreferenceScreen preferenceScreen,
                                    @NonNull Preference preference) {
            Group group;
            if (preference instanceof CheckBoxPreference) {
                group = new Group(mConfig, preference);
            } else {
                throw new IllegalArgumentException("Syncer only supports some kinds of Preferences");
            }

            // Remove preference from preference screen
            // if needed.
            if (preferenceScreen != null) {
                if (!Device.hasTargetApi(group.option.minSdkVersion)) {
                    preferenceScreen.removePreference(preference);
                    return this;
                }
            }

            mGroups.add(group);

            if (mStarted) {
                startListeningGroup(group);
            }

            return this;
        }

        /**
         * Updates all preferences and starts to listen to the changes.
         */
        public void start() {
            mStarted = true;
            mConfig.registerListener(mConfigListener);
            for (Group group : mGroups) {
                startListeningGroup(group);
            }
        }

        private void startListeningGroup(@NonNull Group group) {
            group.preference.setOnPreferenceChangeListener(mPreferenceListener);
            setPreferenceValue(group, group.option.read(mConfig));
        }

        /**
         * Stops to listen to the changes.
         */
        public void stop() {
            mStarted = false;
            mConfig.unregisterListener(mConfigListener);
            for (Group group : mGroups) {
                group.preference.setOnPreferenceChangeListener(null);
            }
        }
    }

    // //////////////////////////////////////////
    // //////////// -- TRIGGERS -- //////////////
    // //////////////////////////////////////////

    /**
     * Contains
     *
     * @author Artem Chepurnoy
     */
    public class Triggers {

        public void setPreviousVersion(Context context, int versionCode, OnConfigChangedListener listener) {
            boolean changed = mTrigPreviousVersion != (mTrigPreviousVersion = versionCode);
            saveOption(context, KEY_TRIG_PREVIOUS_VERSION, versionCode, listener, changed);
        }

        public void setHelpRead(Context context, boolean isRead, OnConfigChangedListener listener) {
            boolean changed = mTrigHelpRead != (mTrigHelpRead = isRead);
            saveOption(context, KEY_TRIG_HELP_READ, isRead, listener, changed);
        }

        /**
         * As set by {@link com.achep.acdisplay.activities.MainActivity}, it returns version
         * code of previously installed AcDisplay, {@code 0} if first install.
         *
         * @return version code of previously installed AcDisplay, {@code 0} if first install.
         * @see #setPreviousVersion(android.content.Context, int, Config.OnConfigChangedListener)
         */
        public int getPreviousVersion() {
            return mTrigPreviousVersion;
        }

        /**
         * @return {@code true} if {@link com.achep.acdisplay.fragments.HelpDialog} been read, {@code false} otherwise
         * @see #setHelpRead(android.content.Context, boolean, Config.OnConfigChangedListener)
         */
        public boolean isHelpRead() {
            return mTrigHelpRead;
        }

    }

}
