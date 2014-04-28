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
package com.achep.activedisplay.cryptocoin;

import android.content.Intent;
import android.net.Uri;

/**
 * Base class for simple virtual coins.
 *
 * @author Artem Chepurnoy
 */
public abstract class Coin {

    public static Intent getPaymentIntent(Coin coin) {
        return getPaymentIntent(coin, coin.getPaymentAmount());
    }

    public static Intent getPaymentIntent(Coin coin, double amount) {
        Uri uri = coin.getPaymentUri(amount);
        return new Intent(Intent.ACTION_VIEW, uri);
    }

    public abstract int getId();

    public abstract int getIconResource();

    public abstract int getNameResource();

    /**
     * @return The receiver's key. It may be an email or just "any string".
     */
    public abstract String getPaymentKey();

    /**
     * @return The default amount of money.
     * @see #getPaymentIntent(Coin)
     * @see #getPaymentIntent(Coin, double)
     */
    public abstract double getPaymentAmount();

    /**
     * @return Uri to page through which you can sends me moneys.
     * @see #getPaymentIntent(Coin)
     * @see #getPaymentIntent(Coin, double)
     */
    public abstract Uri getPaymentUri(double amount);

    /**
     * @return Uri to page that shows my current money.
     * @see #getUriWiki()
     * @see #getUriTutorial()
     */
    public abstract Uri getUriBrowseWallet();

    /**
     * @return Uri to page that explains the coin.
     * @see #getUriBrowseWallet()
     * @see #getUriTutorial()
     */
    public abstract Uri getUriWiki();

    /**
     * @return Uri to page that explains how to use that coin: send / receive money etc.
     * @see #getUriWiki()
     * @see #getUriBrowseWallet()
     */
    public abstract Uri getUriTutorial();

}
