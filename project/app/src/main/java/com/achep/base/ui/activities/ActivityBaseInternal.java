package com.achep.base.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.achep.base.AppHeap;
import com.achep.base.interfaces.IActivityBase;
import com.achep.base.tests.Check;
import com.achep.base.utils.power.PowerSaveDetector;

import org.solovyev.android.checkout.ActivityCheckout;
import org.solovyev.android.checkout.Checkout;

/**
 * Created by Artem Chepurnoy on 08.03.2015.
 */
final class ActivityBaseInternal implements IActivityBase {

    private Activity mActivity;
    private ActivityCheckout mCheckout;
    private PowerSaveDetector mPowerSaveDetector;

    private boolean mCheckoutRequest;

    void onCreate(Activity activity, Bundle savedInstanceState) {
        if (mCheckoutRequest) mCheckout = Checkout.forActivity(activity, AppHeap.getCheckout());
        mPowerSaveDetector = PowerSaveDetector.newInstance(activity);
        mActivity = activity;
    }

    void onStart() {
        if (mCheckout != null) {
            AppHeap.getCheckoutInternal().requestConnect();
            mCheckout.start();
        }
        mPowerSaveDetector.start();
    }

    void onStop() {
        if (mCheckout != null) {
            mCheckout.stop();
            AppHeap.getCheckoutInternal().requestDisconnect();
        }
        mPowerSaveDetector.stop();
    }

    void onDestroy() {
        mCheckout = null;
    }

    boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        return mCheckout != null && mCheckout.onActivityResult(requestCode, resultCode, data);
    }

    //-- IActivityBase --------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestCheckout() {
        Check.getInstance().isFalse(mCheckoutRequest);
        Check.getInstance().isNull(mPowerSaveDetector); // not created yet.
        mCheckoutRequest = true;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Activity getActivity() {
        return mActivity;
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public ActivityCheckout getCheckout() {
        return mCheckout;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public PowerSaveDetector getPowerSaveDetector() {
        return mPowerSaveDetector;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPowerSaveMode() {
        return mPowerSaveDetector.isPowerSaveMode();
    }
}
