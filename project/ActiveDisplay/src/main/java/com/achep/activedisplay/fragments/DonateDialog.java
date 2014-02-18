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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.achep.activedisplay.DialogHelper;
import com.achep.activedisplay.R;
import com.achep.activedisplay.cryptocoin.Bitcoin;
import com.achep.activedisplay.cryptocoin.Coin;
import com.achep.activedisplay.utils.ViewUtils;

import java.util.List;

/**
 * Donation dialog fragment.
 * <p/>
 * Provides an description of cryptocoin and ability
 * to donate.
 */
public class DonateDialog extends DialogFragment {

    private static class OnClickIntentLauncher implements DialogInterface.OnClickListener {

        private final Context mContext;
        private final Intent mIntent;

        public OnClickIntentLauncher(Context context, Intent intent) {
            mContext = context;
            mIntent = intent;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            try {
                mContext.startActivity(mIntent);
            } catch (android.content.ActivityNotFoundException ex) {
                // TODO: Show toast message
                ex.printStackTrace();
            }
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Coin coin = new Bitcoin();

        Activity activity = getActivity();
        assert activity != null;

        Drawable icon = getResources().getDrawable(coin.getIconResource());
        CharSequence title = getString(R.string.donate_title);
        CharSequence message = Html.fromHtml(getString(R.string.donate_message,
                getString(R.string.app_name),
                coin.getBrowseUri().toString(),
                coin.getWikiUri().toString(),
                getString(coin.getNameResource())));

        LayoutInflater inflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        TextView messageTextView = (TextView) inflater.inflate(R.layout.layout_dialog_message, null);
        assert messageTextView != null;

        // Setting up message
        messageTextView.setMovementMethod(new LinkMovementMethod());
        ViewUtils.safelySetText(messageTextView, message);

        AlertDialog.Builder builder = new DialogHelper.Builder(getActivity())
                .setIcon(icon)
                .setTitle(title)
                .setView(messageTextView)
                .createAlertDialogBuilder()
                .setNegativeButton(R.string.close, null);

        final Intent paymentIntent = Coin.getPaymentIntent(coin);
        @SuppressWarnings("ConstantConditions")
        final List apps = activity.getPackageManager().queryIntentActivities(paymentIntent, 0);

        if (apps != null && apps.size() > 0) {

            // If user has any coin's wallet installed
            // slap him with a large "Donate" button.
            builder.setPositiveButton(R.string.donate,
                    new OnClickIntentLauncher(getActivity(), paymentIntent));
        } else {
            Uri howtoUri = coin.getHowToUri();
            if (howtoUri != null) {
                Intent howtoIntent = new Intent(Intent.ACTION_VIEW, howtoUri);
                builder.setPositiveButton(R.string.donate_how_to,
                        new OnClickIntentLauncher(getActivity(), howtoIntent));
            }
        }

        return builder.create();
    }

}
