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
package com.achep.base.billing;

import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.achep.acdisplay.R;
import com.achep.base.interfaces.ICoin;

/**
 * Bitcoin's very basics.
 * <p></p>
 * Bitcoin is a peer-to-peer payment system introduced as open source
 * software in 2009 by developer Satoshi Nakamoto. The digital currency
 * created and used in the system is also called bitcoin and is
 * alternatively referred to as a virtual currency, electronic money,
 * or cryptocurrency. The bitcoin system is not controlled by a single entity,
 * like a central bank, which has led the US Treasury to call bitcoin a
 * decentralized currency.
 *
 * @author Artem Chepurnoy
 */
public class Bitcoin implements ICoin {

    private static final String KEY = "1GYj49ZnMByKj2f6p7r4f92GQi5pR6BSMz";
    private static final double AMOUNT = 0.01; // BTC

    /**
     * {@inheritDoc}
     */
    @DrawableRes
    @Override
    public int getIconResource() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @StringRes
    @Override
    public int getNameResource() {
        return R.string.bitcoin;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public String getPaymentKey() {
        return KEY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getPaymentAmount() {
        return AMOUNT;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Uri getPaymentUri(double amount) {
        return Uri.parse("bitcoin:" + getPaymentKey() + "?amount=" + Double.toString(amount));
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Uri getUriBrowseWallet() {
        return Uri.parse("https://www.biteasy.com/blockchain/addresses/" + getPaymentKey());
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Uri getUriWhatIsIt() {
        return Uri.parse("http://www.youtube.com/watch?v=Gc2en3nHxA4");
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Uri getUriTutorial() {
        return Uri.parse("https://www.trybtc.com/");
    }

}
