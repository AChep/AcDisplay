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
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import com.achep.acdisplay.blacklist.Blacklist;
import com.achep.acdisplay.services.KeyguardService;
import com.achep.acdisplay.services.SensorsDumpService;
import com.achep.acdisplay.services.activemode.ActiveModeService;
import com.achep.base.Build;

import org.solovyev.android.checkout.Billing;
import org.solovyev.android.checkout.Checkout;
import org.solovyev.android.checkout.Inventory;
import org.solovyev.android.checkout.ProductTypes;
import org.solovyev.android.checkout.Products;
import org.solovyev.android.checkout.RobotmediaDatabase;
import org.solovyev.android.checkout.RobotmediaInventory;

import java.util.Arrays;
import java.util.concurrent.Executor;

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
     * For better performance billing class should be used as singleton
     */
    @NonNull
    private final Billing mBilling = new Billing(this, new Billing.DefaultConfiguration() {

        @NonNull
        @Override
        public String getPublicKey() {
            // TODO: Somehow replace those local variables on build.
            // final String k = "__BUILD_SCRIPT__:ENCRYPTED_PUBLIC_KEY:";
            // final String s = "__BUILD_SCRIPT__:ENCRYPTED_PUBLIC_KEY_SALT:";
            final String k = Build.GOOGLE_PLAY_PUBLIC_KEY_ENCRYPTED;
            final String s = Build.GOOGLE_PLAY_PUBLIC_KEY_SALT;
            try {
                return fromX(k, s);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Failed to decode the public Google Play key!");
            }
            return "fail";
        }

        @Nullable
        @Override
        public Inventory getFallbackInventory(@NonNull Checkout checkout,
                                              @NonNull Executor onLoadExecutor) {
            if (RobotmediaDatabase.exists(mBilling.getContext())) {
                return new RobotmediaInventory(checkout, onLoadExecutor);
            } else {
                return null;
            }
        }

    });

    /**
     * Application wide {@link org.solovyev.android.checkout.Checkout} instance
     * (can be used anywhere in the app). This instance contains all available
     * products in the app.
     */
    @NonNull
    private final Checkout mCheckout = Checkout.forApplication(mBilling, sProducts);

    /**
     * Method deciphers previously ciphered message
     *
     * @param message ciphered message
     * @param salt    salt which was used for ciphering
     * @return deciphered message
     */
    @NonNull
    static String fromX(@NonNull String message, @NonNull String salt)
            throws IllegalArgumentException {
        return x(new String(Base64.decode(message, Base64.URL_SAFE)), salt);
    }

    /**
     * Symmetric algorithm used for ciphering/deciphering.
     *
     * @param message message
     * @param salt    salt
     * @return ciphered/deciphered message
     */
    @NonNull
    static String x(@NonNull String message, @NonNull String salt) {
        final char[] m = message.toCharArray();
        final char[] s = salt.toCharArray();

        final int ml = m.length;
        final int sl = s.length;
        final char[] result = new char[ml];

        for (int i = 0; i < ml; i++) {
            result[i] = (char) (m[i] ^ s[i % sl]);
        }
        return new String(result);
    }

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
        mBilling.connect();

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
        return instance.mCheckout;
    }

}
