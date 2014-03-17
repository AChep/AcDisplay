/*
 * Copyright (C) 2013 AChep@xda <artemchep@gmail.com>
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
package com.achep.activedisplay.cryptocoin;

import android.net.Uri;

import com.achep.activedisplay.R;

/**
 * Created by Artem on 06.02.14.
 */
public class Bitcoin extends Coin {

    public static final String DONATION_KEY = "1GYj49ZnMByKj2f6p7r4f92GQi5pR6BSMz";
    public static final double DONATION_AMOUNT = 0.005;

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public int getIconResource() {
        return R.drawable.ic_bitcoin;
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
    public Uri getBrowseUri() {
        return Uri.parse("https://www.biteasy.com/blockchain/addresses/" + getPaymentKey());
    }

    @Override
    public Uri getWikiUri() {
        return Uri.parse("http://www.youtube.com/watch?v=Um63OQz3bjo");
    }

    @Override
    public Uri getHowToUri() {
        return Uri.parse("https://www.trybtc.com/");
    }
}
