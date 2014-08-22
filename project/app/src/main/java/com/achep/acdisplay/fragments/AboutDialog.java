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
package com.achep.acdisplay.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Toast;

import com.achep.acdisplay.App;
import com.achep.acdisplay.Build;
import com.achep.acdisplay.DialogHelper;
import com.achep.acdisplay.R;
import com.achep.acdisplay.utils.ToastUtils;

/**
 * Dialog fragment that shows some info about this application.
 *
 * @author Artem Chepurnoy
 */
public class AboutDialog extends DialogFragment implements View.OnClickListener {

    private static final String VERSION_UNAVAILABLE = "N/A";
    private static final int EASTER_EGGS_CLICK_NUMBER = 5;

    private int mTitleClickNumber;
    private Toast mTimeStampToast;

    /**
     * Merges app name and version name into one.
     */
    public static CharSequence getVersionName(Context context) {
        PackageManager pm = context.getPackageManager();
        String packageName = context.getPackageName();
        String versionName;
        try {
            PackageInfo info = pm.getPackageInfo(packageName, 0);
            versionName = info.versionName;

            // Make the info part of version name a bit smaller.
            if (versionName.indexOf('-') >= 0) {
                versionName = versionName.replaceFirst("\\-", "<small>-") + "</small>";
            }
        } catch (PackageManager.NameNotFoundException e) {
            versionName = VERSION_UNAVAILABLE;
        }

        Resources res = context.getResources();
        return Html.fromHtml(
                res.getString(R.string.about_title,
                        res.getString(R.string.app_name), versionName)
        );
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        assert context != null;

        CharSequence message = Html.fromHtml(getString(
                R.string.about_message, getString(R.string.about_thanks)));

        View view = new DialogHelper.Builder(context)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(getVersionName(context))
                .setMessage(message)
                .createCommonView();
        View title = view.findViewById(R.id.title);
        title.setOnClickListener(this);

        return new AlertDialog.Builder(context)
                .setView(view)
                .setNeutralButton(R.string.close, null)
                .create();
    }

    @Override
    public void onClick(View v) {
        if (mTimeStampToast != null) {
            // Cancel previous toast to avoid of
            // continuous spam after.
            mTimeStampToast.cancel();
        }

        Context context = getActivity();

        if (++mTitleClickNumber == EASTER_EGGS_CLICK_NUMBER) {
            App.startEasterEggs(context);
            mTitleClickNumber = 0;
        } else {
            mTimeStampToast = ToastUtils.showLong(context, Build.TIME_STAMP);
        }
    }
}
