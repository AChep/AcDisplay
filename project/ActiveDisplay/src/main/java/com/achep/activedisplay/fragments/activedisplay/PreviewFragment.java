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

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.achep.activedisplay.Operator;
import com.achep.activedisplay.R;
import com.achep.activedisplay.fragments.MyFragment;
import com.achep.activedisplay.notifications.NotificationPresenter;
import com.achep.activedisplay.notifications.OpenStatusBarNotification;
import com.achep.activedisplay.utils.ViewUtils;
import com.achep.activedisplay.widgets.NotificationPreviewLayout;

/**
 * Created by Artem on 25.01.14.
 */
public class PreviewFragment extends MyFragment {

    private static final String TAG = "PreviewFragment";

    private static final int REFRESH_UI_NOTIFICATION_LIST = 1;

    private LinearLayout mContainer;

    private NotificationPresenter mPresenter;
    private NotificationListener mListener = new NotificationListener();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mPresenter = NotificationPresenter.getInstance();
        synchronized (mPresenter.monitor) {
            mPresenter.addOnNotificationListChangedListener(mListener);
            updateNotificationList();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        synchronized (mPresenter.monitor) {
            mPresenter.removeOnNotificationListChangedListener(mListener);
            mPresenter = null;
        }
    }

    @Override
    protected void handleTodoList(int v) {
        if (Operator.bitandCompare(v, REFRESH_UI_NOTIFICATION_LIST)) {
            updateNotificationList();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ad_preview_list, container, false);
        assert view != null;
        mContainer = (LinearLayout) view.findViewById(R.id.container);
        return view;
    }

    private void updateNotificationList() {
        updateNotificationList(true);
    }

    @SuppressWarnings("ConstantConditions")
    private void updateNotificationList(boolean updateAll) {
        if (tryPutTodo(REFRESH_UI_NOTIFICATION_LIST)) return;

        synchronized (mPresenter.monitor) {
            if (updateAll) {
                boolean visible = Helper.updateNotificationList(mPresenter, mContainer,
                        R.layout.item_ad_preview, getActivity().getLayoutInflater());
                if (!visible) return;
            }

            // Check current notification
            final int size = mPresenter.getList().size();
            final OpenStatusBarNotification notification = mPresenter.getSelectedNotification();
            for (int i = 0; i < size; i++) {
                NotificationPreviewLayout child = (NotificationPreviewLayout) mContainer.getChildAt(i);
                assert child != null;
                ViewUtils.setVisible(child, notification != child.getNotification());
            }
        }
    }

    // //////////////////////////////////////////
    // ///////////// -- CLASSES -- //////////////
    // //////////////////////////////////////////

    private class NotificationListener extends NotificationPresenter.SimpleOnNotificationListChangedListener {

        @Override
        // running on wrong thread
        public void onNotificationSelected(final NotificationPresenter nm,
                                           final OpenStatusBarNotification notification,
                                           boolean isChanged) {
            super.onNotificationSelected(nm, notification, isChanged);

            // We will update list on onChanged() event some millis later.
            if (!isChanged) {
                updateNotificationListInternal(false);
            }
        }

        @Override
        // running on wrong thread
        public void onNotificationEvent(NotificationPresenter nm,
                                        OpenStatusBarNotification notification,
                                        int event) {
            super.onNotificationEvent(nm, notification, event);
            if (event != SELECTED) {
                updateNotificationListInternal(true);
            }
        }

        @SuppressWarnings("ConstantConditions")
        private void updateNotificationListInternal(final boolean updateAll) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateNotificationList(updateAll);
                }
            });
        }
    }

}
