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
package com.achep.activedisplay.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;

import com.achep.activedisplay.DialogHelper;
import com.achep.activedisplay.R;
import com.achep.activedisplay.cryptocoin.Bitcoin;
import com.achep.activedisplay.cryptocoin.Coin;
import com.achep.activedisplay.utils.IntentUtils;

/**
 * Donation dialog fragment.
 * <p/>
 * Provides an description of cryptocoin and ability
 * to donate via any cryptocoin or PayPal.
 */
public class DonateDialog extends DialogFragment {

    private static final String PAYPAL_DONATION_URL =
            "http://goo.gl/UrecGo"; // shortened link to be able to get some stats

    private static class OnClickIntentLauncher implements DialogInterface.OnClickListener {

        private final Context mContext;
        private final Intent mIntent;

        public OnClickIntentLauncher(Context context, Intent intent) {
            mContext = context;
            mIntent = intent;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            if (IntentUtils.hasActivityForThat(mContext, mIntent)) {
                mContext.startActivity(mIntent);
            } else {
                // TODO: Show toast message
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Coin coin = new Bitcoin();
        Activity activity = getActivity();

        CharSequence messageText = Html.fromHtml(getString(R.string.donate_message,
                getString(R.string.app_name),
                coin.getBrowseUri().toString(),
                coin.getWikiUri().toString(),
                getString(coin.getNameResource())));
        OnClickIntentLauncher payPalClick = createBrowserClicker(Uri.parse(PAYPAL_DONATION_URL));

        AlertDialog.Builder builder = new DialogHelper.Builder(getActivity())
                .setIcon(coin.getIconResource())
                .setTitle(R.string.donate_title)
                .setMessage(messageText)
                .wrap()
                .setNegativeButton(R.string.close, null)
                .setNeutralButton(R.string.donate_use_paypal, payPalClick);

        final Intent paymentIntent = Coin.getPaymentIntent(coin);
        if (IntentUtils.hasActivityForThat(activity, paymentIntent)) {

            // There's a wallet installed so show donation button
            // to make the process faster.
            builder.setPositiveButton(R.string.donate, createClicker(paymentIntent));
        } else {

            // Show tutorial button if link is present because user
            // probably doesn't know about cryptocoins.
            Uri howToUri = coin.getHowToUri();
            if (howToUri != null) {
                builder.setPositiveButton(R.string.donate_how_to, createBrowserClicker(howToUri));
            }
        }

        return builder.create();
    }

    private OnClickIntentLauncher createClicker(Intent intent) {
        return new OnClickIntentLauncher(getActivity(), intent);
    }

    private OnClickIntentLauncher createBrowserClicker(Uri uri) {
        return createClicker(new Intent(Intent.ACTION_VIEW, uri));
    }

}
