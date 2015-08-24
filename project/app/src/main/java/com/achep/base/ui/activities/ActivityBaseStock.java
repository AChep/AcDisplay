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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.achep.base.interfaces.IActivityBase;
import com.achep.base.utils.power.PowerSaveDetector;

import org.solovyev.android.checkout.ActivityCheckout;

/**
 * Created by Artem Chepurnoy on 28.12.2014.
 */
public abstract class ActivityBaseStock extends Activity implements IActivityBase {

    @NonNull
    private final ActivityBaseInternal mAbs = new ActivityBaseInternal();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mAbs.onCreate(this, savedInstanceState);
        super.onCreate(savedInstanceState);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart() {
        super.onStart();
        mAbs.onStart();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop() {
        mAbs.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mAbs.onDestroy();
        super.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!mAbs.onActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    //-- IActivityBase --------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestCheckout() {
        mAbs.requestCheckout();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestInputMethodReset() {
        mAbs.requestInputMethodReset();
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Activity getActivity() {
        return mAbs.getActivity();
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public ActivityCheckout getCheckout() {
        return mAbs.getCheckout();
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public PowerSaveDetector getPowerSaveDetector() {
        return mAbs.getPowerSaveDetector();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPowerSaveMode() {
        return mAbs.isPowerSaveMode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNotPowerSaveMode() {
        return mAbs.isNotPowerSaveMode();
    }

}
