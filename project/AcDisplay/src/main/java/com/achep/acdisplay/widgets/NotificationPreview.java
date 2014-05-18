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
import android.graphics.Bitmap;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.achep.acdisplay.R;
import com.achep.acdisplay.notifications.NotificationData;
import com.achep.acdisplay.notifications.OpenStatusBarNotification;

/**
 * Simple notification widget that shows the title of notification and
 * its message.
 *
 * @see com.achep.acdisplay.widgets.NotificationWidget
 * @author Artem Chepurnoy
 */
public class NotificationPreview extends RelativeLayout implements NotificationView {

    private NotificationIcon mIcon;
    private TextView mTitleTextView;
    private TextView mMessageTextView;

    private OpenStatusBarNotification mNotification;

    public NotificationPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NotificationPreview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mIcon = (NotificationIcon) findViewById(R.id.icon);
        mTitleTextView = (TextView) findViewById(R.id.title);
        mMessageTextView = (TextView) findViewById(R.id.message);

        mIcon.setNotificationIndicateReadStateEnabled(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenStatusBarNotification getNotification() {
        return mNotification;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNotification(OpenStatusBarNotification osbn) {
        mNotification = osbn;
        if (osbn == null) {
            // TODO: Hide everything or show a notice to user.
            return;
        }

        StatusBarNotification sbn = osbn.getStatusBarNotification();
        NotificationData data = osbn.getNotificationData();

        CharSequence message = data.getLargeMessage();

        // If message is empty hide the view to free space
        // taken by margins.
        if (!TextUtils.isEmpty(message)) {
            mMessageTextView.setText(message);
            mMessageTextView.setVisibility(VISIBLE);
        } else {
            mMessageTextView.setVisibility(GONE);
        }

        mTitleTextView.setText(data.titleText);

        Bitmap bitmap = data.getCircleIcon();
        if (bitmap == null) bitmap = sbn.getNotification().largeIcon;
        if (bitmap != null) {

            // Disable tracking notification's icon
            // and set large icon.
            mIcon.setNotification(null);
            mIcon.setImageBitmap(bitmap);
        } else {
            mIcon.setNotification(osbn);
        }
    }
}
