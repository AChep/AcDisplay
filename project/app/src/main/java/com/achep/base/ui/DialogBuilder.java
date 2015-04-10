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
package com.achep.base.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.achep.acdisplay.R;
import com.achep.base.Device;
import com.achep.base.utils.ResUtils;
import com.afollestad.materialdialogs.MaterialDialog;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Helper class to implement custom dialog's design.
 */
public class DialogBuilder {

    /**
     * Layout where content's layout exists in a {@link android.widget.ScrollView}.
     * This is nice to display simple layout without scrollable elements such as
     * {@link android.widget.ListView} or any similar. Use {@link #LAYOUT_SKELETON}
     * for them.
     *
     * @see #LAYOUT_SKELETON
     * @see #createView()
     * @see #createAlertDialogBuilder(int)
     */
    public static final int LAYOUT_COMMON = 0;

    /**
     * The skeleton of dialog's layout. The only thing that is here is the custom
     * view you set and the title / icon. Use it to display scrollable elements such as
     * {@link android.widget.ListView}.
     *
     * @see #LAYOUT_COMMON
     * @see #createSkeletonView()
     * @see #createAlertDialogBuilder(int)
     */
    public static final int LAYOUT_SKELETON = 1;

    protected final Context mContext;

    private Drawable mIcon;
    private CharSequence mTitleText;
    private CharSequence mMessageText;
    private View mView;
    private int mViewRes;
    private int mContentViewRes;

    public DialogBuilder(@NonNull Context context) {
        mContext = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(201, 17)
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
        if (!(o instanceof DialogBuilder))
            return false;

        DialogBuilder builder = (DialogBuilder) o;
        return new EqualsBuilder()
                .append(mContext, builder.mContext)
                .append(mIcon, builder.mIcon)
                .append(mTitleText, builder.mTitleText)
                .append(mMessageText, builder.mMessageText)
                .append(mViewRes, builder.mViewRes)
                .append(mView, builder.mView)
                .isEquals();
    }

    /**
     * Sets the icon of the title.
     *
     * @see #setIcon(int)
     */
    @NonNull
    public DialogBuilder setIcon(@Nullable Drawable icon) {
        mIcon = icon;
        return this;
    }

    /**
     * Sets the icon of the title.
     *
     * @see #setIcon(android.graphics.drawable.Drawable)
     */
    @NonNull
    public DialogBuilder setIcon(@DrawableRes int drawableResource) {
        return setIcon(drawableResource == 0
                ? null
                : ResUtils.getDrawable(mContext, drawableResource));
    }

    /**
     * Sets the title text.
     *
     * @see #setTitle(int)
     * @see #setIcon(int)
     */
    @NonNull
    public DialogBuilder setTitle(@Nullable CharSequence title) {
        mTitleText = title;
        return this;
    }

    /**
     * Sets the title text.
     *
     * @see #setTitle(CharSequence)
     * @see #setIcon(android.graphics.drawable.Drawable)
     */
    @NonNull
    public DialogBuilder setTitle(@StringRes int titleRes) {
        return setTitle(titleRes == 0
                ? null
                : getString(titleRes));
    }

    @NonNull
    public DialogBuilder setMessage(@Nullable CharSequence message) {
        mMessageText = message;
        return this;
    }

    @NonNull
    public DialogBuilder setMessage(@StringRes int messageRes) {
        return setMessage(messageRes == 0
                ? null
                : getString(messageRes));
    }

    private String getString(int stringRes) {
        return mContext.getResources().getString(stringRes);
    }

    public DialogBuilder setView(View view) {
        mView = view;
        mViewRes = 0;
        return this;
    }

    public DialogBuilder setView(@LayoutRes int layoutRes) {
        mView = null;
        mViewRes = layoutRes;
        return this;
    }

    public DialogBuilder setContentView(@LayoutRes int layoutRes) {
        mContentViewRes = layoutRes;
        return this;
    }

    //-- CREATING VIEW --------------------------------------------------------

    public View createView() {
        return createView(LAYOUT_COMMON);
    }

    public View createSkeletonView() {
        return createView(LAYOUT_SKELETON);
    }

    /**
     * Builds dialog's view
     *
     * @throws IllegalArgumentException when type is not one of defined.
     * @see #LAYOUT_COMMON
     * @see #LAYOUT_SKELETON
     */
    public View createView(int type) {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        ViewGroup rootLayout = (ViewGroup) createSkeleton();
        ViewGroup contentLayout = rootLayout;

        switch (type) {
            case LAYOUT_COMMON:
                final boolean hasMessageOnly = mView == null && mViewRes == 0;
                final int layoutResource = mContentViewRes != 0
                        ? mContentViewRes : hasMessageOnly
                        ? R.layout.dialog_message
                        : R.layout.dialog_content;

                ViewStub viewStub = (ViewStub) inflater
                        .inflate(R.layout.dialog_main_body, rootLayout, true)
                        .findViewById(R.id.placeholder);
                viewStub.setLayoutResource(layoutResource);

                contentLayout = (ViewGroup) viewStub.inflate().findViewById(R.id.content);
                if (contentLayout == null) contentLayout = rootLayout;
                TextView messageView = (TextView) contentLayout.findViewById(R.id.message);

                if (messageView != null) {
                    if (!TextUtils.isEmpty(mMessageText)) {
                        messageView.setMovementMethod(new LinkMovementMethod());
                        messageView.setText(mMessageText);
                    } else {
                        ViewGroup vg = (ViewGroup) messageView.getParent();
                        vg.removeView(messageView);
                    }
                }

                // Fall down.
            case LAYOUT_SKELETON:

                if (mViewRes != 0) {
                    inflater.inflate(mViewRes, contentLayout, true);
                } else if (mView != null) {
                    contentLayout.addView(mView);
                }

                return rootLayout;
            default:
                throw new IllegalArgumentException();
        }
    }

    private View createSkeleton() {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout rootLayout = (LinearLayout) inflater.inflate(
                R.layout.dialog_main_skeleton, new FrameLayout(mContext), false);
        TextView titleView = (TextView) rootLayout.findViewById(R.id.title);

        if (Device.hasLollipopApi()) {
            // The dividers are quite ugly with material design.
            rootLayout.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);
        }

        if (mTitleText == null && mIcon == null) {
            rootLayout.removeView(titleView);
        } else {
            if (mTitleText != null)
                titleView.setText(mTitleText);
            if (mIcon != null)
                titleView.setCompoundDrawablesWithIntrinsicBounds(mIcon, null, null, null);
        }

        return rootLayout;
    }

    //-- ALERT DIALOG ---------------------------------------------------------

    public MaterialDialog.Builder createAlertDialogBuilder() {
        return createAlertDialogBuilder(LAYOUT_COMMON);
    }

    /**
     * Creates view and {@link MaterialDialog.Builder#customView(View, boolean) sets}
     * to a new {@link MaterialDialog.Builder}.
     *
     * @param type type of container layout
     * @return MaterialDialog.Builder with set custom view
     */
    public MaterialDialog.Builder createAlertDialogBuilder(int type) {
        return new MaterialDialog.Builder(mContext).customView(createView(type), false);
    }

}
