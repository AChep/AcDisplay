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
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.achep.acdisplay.R;
import com.achep.base.AppHeap;
import com.achep.base.interfaces.IConfiguration;
import com.afollestad.materialdialogs.MaterialDialog;

/**
 * Dialog fragment that shows FAQ.
 *
 * @author Artem Chepurnoy
 */
public class HelpDialog extends DialogFragment {

    @NonNull
    private final Handler mHandler = new Handler();
    @NonNull
    private final Runnable mReadRunnable = new Runnable() {
        @Override
        public void run() {
            AppHeap.getInstance().getConfiguration().getHelp().onUserReadHelp();
        }
    };

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final IConfiguration configuration = AppHeap.getInstance().getConfiguration();
        return new MaterialDialog.Builder(getActivity())
                .iconRes(R.drawable.ic_help_circle_white_24dp)
                .title(R.string.help_dialog_title)
                .content(configuration.getHelp().getText(getActivity()))
                .negativeText(R.string.close)
                .build();
    }

    @Override
    public void onResume() {
        super.onResume();
        final int duration = getResources().getInteger(R.integer.config_maxHelpUserReadFuckyou);
        mHandler.postDelayed(mReadRunnable, duration);
    }

    @Override
    public void onPause() {
        mHandler.removeCallbacks(mReadRunnable);
        super.onPause();
    }

}
