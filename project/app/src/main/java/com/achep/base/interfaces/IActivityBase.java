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
package com.achep.base.interfaces;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.achep.base.utils.power.PowerSaveDetector;

import org.solovyev.android.checkout.ActivityCheckout;

/**
 * Created by Artem Chepurnoy on 08.03.2015.
 */
public interface IActivityBase {

    void requestCheckout();

    @NonNull
    Activity getActivity();

    @Nullable
    ActivityCheckout getCheckout();

    /**
     * @see #isPowerSaveMode()
     */
    @NonNull
    PowerSaveDetector getPowerSaveDetector();

    /**
     * Returns {@code true} if the device is currently in power save mode.
     * When in this mode, applications should reduce their functionality
     * in order to conserve battery as much as possible.
     *
     * @return {@code true} if the device is currently in power save mode, {@code false} otherwise.
     */
    boolean isPowerSaveMode();

}
