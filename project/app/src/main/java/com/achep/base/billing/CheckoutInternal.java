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
package com.achep.base.billing;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.achep.base.Build;
import com.achep.base.tests.Check;
import com.achep.base.utils.EncryptionUtils;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.solovyev.android.checkout.Billing;
import org.solovyev.android.checkout.Checkout;
import org.solovyev.android.checkout.Inventory;
import org.solovyev.android.checkout.Products;
import org.solovyev.android.checkout.RobotmediaDatabase;
import org.solovyev.android.checkout.RobotmediaInventory;

import java.util.concurrent.Executor;

import timber.log.Timber;

/**
 * @author Artem Chepurnoy
 */
public class CheckoutInternal {

    private static final String TAG = "CheckoutInternal";

    /**
     * For better performance billing class should be used as singleton
     */
    @NonNull
    private final Billing mBilling;

    /**
     * Application wide {@link org.solovyev.android.checkout.Checkout} instance
     * (can be used anywhere in the app). This instance contains all available
     * products in the app.
     */
    @NonNull
    private final Checkout mCheckout;

    /**
     * The number of active {@link #requestConnect() connect requests}.
     */
    private int mConnections;

    public CheckoutInternal(@NonNull Context context, @NonNull Products products) {
        mBilling = new Billing(context, new Configuration());
        mCheckout = Checkout.forApplication(mBilling, products);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(15689, 31)
                .append(mBilling)
                .append(mCheckout)
                .append(mConnections)
                .toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (!(o instanceof CheckoutInternal)) return false;

        CheckoutInternal ci = (CheckoutInternal) o;
        return new EqualsBuilder()
                .append(mConnections, ci.mConnections)
                .append(mBilling, ci.mBilling)
                .append(mCheckout, ci.mCheckout)
                .isEquals();
    }

    @NonNull
    public Billing getBilling() {
        return mBilling;
    }

    @NonNull
    public Checkout getCheckout() {
        return mCheckout;
    }

    /**
     * Connects to the billing service.
     *
     * @see #disconnect()
     */
    private void connect() {
        mBilling.connect();
    }

    /**
     * Disconnect from the billing service.
     *
     * @see #connect()
     */
    private void disconnect() {
        mBilling.disconnect();
    }

    public void requestConnect() {
        if (mConnections++ == 0) connect();
    }

    public void requestDisconnect() {
        if (mConnections-- == 1) disconnect();
        Check.getInstance().isTrue(mConnections >= 0);
    }

    private class Configuration extends Billing.DefaultConfiguration {

        @NonNull
        @Override
        public String getPublicKey() {
            // TODO: Somehow replace those local variables on build.
            // final String k = "__BUILD_SCRIPT__:ENCRYPTED_PUBLIC_KEY:";
            // final String s = "__BUILD_SCRIPT__:ENCRYPTED_PUBLIC_KEY_SALT:";
            final String k = Build.GOOGLE_PLAY_PUBLIC_KEY_ENCRYPTED;
            final String s = Build.GOOGLE_PLAY_PUBLIC_KEY_SALT;
            try {
                return EncryptionUtils.fromX(k, s);
            } catch (IllegalArgumentException e) {
                Timber.tag(TAG).e("Failed to decode the public Google Play key!");
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

    }

}
