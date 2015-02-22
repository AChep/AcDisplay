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
package com.achep.acdisplay.ui.components;

import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.acdisplay.interfaces.INotificatiable;
import com.achep.acdisplay.notifications.NotificationUtils;
import com.achep.acdisplay.notifications.OpenNotification;
import com.achep.acdisplay.ui.fragments.AcDisplayFragment;
import com.achep.acdisplay.ui.widgets.notification.NotificationIconWidget;
import com.achep.acdisplay.ui.widgets.notification.NotificationWidget;
import com.achep.acdisplay.utils.PendingIntentUtils;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Created by Artem on 02.04.2014.
 */
public class NotifyWidget extends Widget implements
        OpenNotification.OnNotificationDataChangedListener,
        INotificatiable {

    private NotificationIconWidget mIconView;
    private OpenNotification mNotification;
    private NotificationWidget mNotifyWidget;

    public NotifyWidget(@NonNull Callback callback, @NonNull AcDisplayFragment fragment) {
        super(callback, fragment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(65, 3)
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
        if (!(o instanceof NotifyWidget))
            return false;

        NotifyWidget widget = (NotifyWidget) o;
        return new EqualsBuilder()
                .append(mIconView, widget.mIconView)
                .append(mNotification, widget.mNotification)
                .append(mNotifyWidget, widget.mNotifyWidget)
                .isEquals();
    }

    /**
     * @return {@code true} if notification can be dismissed by user, {@code false} otherwise.
     */
    @Override
    public boolean isDismissible() {
        return mNotification.isDismissible() && super.isDismissible();
    }

    @Override
    public void onDismiss() {
        assert mNotifyWidget.getNotification() != null;
        mNotifyWidget.getNotification().dismiss();
    }

    @Override
    public boolean isReadable() {
        return !mNotification.isContentSecret(getHostFragment().getActivity());
    }

    @Override
    protected View onCreateIconView(
            @NonNull LayoutInflater inflater,
            @NonNull ViewGroup container) {
        View view = inflater.inflate(R.layout.notification_icon, container, false);
        assert view != null;

        mIconView = (NotificationIconWidget) view;
        mIconView.setNotification(mNotification);
        return view;
    }

    @Override
    protected ViewGroup onCreateView(
            @NonNull LayoutInflater inflater,
            @NonNull ViewGroup container,
            @Nullable ViewGroup sceneView) {
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
            public void onClick(NotificationWidget widget, View v) {
                final OpenNotification osbn = widget.getNotification();
                if (osbn != null) {
                    getHostFragment().unlock(new Runnable() {
                        @Override
                        public void run() {
                            osbn.click();
                        }
                    }, false);
                }
            }

            @Override
            public void onActionButtonClick(NotificationWidget widget, View v, final PendingIntent pendingIntent) {
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

    @NonNull
    @Override
    public OpenNotification getNotification() {
        return mNotification;
    }

    public boolean hasIdenticalIds(OpenNotification notification) {
        return NotificationUtils.hasIdenticalIds(mNotification, notification);
    }

    @Override
    public void onViewAttached() {
        super.onViewAttached();
        mNotification.markAsRead();
        mNotification.registerListener(this);
        mNotifyWidget.setNotification(mNotification);
    }

    @Override
    public void onViewDetached() {
        mNotification.unregisterListener(this);
        super.onViewDetached();
    }

    @Override
    public void onNotificationDataChanged(@NonNull OpenNotification notification, int event) {
        switch (event) {
            case OpenNotification.EVENT_BACKGROUND:
                mCallback.requestBackgroundUpdate(this);
                break;
        }
    }

    @Override
    public void setNotification(OpenNotification notification) {
        mNotification = notification;
        mIconView.setNotification(notification);

        // Don't update the content of notification widget, because
        // it may be used by any of its relatives.
        if (isViewAttached()) {
            mNotifyWidget.setNotification(notification);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public Bitmap getBackground() {
        return mNotification.getBackground();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBackgroundMask() {
        return Config.DYNAMIC_BG_NOTIFICATION_MASK;
    }

}
