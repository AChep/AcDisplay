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
package com.achep.acdisplay.cryptocoin;

import android.net.Uri;

import com.achep.acdisplay.R;

/**
 * An implementation of Bitcoin.
 * <p></p>
 * Bitcoin is a peer-to-peer payment system introduced as open source
 * software in 2009 by developer Satoshi Nakamoto. The digital currency
 * created and used in the system is also called bitcoin and is
 * alternatively referred to as a virtual currency, electronic money,
 * or cryptocurrency. The bitcoin system is not controlled by a single entity,
 * like a central bank, which has led the US Treasury to call bitcoin a
 * decentralized currency. Economists generally agree that it does not
 * meet the definition of money.
 *
 * @author Artem Chepurnoy
 */
public class Bitcoin extends Coin {

    private static final String DONATION_KEY = "1GYj49ZnMByKj2f6p7r4f92GQi5pR6BSMz";
    private static final double DONATION_AMOUNT = 0.005;

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public int getIconResource() {
        return 0;
    }

    @Override
    public int getNameResource() {
        return R.string.bitcoin;
    }

    @Override
    public String getPaymentKey() {
        return DONATION_KEY;
    }

    @Override
    public double getPaymentAmount() {
        return DONATION_AMOUNT;
    }

    @Override
    public Uri getPaymentUri(double amount) {
        return Uri.parse("bitcoin:" + getPaymentKey() + "?amount=" + Double.toString(amount));
    }

    @Override
    public Uri getUriBrowseWallet() {
        return Uri.parse("https://www.biteasy.com/blockchain/addresses/" + getPaymentKey());
    }

    @Override
    public Uri getUriWiki() {
        return Uri.parse("http://www.youtube.com/watch?v=Um63OQz3bjo");
    }

    @Override
    public Uri getUriTutorial() {
        return Uri.parse("https://www.trybtc.com/");
    }
}
