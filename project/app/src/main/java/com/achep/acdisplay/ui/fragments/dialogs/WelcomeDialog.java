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
package com.achep.acdisplay.ui.fragments.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Html;
import android.view.View;

import com.achep.acdisplay.R;
import com.achep.base.ui.DialogBuilder;
import com.achep.base.ui.fragments.dialogs.DialogFragment;

/**
 * Created by Artem Chepurnoy on 22.10.2014.
 */
public class WelcomeDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        assert context != null;

        CharSequence message = Html.fromHtml(getString(R.string.cry_dialog_message));

        View view = new DialogBuilder(context)
                .setIcon(R.drawable.ic_action_about_white)
                .setTitle(R.string.cry_dialog_title)
                .setMessage(message)
                .createView();

        return new AlertDialog.Builder(context)
                .setView(view)
                .setNegativeButton(R.string.close, null)
                .create();
    }

}
