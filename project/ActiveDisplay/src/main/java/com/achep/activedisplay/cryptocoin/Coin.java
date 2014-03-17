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

import android.content.Intent;
import android.net.Uri;

/**
 * Created by Artem on 06.02.14.
 */
public abstract class Coin {

    static final Class[] COINS = new Class[]{Bitcoin.class};

    // ///////////// -- BASICS -- ///////////////

    public abstract int getId();

    public abstract int getIconResource();

    public abstract int getNameResource();

    // //////////// -- PAYMENT -- ///////////////

    public abstract String getPaymentKey();

    public abstract double getPaymentAmount();

    public abstract Uri getPaymentUri(double amount);

    public abstract Uri getBrowseUri();

    public abstract Uri getWikiUri();

    public abstract Uri getHowToUri();

    // ///////////// -- GLOBAL -- ///////////////

    public static Intent getPaymentIntent(int id) {
        for (Class clazz : COINS) {
            try {
                Coin coin = (Coin) clazz.newInstance();
                if (id == coin.getId()) {
                    return getPaymentIntent(coin);
                }
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static Intent getPaymentIntent(Coin coin) {
        Uri uri = coin.getPaymentUri(coin.getPaymentAmount());
        return new Intent(Intent.ACTION_VIEW, uri);
    }

}
