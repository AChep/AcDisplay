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
package com.achep.acdisplay;

import android.app.Application;
import android.support.annotation.NonNull;

import com.achep.acdisplay.blacklist.Blacklist;
import com.achep.acdisplay.services.KeyguardService;
import com.achep.acdisplay.services.SensorsDumpService;
import com.achep.acdisplay.services.activemode.ActiveModeService;
import com.achep.base.billing.CheckoutInternal;

import org.solovyev.android.checkout.Checkout;
import org.solovyev.android.checkout.ProductTypes;
import org.solovyev.android.checkout.Products;

import java.util.Arrays;

/**
 * Created by Artem on 22.02.14.
 */
public class App extends Application {

    private static final String TAG = "App";

    public static final int ID_NOTIFY_INIT = 30;
    public static final int ID_NOTIFY_TEST = 40;
    public static final int ID_NOTIFY_BATH = 50;

    public static final String ACTION_BIND_MEDIA_CONTROL_SERVICE = "com.achep.acdisplay.BIND_MEDIA_CONTROL_SERVICE";

    public static final String ACTION_ENABLE = "com.achep.acdisplay.ENABLE";
    public static final String ACTION_DISABLE = "com.achep.acdisplay.DISABLE";
    public static final String ACTION_TOGGLE = "com.achep.acdisplay.TOGGLE";

    public static final String ACTION_EAT_HOME_PRESS_START = "com.achep.acdisplay.EAT_HOME_PRESS_START";
    public static final String ACTION_EAT_HOME_PRESS_STOP = "com.achep.acdisplay.EAT_HOME_PRESS_STOP";

    public static final String ACTION_INTERNAL_TIMEOUT = "TIMEOUT";
    public static final String ACTION_INTERNAL_PING_SENSORS = "PING_SENSORS";

    @NonNull
    private static final Products sProducts = Products.create()
            .add(ProductTypes.IN_APP, Arrays.asList(
                    "donation_1",
                    "donation_4",
                    "donation_10",
                    "donation_20",
                    "donation_50",
                    "donation_99"))
            .add(ProductTypes.SUBSCRIPTION, Arrays.asList(""));

    /**
     * Application wide {@link org.solovyev.android.checkout.Checkout} instance
     * (can be used anywhere in the app). This instance contains all available
     * products in the app.
     */
    @NonNull
    private final CheckoutInternal mCheckoutInternal = new CheckoutInternal(this, sProducts);

    @NonNull
    private static App instance;

    public App() {
        instance = this;
    }

    @Override
    public void onCreate() {
        Config.getInstance().init(this);
        Blacklist.getInstance().init(this);

        super.onCreate();
        mCheckoutInternal.getBilling().connect();

        // Launch keyguard and (or) active mode on
        // app launch.
        KeyguardService.handleState(this);
        ActiveModeService.handleState(this);
        SensorsDumpService.handleState(this);
    }

    @Override
    public void onLowMemory() {
        Config.getInstance().onLowMemory();
        Blacklist.getInstance().onLowMemory();
        super.onLowMemory();
    }

    @NonNull
    public static App get() {
        return instance;
    }

    @NonNull
    public static Checkout getCheckout() {
        return instance.mCheckoutInternal.getCheckout();
    }

}
