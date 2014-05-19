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
package com.achep.acdisplay.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.achep.acdisplay.R;
import com.achep.acdisplay.notifications.NotificationData;
import com.achep.acdisplay.notifications.OpenStatusBarNotification;
import com.achep.acdisplay.utils.ViewUtils;

/**
 * Created by Artem on 12.01.14.
 */
public class NotificationIconWidget extends FrameLayout implements NotificationView {

    private OpenStatusBarNotification mNotification;

    private NotificationIcon mIcon;
    private TextView mNumberTextView;

    public NotificationIconWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mIcon = (NotificationIcon) findViewById(R.id.icon);
        mNumberTextView = (TextView) findViewById(R.id.number);
    }

    @Override
    public void setNotification(OpenStatusBarNotification notification) {
        if (mNotification == notification) return;
        mNotification = notification;

        mIcon.setNotification(mNotification);
        NotificationData data = mNotification.getNotificationData();
        String tileText = data.number > 0 ? Integer.toString(data.number) : null;
        ViewUtils.safelySetText(mNumberTextView, tileText);
    }

    @Override
    public OpenStatusBarNotification getNotification() {
        return mNotification;
    }
}
