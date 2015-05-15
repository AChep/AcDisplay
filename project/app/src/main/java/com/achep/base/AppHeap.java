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
package com.achep.base;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

import com.achep.base.billing.CheckoutInternal;
import com.achep.base.interfaces.IOnLowMemory;
import com.achep.base.tests.Check;
import com.drivemode.android.typeface.TypefaceHelper;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import org.solovyev.android.checkout.Checkout;
import org.solovyev.android.checkout.ProductTypes;
import org.solovyev.android.checkout.Products;

import java.util.Arrays;
import java.util.Collections;

/**
 * Created by Artem Chepurnoy on 17.02.2015.
 */
public class AppHeap implements IOnLowMemory {

    private static AppHeap sInstance;

    @NonNull
    public static AppHeap getInstance() {
        if (sInstance == null) {
            sInstance = new AppHeap();
        }
        return sInstance;
    }

    @NonNull
    public static Checkout getCheckout() {
        return getCheckoutInternal().getCheckout();
    }

    @NonNull
    public static CheckoutInternal getCheckoutInternal() {
        Check.getInstance().isNonNull(getInstance().mCheckoutInternal);
        return getInstance().mCheckoutInternal;
    }

    @NonNull
    public static Context getContext() {
        Check.getInstance().isNonNull(getInstance().mApplication);
        return getInstance().mApplication;
    }

    @NonNull
    public static RefWatcher getRefWatcher() {
        Check.getInstance().isNonNull(getInstance().mApplication);
        return getInstance().mRefWatcher;
    }

    @NonNull
    private static final Products sProducts = Products.create()
            .add(ProductTypes.IN_APP, Arrays.asList(
                    "donation_1",
                    "donation_4",
                    "donation_10",
                    "donation_20",
                    "donation_50",
                    "donation_99"))
            .add(ProductTypes.SUBSCRIPTION, Collections.singletonList(""));

    /**
     * Application wide {@link org.solovyev.android.checkout.Checkout} instance
     * (can be used anywhere in the app). This instance contains all available
     * products in the app.
     */
    @SuppressWarnings("NullableProblems")
    @NonNull
    private CheckoutInternal mCheckoutInternal;
    private Application mApplication;
    private RefWatcher mRefWatcher;

    /**
     * Must be called at {@link android.app.Application#onCreate()}
     */
    public void init(@NonNull Application application) {
        mRefWatcher = LeakCanary.install(application);

        mCheckoutInternal = new CheckoutInternal(application, sProducts);
        mApplication = application;

        TypefaceHelper.initialize(application);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLowMemory() {
    }
}
