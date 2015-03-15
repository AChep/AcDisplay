/*
 * Copyright (C) 2015 AChep@xda <artemchep@gmail.com>
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
package com.achep.acdisplay.services;

import android.content.Context;
import android.support.annotation.NonNull;

import com.achep.acdisplay.Config;
import com.achep.base.content.ConfigBase;
import com.achep.base.tests.Check;

/**
 * Provides a callback when <b>something</b> should be started or stopped
 * to satisfy any options or for power-save reason.
 *
 * @author Artem Chepurnoy
 */
public abstract class Switch {

    private boolean mCreated;

    @NonNull
    private final Context mContext;
    @NonNull
    private final Callback mCallback;

    /**
     * Provides control callback to main service.
     *
     * @author Artem Chepurnoy
     */
    public interface Callback {

        /**
         * Asks main service to start <b>something</b>.
         * At this moment calling {@link #isActive()} should return {@code true}.
         *
         * @see #isActive()
         * @see #requestInactive()
         */
        void requestActive();

        /**
         * Asks main service to stop <b>something</b>.
         * At this moment calling {@link #isActive()} should return {@code false}.
         *
         * @see #isActive()
         * @see #requestActive()
         */
        void requestInactive();

    }

    public Switch(@NonNull Context context, @NonNull Callback callback) {
        mContext = context;
        mCallback = callback;
    }

    /**
     * Same as calling {@code getCallback().requestActive()}.
     *
     * @see Callback#requestActive()
     * @see #getCallback()
     */
    protected void requestActive() {
        getCallback().requestActive();
    }

    /**
     * Same as calling {@code getCallback().requestInactive()}.
     *
     * @see Callback#requestInactive()
     * @see #getCallback()
     */
    protected void requestInactive() {
        getCallback().requestInactive();
    }

    @NonNull
    public Callback getCallback() {
        return mCallback;
    }

    /**
     * @return {@link com.achep.acdisplay.services.activemode.ActiveModeService Service}'s context.
     */
    @NonNull
    public Context getContext() {
        return mContext;
    }

    /**
     * Called by the {@link SwitchService} when the service is created.
     */
    public abstract void onCreate();

    /**
     * Called by the {@link SwitchService} to notify
     * that this class is no longer used and is being removed.
     * Here you should clean up any resources it holds
     * (threads, registered receivers, etc) at this point.
     */
    public abstract void onDestroy();

    /**
     * @return {@code true} if starting <b>something</b> is fine, {@code false} otherwise.
     */
    public abstract boolean isActive();

    final void create() {
        mCreated = true;
        onCreate();
    }

    final void destroy() {
        onDestroy();
        mCreated = false;
    }

    final boolean isCreated() {
        return mCreated;
    }

    public abstract static class Optional extends Switch implements
            ConfigBase.OnConfigChangedListener {

        @NonNull
        private Config mConfig;

        private final boolean mOptionInverted;
        @NonNull
        private final ConfigBase.Option mOption;
        @NonNull
        private final String mOptionKey;

        /**
         * @param isOptionInverted {@code false} if enabled option means <i>actually</i> enabled
         *                         feature, {@code true} if it's inversed
         */
        public Optional(
                @NonNull Context context,
                @NonNull Callback callback,
                @NonNull ConfigBase.Option option, boolean isOptionInverted) {
            super(context, callback);
            mConfig = Config.getInstance();
            mOption = option;
            mOptionKey = mOption.getKey(mConfig);
            mOptionInverted = isOptionInverted;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCreate() {
            mConfig.registerListener(this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onDestroy() {
            mConfig.unregisterListener(this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public final boolean isActive() {
            return !isFeatureEnabled() || isActiveInternal();
        }

        /**
         * @see #isActive()
         */
        public abstract boolean isActiveInternal();

        public void requestActiveInternal() {
            Check.getInstance().isTrue(isActive());
            requestActive();
        }

        public void requestInactiveInternal() {
            if (isFeatureEnabled()) {
                Check.getInstance().isFalse(isActive());
                requestInactive();
            }
        }

        @Override
        public void onConfigChanged(@NonNull ConfigBase configBase,
                                    @NonNull String key,
                                    @NonNull Object value) {
            if (key.equals(mOptionKey)) {
                if (isFeatureEnabled((boolean) value)) {
                    if (isActiveInternal()) {
                        requestActive();
                    } else {
                        requestInactive();
                    }
                } else {
                    // If you've disabled the active mode, check the
                    // amount of notifications and probably stop
                    // listening.
                    requestActive();
                }
            }
        }

        private boolean isFeatureEnabled() {
            return isFeatureEnabled((boolean) mOption.read(mConfig));
        }

        private boolean isFeatureEnabled(boolean on) {
            return mOptionInverted != on;
        }

    }

}
