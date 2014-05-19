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

package com.achep.acdisplay.acdisplay.components;

import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.acdisplay.acdisplay.AcDisplayFragment;
import com.achep.acdisplay.notifications.OpenStatusBarNotification;
import com.achep.acdisplay.utils.BitmapUtils;
import com.achep.acdisplay.utils.PendingIntentUtils;
import com.achep.acdisplay.widgets.NotificationIconWidget;
import com.achep.acdisplay.widgets.NotificationView;
import com.achep.acdisplay.widgets.NotificationWidget;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Created by Artem on 02.04.2014.
 */
public class NotificationUI extends Widget implements NotificationView {

    private NotificationIconWidget mIconView;
    private OpenStatusBarNotification mNotification;
    private NotificationWidget mNotifyWidget;

    public NotificationUI(AcDisplayFragment fragment) {
        super(fragment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(65, 810)
                .append(mIconView)
                .append(mNotification)
                .append(mNotifyWidget)
                .append(super.hashCode())
                .toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NotificationUI))
            return false;

        NotificationUI widget = (NotificationUI) o;
        return new EqualsBuilder()
                .append(mIconView, widget.mIconView)
                .append(mNotification, widget.mNotification)
                .append(mNotifyWidget, widget.mNotifyWidget)
                .isEquals();
    }

    @Override
    public boolean isDismissible() {
        return true;
    }

    @Override
    public void onDismissed(boolean right) {
        mNotifyWidget.getNotification().dismiss();
    }

    @Override
    public Bitmap getBackground() {
        Bitmap bitmap = mNotification.getNotificationData().getBackground();
        return bitmap != null && !BitmapUtils.hasTransparentCorners(bitmap) ? bitmap : null;
    }

    @Override
    protected View onCreateCollapsedView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.widget_notification_icon, container, false);
        assert view != null;

        mIconView = (NotificationIconWidget) view;
        int size = Config.getInstance().getIconSize("px");
        mIconView.setLayoutParams(new ViewGroup.LayoutParams(size, size));
        mIconView.setNotification(mNotification);
        return view;
    }

    @Override
    protected ViewGroup onCreateExpandedView(LayoutInflater inflater, ViewGroup container, ViewGroup sceneView) {
        boolean initialize = sceneView == null;
        if (initialize) {
            sceneView = (ViewGroup) inflater.inflate(R.layout.acdisplay_scene_notification, container, false);
            assert sceneView != null;
        }

        mNotifyWidget = (NotificationWidget) sceneView.findViewById(R.id.notification);

        if (!initialize) {
            return sceneView;
        }

        mNotifyWidget.setOnClickListener(new NotificationWidget.OnClickListener() {

            @Override
            public void onClick(View v) {
                final OpenStatusBarNotification osbn = mNotifyWidget.getNotification();
                if (osbn != null) {
                    getHostFragment().showMainWidget();
                    getHostFragment().unlock(new Runnable() {
                        @Override
                        public void run() {
                            osbn.click();
                        }
                    }, false);
                }
            }

            @Override
            public void onActionButtonClick(View v, final PendingIntent pendingIntent) {
                getHostFragment().showMainWidget();
                getHostFragment().unlock(new Runnable() {
                    @Override
                    public void run() {
                        PendingIntentUtils.sendPendingIntent(pendingIntent);
                    }
                }, false);
            }

        });

        return sceneView;
    }

    @Override
    public void onExpandedViewAttached() {
        mNotification.getNotificationData().markAsRead(true);
        mNotifyWidget.setNotification(mNotification);
    }

    @Override
    public void setNotification(OpenStatusBarNotification notification) {
        mNotification = notification;
        mIconView.setNotification(notification);

        // Don't update the content of notification widget, because
        // it may be used by any of its relatives.
        if (isExpandedViewAttached()) {
            mNotifyWidget.setNotification(notification);
        }
    }

    @Override
    public OpenStatusBarNotification getNotification() {
        return mNotification;
    }
}
