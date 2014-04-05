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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;

import com.achep.activedisplay.DialogHelper;
import com.achep.activedisplay.Project;
import com.achep.activedisplay.R;

/**
 * Created by Artem on 30.01.14.
 */
public class AboutDialog extends DialogFragment {

    private static final String VERSION_UNAVAILABLE = "N/A";

    static CharSequence getVersionTitle(Context context) {
        PackageManager pm = context.getPackageManager();
        String packageName = context.getPackageName();
        String versionName;
        try {
            PackageInfo info = pm.getPackageInfo(packageName, 0);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = VERSION_UNAVAILABLE;
        }

        if (Project.DEBUG) versionName += "-dev";

        Resources res = context.getResources();
        return Html.fromHtml(
                res.getString(R.string.about_title,
                        res.getString(R.string.app_name), versionName)
        );
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new DialogHelper.Builder(getActivity())
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(getVersionTitle(getActivity()))
                .setMessage(Html.fromHtml(getString(R.string.about_message)))
                .wrap()
                .setPositiveButton(R.string.close, null)
                .create();
    }
}
