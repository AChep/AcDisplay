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

package com.achep.activedisplay.widgets;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.achep.activedisplay.Device;
import com.achep.activedisplay.R;
import com.achep.activedisplay.notifications.NotificationData;
import com.achep.activedisplay.notifications.NotificationUtils;
import com.achep.activedisplay.notifications.OpenStatusBarNotification;
import com.achep.activedisplay.utils.ViewUtils;
import com.achep.activedisplay.view.NotifyingLayout;

/**
 * Created by Artem on 20.03.14.
 */
public class NotificationWidget extends RelativeLayout implements NotificationView {

    private NotifyingLayout mDismissBtnContainer;
    private NotificationIcon mIcon;
    private TextView mTitleTextView;
    private TextView mMessageTextView;
    private TextView mWhenTextView;
    private TextView mSubtextTextView;
    private LinearLayout mActionsContainer;

    private OnClickListener mOnClickListener;
    private OpenStatusBarNotification mNotification;
    private ViewGroup mContent;

    public interface OnClickListener extends View.OnClickListener {
        void onActionButtonClick(View v, PendingIntent intent);
        void onDismissButtonClick(View v, OpenStatusBarNotification osbn);
    }

    public NotificationWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NotificationWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setOnClickListener(OnClickListener l) {
        mContent.setOnClickListener(l);
        mOnClickListener = l;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mContent = (ViewGroup) findViewById(R.id.content);
        mIcon = (NotificationIcon) findViewById(R.id.icon);
        mTitleTextView = (TextView) findViewById(R.id.title);
        mMessageTextView = (TextView) findViewById(R.id.message);
        mWhenTextView = (TextView) findViewById(R.id.when);
        mSubtextTextView = (TextView) findViewById(R.id.subtext);
        mActionsContainer = (LinearLayout) findViewById(R.id.actions);
        mDismissBtnContainer = (NotifyingLayout) findViewById(R.id.dismiss);

        mIcon.setNotificationIndicateReadStateEnabled(false);
        mDismissBtnContainer.setAlpha(isPressed() ? 1f : 0f);
        mDismissBtnContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnClickListener != null) {
                    mOnClickListener.onDismissButtonClick(v, getNotification());
                }
            }
        });
        mDismissBtnContainer.setOnPressStateChangedListener(
                new NotifyingLayout.OnPressStateChangedListener() {
            @Override
            public void onPressStateChanged(NotifyingLayout view, boolean pressed) {
                if (pressed) {
                    view.animate().alpha(1f);
                } else {
                    view.animate().alpha(0f);
                }
            }
        });
    }

    public void setNotification(OpenStatusBarNotification osbn) {
        mNotification = osbn;
        if (osbn == null) {
            // TODO: Hide everything or show a notice to user.
            return;
        }

        StatusBarNotification sbn = osbn.getStatusBarNotification();
        NotificationData data = osbn.getNotificationData();

        ViewUtils.safelySetText(mTitleTextView, data.titleText);
        ViewUtils.safelySetText(mMessageTextView, data.getLargeMessage());
        ViewUtils.safelySetText(mSubtextTextView, data.infoText == null ? data.subText : data.infoText);

        mWhenTextView.setText(DateUtils.formatDateTime(getContext(), sbn.getPostTime(), DateUtils.FORMAT_SHOW_TIME));

        Bitmap bitmap = sbn.getNotification().largeIcon;
        if (bitmap != null) {
            mIcon.setNotification(null);
            mIcon.setImageBitmap(bitmap);
            mIcon.setScaleType(bitmap.getPixel(0, 0) != Color.TRANSPARENT
                    ? ImageView.ScaleType.CENTER_CROP
                    : ImageView.ScaleType.CENTER_INSIDE);
        } else {
            mIcon.setNotification(osbn);
            mIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }

        if (Device.hasKitKatApi()) {
            updateNotificationActions(sbn);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void updateNotificationActions(StatusBarNotification sbn) {
        Notification.Action[] actions = sbn.getNotification().actions;
        ViewUtils.setVisible(mActionsContainer, actions != null);
        if (actions != null) {
            int actionCount = actions.length;

            View[] rootViews = new View[actionCount];
            TextView[] actionViews = new TextView[actionCount];

            int length = Math.min(mActionsContainer.getChildCount(), actionCount);
            for (int i = 0; i < length; i++) {
                rootViews[i] = mActionsContainer.getChildAt(i);
                actionViews[i] = (TextView) rootViews[i].findViewById(R.id.title);
            }
            for (int i = mActionsContainer.getChildCount() - 1; i >= length; i--) {
                mActionsContainer.removeViewAt(i);
            }

            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            for (int i = 0; i < actionCount; i++) {
                final Notification.Action action = actions[i];

                View view = rootViews[i];
                TextView actionView = actionViews[i];
                if (actionView == null) {
                    view = inflater.inflate(
                            R.layout.widget_notification_action,
                            mActionsContainer, false);
                    mActionsContainer.addView(view);

                    actionView = (TextView) view.findViewById(R.id.title);
                }

                actionView.setText(action.title);
                actionView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        NotificationUtils.getDrawable(getContext(), sbn, action.icon),
                        null, null, null);

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mOnClickListener != null) {
                            mOnClickListener.onActionButtonClick(v, action.actionIntent);
                        }
                    }
                });
            }

        }
    }

    @Override
    public OpenStatusBarNotification getNotification() {
        return mNotification;
    }
}
