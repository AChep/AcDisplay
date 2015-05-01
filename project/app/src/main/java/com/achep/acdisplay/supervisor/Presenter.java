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
package com.achep.acdisplay.supervisor;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.achep.base.interfaces.ISubscriptable;

/**
 * Created by Artem Chepurnoy on 27.04.2015.
 */
public abstract class Presenter implements ISubscriptable<Presenter.State> {

    @NonNull
    public static Presenter newInstance() {
        throw new RuntimeException();
    }

    public enum State {
        /**
         *
         */
        UNKNOWN,
        /**
         * Screen is turned off and the AcDisplay is awaiting
         * just below its darkness.
         */
        IDLE,
        /**
         *
         */
        SHOWN_MODE_ACTIVE,
        /**
         *
         */
        SHOWN_MODE_PASSIVE,
        /**
         * The AcDisplay activity is destroyed.
         */
        DEAD,
    }

    @NonNull
    private State mState = State.UNKNOWN;

    /**
     * @return the current state of the AcDisplay.
     */
    @NonNull
    public State getState() {
        return mState;
    }

    protected void setState(@NonNull State state) {
        mState = state;
    }

    //-- REQUESTS -------------------------------------------------------------

    public abstract void requestState(@NonNull State state, @Nullable Extras extras);

    //-- CALLBACK -------------------------------------------------------------

    /**
     * The callback of the {@link linked Activity}.
     *
     * @author Artem Chepurnoy
     */
    public interface Callback {

    }

    //-- EXTRAS ---------------------------------------------------------------

    /**
     * The details of the launch.
     *
     * @author Artem Chepurnoy
     */
    public static final class Extras {
        public final String data;

        public Extras(@Nullable String data) {
            this.data = data;
        }
    }
}
