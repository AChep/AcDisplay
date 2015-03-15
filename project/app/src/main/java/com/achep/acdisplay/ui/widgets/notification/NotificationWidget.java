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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.achep.acdisplay.R;
import com.achep.acdisplay.interfaces.INotificatiable;
import com.achep.acdisplay.notifications.NotificationUiHelper;
import com.achep.acdisplay.notifications.OpenNotification;

/**
 * Simple notification widget that shows the title of notification,
 * its message, icon, actions and more.
 *
 * @author Artem Chepurnoy
 */
public class NotificationWidget extends LinearLayout implements
        INotificatiable, NotificationUiHelper.OnNotificationContentChanged {

    private NotificationUiHelper mHelper;

    // Views
    private ViewGroup mContent;
    private ImageView mLargeIcon;
    private TextView mWhenTextView;
    private TextView mTitleTextView;
    private TextView mSubtitleTextView;
    private NotificationMessages mMessageContainer;
    private NotificationActions mActionsContainer;

    private Callback mCallback;

    public interface Callback extends NotificationActions.Callback {

        /**
         * Called on content view click.
         *
         * @param v clicked view
         * @see NotificationWidget#getNotification()
         */
        void onContentClick(@NonNull NotificationWidget widget, @NonNull View v);

    }

    public NotificationWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mHelper = new NotificationUiHelper(context, this);
        mHelper.setBig(true);
    }

    /**
     * Register a callback to be invoked when notification views are clicked.
     */
    public void setCallback(@Nullable Callback callback) {
        mCallback = callback;

        // Set the callback
        mActionsContainer.setCallback(callback);
        mContent.setOnClickListener(callback == null
                ? null
                : new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallback != null) {
                    NotificationWidget widget = NotificationWidget.this;
                    mCallback.onContentClick(widget, v);
                }
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mContent = (ViewGroup) findViewById(R.id.content);
        mLargeIcon = (ImageView) findViewById(R.id.icon);
        mWhenTextView = (TextView) findViewById(R.id.when);
        mTitleTextView = (TextView) findViewById(R.id.title);
        mSubtitleTextView = (TextView) findViewById(R.id.subtext);
        mMessageContainer = (NotificationMessages) findViewById(R.id.message_container);
        mActionsContainer = (NotificationActions) findViewById(R.id.actions);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mHelper.resume();
    }

    @Override
    protected void onDetachedFromWindow() {
        mHelper.pause();
        super.onDetachedFromWindow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNotificationContentChanged(@NonNull NotificationUiHelper helper, int event) {
        switch (event) {
            case NotificationUiHelper.EVENT_LARGE_ICON_CHANGED:
                mLargeIcon.setImageBitmap(helper.getLargeIcon());
                break;
            case NotificationUiHelper.EVENT_TIMESTAMP_CHANGED:
                mWhenTextView.setText(helper.getTimestamp());
                break;
            case NotificationUiHelper.EVENT_TITLE_CHANGED:
                mTitleTextView.setText(helper.getTitle());
                break;
            case NotificationUiHelper.EVENT_SUBTITLE_CHANGED:
                mSubtitleTextView.setText(helper.getSubtitle());
                break;
            case NotificationUiHelper.EVENT_MESSAGE_CHANGED:
                mMessageContainer.setMessages(helper.getMessages());
                break;
            case NotificationUiHelper.EVENT_ACTIONS_CHANGED:
                mActionsContainer.setActions(helper.getNotification(), helper.getActions());
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenNotification getNotification() {
        return mHelper.getNotification();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNotification(OpenNotification n) {
        mHelper.setNotification(n);
    }

}
