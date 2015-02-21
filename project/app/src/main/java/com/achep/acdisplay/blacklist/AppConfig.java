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
package com.achep.acdisplay.blacklist;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import com.achep.base.content.SharedList;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * <b>Attention: its equality key is package name only!<b/>
 *
 * @author Artem Chepurnoy
 */
public class AppConfig {

    private static final String TAG = "AppConfig";

    public static final int DIFF_RESTRICTED = 2;
    public static final int DIFF_HIDDEN = 4;
    public static final int DIFF_NON_CLEARABLE = 8;

    static final boolean DEFAULT_RESTRICTED = false;
    static final boolean DEFAULT_HIDDEN = false;
    static final boolean DEFAULT_NON_CLEARABLE = false;

    public String packageName;
    public final boolean[] restricted = new boolean[]{DEFAULT_RESTRICTED};
    public final boolean[] hidden = new boolean[]{DEFAULT_HIDDEN};
    public final boolean[] nonClearable = new boolean[]{DEFAULT_NON_CLEARABLE};

    public AppConfig(String packageName) {
        this(packageName,
                DEFAULT_RESTRICTED,
                DEFAULT_HIDDEN,
                DEFAULT_NON_CLEARABLE);
    }

    public AppConfig(String packageName,
                     boolean restricted,
                     boolean hidden,
                     boolean nonClearable) {
        this.packageName = packageName;
        setRestricted(restricted);
        setHidden(hidden);
        setNonClearableEnabled(nonClearable);
    }

    /**
     * Resets all (except package name!) to default values.
     */
    public static void reset(AppConfig config) {
        config.setRestricted(DEFAULT_RESTRICTED);
        config.setHidden(DEFAULT_HIDDEN);
        config.setNonClearableEnabled(DEFAULT_NON_CLEARABLE);
    }

    /**
     * Copies data of the first config into the second one.
     *
     * @param config origin config
     * @param clone  clone config
     * @return Cloned config
     * @see #reset(AppConfig)
     */
    public static AppConfig copy(AppConfig config, AppConfig clone) {
        clone.packageName = config.packageName;
        clone.setRestricted(config.isRestricted());
        clone.setHidden(config.isHidden());
        clone.setNonClearableEnabled(config.isNonClearableEnabled());
        return clone;
    }

    /**
     * Logs given config with a debug output level.
     */
    public static void log(String tag, AppConfig config) {
        Log.d(tag, config.toString());
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
     * Compares given {@link com.achep.acdisplay.blacklist.AppConfig} with
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "AppConfig ["
                + "restricted=" + isRestricted()
                + " hidden=" + isHidden()
                + " non-clearable=" + isNonClearableEnabled()
                + " pkg=" + packageName
                + "]";
    }

    public void setRestricted(boolean restricted) {
        this.restricted[0] = restricted;
    }

    public void setHidden(boolean hidden) {
        this.hidden[0] = hidden;
    }

    public void setNonClearableEnabled(boolean enabled) {
        this.nonClearable[0] = enabled;
    }

    public boolean isRestricted() {
        return restricted[0];
    }

    public boolean isHidden() {
        return hidden[0];
    }

    /**
     * @return {@code true} if showing non-clearable notifications is allowed for
     * this app, {@code false} otherwise.
     * @see #setNonClearableEnabled(boolean)
     * @see #DEFAULT_NON_CLEARABLE
     * @see #DIFF_NON_CLEARABLE
     */
    public boolean isNonClearableEnabled() {
        return nonClearable[0];
    }

    /**
     * @return {@code true} if all options are set to default, {@code false} otherwise.
     * @see AppConfig#DEFAULT_RESTRICTED
     * @see AppConfig#DEFAULT_HIDDEN
     * @see AppConfig#DEFAULT_NON_CLEARABLE
     * @see #reset(AppConfig)
     */
    @SuppressWarnings("PointlessBooleanExpression")
    boolean equalsToDefault() {
        return isRestricted() == AppConfig.DEFAULT_RESTRICTED
                && isHidden() == AppConfig.DEFAULT_HIDDEN
                && isNonClearableEnabled() == AppConfig.DEFAULT_NON_CLEARABLE;
    }

    /**
     * Saves and restores AppConfig from and to shared preferences.
     *
     * @author Artem Chepurnoy
     */
    static final class Saver extends SharedList.Saver<AppConfig> {

        private static final String KEY_PACKAGE = "package_name_";
        private static final String KEY_RESTRICTED = "restricted_";
        private static final String KEY_HIDDEN = "hidden_";
        private static final String KEY_NON_CLEARABLE = "non-clearable_";

        /**
         * {@inheritDoc}
         */
        @NonNull
        @Override
        public SharedPreferences.Editor put(@NonNull AppConfig ps,
                                            @NonNull SharedPreferences.Editor editor,
                                            int position) {
            editor.putString(KEY_PACKAGE + position, ps.packageName);
            editor.putBoolean(KEY_RESTRICTED + position, ps.isRestricted());
            editor.putBoolean(KEY_HIDDEN + position, ps.isHidden());
            editor.putBoolean(KEY_NON_CLEARABLE + position, ps.isNonClearableEnabled());
            return editor;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AppConfig get(@NonNull SharedPreferences prefs, int position) {
            String pkg = prefs.getString(KEY_PACKAGE + position, null);
            boolean restricted = prefs.getBoolean(KEY_RESTRICTED + position, DEFAULT_RESTRICTED);
            boolean hidden = prefs.getBoolean(KEY_HIDDEN + position, DEFAULT_HIDDEN);
            boolean ongoing = prefs.getBoolean(KEY_NON_CLEARABLE + position, DEFAULT_NON_CLEARABLE);
            return new AppConfig(pkg, restricted, hidden, ongoing);
        }
    }

    /**
     * Compares different AppConfigs.
     *
     * @author Artem Chepurnoy
     */
    static final class Comparator extends SharedList.Comparator<AppConfig> {

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(@NonNull AppConfig object, AppConfig old) {
            return orZero(DIFF_HIDDEN, object.isHidden(), old.isHidden())
                    | orZero(DIFF_RESTRICTED, object.isRestricted(), old.isRestricted())
                    | orZero(DIFF_NON_CLEARABLE, object.isNonClearableEnabled(), old.isNonClearableEnabled());
        }

        private int orZero(int value, boolean a, boolean b) {
            return a != b ? value : 0;
        }
    }
}
