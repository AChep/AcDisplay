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
package com.achep.base.ui.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.achep.base.AppHeap;
import com.achep.base.interfaces.IActivityBase;
import com.achep.base.tests.Check;
import com.achep.base.utils.power.PowerSaveDetector;
import com.squareup.leakcanary.RefWatcher;

import org.solovyev.android.checkout.ActivityCheckout;
import org.solovyev.android.checkout.Checkout;

import java.lang.reflect.Method;

import static com.achep.base.Build.DEBUG;

/**
 * Created by Artem Chepurnoy on 08.03.2015.
 */
final class ActivityBaseInternal implements IActivityBase {

    private static final String TAG = "ActivityBaseInternal";

    //-- DEBUG ----------------------------------------------------------------

    private static int sInstancesCount = 0;

    /* Only for debug purposes! */
    @SuppressWarnings({"FieldCanBeLocal", "ConstantConditions"})
    private final Object dFinalizeWatcher = DEBUG ? new Object() {

        /**
         * Logs the notifications' removal.
         */
        @Override
        protected void finalize() throws Throwable {
            try {
                Log.d(TAG, "Finalizing the instance=" + this + " n=" + --sInstancesCount);
            } finally {
                super.finalize();
            }
        }

    } : null;

    //-- BEGIN ----------------------------------------------------------------

    private Activity mActivity;
    private ActivityCheckout mCheckout;
    private PowerSaveDetector mPowerSaveDetector;

    private boolean mCheckoutRequest;
    private boolean mInputMethodResetRequest;

    private boolean mCreated;

    public ActivityBaseInternal() {
        // Logs the current number of the Activities and
        // increases its count. Check `dFinalizeWatcher` field
        // for more information.
        if (DEBUG) Log.d(TAG, "Creating an instance=" + this
                + " watcher=" + dFinalizeWatcher
                + " n=" + ++sInstancesCount);
    }

    /* Mirrors Activity#onCreate(...) */
    void onCreate(Activity activity, Bundle savedInstanceState) {
        if (mCheckoutRequest) mCheckout = Checkout.forActivity(activity, AppHeap.getCheckout());
        mPowerSaveDetector = PowerSaveDetector.newInstance(activity);
        mActivity = activity;
        mCreated = true;
    }

    /* Mirrors Activity#onStart(...) */
    void onStart() {
        if (mCheckout != null) {
            AppHeap.getCheckoutInternal().requestConnect();
            mCheckout.start();
        }
        mPowerSaveDetector.start();
    }

    /* Mirrors Activity#onStop(...) */
    void onStop() {
        if (mCheckout != null) {
            mCheckout.stop();
            AppHeap.getCheckoutInternal().requestDisconnect();
        }
        mPowerSaveDetector.stop();
    }

    /* Mirrors Activity#onDestroy(...) */
    void onDestroy() {
        mCheckout = null;
        if (mInputMethodResetRequest) performInputMethodServiceReset();
        // Watch for the activity to detect possible leaks.
        RefWatcher refWatcher = AppHeap.getRefWatcher();
        refWatcher.watch(this);
    }

    /**
     * @see #requestInputMethodReset()
     */
    private void performInputMethodServiceReset() {
        Object imm = mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        Reflector.TypedObject windowToken = new Reflector.TypedObject(
                mActivity.getWindow().getDecorView().getWindowToken(), IBinder.class);
        Reflector.invokeMethodExceptionSafe(imm, "windowDismissed", windowToken);
        Reflector.TypedObject view = new Reflector.TypedObject(null, View.class);
        Reflector.invokeMethodExceptionSafe(imm, "startGettingWindowFocus", view);
    }

    /* Mirrors Activity#onActivityResult(...) */
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
        Check.getInstance().isFalse(mCreated); // not created yet.
        mCheckoutRequest = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestInputMethodReset() {
        mInputMethodResetRequest = true;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNotPowerSaveMode() {
        return mPowerSaveDetector.isNotPowerSaveMode();
    }

    //-- OTHER ----------------------------------------------------------------

    private static class Reflector {

        public static final class TypedObject {

            private final Object object;
            private final Class type;

            public TypedObject(@Nullable Object object, @Nullable Class type) {
                this.object = object;
                this.type = type;
            }

            @Nullable
            private Object getObject() {
                return object;
            }

            @Nullable
            private Class getType() {
                return type;
            }
        }

        public static void invokeMethodExceptionSafe(final Object methodOwner, final String method,
                                                     final TypedObject... arguments) {
            if (methodOwner == null) return;
            try {
                final Class<?>[] types;
                final Object[] objects;
                if (arguments != null) {
                    int length = arguments.length;
                    types = new Class[length];
                    objects = new Object[length];
                    for (int i = 0; i < length; i++) {
                        types[i] = arguments[i].getType();
                        objects[i] = arguments[i].getObject();
                    }
                } else {
                    types = new Class[0];
                    objects = new Object[0];
                }

                final Method declaredMethod = methodOwner.getClass().getDeclaredMethod(method, types);
                declaredMethod.setAccessible(true);
                declaredMethod.invoke(methodOwner, objects);
            } catch (Throwable ignored) { /* unused */ }
        }
    }
}
