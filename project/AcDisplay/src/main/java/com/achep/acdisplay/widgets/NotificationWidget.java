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

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.achep.acdisplay.Device;
import com.achep.acdisplay.R;
import com.achep.acdisplay.notifications.NotificationData;
import com.achep.acdisplay.notifications.NotificationUtils;
import com.achep.acdisplay.notifications.OpenNotification;
import com.achep.acdisplay.utils.ViewUtils;

/**
 * Simple notification widget that shows the title of notification,
 * its message, icon, actions and more.
 *
 * @author Artem Chepurnoy
 */
public class NotificationWidget extends RelativeLayout implements NotificationView {

    private NotificationIcon mIcon;
    private TextView mTitleTextView;
    private TextView mMessageTextView;
    private TextView mWhenTextView;
    private TextView mSubtextTextView;
    private LinearLayout mActionsContainer;

    private OnClickListener mOnClickListener;
    private OpenNotification mNotification;
    private ViewGroup mContent;

    private int mActionContainerAlignment = ALIGN_TOP;

    /**
     * Interface definition for a callback to be invoked
     * when a notification's views are clicked.
     */
    public interface OnClickListener extends View.OnClickListener {

        /**
         * Called on content view click.
         *
         * @param v clicked view
         * @see NotificationWidget#getNotification()
         */
        @Override
        public void onClick(View v);

        /**
         * Called on action button click.
         *
         * @param v      clicked view
         * @param intent action's intent
         */
        void onActionButtonClick(View v, PendingIntent intent);
    }

    public NotificationWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NotificationWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Register a callback to be invoked when notification views are clicked.
     * If some of them are not clickable, they becomes clickable.
     */
    public void setOnClickListener(OnClickListener l) {
        mContent.setOnClickListener(l);
        mOnClickListener = l;
    }

    /**
     * Sets an alignment of action buttons.
     *
     * @param alignment may be {@link #ALIGN_TOP} or {@link #ALIGN_BOTTOM}
     */
    public void setActionButtonsAlignment(int alignment) {
        if (alignment != ALIGN_BOTTOM && alignment != ALIGN_TOP) {
            throw new IllegalArgumentException("It may be ALIGN_BOTTOM or ALIGN_TOP only!");
        }

        mActionContainerAlignment = alignment;

        if (mContent != null && mActionsContainer != null) {
            updateActionButtonsAlignment();
        }
    }

    private void updateActionButtonsAlignment() {
        RelativeLayout.LayoutParams lp = (LayoutParams) mActionsContainer.getLayoutParams();
        lp.removeRule(RelativeLayout.ABOVE);
        lp.removeRule(RelativeLayout.BELOW);
        if (mActionContainerAlignment == ALIGN_BOTTOM)
            lp.addRule(RelativeLayout.BELOW, mContent.getId());
        mActionsContainer.setLayoutParams(lp);

        lp = (LayoutParams) mContent.getLayoutParams();
        lp.removeRule(RelativeLayout.ABOVE);
        lp.removeRule(RelativeLayout.BELOW);
        if (mActionContainerAlignment == ALIGN_TOP)
            lp.addRule(RelativeLayout.BELOW, mActionsContainer.getId());
        mContent.setLayoutParams(lp);
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

        mIcon.setNotificationIndicateReadStateEnabled(false);
        updateActionButtonsAlignment();
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
    public void setNotification(OpenNotification osbn) {
        mNotification = osbn;
        if (osbn == null) {
            // TODO: Hide everything or show a notice to user.
            return;
        }

        StatusBarNotification sbn = osbn.getStatusBarNotification();
        NotificationData data = osbn.getNotificationData();

        CharSequence message = data.getLargeMessage();
        CharSequence subText = data.infoText == null ? data.subText : data.infoText;
        CharSequence whenText = DateUtils.formatDateTime(getContext(),
                sbn.getPostTime(), DateUtils.FORMAT_SHOW_TIME);

        // If message is empty hide the view to free space
        // taken by margins.
        if (!TextUtils.isEmpty(message)) {
            mMessageTextView.setText(message);
            mMessageTextView.setVisibility(VISIBLE);
        } else {
            mMessageTextView.setVisibility(GONE);
        }

        mTitleTextView.setText(data.titleText);
        mSubtextTextView.setText(subText);
        mWhenTextView.setText(whenText);

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

        if (Device.hasKitKatApi()) {
            updateActionButtons(osbn);
        }
    }

    /**
     * Updates {@link #mActionsContainer actions container} with actions
     * from given notification. Actually needs {@link android.os.Build.VERSION_CODES#KITKAT KitKat}
     * or higher Android version.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void updateActionButtons(OpenNotification osbn) {
        StatusBarNotification sbn = osbn.getStatusBarNotification();
        Notification.Action[] actions = sbn.getNotification().actions;

        ViewUtils.setVisible(mActionsContainer, actions != null);
        if (actions != null) {
            int actionCount = actions.length;

            View[] rootViews = new View[actionCount];
            TextView[] actionViews = new TextView[actionCount];

            // Find available views.
            int length = Math.min(mActionsContainer.getChildCount(), actionCount);
            for (int i = 0; i < length; i++) {
                rootViews[i] = mActionsContainer.getChildAt(i);
                actionViews[i] = (TextView) rootViews[i].findViewById(R.id.title);
            }

            // Remove redundant views.
            for (int i = mActionsContainer.getChildCount() - 1; i >= length; i--) {
                mActionsContainer.removeViewAt(i);
            }

            // Setup every view's content and inflate missing items.
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            for (int i = 0; i < actionCount; i++) {
                final Notification.Action action = actions[i];

                View root = rootViews[i];
                TextView actionTextView = actionViews[i];
                if (actionTextView == null) {
                    // Create new view.
                    root = inflater.inflate(
                            R.layout.widget_notification_action,
                            mActionsContainer, false);
                    mActionsContainer.addView(root);

                    actionTextView = (TextView) root.findViewById(R.id.title);
                }

                // Setup content.
                Drawable icon = NotificationUtils.getDrawable(getContext(), sbn, action.icon);
                actionTextView.setText(action.title);
                actionTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);

                // Transfer click.
                root.setOnClickListener(new View.OnClickListener() {
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
}
