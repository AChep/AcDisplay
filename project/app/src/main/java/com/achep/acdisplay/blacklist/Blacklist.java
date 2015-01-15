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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.achep.base.content.SharedList;

/**
 * The blacklist (also known as per-app-features.)
 *
 * @author Artem Chepurnoy
 * @see #saveAppConfig(android.content.Context, AppConfig, SharedList.OnSharedListChangedListener)
 * @see #getAppConfig(String)
 */
public final class Blacklist extends SharedList<AppConfig, AppConfig.Saver> {

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
         * @param diff      The difference between two configs.
         */
        public abstract void onBlacklistChanged(
                @NonNull AppConfig configNew,
                @NonNull AppConfig configOld, int diff);

        /**
         * {@inheritDoc}
         */
        @Override
        public final void onPut(@NonNull AppConfig objectNew, AppConfig objectOld, int diff) {
            onBlacklistChanged(objectNew, objectOld, diff);
        }

        /**
         * Should never be called.
         *
         * @see #onBlacklistChanged(AppConfig, AppConfig, int)
         */
        @Override
        public final void onRemoved(@NonNull AppConfig objectRemoved) {
            Log.wtf(TAG, "Notified about removing an item from blacklist.");
        }
    }

    public static synchronized Blacklist getInstance() {
        if (sBlacklist == null) {
            sBlacklist = new Blacklist();
        }
        return sBlacklist;
    }

    private Blacklist() {
        super();
    }

    /**
     * This is called on {@link com.achep.acdisplay.App#onCreate() App create}.
     *
     * @see com.achep.base.content.SharedList#init(android.content.Context)
     */
    @Override
    public void init(@NonNull Context context) {
        super.init(context);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    protected String getPreferencesFileName() {
        return PREF_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    protected AppConfig.Saver onCreateSaver() {
        return new AppConfig.Saver();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Comparator onCreateComparator() {
        return new AppConfig.Comparator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isOverwriteAllowed(@NonNull AppConfig object) {
        return true;
    }

    //-- MANAGING APP CONFIG --------------------------------------------------

    public void saveAppConfig(Context context, AppConfig config,
                              OnSharedListChangedListener listener) {
        if (config.equalsToDefault()) {

            // We need to remove defaults to save blacklist's size.
            super.remove(context, config, listener);
            return;
        }

        AppConfig clone = new AppConfig(config.packageName);
        AppConfig.copy(config, clone);
        super.put(context, clone, listener); // overwriting is enabled.
    }

    /**
     * <b>Creates</b> new instance of {@link com.achep.acdisplay.blacklist.AppConfig} and
     * fills it with present data.
     *
     * @param packageName The package name of need application.
     * @return New instance of app's config filled with present data.
     * @see #fill(AppConfig)
     */
    @NonNull
    public AppConfig getAppConfig(@NonNull String packageName) {
        return fill(new AppConfig(packageName));
    }

    @NonNull
    public AppConfig fill(@NonNull AppConfig config) {
        for (AppConfig c : values()) {
            if (c.equals(config)) {
                AppConfig.copy(c, config);
                return config;
            }
        }
        AppConfig.reset(config);
        return config;
    }

    //-- BULL SHIT PROTECTION -------------------------------------------------

    /**
     * This will throw an exception! Please, use
     * {@link #saveAppConfig(Context, AppConfig, SharedList.OnSharedListChangedListener)} instead.
     */
    @Nullable
    @Override
    public AppConfig put(@NonNull Context context, @NonNull AppConfig object,
                         @Nullable OnSharedListChangedListener l) {
        throw new RuntimeException();
    }

    /**
     * This will throw an exception! Please, use
     * {@link #saveAppConfig(Context, AppConfig, SharedList.OnSharedListChangedListener)} instead.
     */
    @Override
    public void remove(@NonNull Context context, @NonNull AppConfig object,
                       @Nullable OnSharedListChangedListener l) {
        throw new RuntimeException();
    }

    //-- DISMISSING WIDGET ----------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void notifyOnRemoved(@NonNull AppConfig object, OnSharedListChangedListener l) {

        // Change remove-event to event about putting
        // empty config to the list.
        notifyOnPut(new AppConfig(object.packageName), object, l);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void notifyOnPut(AppConfig object, AppConfig old, OnSharedListChangedListener l) {
        if (old == null) {
            // Do not allow nulls.
            old = new AppConfig(object.packageName);
        }

        // Notify all listeners
        super.notifyOnPut(object, old, l);
    }
}

