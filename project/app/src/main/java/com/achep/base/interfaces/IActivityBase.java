package com.achep.base.interfaces;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.achep.base.utils.power.PowerSaveDetector;

import org.solovyev.android.checkout.ActivityCheckout;

/**
 * Created by Artem Chepurnoy on 08.03.2015.
 */
public interface IActivityBase {

    public void requestCheckout();

    @Nullable
    public ActivityCheckout getCheckout();

    /**
     * @see #isPowerSaveMode()
     */
    @NonNull
    public PowerSaveDetector getPowerSaveDetector();

    /**
     * Returns {@code true} if the device is currently in power save mode.
     * When in this mode, applications should reduce their functionality
     * in order to conserve battery as much as possible.
     *
     * @return {@code true} if the device is currently in power save mode, {@code false} otherwise.
     */
    public boolean isPowerSaveMode();

}
