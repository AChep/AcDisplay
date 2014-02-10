/*
 * Copyright (C) 2013-2014 AChep@xda <artemchep@gmail.com>
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
package com.achep.activedisplay.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;

import com.achep.activedisplay.R;
import com.achep.activedisplay.cryptocoin.Bitcoin;
import com.achep.activedisplay.cryptocoin.Coin;

import java.util.List;

/**
 * Created by Artem on 30.01.14.
 */
public class DonateDialog extends AboutDialog {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Coin coin = new Bitcoin();

        Drawable icon = getResources().getDrawable(coin.getIconResource());
        CharSequence title = getString(R.string.donate_title);
        CharSequence message = Html.fromHtml(getString(R.string.donate_message,
                getString(R.string.app_name),
                coin.getBrowseUri().toString(),
                coin.getWikiUri().toString(),
                getString(coin.getNameResource())));

        AlertDialog.Builder builder = new Builder(getActivity())
                .setIcon(icon)
                .setTitle(title)
                .setMessage(message)
                .create()
                .setNegativeButton(R.string.close, null);

        final Intent paymentIntent = Coin.getPaymentIntent(coin);
        @SuppressWarnings("ConstantConditions")
        final List apps = getActivity().getPackageManager()
                .queryIntentActivities(paymentIntent, 0);

        if (apps != null && apps.size() > 0) {

            // If user has any coin's Wallet installed
            // slap him with a large "Donate" button.
            builder.setPositiveButton(R.string.donate_action, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    startActivity(paymentIntent);
                }
            });
        } else {
            final Uri howToUri = coin.getHowToUri();
            if (howToUri != null) {
                builder.setPositiveButton(R.string.donate_how_to, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivity(new Intent(Intent.ACTION_VIEW, howToUri));
                    }
                });
            }
        }

        return builder.create();
    }

}
