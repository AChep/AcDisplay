/*
 * Copyright (C) 2013 AChep@xda <artemchep@gmail.com>
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
 * Created by Artem on 09.02.14.
 */
public final class Blacklist extends SharedList<AppConfig, AppConfig.AppConfigSaver> {

    private static final String TAG = "Blacklist";

    public static final String PREF_NAME = "blacklist";

    private static SoftReference<Blacklist> sBlacklistSoft;

    public static synchronized Blacklist getInstance(Context context) {
        Blacklist instance;
        if (sBlacklistSoft == null || (instance = sBlacklistSoft.get()) == null) {
            Log.i(TAG, "Blacklist initialized.");

            instance = new Blacklist(context);
            sBlacklistSoft = new SoftReference<>(instance);
            return instance;
        }
        return instance;
    }

    private Blacklist(Context context) {
        super(context, AppConfig.AppConfigSaver.class);
    }

    @Override
    protected String getPreferencesFileName() {
        return PREF_NAME;
    }

    @Override
    protected Comparator<AppConfig> onCreateComparator() {
        return new AppConfig.AppConfigComparator();
    }

    @Override
    protected boolean isOverwriteAllowed(AppConfig object) {
        return true;
    }

    public void saveAppConfig(Context context, AppConfig config,
                              OnSharedListChangedListener listener) {
        if (config.enabled == AppConfig.DEFAULT_ENABLED
                && config.isRestricted() == AppConfig.DEFAULT_RESTRICTED
                && config.isHidden() == AppConfig.DEFAULT_HIDDEN) {

            // The config is empty. We can delete it without
            // any cares of losing its data.
            remove(context, config, listener);
            return;
        }

        AppConfig clone = AppConfig.wrap(config.packageName);
        AppConfig.copy(config, clone);
        put(context, clone, listener);
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

    public static abstract class OnBlacklistChangedListener implements
            OnSharedListChangedListener<AppConfig> {

        public abstract void onBlacklistChanged(AppConfig configNew, AppConfig configOld, int diff);

        @Override
        public final void onPut(AppConfig objectNew, AppConfig objectOld, int diff) {
            onBlacklistChanged(objectNew, objectOld, diff);
        }

        @Override
        public final void onRemoved(AppConfig objectRemoved) { /* unused */ }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // No more Removed event
    protected void notifyOnRemoved(AppConfig object, OnSharedListChangedListener l) {
        super.notifyOnPut(AppConfig.wrap(object.packageName), object, l);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // Do not allow null as old object
    protected void notifyOnPut(AppConfig object, AppConfig old, OnSharedListChangedListener l) {
        super.notifyOnPut(object, old == null ? AppConfig.wrap(object.packageName) : old, l);
    }
}

