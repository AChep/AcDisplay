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

import android.content.SharedPreferences;
import android.util.Log;

import com.achep.activedisplay.utils.MathUtils;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * <b>Attention: its equality key is package name only!<b/>
 *
 * @author Artem Chepurnoy
 */
public class AppConfig {

    private static final String TAG = "AppConfig";

    public static final int DIFF_ENABLED = 1;
    public static final int DIFF_RESTRICTED = 2;
    public static final int DIFF_HIDDEN = 4;

    static final boolean DEFAULT_ENABLED = false;
    static final boolean DEFAULT_RESTRICTED = false;
    static final boolean DEFAULT_HIDDEN = false;

    public String packageName;
    public boolean enabled = DEFAULT_ENABLED;
    public boolean[] restricted = new boolean[] { DEFAULT_RESTRICTED };
    public boolean[] hidden = new boolean[] { DEFAULT_HIDDEN };

    public AppConfig(String packageName) {
        this(packageName, DEFAULT_ENABLED, DEFAULT_RESTRICTED, DEFAULT_HIDDEN);
    }

    public AppConfig(String packageName, boolean enabled,
                     boolean restricted, boolean hidden) {
        this.enabled = enabled;
        this.packageName = packageName;
        this.restricted[0] = restricted;
        this.hidden[0] = hidden;
    }

    /**
     * Resets all (except package name!) to default values.
     */
    public static void reset(AppConfig config) {
        config.enabled = DEFAULT_ENABLED;
        config.setRestricted(DEFAULT_RESTRICTED);
        config.setHidden(DEFAULT_HIDDEN);
    }

    /**
     * Copies data of the first config into the second one.
     *
     * @param config origin config
     * @param clone clone config
     * @return Cloned config
     * @see #reset(AppConfig)
     */
    public static AppConfig copy(AppConfig config, AppConfig clone) {
        clone.packageName = config.packageName;
        clone.enabled = config.enabled;
        clone.setRestricted(config.isRestricted());
        clone.setHidden(config.isHidden());
        return clone;
    }

    /**
     * Logs given config with a debug output level.
     */
    public static void log(String tag, AppConfig config) {
        Log.d(tag, "enabled=" + config.enabled
                + " restricted=" + config.isRestricted()
                + " hidden=" + config.isHidden()
                + " pkg=" + config.packageName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(279, 351)
                .append(packageName)
                .toHashCode();
    }

    /**
     * Compares given {@link com.achep.activedisplay.blacklist.AppConfig} with
     * this one. <b>Warning: </b> the only criterion of equality is the package name!
     */
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AppConfig))
            return false;

        AppConfig ps = (AppConfig) o;
        return new EqualsBuilder()
                .append(packageName, ps.packageName)
                .isEquals();
    }

    public void setRestricted(boolean restricted) {
        this.restricted[0] = restricted;
    }

    public void setHidden(boolean hidden) {
        this.hidden[0] = hidden;
    }

    public boolean isRestricted() {
        return restricted[0];
    }

    public boolean isRestrictedReal() {
        return restricted[0] && enabled;
    }

    public boolean isHidden() {
        return hidden[0];
    }

    public boolean isHiddenReal() {
        return hidden[0] && enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return True if all options are equals to defaults, False otherwise.
     * @see AppConfig#DEFAULT_ENABLED
     * @see AppConfig#DEFAULT_RESTRICTED
     * @see AppConfig#DEFAULT_HIDDEN
     */
    boolean isEmpty() {
        return isEnabled() == AppConfig.DEFAULT_ENABLED
                && isRestricted() == AppConfig.DEFAULT_RESTRICTED
                && isHidden() == AppConfig.DEFAULT_HIDDEN;
    }

    /**
     * Saves and restores AppConfig from and to shared preferences.
     *
     * @author Artem Chepurnoy
     */
    static final class AppConfigSaver extends SharedList.Saver<AppConfig> {

        private static final String KEY_PACKAGE = "package_name_";
        private static final String KEY_ENABLED = "enabled_";
        private static final String KEY_RESTRICTED = "restricted_";
        private static final String KEY_HIDDEN = "hidden_";

        /**
         * {@inheritDoc}
         */
        @Override
        public SharedPreferences.Editor put(AppConfig ps, SharedPreferences.Editor editor, int position) {
            editor.putString(KEY_PACKAGE + position, ps.packageName);
            editor.putBoolean(KEY_ENABLED + position, ps.enabled);
            editor.putBoolean(KEY_RESTRICTED + position, ps.isRestricted());
            editor.putBoolean(KEY_HIDDEN + position, ps.isHidden());
            return editor;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AppConfig get(SharedPreferences prefs, int position) {
            String pkg = prefs.getString(KEY_PACKAGE + position, null);
            boolean enabled = prefs.getBoolean(KEY_ENABLED + position, DEFAULT_ENABLED);
            boolean restricted = prefs.getBoolean(KEY_RESTRICTED + position, DEFAULT_RESTRICTED);
            boolean hidden = prefs.getBoolean(KEY_HIDDEN + position, DEFAULT_HIDDEN);
            return new AppConfig(pkg, enabled, restricted, hidden);
        }
    }

    /**
     * Compares different AppConfigs.
     *
     * @author Artem Chepurnoy
     */
    static final class AppConfigComparator extends SharedList.Comparator<AppConfig> {

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(AppConfig object, AppConfig old) {
            return orZero(DIFF_ENABLED, object.enabled, old.enabled)
                    | orZero(DIFF_HIDDEN, object.isHidden(), old.isHidden())
                    | orZero(DIFF_RESTRICTED, object.isRestricted(), old.isRestricted());
        }

        private int orZero(int value, boolean a, boolean b) {
            return a != b ? value : 0;
        }
    }
}
