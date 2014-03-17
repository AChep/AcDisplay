/*
 * Copyright (C) 2013 AChep@xda <artemchep@gmail.com>
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
import android.graphics.drawable.InsetDrawable;
import android.util.AttributeSet;
import android.widget.RadioButton;

import com.achep.activedisplay.R;
import com.achep.activedisplay.notifications.OpenStatusBarNotification;

/**
 * Created by Artem on 12.01.14.
 */
public class NotificationRadioButton extends RadioButton implements NotificationView {

    private OpenStatusBarNotification mNotification;

    public NotificationRadioButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setNotification(OpenStatusBarNotification notification) {
        mNotification = notification;

        final int padding = (getResources().getDimensionPixelSize(R.dimen.stat_notifications_list_item_size)
                - getResources().getDimensionPixelSize(R.dimen.stat_icon_size)) / 2;
        InsetDrawable drawable = new InsetDrawable(mNotification
                .getSmallIcon(getContext()), padding);
        setBackground(drawable);
    }

    @Override
    public OpenStatusBarNotification getNotification() {
        return mNotification;
    }

}
