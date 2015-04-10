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
package com.achep.base.ui.fragments.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Html;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.base.utils.RawReader;
import com.afollestad.materialdialogs.MaterialDialog;

/**
 * Dialog fragment that shows FAQ.
 *
 * @author Artem Chepurnoy
 */
public class HelpDialog extends DialogFragment {

    private final Handler mHandler = new Handler();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String source = RawReader.readText(getActivity(), R.raw.faq);
        CharSequence message = Html.fromHtml(source);
        return new MaterialDialog.Builder(getActivity())
                .iconRes(R.drawable.ic_action_help_white)
                .title(R.string.help_dialog_title)
                .content(message)
                .negativeText(R.string.close)
                .build();
    }

    @Override
    public void onResume() {
        super.onResume();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Context context = getActivity();
                assert context != null;
                Config.getInstance().getTriggers().setHelpRead(context, true, null);
            }
        }, getResources().getInteger(R.integer.config_maxHelpUserReadFuckyou));
    }

    @Override
    public void onPause() {
        mHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

}
