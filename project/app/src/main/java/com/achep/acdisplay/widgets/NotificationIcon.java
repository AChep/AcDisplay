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
import android.util.AttributeSet;
import android.widget.ImageView;

import com.achep.acdisplay.notifications.NotificationData;
import com.achep.acdisplay.notifications.OpenNotification;

/**
 * Created by Artem on 25.03.2014.
 */
public class NotificationIcon extends ImageView implements
        NotificationView,
        NotificationData.OnNotificationDataChangedListener {

    private boolean mAttached;

    private OpenNotification mNotification;
    private int mIconAlpha;

    private boolean mAdjustAlphaEnabled = true;

    public NotificationIcon(Context context) {
        super(context);
        init();
    }

    public NotificationIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NotificationIcon(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @SuppressWarnings("EmptyMethod")
    private void init() {
        /*
        float c[] = new float[]{
                1f, 0f, 0f, // red
                0f, 1f, 0f, // green
                0.2f, 0.2f, 1f, // blue
                1f, 0.2f, 1f, // pink
                0.5f, 1f, 1f, // :)
                1f, 1f, 0f, // orange
        };
        int i = (int) (Math.random() * (c.length / 3 - 1));

        float[] colorMatrix = {
                c[i], 0, 0, 0, 0,
                0, c[i + 1], 0, 0, 0,
                0, 0, c[i + 2], 0, 0,
                0, 0, 0, 1, 0
        };

        ColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);
        setColorFilter(colorFilter);
        */
    }

    private void registerListenerAndUpdateIcon() {
        if (mNotification == null) return;

        NotificationData data = mNotification.getNotificationData();
        handleIconChanged(data.getIcon());
        handleReadStateChanged(data.isRead);
        data.registerListener(this);
    }

    private void unregisterListener() {
        if (mNotification == null) return;

        NotificationData data = mNotification.getNotificationData();
        data.unregisterListener(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttached = true;
        registerListenerAndUpdateIcon();
    }

    @Override
    protected void onDetachedFromWindow() {
        mAttached = false;
        super.onDetachedFromWindow();
        unregisterListener();
    }

    public void setNotificationIndicateReadStateEnabled(boolean enabled) {
        mAdjustAlphaEnabled = enabled;
        handleReadStateChanged(false);
    }

    @Override
    public void setNotification(OpenNotification notification) {
        if (mAttached) {
            unregisterListener();
            mNotification = notification;
            registerListenerAndUpdateIcon();
        } else mNotification = notification;
    }

    private void handleIconChanged(Bitmap icon) {
        setImageBitmap(icon);
        setImageAlpha(mIconAlpha);
    }

    /**
     * Updates icon's alpha level to indicate is notification read or not.
     */
    private void handleReadStateChanged(boolean isRead) {
        mIconAlpha = isRead && mAdjustAlphaEnabled ? 120 : 255;
        setImageAlpha(mIconAlpha);
    }

    @Override
    public void onNotificationDataChanged(NotificationData data, int changeId) {
        switch (changeId) {
            case NotificationData.ICON:
                handleIconChanged(data.getIcon());
                break;
            case NotificationData.READ:
                handleReadStateChanged(data.isRead);
                break;
        }
    }

    @Override
    public OpenNotification getNotification() {
        return mNotification;
    }
}
