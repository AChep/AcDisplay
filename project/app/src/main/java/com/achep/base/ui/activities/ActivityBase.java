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
package com.achep.base.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;

import com.achep.acdisplay.App;
import com.achep.base.tests.Check;
import com.achep.base.utils.power.PowerSaveDetector;

import org.solovyev.android.checkout.ActivityCheckout;
import org.solovyev.android.checkout.Checkout;

/**
 * Created by Artem Chepurnoy on 28.12.2014.
 */
public abstract class ActivityBase extends ActionBarActivity {

    protected ActivityCheckout mCheckout;
    protected PowerSaveDetector mPowerSaveDetector;

    private boolean mCheckoutRequest;

    public void requestCheckout() {
        Check.getInstance().isFalse(mCheckoutRequest);
        Check.getInstance().isNull(mPowerSaveDetector); // not created yet.
        mCheckoutRequest = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (mCheckoutRequest) {
            App.getCheckoutInternal().requestConnect();
            mCheckout = Checkout.forActivity(this, App.getCheckout());
        }
        mPowerSaveDetector = PowerSaveDetector.newInstance(this);
        super.onCreate(savedInstanceState);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart() {
        super.onStart();
        if (mCheckout != null) mCheckout.start();
        mPowerSaveDetector.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop() {
        if (mCheckout != null) mCheckout.stop();
        mPowerSaveDetector.stop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mCheckout != null) {
            App.getCheckoutInternal().requestDisconnect();
            mCheckout = null;
        }
        super.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mCheckout != null) {
            boolean handled = mCheckout.onActivityResult(requestCode, resultCode, data);
            if (handled) return;
        }

        // Pass to parent.
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Nullable
    public ActivityCheckout getCheckout() {
        return mCheckout;
    }

    /**
     * @see #isPowerSaveMode()
     */
    @NonNull
    public PowerSaveDetector getPowerSaveDetector() {
        return mPowerSaveDetector;
    }

    /**
     * Returns {@code true} if the device is currently in power save mode.
     * When in this mode, applications should reduce their functionality
     * in order to conserve battery as much as possible.
     *
     * @return {@code true} if the device is currently in power save mode, {@code false} otherwise.
     */
    public boolean isPowerSaveMode() {
        return mPowerSaveDetector.isPowerSaveMode();
    }

}
