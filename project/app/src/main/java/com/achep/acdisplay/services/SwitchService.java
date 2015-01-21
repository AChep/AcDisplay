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

import android.support.annotation.NonNull;

import com.achep.acdisplay.Atomic;

/**
 * @author Artem Chepurnoy
 */
public abstract class SwitchService extends BathService.ChildService implements
        Atomic.Callback,
        Switch.Callback {

    protected Atomic mListeningAtom;
    protected Switch[] mSwitches;

    @NonNull
    public abstract Switch[] onBuildSwitches();

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        mListeningAtom = new Atomic(this);
        mSwitches = onBuildSwitches();

        for (Switch switch_ : mSwitches) {
            switch_.create();
        }

        requestActive();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        for (Switch switch_ : mSwitches) {
            switch_.destroy();
        }

        stop();

        mListeningAtom = null;
        mSwitches = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestActive() {
        if (mListeningAtom.isRunning()) {
            return; // Already listening, no need to check all handlers.
        }

        // Check through all available handlers.
        for (Switch switch_ : mSwitches) {
            if (!switch_.isCreated() || !switch_.isActive()) {
                return;
            }
        }

        start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestInactive() {
        stop();
    }

    protected void stop() {
        mListeningAtom.stop();
    }

    protected void start() {
        mListeningAtom.start();
    }

}
