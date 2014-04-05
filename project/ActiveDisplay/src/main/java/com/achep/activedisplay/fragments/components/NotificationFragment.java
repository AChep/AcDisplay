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

package com.achep.activedisplay.fragments.components;

import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.achep.activedisplay.R;
import com.achep.activedisplay.fragments.AcDisplayFragment;
import com.achep.activedisplay.notifications.NotificationHelper;
import com.achep.activedisplay.notifications.OpenStatusBarNotification;
import com.achep.activedisplay.utils.PendingIntentUtils;
import com.achep.activedisplay.view.SwipeDismissTouchListener;
import com.achep.activedisplay.widgets.NotificationIconWidget;
import com.achep.activedisplay.widgets.NotificationView;
import com.achep.activedisplay.widgets.NotificationWidget;

/**
 * Created by Artem on 02.04.2014.
 */
public class NotificationFragment extends AcDisplayFragment.Widget implements NotificationView {

    private static final String TAG = "NotificationFragment";

    private NotificationIconWidget mIconView;
    private ViewGroup mSceneView;

    private OpenStatusBarNotification mNotification;
    private NotificationWidget mNotifyWidget;

    public NotificationFragment(AcDisplayFragment fragment) {
        super(fragment);
    }

    @Override
    public int getType() {
        return AcDisplayFragment.SCENE_NOTIFICATION;
    }

    @Override
    public View getCollapsedView() {
        return mIconView;
    }

    @Override
    public ViewGroup getExpandedView() {
        return mSceneView;
    }

    @Override
    public View onCreateCollapsedView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.widget_notification_icon, container, false);
        assert view != null;

        mIconView = (NotificationIconWidget) view;
        mIconView.setNotification(mNotification);
        return view;
    }

    @Override
    public ViewGroup onCreateExpandedView(LayoutInflater inflater, ViewGroup container, ViewGroup sceneView) {
        boolean initialize = sceneView == null;
        if (initialize) {
            sceneView = (ViewGroup) inflater.inflate(R.layout.acdisplay_scene_notification, container, false);
            assert sceneView != null;
        }

        mNotifyWidget = (NotificationWidget) sceneView.findViewById(R.id.notification);

        if (initialize) {
            // I do keep in mind that those settings are
            // shared over all who uses this scene too.

            mNotifyWidget.setOnTouchListener(new SwipeDismissTouchListener(
                    mNotifyWidget, null,
                    new SwipeDismissTouchListener.DismissCallbacks() {

                        @Override
                        public boolean canDismiss(Object token) {
                            OpenStatusBarNotification osbn = mNotifyWidget.getNotification();
                            return osbn != null && osbn.getStatusBarNotification().isClearable();
                        }

                        @Override
                        public void onDismiss(View view, Object token) {
                            NotificationHelper.dismissNotification(mNotifyWidget
                                    .getNotification()
                                    .getStatusBarNotification());
                            getHostFragment().showMainWidget();
                        }
                    }
            ));

            mNotifyWidget.setOnClickListener(new NotificationWidget.OnClickListener() {

                @Override
                public void onClick(View v) {
                    final OpenStatusBarNotification osbn = mNotifyWidget.getNotification();

                    if (osbn == null) {
                        throw new IllegalStateException("Notification widget is shown but it doesn\'t contain notification.");
                    }

                    getHostFragment().showMainWidget();
                    postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            NotificationHelper.startContentIntent(osbn);
                        }
                    });
                }

                @Override
                public void onActionClick(View v, final PendingIntent pendingIntent) {
                    getHostFragment().showMainWidget();
                    postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            PendingIntentUtils.sendPendingIntent(pendingIntent);
                        }
                    });
                }

                private void postRunnable(Runnable runnable) {
                    getHostFragment().unlock(runnable, true);
                }
            });
        }

        mSceneView = sceneView;
        return sceneView;
    }

    @Override
    public void onExpandedViewAttached() {
        mNotification.getNotificationData().markAsRead(true);
        mNotifyWidget.setNotification(mNotification);
        displayBackgroundBitmap();
    }

    private void displayBackgroundBitmap() {
        Bitmap bitmap = mNotification.getNotificationData().getBackground();

        if (bitmap == null
                || bitmap.getPixel(0, 0) == Color.TRANSPARENT) {
            getHostFragment().dispatchSetBackground(null);
            return;
        }

        getHostFragment().dispatchSetBackground(bitmap);
    }

    @Override
    public void setNotification(OpenStatusBarNotification notification) {
        if (notification == null) {
            throw new IllegalArgumentException("Notification may not be null!");
        }

        mNotification = notification;
        mIconView.setNotification(notification);

        if (isShown()) {
            mNotifyWidget.setNotification(notification);
        }
    }

    @Override
    public OpenStatusBarNotification getNotification() {
        return mNotification;
    }
}
