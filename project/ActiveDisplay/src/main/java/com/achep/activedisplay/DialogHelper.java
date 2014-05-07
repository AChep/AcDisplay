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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

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

        /**
         * Layout where content's layout exists in a {@link android.widget.ScrollView}.
         * This is nice to display simple layout without scrollable elements such as
         * {@link android.widget.ListView} or any similar. Use {@link #LAYOUT_SKELETON}
         * for them.
         *
         * @see #LAYOUT_SKELETON
         * @see #createCommonView()
         * @see #wrap(android.view.View[], int)
         */
        public static final int LAYOUT_COMMON = 0;

        /**
         * The skeleton of dialog's layout. The only thing that is here is the custom
         * view you set and the title / icon. Use it to display scrollable elements such as
         * {@link android.widget.ListView}.
         *
         * @see #LAYOUT_COMMON
         * @see #createSkeletonView()
         * @see #wrap(android.view.View[], int)
         */
        public static final int LAYOUT_SKELETON = 1;

        protected final Context mContext;

        private Drawable mIcon;
        private CharSequence mTitleText;
        private CharSequence mMessageText;
        private View mView;
        private int mViewRes;

        public Builder(Context context) {
            mContext = context;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return new HashCodeBuilder(201, 32)
                    .append(mContext)
                    .append(mIcon)
                    .append(mTitleText)
                    .append(mMessageText)
                    .append(mViewRes)
                    .append(mView)
                    .toHashCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if (o == null)
                return false;
            if (o == this)
                return true;
            if (!(o instanceof Builder))
                return false;

            Builder builder = (Builder) o;
            return new EqualsBuilder()
                    .append(mContext, builder.mContext)
                    .append(mIcon, builder.mIcon)
                    .append(mTitleText, builder.mTitleText)
                    .append(mMessageText, builder.mMessageText)
                    .append(mViewRes, builder.mViewRes)
                    .append(mView, builder.mView)
                    .isEquals();
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
            return setTitle(titleRes == 0 ? null : getString(titleRes));
        }

        public Builder setMessage(int messageRes) {
            return setMessage(messageRes == 0 ? null : getString(messageRes));
        }

        private String getString(int stringRes) {
            return mContext.getResources().getString(stringRes);
        }

        public Builder setView(View view) {
            mView = view;
            mViewRes = 0;
            return this;
        }

        public Builder setView(int layoutRes) {
            mView = null;
            mViewRes = layoutRes;
            return this;
        }

        /**
         * Builds dialog's view
         *
         * @see #LAYOUT_COMMON
         * @see #LAYOUT_SKELETON
         */
        public View createView(int type) {
            switch (type) {
                case LAYOUT_COMMON:
                    return createCommonView();
                case LAYOUT_SKELETON:
                    return createSkeletonView();
                default:
                    throw new IllegalArgumentException();
            }
        }

        /**
         * @see #LAYOUT_COMMON
         * @see #createView(int)
         */
        public View createCommonView() {

            // Creating skeleton layout will also
            // add custom view. Avoid of doing it.
            int customViewRes = mViewRes;
            View customView = mView;
            mViewRes = 0;
            mView = null;

            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            ViewGroup rootLayout = (ViewGroup) createSkeletonView();
            View bodyRootView = inflater.inflate(R.layout.dialog_common, rootLayout, false);
            ViewGroup bodyLayout = (ViewGroup) bodyRootView.findViewById(R.id.content);
            TextView messageView = (TextView) bodyLayout.findViewById(R.id.message);

            rootLayout.addView(bodyRootView);

            // Setup content
            bodyLayout.removeView(messageView);
            if (!TextUtils.isEmpty(mMessageText)) {
                messageView.setMovementMethod(new LinkMovementMethod());
                messageView.setText(mMessageText);
                bodyLayout.addView(messageView);
            }

            // Custom view
            if (customViewRes != 0) customView = inflater.inflate(customViewRes, bodyLayout, false);
            if (customView != null) bodyLayout.addView(customView);

            return rootLayout;
        }

        /**
         * @see #LAYOUT_SKELETON
         * @see #createView(int)
         */
        public View createSkeletonView() {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            ViewGroup rootLayout = (ViewGroup) inflater.inflate(R.layout.dialog_skeleton, null);
            TextView titleView = (TextView) rootLayout.findViewById(R.id.title);

            // Setup icon
            Drawable left = (mContext.getResources().getConfiguration().screenLayout &
                    Configuration.SCREENLAYOUT_SIZE_MASK) !=
                    Configuration.SCREENLAYOUT_SIZE_LARGE ? mIcon : null;
            Drawable top = left == null ? mIcon : null;

            // Setup title
            if (mTitleText != null) {
                titleView.setText(mTitleText);
                titleView.setCompoundDrawablesWithIntrinsicBounds(left, top, null, null);
            } else {
                // This also removes an icon.
                rootLayout.removeView(titleView);
            }

            // Custom view
            if (mViewRes != 0) mView = inflater.inflate(mViewRes, rootLayout, false);
            if (mView != null) rootLayout.addView(mView);

            return rootLayout;
        }

        public AlertDialog.Builder wrap() {
            return wrap(null, LAYOUT_COMMON);
        }

        public AlertDialog.Builder wrap(View[] customView, int type) {
            View view = createView(type);

            if (customView != null && customView.length == 1) {
                customView[0] = mView;
            }

            return new AlertDialog.Builder(mContext).setView(view);
        }

    }

}