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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.achep.activedisplay.R;
import com.achep.activedisplay.utils.ViewUtils;

/**
 * Created by Artem on 30.01.14.
 */
public class AboutDialog extends DialogFragment {

    private static final String VERSION_UNAVAILABLE = "N/A";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        PackageManager pm = getActivity().getPackageManager();
        String packageName = getActivity().getPackageName();
        String versionName;
        try {
            PackageInfo info = pm.getPackageInfo(packageName, 0);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = VERSION_UNAVAILABLE;
        }

        Drawable icon = getResources().getDrawable(R.mipmap.ic_launcher);
        CharSequence title = Html.fromHtml(getString(R.string.about_title,
                getString(R.string.app_name), versionName));
        CharSequence message = Html.fromHtml(getString(R.string.about_message));

        return new Builder(getActivity())
                .setIcon(icon)
                .setTitle(title)
                .setMessage(message)
                .create()
                .setPositiveButton(R.string.close, null)
                .create();
    }

    static class Builder {

        private final Context mContext;

        private Drawable mIcon;
        private CharSequence mTitleText;
        private CharSequence mMessageText;

        public Builder(Context context) {
            mContext = context;
        }

        public Builder setIcon(Drawable icon) {
            mIcon = icon;
            return this;
        }

        public Builder setTitle(CharSequence title) {
            mTitleText = title;
            return this;
        }

        public Builder setMessage(CharSequence message) {
            mMessageText = message;
            return this;
        }

        public AlertDialog.Builder create() {
            final LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View root = inflater.inflate(R.layout.dialog_base_message, null);

            Drawable left = (mContext.getResources().getConfiguration().screenLayout &
                    Configuration.SCREENLAYOUT_SIZE_MASK) !=
                    Configuration.SCREENLAYOUT_SIZE_LARGE ? mIcon : null;
            Drawable top = left == null ? mIcon : null;

            // Setting up title
            TextView title = (TextView) root.findViewById(R.id.title);
            ViewUtils.safelySetText(title, mTitleText);
            title.setCompoundDrawablesWithIntrinsicBounds(left, top, null, null);

            // Setting up message
            TextView message = (TextView) root.findViewById(R.id.message);
            message.setMovementMethod(new LinkMovementMethod());
            ViewUtils.safelySetText(message, mMessageText);

            return new AlertDialog.Builder(mContext).setView(root);
        }

    }
}
