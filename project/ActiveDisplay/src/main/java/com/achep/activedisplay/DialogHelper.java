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
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.achep.activedisplay.fragments.AboutDialog;
import com.achep.activedisplay.fragments.DonateDialog;
import com.achep.activedisplay.fragments.FeedbackDialog;
import com.achep.activedisplay.fragments.HelpDialog;
import com.achep.activedisplay.fragments.NewsDialog;

/**
 * Helper class for showing fragment dialogs.
 */
public class DialogHelper {

    public static void showAboutDialog(Activity activity) {
        showDialog(activity, AboutDialog.class, "dialog_about");
    }

    public static void showHelpDialog(Activity activity) {
        showDialog(activity, HelpDialog.class, "dialog_help");
    }

    public static void showDonateDialog(Activity activity) {
        showDialog(activity, DonateDialog.class, "dialog_donate");
    }

    public static void showFeedbackDialog(Activity activity) {
        showDialog(activity, FeedbackDialog.class, "dialog_feedback");
    }

    public static void showNewsDialog(Activity activity) {
        showDialog(activity, NewsDialog.class, "dialog_news");
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

        protected final Context mContext;

        private Drawable mIcon;
        private CharSequence mTitleText;
        private CharSequence mMessageText;
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

        public Builder setMessage(CharSequence message) {
            mMessageText = message;
            return this;
        }

        public Builder setIcon(int iconRes) {
            return setIcon(iconRes == 0 ? null : mContext.getResources().getDrawable(iconRes));
        }

        public Builder setTitle(int titleRes) {
            return setTitle(titleRes == 0 ? null : mContext.getResources().getString(titleRes));
        }

        public Builder setMessage(int messageRes) {
            return setMessage(messageRes == 0 ? null : mContext.getResources().getString(messageRes));
        }

        public Builder setView(View view) {
            mView = view;
            return this;
        }

        /**
         * Builds dialog's view
         */
        public View create() {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            ViewGroup rootLayout = (ViewGroup) inflater.inflate(R.layout.dialog_base, null);
            TextView titleView = (TextView) rootLayout.findViewById(R.id.title);
            ViewGroup bodyLayout = (ViewGroup) rootLayout.findViewById(R.id.content);
            TextView messageView = (TextView) rootLayout.findViewById(R.id.message);

            Drawable left = (mContext.getResources().getConfiguration().screenLayout &
                    Configuration.SCREENLAYOUT_SIZE_MASK) !=
                    Configuration.SCREENLAYOUT_SIZE_LARGE ? mIcon : null;
            Drawable top = left == null ? mIcon : null;

            // Setup title
            if (mTitleText != null) {
                titleView.setText(mTitleText);
                titleView.setCompoundDrawablesWithIntrinsicBounds(left, top, null, null);
            } else {
                rootLayout.removeView(titleView);
            }

            // Setup content
            bodyLayout.removeView(messageView);
            if (mView != null) bodyLayout.addView(mView);
            if (!TextUtils.isEmpty(mMessageText)) {
                messageView.setMovementMethod(new LinkMovementMethod());
                messageView.setText(mMessageText);
                bodyLayout.addView(messageView);
            }

            return rootLayout;
        }

        public AlertDialog.Builder wrap() {
            return new AlertDialog.Builder(mContext).setView(create());
        }

    }

}
