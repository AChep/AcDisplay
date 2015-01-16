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
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.achep.acdisplay.plugins.powertoggles.ToggleReceiver;
import com.achep.acdisplay.services.KeyguardService;
import com.achep.acdisplay.services.SensorsDumpService;
import com.achep.acdisplay.services.activemode.ActiveModeService;
import com.achep.acdisplay.utils.AccessUtils;
import com.achep.base.content.ConfigBase;

import java.util.HashMap;

/**
 * Saves all the configurations for the app.
 *
 * @author Artem Chepurnoy
 * @since 21.01.14
 */
@SuppressWarnings("ConstantConditions")
public final class Config extends ConfigBase {

    private static final String TAG = "Config";

    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_ONLY_WHILE_CHARGING = "only_while_charging";

    // notifications
    public static final String KEY_NOTIFY_MIN_PRIORITY = "notify_min_priority";
    public static final String KEY_NOTIFY_MAX_PRIORITY = "notify_max_priority";
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
    public static final String KEY_KEYGUARD_WITHOUT_NOTIFICATIONS = "keyguard_without_notifications";

    // active mode
    public static final String KEY_ACTIVE_MODE = "active_mode";
    public static final String KEY_ACTIVE_MODE_WITHOUT_NOTIFICATIONS = "active_mode_without_notifications";

    // interface
    public static final String KEY_UI_FULLSCREEN = "ui_fullscreen";
    public static final String KEY_UI_WALLPAPER_SHOWN = "wallpaper_shown";
    public static final String KEY_UI_DYNAMIC_BACKGROUND_MODE = "dynamic_background_mode";
    public static final int DYNAMIC_BG_ARTWORK_MASK = 1;
    public static final int DYNAMIC_BG_NOTIFICATION_MASK = 2;
    public static final String KEY_UI_STATUS_BATTERY_STICKY = "ui_status_battery_sticky";
    public static final String KEY_UI_ICON_SIZE = "ui_condensed_view_size";
    public static final String ICON_SIZE_PX = "px";
    public static final String ICON_SIZE_DP = "dp";
    public static final String KEY_UI_UNLOCK_ANIMATION = "unlock_animation";
    public static final String KEY_UI_CIRCLE_COLOR_INNER = "ui_circle_color_inner";
    public static final String KEY_UI_CIRCLE_COLOR_OUTER = "ui_circle_color_outer";
    public static final String KEY_UI_OVERRIDE_FONTS = "ui_override_fonts";

    // behavior
    public static final String KEY_FEEL_SCREEN_OFF_AFTER_LAST_NOTIFY = "feel_widget_screen_off_after_last_notify";
    public static final String KEY_FEEL_WIDGET_PINNABLE = "feel_widget_pinnable";
    public static final String KEY_FEEL_WIDGET_READABLE = "feel_widget_readable";

    // development
    public static final String KEY_DEV_SENSORS_DUMP = "dev_sensors_dump";

    // triggers
    public static final String KEY_TRIG_PREVIOUS_VERSION = "trigger_previous_version";
    public static final String KEY_TRIG_HELP_READ = "trigger_dialog_help";
    public static final String KEY_TRIG_TRANSLATED = "trigger_translated";
    public static final String KEY_TRIG_LAUNCH_COUNT = "trigger_launch_count";
    public static final String KEY_TRIG_DONATION_ASKED = "trigger_donation_asked";

    private static Config sConfig;

    private boolean mAcDisplayEnabled;
    private boolean mKeyguardEnabled;
    private boolean mKeyguardWithoutNotifies;
    private boolean mActiveMode;
    private boolean mActiveModeWithoutNotifies;
    private boolean mEnabledOnlyWhileCharging;
    private boolean mScreenOffAfterLastNotify;
    private boolean mFeelWidgetPinnable;
    private boolean mFeelWidgetReadable;
    private boolean mNotifyWakeUpOn;
    private int mNotifyMinPriority;
    private int mNotifyMaxPriority;
    private int mTimeoutNormal;
    private int mTimeoutShort;
    private int mInactiveTimeFrom;
    private int mInactiveTimeTo;
    private int mUiDynamicBackground;
    private int mUiIconSize; // dp.
    private int mUiCircleColorInner;
    private int mUiCircleColorOuter;
    private boolean mInactiveTimeEnabled;
    private boolean mUiFullScreen;
    private boolean mUiOverrideFonts;
    private boolean mUiWallpaper;
    private boolean mUiBatterySticky;
    private boolean mUiUnlockAnimation;

    private boolean mDevSensorsDump;

    private final Triggers mTriggers;
    private int mTrigPreviousVersion;
    private int mTrigLaunchCount;
    private boolean mTrigTranslated;
    private boolean mTrigHelpRead;
    private boolean mTrigDonationAsked;

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

    /**
     * Loads saved values from shared preferences.
     * This is called on {@link App app's} create.
     */
    void init(@NonNull Context context) {
        Resources res = context.getResources();
        SharedPreferences prefs = getSharedPreferences(context);
        mAcDisplayEnabled = prefs.getBoolean(KEY_ENABLED,
                res.getBoolean(R.bool.config_default_enabled));
        mKeyguardEnabled = prefs.getBoolean(KEY_KEYGUARD,
                res.getBoolean(R.bool.config_default_keyguard_enabled));
        mKeyguardWithoutNotifies = prefs.getBoolean(KEY_KEYGUARD_WITHOUT_NOTIFICATIONS,
                res.getBoolean(R.bool.config_default_keyguard_without_notifies_enabled));
        mActiveMode = prefs.getBoolean(KEY_ACTIVE_MODE,
                res.getBoolean(R.bool.config_default_active_mode_enabled));
        mActiveModeWithoutNotifies = prefs.getBoolean(KEY_ACTIVE_MODE_WITHOUT_NOTIFICATIONS,
                res.getBoolean(R.bool.config_default_active_mode_without_notifies_enabled));

        // notifications
        mNotifyMinPriority = prefs.getInt(KEY_NOTIFY_MIN_PRIORITY,
                res.getInteger(R.integer.config_default_notify_min_priority));
        mNotifyMaxPriority = prefs.getInt(KEY_NOTIFY_MAX_PRIORITY,
                res.getInteger(R.integer.config_default_notify_max_priority));
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
        mUiDynamicBackground = prefs.getInt(KEY_UI_DYNAMIC_BACKGROUND_MODE,
                res.getInteger(R.integer.config_default_ui_show_shadow_dynamic_bg));
        mUiBatterySticky = prefs.getBoolean(KEY_UI_STATUS_BATTERY_STICKY,
                res.getBoolean(R.bool.config_default_ui_status_battery_sticky));
        mUiFullScreen = prefs.getBoolean(KEY_UI_FULLSCREEN,
                res.getBoolean(R.bool.config_default_ui_full_screen));
        mUiUnlockAnimation = prefs.getBoolean(KEY_UI_UNLOCK_ANIMATION,
                res.getBoolean(R.bool.config_default_ui_unlock_animation));
        mUiIconSize = prefs.getInt(KEY_UI_ICON_SIZE,
                res.getInteger(R.integer.config_default_ui_icon_size_dp));
        mUiCircleColorInner = prefs.getInt(KEY_UI_CIRCLE_COLOR_INNER, 0xFFF0F0F0);
        mUiCircleColorOuter = prefs.getInt(KEY_UI_CIRCLE_COLOR_OUTER, 0xFF303030);
        mUiOverrideFonts = prefs.getBoolean(KEY_UI_OVERRIDE_FONTS,
                res.getBoolean(R.bool.config_default_ui_override_fonts));

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
        mTrigTranslated = prefs.getBoolean(KEY_TRIG_TRANSLATED, false);
        mTrigPreviousVersion = prefs.getInt(KEY_TRIG_PREVIOUS_VERSION, 0);
        mTrigLaunchCount = prefs.getInt(KEY_TRIG_LAUNCH_COUNT, 0);
        mTrigDonationAsked = prefs.getBoolean(KEY_TRIG_TRANSLATED, false);
    }

    @Override
    protected void onCreateHashMap(@NonNull HashMap<String, ConfigBase.Option> hashMap) {
        hashMap.put(KEY_ENABLED, new ConfigBase.Option(
                "setEnabled", "isEnabled", boolean.class));
        hashMap.put(KEY_KEYGUARD, new ConfigBase.Option(
                "setKeyguardEnabled", "isKeyguardEnabled", boolean.class));
        hashMap.put(KEY_KEYGUARD_WITHOUT_NOTIFICATIONS, new ConfigBase.Option(
                "setKeyguardWithoutNotificationsEnabled",
                "isKeyguardWithoutNotifiesEnabled", boolean.class));
        hashMap.put(KEY_ACTIVE_MODE, new ConfigBase.Option(
                "setActiveModeEnabled", "isActiveModeEnabled", boolean.class));
        hashMap.put(KEY_ACTIVE_MODE_WITHOUT_NOTIFICATIONS, new ConfigBase.Option(
                "setActiveModeWithoutNotificationsEnabled",
                "isActiveModeWithoutNotifiesEnabled", boolean.class));
        hashMap.put(KEY_NOTIFY_WAKE_UP_ON, new ConfigBase.Option(
                "setWakeUpOnNotifyEnabled",
                "isNotifyWakingUp", boolean.class));
        hashMap.put(KEY_NOTIFY_MIN_PRIORITY, new ConfigBase.Option(
                "setNotifyMinPriority",
                "getNotifyMinPriority", int.class));
        hashMap.put(KEY_NOTIFY_MAX_PRIORITY, new ConfigBase.Option(
                "setNotifyMaxPriority",
                "getNotifyMaxPriority", int.class));
        hashMap.put(KEY_ONLY_WHILE_CHARGING, new ConfigBase.Option(
                "setEnabledOnlyWhileCharging",
                "isEnabledOnlyWhileCharging", boolean.class));
        hashMap.put(KEY_UI_FULLSCREEN, new ConfigBase.Option(
                "setFullScreen", "isFullScreen", boolean.class));
        hashMap.put(KEY_UI_WALLPAPER_SHOWN, new ConfigBase.Option(
                "setWallpaperShown", "isWallpaperShown", boolean.class));
        hashMap.put(KEY_UI_STATUS_BATTERY_STICKY, new ConfigBase.Option(
                "setStatusBatterySticky",
                "isStatusBatterySticky", boolean.class));
        hashMap.put(KEY_UI_DYNAMIC_BACKGROUND_MODE, new ConfigBase.Option(
                "setDynamicBackgroundMode",
                "getDynamicBackgroundMode", int.class));
        hashMap.put(KEY_UI_CIRCLE_COLOR_INNER, new ConfigBase.Option(
                "setCircleInnerColor",
                "getCircleInnerColor", int.class));
        hashMap.put(KEY_UI_CIRCLE_COLOR_OUTER, new ConfigBase.Option(
                "setCircleOuterColor",
                "getCircleOuterColor", int.class));
        hashMap.put(KEY_UI_UNLOCK_ANIMATION, new ConfigBase.Option(
                "setUnlockAnimationEnabled",
                "isUnlockAnimationEnabled", boolean.class));
        hashMap.put(KEY_UI_OVERRIDE_FONTS, new ConfigBase.Option(
                "setOverridingFontsEnabled",
                "isOverridingFontsEnabled", boolean.class));
        hashMap.put(KEY_FEEL_SCREEN_OFF_AFTER_LAST_NOTIFY, new ConfigBase.Option(
                "setScreenOffAfterLastNotify",
                "isScreenOffAfterLastNotify", boolean.class));
        hashMap.put(KEY_FEEL_WIDGET_PINNABLE, new ConfigBase.Option(
                "setWidgetPinnable",
                "isWidgetPinnable", boolean.class));
        hashMap.put(KEY_FEEL_WIDGET_READABLE, new ConfigBase.Option(
                "setWidgetReadable",
                "isWidgetReadable", boolean.class));
        hashMap.put(KEY_DEV_SENSORS_DUMP, new ConfigBase.Option(
                "setDevSensorsDumpEnabled",
                "isDevSensorsDumpEnabled", boolean.class));
    }

    /**
     * Separated group of different internal triggers.
     */
    @NonNull
    public Triggers getTriggers() {
        return mTriggers;
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

    public void setKeyguardWithoutNotificationsEnabled(Context context, boolean enabled, OnConfigChangedListener listener) {
        boolean changed = mKeyguardWithoutNotifies != (mKeyguardWithoutNotifies = enabled);
        saveOption(context, KEY_KEYGUARD_WITHOUT_NOTIFICATIONS, enabled, listener, changed);
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
    public void setEnabledOnlyWhileCharging(Context context, boolean enabled,
                                            OnConfigChangedListener listener) {
        boolean changed = mEnabledOnlyWhileCharging != (mEnabledOnlyWhileCharging = enabled);
        saveOption(context, KEY_ONLY_WHILE_CHARGING, enabled, listener, changed);

        if (changed) {
            ActiveModeService.handleState(context);
            KeyguardService.handleState(context);
        }
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

    /**
     * Allow the background to change based on the notification.
     */
    public void setDynamicBackgroundMode(Context context, int mode, OnConfigChangedListener listener) {
        boolean changed = mUiDynamicBackground != (mUiDynamicBackground = mode);
        saveOption(context, KEY_UI_DYNAMIC_BACKGROUND_MODE, mode, listener, changed);
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

    public void setOverridingFontsEnabled(Context context, boolean enabled, OnConfigChangedListener listener) {
        boolean changed = mUiOverrideFonts != (mUiOverrideFonts = enabled);
        saveOption(context, KEY_UI_OVERRIDE_FONTS, enabled, listener, changed);
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

    public void setCircleInnerColor(Context context, int color, OnConfigChangedListener listener) {
        boolean changed = mUiCircleColorInner != (mUiCircleColorInner = color);
        saveOption(context, KEY_UI_CIRCLE_COLOR_INNER, color, listener, changed);
    }

    public void setCircleOuterColor(Context context, int color, OnConfigChangedListener listener) {
        boolean changed = mUiCircleColorOuter != (mUiCircleColorOuter = color);
        saveOption(context, KEY_UI_CIRCLE_COLOR_OUTER, color, listener, changed);
    }

    /**
     * Sets the minimum notification's priority to be shown.
     *
     * @param priority minimum notification's priority to be shown.
     * @see #getNotifyMinPriority()
     * @see #setNotifyMaxPriority(Context, int, OnConfigChangedListener)
     * @see android.app.Notification#priority
     */
    public void setNotifyMinPriority(Context context, int priority, OnConfigChangedListener listener) {
        boolean changed = mNotifyMinPriority != (mNotifyMinPriority = priority);
        saveOption(context, KEY_NOTIFY_MIN_PRIORITY, priority, listener, changed);
    }

    /**
     * Sets the maximum notification's priority to be shown.
     *
     * @param priority maximum notification's priority to be shown.
     * @see #getNotifyMaxPriority()
     * @see #setNotifyMinPriority(Context, int, OnConfigChangedListener)
     * @see android.app.Notification#priority
     */
    public void setNotifyMaxPriority(Context context, int priority, OnConfigChangedListener listener) {
        boolean changed = mNotifyMaxPriority != (mNotifyMaxPriority = priority);
        saveOption(context, KEY_NOTIFY_MAX_PRIORITY, priority, listener, changed);
    }

    /**
     * @return minimal {@link android.app.Notification#priority} of notification to be shown.
     * @see #setNotifyMinPriority(Context, int, OnConfigChangedListener)
     * @see #getNotifyMaxPriority()
     * @see android.app.Notification#priority
     */
    public int getNotifyMinPriority() {
        return mNotifyMinPriority;
    }

    /**
     * @return maximum {@link android.app.Notification#priority} of notification to be shown.
     * @see #setNotifyMaxPriority(Context, int, OnConfigChangedListener)
     * @see #getNotifyMinPriority()
     * @see android.app.Notification#priority
     */
    public int getNotifyMaxPriority() {
        return mNotifyMaxPriority;
    }

    public int getCircleInnerColor() {
        return mUiCircleColorInner;
    }

    public int getCircleOuterColor() {
        return mUiCircleColorOuter;
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

    public boolean isKeyguardWithoutNotifiesEnabled() {
        return mKeyguardWithoutNotifies;
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

    public boolean isNotifyWakingUp() {
        return mNotifyWakeUpOn;
    }

    public boolean isWallpaperShown() {
        return mUiWallpaper;
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

    public boolean isInactiveTimeEnabled() {
        return mInactiveTimeEnabled;
    }

    public boolean isFullScreen() {
        return mUiFullScreen;
    }

    public boolean isOverridingFontsEnabled() {
        return mUiOverrideFonts;
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

        public void setDonationAsked(Context context, boolean isAsked, OnConfigChangedListener listener) {
            boolean changed = mTrigDonationAsked != (mTrigDonationAsked = isAsked);
            saveOption(context, KEY_TRIG_DONATION_ASKED, isAsked, listener, changed);
        }

        public void setTranslated(Context context, boolean translated, OnConfigChangedListener listener) {
            boolean changed = mTrigTranslated != (mTrigTranslated = translated);
            saveOption(context, KEY_TRIG_TRANSLATED, translated, listener, changed);
        }

        /**
         * @param context
         * @param listener
         * @see #setLaunchCount(android.content.Context, int, com.achep.base.content.ConfigBase.OnConfigChangedListener)
         * @see #getLaunchCount()
         */
        public void incrementLaunchCount(Context context, OnConfigChangedListener listener) {
            setLaunchCount(context, getLaunchCount() + 1, listener);
        }

        /**
         * @param context
         * @param launchCount
         * @param listener
         * @see #incrementLaunchCount(android.content.Context, com.achep.base.content.ConfigBase.OnConfigChangedListener)
         * @see #getLaunchCount()
         */
        public void setLaunchCount(Context context, int launchCount, OnConfigChangedListener listener) {
            boolean changed = mTrigLaunchCount != (mTrigLaunchCount = launchCount);
            saveOption(context, KEY_TRIG_LAUNCH_COUNT, launchCount, listener, changed);
        }

        /**
         * As set by {@link com.achep.acdisplay.ui.activities.MainActivity}, it returns version
         * code of previously installed AcDisplay, {@code 0} if first install.
         *
         * @return version code of previously installed AcDisplay, {@code 0} on first installation.
         * @see #setPreviousVersion(android.content.Context, int, Config.OnConfigChangedListener)
         */
        public int getPreviousVersion() {
            return mTrigPreviousVersion;
        }

        /**
         * @return the number of {@link com.achep.acdisplay.ui.activities.AcDisplayActivity}'s creations.
         * @see #incrementLaunchCount(android.content.Context, com.achep.base.content.ConfigBase.OnConfigChangedListener)
         * @see #setLaunchCount(android.content.Context, int, com.achep.base.content.ConfigBase.OnConfigChangedListener)
         */
        public int getLaunchCount() {
            return mTrigLaunchCount;
        }

        /**
         * @return {@code true} if {@link com.achep.base.ui.fragments.dialogs.HelpDialog} been read,
         * {@code false} otherwise
         * @see #setHelpRead(android.content.Context, boolean, Config.OnConfigChangedListener)
         */
        public boolean isHelpRead() {
            return mTrigHelpRead;
        }

        /**
         * @return {@code true} if the app is fully translated to currently used locale,
         * {@code false} otherwise.
         * @see #setDonationAsked(android.content.Context, boolean, com.achep.base.content.ConfigBase.OnConfigChangedListener)
         */
        public boolean isTranslated() {
            return mTrigTranslated;
        }

        public boolean isDonationAsked() {
            return mTrigDonationAsked;
        }

    }

}
