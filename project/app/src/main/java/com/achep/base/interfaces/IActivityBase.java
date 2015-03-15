package com.achep.base.interfaces;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.achep.base.utils.power.PowerSaveDetector;

import org.solovyev.android.checkout.ActivityCheckout;

/**
 * Created by Artem Chepurnoy on 08.03.2015.
 */
public interface IActivityBase {

    void requestCheckout();

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
