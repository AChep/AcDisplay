/*
 * Copyright (C) 2015 AChep@xda <artemchep@gmail.com>
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
package com.achep.base.ui.preferences;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;

import com.afollestad.materialdialogs.MaterialDialog;

/**
 * @author Artem Chepurnoy
 */
public abstract class MaterialDialogPreference extends DialogPreference {

    private MaterialDialog mDialog;

    public MaterialDialogPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public MaterialDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MaterialDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MaterialDialogPreference(Context context) {
        super(context);
    }

    //-- MAIN -----------------------------------------------------------------

    @Override
    public Dialog getDialog() {
        return mDialog;
    }

    @Override
    protected void showDialog(Bundle state) {
        MaterialDialog.Builder mBuilder = new MaterialDialog.Builder(getContext())
                .icon(getDialogIcon())
                .title(getDialogTitle())
                .content(getDialogMessage())
                .positiveText(getPositiveButtonText())
                .negativeText(getNegativeButtonText())
                .dismissListener(this);

        mDialog = onBuildDialog(mBuilder);
        if (state != null) mDialog.onRestoreInstanceState(state);
        mDialog.show();
    }

    @NonNull
    public abstract MaterialDialog onBuildDialog(@NonNull MaterialDialog.Builder builder);


}
