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

import android.app.PendingIntent;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.achep.acdisplay.R;
import com.achep.acdisplay.interfaces.INotificatiable;
import com.achep.acdisplay.notifications.Action;
import com.achep.acdisplay.notifications.NotificationUiHelper;
import com.achep.acdisplay.notifications.OpenNotification;

/**
 * Simple notification widget that shows the title of notification,
 * its message, icon, actions and more.
 *
 * @author Artem Chepurnoy
 */
public class NotificationWidget extends LinearLayout implements INotificatiable {

    private final int mMessageLayoutRes;
    private final int mMessageMaxLines;
    private final int mActionLayoutRes;
    private final boolean mActionAddIcon;

    private ImageView mIcon;
    private TextView mTitleTextView;
    private TextView mWhenTextView;
    private TextView mSubtextTextView;
    private ViewGroup mMessageContainer;
    private ViewGroup mActionsContainer;

    private OnClickListener mOnClickListener;
    private OpenNotification mNotification;
    private ViewGroup mContent;

    private NotificationUiHelper mHelper;

    /**
     * Interface definition for a callback to be invoked
     * when a notification's views are clicked.
     */
    public interface OnClickListener {

        /**
         * Called on content view click.
         *
         * @param v clicked view
         * @see NotificationWidget#getNotification()
         */
        void onClick(NotificationWidget widget, View v);

        /**
         * Called on action button click.
         *
         * @param v      clicked view
         * @param intent action's intent
         */
        void onActionButtonClick(NotificationWidget widget, View v, PendingIntent intent);
    }

    public NotificationWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NotificationWidget);
        mActionLayoutRes = a.getResourceId(
                R.styleable.NotificationWidget_actionItemLayout,
                R.layout.notification_action);
        mMessageMaxLines = a.getInt(R.styleable.NotificationWidget_messageMaxLines, 4);
        mMessageLayoutRes = a.getResourceId(
                R.styleable.NotificationWidget_messageItemLayout,
                R.layout.notification_message);
        mActionAddIcon = a.getBoolean(R.styleable.NotificationWidget_actionItemShowIcon, true);
        a.recycle();
    }

    /**
     * Register a callback to be invoked when notification views are clicked.
     * If some of them are not clickable, they becomes clickable.
     */
    public void setOnClickListener(OnClickListener l) {
        View.OnClickListener listener = l == null
                ? null
                : new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnClickListener != null) {
                    NotificationWidget widget = NotificationWidget.this;
                    mOnClickListener.onClick(widget, v);
                }
            }
        };

        mOnClickListener = l;
        mContent.setOnClickListener(listener);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mContent = (ViewGroup) findViewById(R.id.content);
        mIcon = (ImageView) findViewById(R.id.icon);
        mTitleTextView = (TextView) findViewById(R.id.title);
        mMessageContainer = (ViewGroup) findViewById(R.id.message_container);
        mWhenTextView = (TextView) findViewById(R.id.when);
        mSubtextTextView = (TextView) findViewById(R.id.subtext);
        mActionsContainer = (ViewGroup) findViewById(R.id.actions);

        mHelper = onCreateUiHelper();
    }

    @NonNull
    protected NotificationUiHelper onCreateUiHelper() {
        return new NotificationUiHelper(getContext(), onCreateUiHelperData());
    }

    @NonNull
    protected NotificationUiHelper.Data onCreateUiHelperData() {
        return new NotificationUiHelper.Data.Builder()
                .setBig(true)
                .setTitleView(mTitleTextView)
                .setSubtitleView(mSubtextTextView)
                .setTimestampView(mWhenTextView)
                .setMessageContainer(mMessageContainer)
                .setMessageItemLayoutRes(mMessageLayoutRes)
                .setMessageMaxLines(mMessageMaxLines)
                .setMessageItemUnderlineFirstLetter(true)
                .setActionContainer(mActionsContainer)
                .setActionItemLayoutRes(mActionLayoutRes)
                .setActionItemIconShown(mActionAddIcon)
                .setActionClickCallback(new NotificationUiHelper.OnActionClick() {
                    @Override
                    public void onActionClick(@NonNull View view, @NonNull Action action) {
                        if (mOnClickListener != null) {
                            NotificationWidget widget = NotificationWidget.this;
                            mOnClickListener.onActionButtonClick(widget, view, action.intent);
                        }
                    }
                })
                .setLargeIconView(mIcon)
                .build();
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
    public OpenNotification getNotification() {
        return mNotification;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNotification(OpenNotification n) {
        mHelper.setNotification(mNotification = n);
    }

}
