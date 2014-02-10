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
package com.achep.activedisplay.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.achep.activedisplay.R;
import com.achep.activedisplay.notifications.NotificationData;
import com.achep.activedisplay.notifications.OpenStatusBarNotification;
import com.achep.activedisplay.utils.ViewUtils;

/**
 * Created by Artem on 12.01.14.
 */
public class NotificationPreviewLayout extends LinearLayout implements NotificationView {

    private ImageView mIcon;
    private TextView mTitleTextView;
    private TextView mMessageTextView;

    private OpenStatusBarNotification mNotification;

    public NotificationPreviewLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mIcon = (ImageView) findViewById(R.id.icon);
        mTitleTextView = (TextView) findViewById(R.id.title);
        mMessageTextView = (TextView) findViewById(R.id.message);
    }

    @Override
    public void setNotification(OpenStatusBarNotification notification) {
        mNotification = notification;

        NotificationData data = mNotification.getNotificationData();
        mIcon.setImageDrawable(mNotification.getSmallIcon(getContext()));
        ViewUtils.safelySetText(mTitleTextView, data.titleText);
        ViewUtils.safelySetText(mMessageTextView, data.messageText);
    }

    @Override
    public OpenStatusBarNotification getNotification() {
        return mNotification;
    }

}
