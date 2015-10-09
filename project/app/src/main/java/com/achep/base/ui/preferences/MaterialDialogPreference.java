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

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.Window;
import android.view.WindowManager;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.lang.reflect.Method;

/**
 * A base class for {@link Preference} objects that are
 * dialog-based. These preferences will, when clicked, open a dialog showing the
 * actual preference controls.
 *
 * @author Artem Chepurnoy
 */
public abstract class MaterialDialogPreference extends DialogPreference {

    private MaterialDialog mDialog;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MaterialDialogPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public MaterialDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MaterialDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
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
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog,
                                        @NonNull DialogAction dialogAction) {
                        MaterialDialogPreference.this.onClick(null, DialogInterface.BUTTON_POSITIVE);
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog,
                                        @NonNull DialogAction dialogAction) {
                        MaterialDialogPreference.this.onClick(null, DialogInterface.BUTTON_NEGATIVE);
                    }
                })
                .dismissListener(this);

        PreferenceManager pm = getPreferenceManager();
        try {
            Method method = pm.getClass().getDeclaredMethod(
                    "registerOnActivityDestroyListener",
                    PreferenceManager.OnActivityDestroyListener.class);
            method.setAccessible(true);
            method.invoke(pm, this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mDialog = onBuildDialog(mBuilder);
        if (state != null) mDialog.onRestoreInstanceState(state);
        if (needInputMethod()) requestInputMethod(mDialog);
        mDialog.show();
    }

    @NonNull
    public abstract MaterialDialog onBuildDialog(@NonNull MaterialDialog.Builder builder);

    /**
     * Sets the required flags on the dialog window to enable input method window to show up.
     */
    private void requestInputMethod(Dialog dialog) {
        Window window = dialog.getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    /**
     * Returns whether the preference needs to display a soft input method when the dialog
     * is displayed. Default is false. Subclasses should override this method if they need
     * the soft input method brought up automatically.
     */
    protected boolean needInputMethod() {
        return false;
    }

}
