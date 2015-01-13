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
package com.achep.acdisplay.ui.widgets.notification;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.achep.acdisplay.R;
import com.achep.acdisplay.interfaces.INotificatiable;
import com.achep.acdisplay.notifications.OpenNotification;

/**
 * Created by Artem on 12.01.14.
 */
public class NotificationIconWidget extends FrameLayout implements INotificatiable {

    @Nullable
    private OpenNotification mNotification;

    // Views
    private NotificationIcon mNotificationIcon;
    private TextView mNumberTextView;

    public NotificationIconWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mNotificationIcon = (NotificationIcon) findViewById(R.id.icon);
        mNumberTextView = (TextView) findViewById(R.id.number);
        if (mNotification != null) setNotification(mNotification);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNotification(@Nullable OpenNotification notification) {
        if (mNotificationIcon == null) {
            // This means that #onFinishInflate() hadn't happen yet.
            // Lets wait for it and re-set notification.
            mNotification = notification;
            return;
        }

        mNotification = null;
        mNotificationIcon.setNotification(notification);

        int number = notification == null ? 0 : notification.getNumber();
        if (number > 0) {
            mNumberTextView.setText(Integer.toString(number));
            mNumberTextView.setVisibility(VISIBLE);
        } else {
            mNumberTextView.setVisibility(GONE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public OpenNotification getNotification() {
        return mNotificationIcon != null
                ? mNotificationIcon.getNotification()
                : mNotification;
    }

}
