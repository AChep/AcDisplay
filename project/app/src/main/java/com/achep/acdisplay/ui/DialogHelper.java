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
package com.achep.acdisplay.ui;

import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;

import com.achep.acdisplay.R;
import com.achep.base.Device;
import com.achep.base.permissions.Permission;
import com.achep.base.tests.Check;
import com.achep.base.ui.fragments.dialogs.AboutDialog;
import com.achep.base.ui.fragments.dialogs.DonateDialog;
import com.achep.base.ui.fragments.dialogs.FeedbackDialog;
import com.achep.base.ui.fragments.dialogs.HelpDialog;
import com.achep.base.ui.fragments.dialogs.PermissionsDialog;
import com.achep.base.utils.ResUtils;
import com.afollestad.materialdialogs.MaterialDialog;

/**
 * Helper class for showing fragment dialogs.
 */
public class DialogHelper {

    public static final String TAG_FRAGMENT_ABOUT = "dialog_about";
    public static final String TAG_FRAGMENT_PERMISSIONS = "dialog_permissions";
    public static final String TAG_FRAGMENT_HELP = "dialog_help";
    public static final String TAG_FRAGMENT_DONATION = "dialog_donate";
    public static final String TAG_FRAGMENT_FEEDBACK = "dialog_feedback";

    /**
     * Shows the "About" dialog that contains some info about the program,
     * developers and used libraries and tools.
     */
    public static void showAboutDialog(@NonNull AppCompatActivity activity) {
        showDialog(activity, AboutDialog.class, TAG_FRAGMENT_ABOUT);
    }

    /**
     * Shows the "Help" dialog that contains some frequently asked questions
     * and some answers on them. This is the dialog that nobody reads :(
     */
    public static void showHelpDialog(@NonNull AppCompatActivity activity) {
        showDialog(activity, HelpDialog.class, TAG_FRAGMENT_HELP);
    }

    public static void showDonateDialog(@NonNull AppCompatActivity activity) {
        showDialog(activity, DonateDialog.class, TAG_FRAGMENT_DONATION);
    }

    public static void showCryDialog(@NonNull AppCompatActivity activity) {
        Check.getInstance().isInMainThread();

        Resources res = activity.getResources();
        CharSequence message = Html.fromHtml(res.getString(R.string.cry_dialog_message));

        new MaterialDialog.Builder(activity)
                .iconRes(R.drawable.ic_action_about_white)
                .title(R.string.cry_dialog_title)
                .content(message)
                .negativeText(R.string.close)
                .build()
                .show();
    }

    public static void showCompatDialog(@NonNull AppCompatActivity activity) {
        Check.getInstance().isInMainThread();

        int[] pairs = {
                R.string.compat_dialog_item_notification,
                android.os.Build.VERSION_CODES.JELLY_BEAN_MR2,
                R.string.compat_dialog_item_immersive_mode,
                android.os.Build.VERSION_CODES.KITKAT,
                R.string.compat_dialog_item_music_widget,
                android.os.Build.VERSION_CODES.KITKAT,
        };

        // Check over all pairs.
        boolean empty = true;
        for (int i = 1; i < pairs.length; i += 2) {
            final int api = pairs[i];
            if (!Device.hasTargetApi(api)) {
                empty = false;
                break;
            }
        }
        if (empty) return;

        String formatter = activity.getString(R.string.compat_dialog_item_formatter);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < pairs.length; i += 2) {
            final int api = pairs[i + 1];
            if (!Device.hasTargetApi(api)) {
                String str = activity.getString(pairs[i]);
                str = String.format(formatter, str);
                builder.append(str);
            }
        }

        String message = ResUtils.getString(activity, R.string.compat_dialog_message, Build.VERSION.RELEASE, builder);
        new MaterialDialog.Builder(activity)
                .iconRes(R.drawable.ic_dialog_compat_white)
                .title(R.string.compat_dialog_title)
                .content(message)
                .negativeText(R.string.close)
                .build()
                .show();
    }

    public static void showFeedbackDialog(@NonNull AppCompatActivity activity) {
        showDialog(activity, FeedbackDialog.class, TAG_FRAGMENT_FEEDBACK);
    }

    public static void showPermissionsDialog(@NonNull AppCompatActivity activity,
                                             @NonNull Permission[] permissions) {
        showDialog(activity, PermissionsDialog.newInstance(permissions), TAG_FRAGMENT_PERMISSIONS);
    }

    //-- INTERNAL -------------------------------------------------------------

    private static void showDialog(@NonNull AppCompatActivity activity,
                                   @NonNull Class clazz,
                                   @NonNull String tag) {
        DialogFragment df;
        try {
            df = (DialogFragment) clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        showDialog(activity, df, tag);
    }

    private static void showDialog(@NonNull AppCompatActivity activity,
                                   @NonNull DialogFragment fragment,
                                   @NonNull String tag) {
        Check.getInstance().isInMainThread();

        FragmentManager fm = activity.getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(tag);
        if (prev != null) ft.remove(prev);
        ft.addToBackStack(null);
        fragment.show(ft, tag);
    }

}