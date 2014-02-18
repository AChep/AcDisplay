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
package com.achep.activedisplay;

/**
 * Created by Artem on 28.01.14.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.achep.activedisplay.fragments.AboutDialog;
import com.achep.activedisplay.fragments.DonateDialog;
import com.achep.activedisplay.fragments.FeedbackDialog;
import com.achep.activedisplay.utils.ViewUtils;

/**
 * Helper class for showing fragment dialogs.
 */
public class DialogHelper {

    public static void showAboutDialog(Activity activity) {
        showDialog(activity, AboutDialog.class, "dialog_about");
    }

    public static void showDonateDialog(Activity activity) {
        showDialog(activity, DonateDialog.class, "dialog_donate");
    }

    public static void showFeedbackDialog(Activity activity) {
        showDialog(activity, FeedbackDialog.class, "dialog_feedback");
    }

    private static void showDialog(Activity activity, Class clazz, String tag) {
        FragmentManager fm = activity.getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(tag);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        try {
            ((DialogFragment) clazz.newInstance()).show(ft, tag);
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper class to implement custom dialog's design.
     */
    public static class Builder {

        private final Context mContext;

        private Drawable mIcon;
        private CharSequence mTitleText;
        private View mView;

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

        public Builder setView(View view) {
            mView = view;
            return this;
        }

        /**
         * Builds dialog's view
         */
        public View create() {
            final LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View root = inflater.inflate(R.layout.dialog_base, null);
            assert root != null;

            Drawable left = (mContext.getResources().getConfiguration().screenLayout &
                    Configuration.SCREENLAYOUT_SIZE_MASK) !=
                    Configuration.SCREENLAYOUT_SIZE_LARGE ? mIcon : null;
            Drawable top = left == null ? mIcon : null;

            // Setting up title
            TextView title = (TextView) root.findViewById(R.id.title);
            ViewUtils.safelySetText(title, mTitleText);
            title.setCompoundDrawablesWithIntrinsicBounds(left, top, null, null);

            // Setting up content
            FrameLayout content = (FrameLayout) root.findViewById(R.id.content);
            content.addView(mView);

            return root;
        }

        public AlertDialog.Builder createAlertDialogBuilder() {
            return new AlertDialog.Builder(mContext).setView(create());
        }

    }

}