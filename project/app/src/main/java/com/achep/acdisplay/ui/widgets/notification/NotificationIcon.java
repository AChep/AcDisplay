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
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.achep.acdisplay.interfaces.INotificatiable;
import com.achep.acdisplay.notifications.OpenNotification;

/**
 * Created by Artem on 25.03.2014.
 */
public class NotificationIcon extends ImageView implements
        OpenNotification.OnNotificationDataChangedListener,
        INotificatiable {

    /* Compat for Jelly Bean */
    private boolean mAttachedToWindow;

    @Nullable
    private OpenNotification mNotification;

    private int mIconAlpha;
    private boolean mIndicateReadState = true;

    public NotificationIcon(Context context) {
        super(context);
    }

    public NotificationIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NotificationIcon(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void registerListenerAndUpdateAll() {
        if (mNotification == null) {
            // TODO: What to do if the notification is null?
            return;
        }

        handleIconChanged();
        handleReadStateChanged();
        mNotification.registerListener(this);
    }

    private void unregisterListener() {
        if (mNotification == null) return;

        mNotification.unregisterListener(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttachedToWindow = true;
        registerListenerAndUpdateAll();
    }

    @Override
    protected void onDetachedFromWindow() {
        mAttachedToWindow = false;
        unregisterListener();
        super.onDetachedFromWindow();
    }

    public void setNotificationIndicateReadStateEnabled(boolean enabled) {
        mIndicateReadState = enabled;
        if (mNotification != null) handleReadStateChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNotification(@Nullable OpenNotification notification) {
        if (mAttachedToWindow) {
            unregisterListener();
            mNotification = notification;
            registerListenerAndUpdateAll();
        } else mNotification = notification;
    }

    private void handleIconChanged() {
        assert mNotification != null;
        Bitmap icon = mNotification.getIcon();
        setImageBitmap(icon);
        setImageAlpha(mIconAlpha);
    }

    /**
     * Updates icon's alpha level to indicate is notification read or not.
     */
    private void handleReadStateChanged() {
        assert mNotification != null;
        // TODO: Move the alpha levels up to resources.
        mIconAlpha = mNotification.isRead() && mIndicateReadState ? 120 : 255;
        setImageAlpha(mIconAlpha);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNotificationDataChanged(@NonNull OpenNotification notification, int event) {
        switch (event) {
            case OpenNotification.EVENT_ICON:
                handleIconChanged();
                break;
            case OpenNotification.EVENT_READ:
                handleReadStateChanged();
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public OpenNotification getNotification() {
        return mNotification;
    }

}
