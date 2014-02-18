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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.achep.activedisplay.DialogHelper;
import com.achep.activedisplay.Project;
import com.achep.activedisplay.R;

/**
 * Donation dialog fragment.
 * <p/>
 * Provides an UI for sending bugs & suggestions to my email.
 */
public class FeedbackDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        assert activity != null;

    //    activity.startService(new Intent(activity, BreathingService.class));

        Drawable icon = getResources().getDrawable(R.drawable.ic_mail);
        CharSequence title = getString(R.string.feedback_title);

        LayoutInflater inflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View root = inflater.inflate(R.layout.layout_dialog_feedback, null);
        assert root != null;

        final Spinner spinner = (Spinner) root.findViewById(R.id.type);
        final EditText editText = (EditText) root.findViewById(R.id.message);
        final AlertDialog alertDialog = new DialogHelper.Builder(getActivity())
                .setIcon(icon)
                .setTitle(title)
                .setView(root)
                .createAlertDialogBuilder()
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.feedback_send, null)
                .create();

        assert spinner != null;
        assert editText != null;

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {
                Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                assert button != null;
                button.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        CharSequence message = editText.getText();

                        // Check for message's length
                        int messageMinLength = getResources() .getInteger(
                                R.integer.config_feedback_minMessageLength);
                        if (message == null || message.length() < messageMinLength) {
                            Toast.makeText(getActivity(), getString(
                                    R.string.feedback_error_msg_too_short,
                                    messageMinLength), Toast.LENGTH_SHORT)
                                    .show();
                            return; // Don't dismiss dialog
                        }

                        PackageInfo pi;
                        try {
                            //noinspection ConstantConditions
                            pi = getActivity().getPackageManager().getPackageInfo(
                                    Project.getPackageName(getActivity()), 0);
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                            return;
                        }

                        CharSequence title = createTitleMessage(pi, spinner.getSelectedItemPosition());
                        CharSequence body = createBodyMessage(pi, message);
                        Intent intent = new Intent(Intent.ACTION_SEND)
                                .setType("message/rfc822")
                                .putExtra(Intent.EXTRA_EMAIL, new String[] {Project.EMAIL})
                                .putExtra(Intent.EXTRA_SUBJECT, title)
                                .putExtra(Intent.EXTRA_TEXT, body);

                        try {
                            startActivity(Intent.createChooser(intent,
                                    getString(R.string.feedback_send_via)));

                            // Dismiss current dialog once everything is ok
                            alertDialog.dismiss();
                        } catch (android.content.ActivityNotFoundException ex) {
                            Toast.makeText(getActivity(), getString(R.string.feedback_error_no_app),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });

        return alertDialog;
    }

    private CharSequence createTitleMessage(PackageInfo pi, int type) {
        CharSequence typeName;
        switch (type) {
            case 0:
                typeName = "bug";
                break;
            case 1:
                typeName = "suggestion";
                break;
            case 2:
                typeName = "other";
                break;
            default:
                typeName = "unknown";
                break;
        }
        return "Feedback: " + typeName + ", " + pi.packageName;
    }

    private CharSequence createBodyMessage(PackageInfo pi, CharSequence msg) {
        return "" + msg +
                '\n' +
                '\n' +
                "- - - - - - - - - - - - -" + '\n' +
                "app_version:" + pi.versionName + '(' + pi.versionCode + ")\n" +
                "android_version:" + Build.VERSION.RELEASE + '(' + Build.VERSION.SDK_INT + ")\n" +
                "build_display:" + Build.DISPLAY + '\n' +
                "build_brand:" + Build.BRAND + '\n' +
                "build_model:" + Build.MODEL;
    }

}
