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
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import com.achep.acdisplay.R;
import com.achep.base.interfaces.ICoin;

/**
 * PayPal's very basics.
 * <p></p>
 * PayPal is an American, international digital wallet based e-commerce
 * business allowing payments and money transfers to be made through the Internet.
 * Online money transfers serve as electronic alternatives to paying with
 * traditional paper methods, such as checks and money orders.<br/>
 * PayPal is one of the world's largest internet payment companies.
 * The company operates as an acquirer, performing payment processing for
 * online vendors, auction sites and other commercial users, for which it charges a fee.
 *
 * @author Artem Chepurnoy
 */
public class PayPal implements ICoin {

    private static final String KEY = "donations.achep@gmail.com";
    private static final double AMOUNT = 0; // Let user choose the amount

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
        return R.string.paypal;
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
        StringBuilder builder = new StringBuilder();
        builder.append("https://www.paypal.com/cgi-bin/webscr?cmd=_donations");
        builder.append("&lc=US&item_name=Artem%20Chepurnoy");
        builder.append("&business=").append(getPaymentKey());
        if (amount > 0) builder.append("&amount=").append(amount);
        return Uri.parse(builder.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public Uri getUriBrowseWallet() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Uri getUriWhatIsIt() {
        return Uri.parse("https://www.youtube.com/watch?v=c_A6OOzndt8");
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Uri getUriTutorial() {
        return Uri.parse("http://www.wikihow.com/Use-PayPal");
    }

}
