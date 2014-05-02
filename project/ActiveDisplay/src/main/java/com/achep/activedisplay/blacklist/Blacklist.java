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
import android.util.Log;

import java.lang.ref.SoftReference;

/**
 * The blacklist (also known as per-app-features.)
 *
 * @see #saveAppConfig(android.content.Context, AppConfig, SharedList.OnSharedListChangedListener)
 * @see #getAppConfig(String)
 * @author Artem Chepurnoy
 */
public final class Blacklist extends SharedList<AppConfig, AppConfig.AppConfigSaver> {

    private static final String TAG = "Blacklist";

    public static final String PREF_NAME = "blacklist";

    private static Blacklist sBlacklist;

    /**
     * Interface definition for a callback to be invoked
     * when a blacklist changed.
     *
     * @author Artem Chepurnoy
     */
    public static abstract class OnBlacklistChangedListener
            implements OnSharedListChangedListener<AppConfig> {

        /**
         * Called on blacklist changed.
         *
         * @param configNew An instance of new app's config.
         * @param configOld An instance of previous app's config (can not be null.)
         * @param diff The difference between two configs.
         */
        public abstract void onBlacklistChanged(AppConfig configNew, AppConfig configOld, int diff);

        /**
         * {@inheritDoc}
         */
        @Override
        public final void onPut(AppConfig objectNew, AppConfig objectOld, int diff) {
            onBlacklistChanged(objectNew, objectOld, diff);
        }

        /**
         * Should never be called.
         * @see #onBlacklistChanged(AppConfig, AppConfig, int)
         */
        @Override
        public final void onRemoved(AppConfig objectRemoved) {
            Log.wtf(TAG, "Notified about removing an item from blacklist.");
        }
    }

    public static synchronized Blacklist getInstance(Context context) {
        if (sBlacklist == null) {
            sBlacklist = new Blacklist(context);
        }
        return sBlacklist;
    }

    private Blacklist(Context context) {
        super(context);
    }

    @Override
    protected String getPreferencesFileName() {
        return PREF_NAME;
    }

    @Override
    protected AppConfig.AppConfigSaver onCreateSaver() {
        return new AppConfig.AppConfigSaver();
    }

    @Override
    protected Comparator<AppConfig> onCreateComparator() {
        return new AppConfig.AppConfigComparator();
    }

    @Override
    protected boolean isOverwriteAllowed(AppConfig object) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // Change remove-event to event about putting
    // empty config to the list.
    protected final void notifyOnRemoved(AppConfig object, OnSharedListChangedListener l) {
        AppConfig emptyConfig = new AppConfig(object.packageName);
        super.notifyOnPut(emptyConfig, object, l);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // Do not allow nulls.
    protected final void notifyOnPut(AppConfig object, AppConfig old, OnSharedListChangedListener l) {
        if (old == null) old = new AppConfig(object.packageName);
        super.notifyOnPut(object, old, l);
    }

    public void saveAppConfig(Context context, AppConfig config,
                              OnSharedListChangedListener listener) {
        if (config.isEmpty()) {

            // The config is empty. We can remove it from list
            // without any cares about losing its data.
            remove(context, config, listener);
            return;
        }

        // Put new config to the list.
        // Note that overwriting is enabled.
        AppConfig clone = new AppConfig(config.packageName);
        AppConfig.copy(config, clone);
        put(context, clone, listener);
    }

    /**
     * <b>Creates</b> new instance of {@link com.achep.activedisplay.blacklist.AppConfig} and
     * fills it with present data.
     *
     * @param packageName The package name of need application.
     * @return New instance of app's config filled with present data.
     * @see #fill(AppConfig)
     */
    public AppConfig getAppConfig(String packageName) {
        return fill(new AppConfig(packageName));
    }

    public AppConfig fill(AppConfig config) {
        for (AppConfig c : valuesSet()) {
            if (c.equals(config)) {
                AppConfig.copy(c, config);
                return config;
            }
        }
        AppConfig.reset(config);
        return config;
    }

}

