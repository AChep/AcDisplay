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
package com.achep.activedisplay.fragments.activedisplay;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.achep.activedisplay.Operator;
import com.achep.activedisplay.R;
import com.achep.activedisplay.fragments.MyFragment;
import com.achep.activedisplay.notifications.NotificationData;
import com.achep.activedisplay.notifications.NotificationPresenter;
import com.achep.activedisplay.notifications.OpenStatusBarNotification;
import com.achep.activedisplay.utils.ViewUtils;

/**
 * Created by Artem on 25.01.14.
 */
public class NotificationFragment extends MyFragment {

    private static final String TAG = "NotificationFragment";

    private static final int UPDATE_UI_NOTIFICATION = 1;

    private ImageView mIcon;
    private ImageView mSmallIcon;
    private TextView mTitleTextView;
    private TextView mMessageTextView;
    private TextView mWhenTextView;
    private TextView mCountTextView;
    private TextView mInfoTextView;
    private View[] mHidingViews;

    private NotificationPresenter mPresenter;
    private NotificationListener mListener = new NotificationListener();
    private OpenStatusBarNotification mNotification;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mPresenter = NotificationPresenter.getInstance(getActivity());
        synchronized (mPresenter.monitor) {
            mPresenter.addOnNotificationListChangedListener(mListener);
            updateNotification();
        }
    }

    @Override
    public void onDestroyView() {
        synchronized (mPresenter.monitor) {
            mPresenter.removeOnNotificationListChangedListener(mListener);
            mPresenter = null;
        }
        super.onDestroyView();
    }

    @Override
    protected void handleTodoList(int v) {
        if (Operator.bitandCompare(v, UPDATE_UI_NOTIFICATION)) {
            updateNotification();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ad_notification, container, false);
        assert view != null;

        mIcon = (ImageView) view.findViewById(R.id.icon);
        mSmallIcon = (ImageView) view.findViewById(R.id.icon_small);
        mTitleTextView = (TextView) view.findViewById(R.id.title);
        mMessageTextView = (TextView) view.findViewById(R.id.message);
        mWhenTextView = (TextView) view.findViewById(R.id.when);
        mCountTextView = (TextView) view.findViewById(R.id.count);
        mInfoTextView = (TextView) view.findViewById(R.id.info);

        // TODO: It's definitely an evil part of this code
        mHidingViews = new View[]{mIcon, mSmallIcon, mTitleTextView,
                mMessageTextView, mWhenTextView, mCountTextView,
                mInfoTextView};

        return view;
    }

    @SuppressWarnings("ConstantConditions")
    private void updateNotification() {
        if (tryPutTodo(UPDATE_UI_NOTIFICATION)) {
            return; // not a good time...
        }

        synchronized (mPresenter.monitor) {
            mNotification = mPresenter.getSelectedNotification();

            // Hide everything if notification is null.
            boolean visible = mNotification != null;
            for (View view : mHidingViews) // getView().setVisibility(GONE) is incorrect way!
                ViewUtils.setVisible(view, visible);
            if (!visible) return;

            StatusBarNotification notification = mNotification.getStatusBarNotification();
            NotificationData data = mNotification.getNotificationData();

            ViewUtils.safelySetText(mTitleTextView, data.titleText);
            ViewUtils.safelySetText(mMessageTextView, data.getLargeMessage());
            ViewUtils.safelySetText(mInfoTextView, data.infoText == null ? data.subText : data.infoText);
            ViewUtils.safelySetText(mCountTextView, data.number > 0 ? Integer.toString(data.number) : null);

            mWhenTextView.setText(DateUtils.formatDateTime(getActivity(),
                    notification.getPostTime(), DateUtils.FORMAT_SHOW_TIME));

            boolean showSmallIcon = mCountTextView.getVisibility() == View.VISIBLE;

            Drawable drawable = mNotification.getSmallIcon(getActivity());
            Bitmap bitmap = notification.getNotification().largeIcon;
            if (bitmap != null) {
                mIcon.setImageBitmap(bitmap);
                showSmallIcon = true;
            } else {
                mIcon.setImageDrawable(drawable);
            }

            if (showSmallIcon) mSmallIcon.setImageDrawable(drawable);
            ViewUtils.setVisible(mSmallIcon, showSmallIcon);
        }
    }

    /**
     * Returns the displaying notification.
     * The return value may differ from {@link NotificationPresenter#getSelectedNotification()}!
     */
    public OpenStatusBarNotification getNotification() {
        return mNotification;
    }

    // //////////////////////////////////////////
    // ///////////// -- CLASSES -- //////////////
    // //////////////////////////////////////////

    private class NotificationListener extends NotificationPresenter.SimpleOnNotificationListChangedListener {

        @SuppressWarnings("ConstantConditions")
        @Override
        // running on wrong thread
        public void onNotificationSelected(final NotificationPresenter nm,
                                           final OpenStatusBarNotification notification,
                                           boolean isChanged) {
            super.onNotificationSelected(nm, notification, isChanged);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateNotification();
                }
            });
        }
    }

}
