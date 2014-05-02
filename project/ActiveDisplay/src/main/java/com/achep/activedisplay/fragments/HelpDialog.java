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
import com.achep.activedisplay.utils.IntentUtils;

/**
 * Dialog fragment that shows FAQ.
 *
 * @author Artem Chepurnoy
 */
public class HelpDialog extends DialogFragment {

    private static final String HELP_FULL_FAQ = "http://goo.gl/PT1sPt";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new DialogHelper.Builder(getActivity())
                .setIcon(R.drawable.ic_dialog_help)
                .setTitle(R.string.help)
                .setMessage(Html.fromHtml(getString(R.string.help_message)))
                .wrap()
                .setNegativeButton(R.string.close, null)
                .setPositiveButton(R.string.help_read_more, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Context context = getActivity();
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(HELP_FULL_FAQ));
                        if (IntentUtils.hasActivityForThat(context, intent)) {
                            context.startActivity(intent);
                        }
                    }
                })
                .create();
    }
}
