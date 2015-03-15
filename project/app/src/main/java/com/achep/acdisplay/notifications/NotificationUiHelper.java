/*
 * Copyright (C) 2015 AChep@xda <artemchep@gmail.com>
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
package com.achep.acdisplay.notifications;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.base.Device;
import com.achep.base.tests.Check;
import com.achep.base.utils.CsUtils;
import com.achep.base.utils.NullUtils;
import com.achep.base.utils.Operator;
import com.achep.base.utils.ViewUtils;

import java.lang.ref.SoftReference;
import java.util.Arrays;

/**
 * Created by Artem Chepurnoy on 11.02.2015.
 */
public class NotificationUiHelper {

    private static final int PENDING_UPDATE_TITLE = 1;
    private static final int PENDING_UPDATE_SUBTITLE = 1 << 1;
    private static final int PENDING_UPDATE_TIMESTAMP = 1 << 2;
    private static final int PENDING_UPDATE_MESSAGE = 1 << 3;
    private static final int PENDING_UPDATE_ACTIONS = 1 << 4;
    private static final int PENDING_UPDATE_LARGE_ICON = 1 << 5;

    private static SoftReference<CharSequence[]> sSecureContentLabelRef;

    protected OpenNotification mOpenNotification;
    protected final Data mData;
    protected final Context mContext;
    protected final OpenNotification.OnNotificationDataChangedListener mListener =
            new OpenNotification.OnNotificationDataChangedListener() {
                @Override
                public void onNotificationDataChanged(
                        @NonNull OpenNotification notification, int event) {
                    Check.getInstance().isTrue(notification == mOpenNotification);
                    switch (event) {
                        case OpenNotification.EVENT_ICON:
                            updateLargeIcon();
                            break;
                    }
                }
            };

    private boolean mResumed;
    private int mPendingUpdates;

    public interface OnActionClick {

        void onActionClick(@NonNull View view, @NonNull Action action);

    }

    public NotificationUiHelper(@NonNull Context context, @NonNull Data data) {
        mContext = context;
        mData = data;
        mData.setHost(this);
    }

    public void setNotification(@Nullable OpenNotification notification) {
        unregisterNotificationListener();
        mOpenNotification = notification;
        registerNotificationListener();

        updateTitle();
        updateSubtitle();
        updateTimestamp();
        updateMessage();
        updateActions();
        updateLargeIcon();
    }

    public void resume() {
        mResumed = true;

        if (Operator.bitAnd(mPendingUpdates, PENDING_UPDATE_TITLE)) updateTitle();
        if (Operator.bitAnd(mPendingUpdates, PENDING_UPDATE_SUBTITLE)) updateSubtitle();
        if (Operator.bitAnd(mPendingUpdates, PENDING_UPDATE_TIMESTAMP)) updateTimestamp();
        if (Operator.bitAnd(mPendingUpdates, PENDING_UPDATE_MESSAGE)) updateMessage();
        if (Operator.bitAnd(mPendingUpdates, PENDING_UPDATE_ACTIONS)) updateActions();
        if (Operator.bitAnd(mPendingUpdates, PENDING_UPDATE_LARGE_ICON)) updateLargeIcon();
        mPendingUpdates = 0;

        registerNotificationListener();
    }

    public void pause() {
        unregisterNotificationListener();
        mResumed = false;
    }

    private void registerNotificationListener() {
        if (mOpenNotification != null) mOpenNotification.registerListener(mListener);
    }

    private void unregisterNotificationListener() {
        if (mOpenNotification != null) mOpenNotification.unregisterListener(mListener);
    }

    protected boolean isSecret(int minVisibility, int privacyMask) {
        return NotificationUtils.isSecret(mContext, mOpenNotification, minVisibility, privacyMask);
    }

    //-- LARGE ICON -----------------------------------------------------------

    protected void updateLargeIcon() {
        if (!mResumed) {
            mPendingUpdates |= PENDING_UPDATE_LARGE_ICON;
            return;
        }
        final ImageView largeIconImageView = mData.largeIconImageView;
        if (largeIconImageView == null) return;
        if (mOpenNotification == null) {
            largeIconImageView.setImageDrawable(null);
            return;
        }

        final boolean secret = isLargeIconSecret();

        if (secret) {
            Drawable drawable = getAppIcon();
            if (drawable != null) {
                largeIconImageView.setImageDrawable(drawable);
                largeIconImageView.setVisibility(View.VISIBLE);
                return;
            }
        }

        Bitmap bitmap = (Bitmap) NullUtils.whileNotNull(
                secret ? null : mOpenNotification.getNotification().largeIcon,
                mOpenNotification.getIcon());
        largeIconImageView.setImageBitmap(bitmap);
        ViewUtils.setVisible(largeIconImageView, bitmap != null);
    }

    protected final boolean isLargeIconSecret() {
        return isSecret(OpenNotification.VISIBILITY_SECRET, Config.PRIVACY_HIDE_CONTENT_MASK);
    }

    @Nullable
    private Drawable getAppIcon() {
        PackageManager pm = mContext.getPackageManager();
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(mOpenNotification.getPackageName(), 0);
            return pm.getApplicationIcon(appInfo);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    //-- TITLE ----------------------------------------------------------------

    protected void updateTitle() {
        if (!mResumed) {
            mPendingUpdates |= PENDING_UPDATE_TITLE;
            return;
        }
        final TextView titleTextView = mData.titleTextView;
        if (titleTextView == null) return;
        if (mOpenNotification == null) {
            titleTextView.setText("");
            titleTextView.setVisibility(View.INVISIBLE);
            return;
        }

        final boolean secret = isTitleSecret();

        CharSequence title;
        if (secret) {
            CharSequence appLabel = getAppLabel();
            title = appLabel != null ? appLabel : "Failed to get the label of application!";
        } else if (mData.big) {
            title = NullUtils.whileNotNull(
                    mOpenNotification.titleBigText,
                    mOpenNotification.titleText
            );
        } else {
            title = mOpenNotification.titleText;
        }

        titleTextView.setText(title);
        titleTextView.setVisibility(View.VISIBLE);
    }

    protected final boolean isTitleSecret() {
        return isSecret(OpenNotification.VISIBILITY_SECRET, Config.PRIVACY_HIDE_CONTENT_MASK);
    }

    @Nullable
    private CharSequence getAppLabel() {
        PackageManager pm = mContext.getPackageManager();
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(mOpenNotification.getPackageName(), 0);
            return pm.getApplicationLabel(appInfo);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    //-- TIMESTAMP ------------------------------------------------------------

    protected void updateTimestamp() {
        if (!mResumed) {
            mPendingUpdates |= PENDING_UPDATE_TIMESTAMP;
            return;
        }
        final TextView timestampTextView = mData.timestampTextView;
        if (timestampTextView == null) return;
        if (mOpenNotification == null) {
            timestampTextView.setText("");
            timestampTextView.setVisibility(View.INVISIBLE);
            return;
        }

        final long when = mOpenNotification.getNotification().when;
        CharSequence cs = DateUtils.formatDateTime(mContext, when, DateUtils.FORMAT_SHOW_TIME);
        timestampTextView.setText(cs);
        timestampTextView.setVisibility(View.VISIBLE);
    }

    //-- SUBTITLE -------------------------------------------------------------

    protected void updateSubtitle() {
        if (!mResumed) {
            mPendingUpdates |= PENDING_UPDATE_SUBTITLE;
            return;
        }
        final CharSequence subtitleText;
        final TextView subtitleTextView = mData.subtitleTextView;
        if (subtitleTextView == null) return;
        if (mOpenNotification == null || TextUtils.isEmpty(subtitleText =
                CsUtils.join(" ", mOpenNotification.subText, mOpenNotification.infoText))) {
            subtitleTextView.setText("");
            subtitleTextView.setVisibility(View.INVISIBLE);
            return;
        }

        subtitleTextView.setText(subtitleText);
        subtitleTextView.setVisibility(View.VISIBLE);
    }

    //-- MESSAGE --------------------------------------------------------------

    protected void updateMessage() {
        if (!mResumed) {
            mPendingUpdates |= PENDING_UPDATE_MESSAGE;
            return;
        }
        if (mData.messageContainer == null) return;
        if (mOpenNotification == null) {
            rebuildMessageViews((CharSequence[]) null);
            return;
        }

        final boolean secret = isMessageSecret();

        // Get message text
        if (secret) {
            CharSequence[] messages;
            if (sSecureContentLabelRef == null || (messages = sSecureContentLabelRef.get()) == null) {
                final CharSequence cs = mContext.getString(R.string.privacy_mode_hidden_content);
                final SpannableString ss = new SpannableString(cs);
                ss.setSpan(new StyleSpan(Typeface.ITALIC),
                        0 /* start */, ss.length() /* end */,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                messages = new CharSequence[]{ss};
                sSecureContentLabelRef = new SoftReference<>(messages);
            }

            rebuildMessageViews(messages);
        } else {
            CharSequence message;
            if (mData.big) {
                if (mOpenNotification.messageTextLines != null) {
                    rebuildMessageViews(mOpenNotification.messageTextLines);
                    return;
                }

                message = NullUtils.whileNotNull(
                        mOpenNotification.messageBigText,
                        mOpenNotification.messageText
                );
            } else {
                message = mOpenNotification.messageText;
            }

            rebuildMessageViews(message);
        }

    }

    protected final boolean isMessageSecret() {
        return isSecret(OpenNotification.VISIBILITY_PRIVATE, Config.PRIVACY_HIDE_CONTENT_MASK);
    }

    private void rebuildMessageViews(@Nullable CharSequence message) {
        rebuildMessageViews(message == null ? null : new CharSequence[]{message});
    }

    /**
     * @param messages an array of non-empty messages.
     */
    private void rebuildMessageViews(final @Nullable CharSequence[] messages) {
        final ViewGroup container = mData.messageContainer;
        Check.getInstance().isNonNull(container);
        assert container != null;

        if (messages == null) {
            // Free messages' container.
            container.removeAllViews();
            return;
        }

        final int length = messages.length;
        int freeLines = mData.messageMaxLines;
        final int viewCount = Math.min(length, freeLines);
        final int[] viewMaxLines = new int[length];
        if (freeLines > length) { // We can reserve more than one line per message

            // Initial setup.
            Arrays.fill(viewMaxLines, 1);
            freeLines -= length;

            // Build list of lengths, so we don't have
            // to recalculate it every time.
            int[] msgLengths = new int[length];
            for (int i = 0; i < length; i++) {
                assert messages[i] != null;
                msgLengths[i] = messages[i].length();
            }

            while (freeLines > 0) {
                int pos = 0;
                float a = 0;
                for (int i = 0; i < length; i++) {
                    final float k = (float) msgLengths[i] / viewMaxLines[i];
                    if (k > a) {
                        a = k;
                        pos = i;
                    }
                }
                viewMaxLines[pos]++;
                freeLines--;
            }
        } else {
            // Show first messages.
            for (int i = 0; freeLines > 0; freeLines--, i++) {
                viewMaxLines[i] = 1;
            }
        }

        View[] views = new View[viewCount];

        // Find available views.
        int childCount = container.getChildCount();
        for (int i = Math.min(childCount, viewCount) - 1; i >= 0; i--) {
            views[i] = container.getChildAt(i);
        }

        // Remove redundant views.
        for (int i = childCount - 1; i >= viewCount; i--) {
            container.removeViewAt(i);
        }

        boolean highlightFirstLetter = mData.messageItemUnderlineFirstLetter && viewCount > 1;

        LayoutInflater inflater = null;
        for (int i = 0; i < viewCount; i++) {
            View root = views[i];

            if (root == null) {
                // Initialize layout inflater only when we really need it.
                if (inflater == null) {
                    inflater = (LayoutInflater) mContext
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    assert inflater != null;
                }

                root = inflater.inflate(
                        mData.messageItemLayoutRes,
                        container, false);
                // FIXME: ?
                // We need to keep all IDs unique to make
                // TransitionManager#beginDelayedTransition(ViewGroup)
                // work correctly!
                root.setId(container.getChildCount() + 1);
                container.addView(root);
            }

            Check.getInstance().isTrue(messages[i].length() != 0);

            final CharSequence text;
            final char char_ = messages[i].charAt(0);
            if (highlightFirstLetter && (Character.isLetter(char_) || Character.isDigit(char_))) {
                SpannableString spannable = new SpannableString(messages[i]);
                spannable.setSpan(new UnderlineSpan(), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                text = spannable;
            } else {
                text = messages[i];
            }

            // Get message view and apply the content.
            TextView textView = root instanceof TextView
                    ? (TextView) root
                    : (TextView) root.findViewById(android.R.id.message);
            textView.setMaxLines(viewMaxLines[i]);
            textView.setText(text);
        }
    }

    //-- ACTIONS --------------------------------------------------------------

    protected void updateActions() {
        if (!mResumed) {
            mPendingUpdates |= PENDING_UPDATE_ACTIONS;
            return;
        }
        if (mData.actionContainer == null) return;
        if (mOpenNotification == null) {
            rebuildActionViews(null);
            return;
        }

        final boolean secret = isActionsSecret();

        rebuildActionViews(secret || !mData.big ? null : mOpenNotification.getActions());
    }

    protected final boolean isActionsSecret() {
        return isSecret(OpenNotification.VISIBILITY_PRIVATE, Config.PRIVACY_HIDE_ACTIONS_MASK);
    }

    @SuppressLint("NewApi")
    private void rebuildActionViews(@Nullable Action[] actions) {
        final ViewGroup container = mData.actionContainer;
        Check.getInstance().isNonNull(container);
        assert container != null;

        if (actions == null) {
            // Free actions' container.
            container.removeAllViews();
            return;
        }

        int count = actions.length;
        View[] views = new View[count];

        // Find available views.
        int childCount = container.getChildCount();
        int a = Math.min(childCount, count);
        for (int i = 0; i < a; i++) {
            views[i] = container.getChildAt(i);
        }

        // Remove redundant views.
        for (int i = childCount - 1; i >= count; i--) {
            container.removeViewAt(i);
        }

        LayoutInflater inflater = null;
        for (int i = 0; i < count; i++) {
            final Action action = actions[i];
            View root = views[i];

            if (root == null) {
                // Initialize layout inflater only when we really need it.
                if (inflater == null) {
                    inflater = (LayoutInflater) mContext
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    assert inflater != null;
                }

                root = inflater.inflate(
                        mData.actionItemLayoutRes,
                        container, false);
                root = initActionView(root);
                // We need to keep all IDs unique to make
                // TransitionManager.beginDelayedTransition(viewGroup, null)
                // work correctly!
                root.setId(container.getChildCount() + 1);
                container.addView(root);
            }

            if (action.intent != null) {
                root.setEnabled(true);
                root.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        assert mData.actionClickCallback != null;
                        mData.actionClickCallback.onActionClick(v, action);
                    }
                });
            } else {
                root.setEnabled(false);
                root.setOnClickListener(null);
            }

            // Get message view and apply the content.
            TextView textView = root instanceof TextView
                    ? (TextView) root
                    : (TextView) root.findViewById(android.R.id.title);
            textView.setText(action.title);

            Drawable icon = null;
            if (mData.actionItemIconShown) {
                icon = NotificationUtils.getDrawable(mContext, mOpenNotification, action.icon);
                if (icon != null) icon = initActionIcon(icon);
            }

            if (Device.hasJellyBeanMR1Api()) {
                textView.setCompoundDrawablesRelative(icon, null, null, null);
            } else {
                textView.setCompoundDrawables(icon, null, null, null);
            }
        }
    }

    @NonNull
    protected View initActionView(@NonNull View view) {
        return view;
    }

    @NonNull
    protected Drawable initActionIcon(@NonNull Drawable icon) {
        Resources res = mContext.getResources();
        int size = res.getDimensionPixelSize(R.dimen.notification_action_icon_size);
        icon = icon.mutate();
        icon.setBounds(0, 0, size, size);

        // The matrix is stored in a single array, and its treated as follows:
        // [ a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t ]
        // When applied to a color [r, g, b, a], the resulting color is computed as (after clamping)
        //   R' = a*R + b*G + c*B + d*A + e;
        //   G' = f*R + g*G + h*B + i*A + j;
        //   B' = k*R + l*G + m*B + n*A + o;
        //   A' = p*R + q*G + r*B + s*A + t;
        ColorFilter colorFilter = new ColorMatrixColorFilter(new float[]{
                0, 0, 0, 0, 255, // Red
                0, 0, 0, 0, 255, // Green
                0, 0, 0, 0, 255, // Blue
                0, 0, 0, 1, 0 //    Alpha
        });
        icon.setColorFilter(colorFilter); // force white color
        return icon;
    }

    //-- MESSAGE --------------------------------------------------------------

    public static class Data {

        // Title
        @Nullable
        public TextView titleTextView;

        // Subtitle
        @Nullable
        public TextView subtitleTextView;

        // Message
        @Nullable
        public ViewGroup messageContainer;
        @LayoutRes
        public int messageItemLayoutRes;
        public int messageMaxLines;
        public boolean messageItemUnderlineFirstLetter;

        // Actions
        @Nullable
        public ViewGroup actionContainer;
        @Nullable
        public OnActionClick actionClickCallback;
        @LayoutRes
        public int actionItemLayoutRes;
        public boolean actionItemIconShown;

        // Timestamp
        @Nullable
        public TextView timestampTextView;

        // Large icon
        @Nullable
        public ImageView largeIconImageView;

        // Other
        public boolean big;

        @NonNull
        private NotificationUiHelper mHost;

        public Data(@Nullable TextView titleTextView,
                    @Nullable TextView subtitleTextView,
                    @Nullable TextView timestampTextView,
                    @Nullable ViewGroup messageContainer, @LayoutRes int messageItemLayoutRes,
                    @Nullable ViewGroup actionContainer, @LayoutRes int actionItemLayoutRes,
                    @Nullable OnActionClick actionClickCallback,
                    @Nullable ImageView largeIconImageView) {
            this.titleTextView = titleTextView;
            this.subtitleTextView = subtitleTextView;
            this.timestampTextView = timestampTextView;
            this.messageContainer = messageContainer;
            this.messageItemLayoutRes = messageItemLayoutRes;
            this.actionContainer = actionContainer;
            this.actionClickCallback = actionClickCallback;
            this.actionItemLayoutRes = actionItemLayoutRes;
            this.largeIconImageView = largeIconImageView;
        }

        void setHost(@NonNull NotificationUiHelper host) {
            mHost = host;
        }

        /**
         *
         */
        // TODO: Update selectively
        public void notifyDataChanged() {
            mHost.updateTitle();
            mHost.updateSubtitle();
            mHost.updateTimestamp();
            mHost.updateMessage();
            mHost.updateActions();
            mHost.updateLargeIcon();
        }

        public static class Builder {

            public Builder() { /* empty */ }

            public Builder(@NonNull Data data) {
                mTitleTextView = data.titleTextView;
                mSubtitleTextView = data.subtitleTextView;
                mTimestampTextView = data.timestampTextView;
                mMessageContainer = data.messageContainer;
                mMessageItemLayoutRes = data.messageItemLayoutRes;
                mMessageMaxLines = data.messageMaxLines;
                mMessageItemUnderlineFirstLetter = data.messageItemUnderlineFirstLetter;
                mActionContainer = data.actionContainer;
                mActionClickCallback = data.actionClickCallback;
                mActionItemIconShown = data.actionItemIconShown;
                mActionItemLayoutRes = data.actionItemLayoutRes;
                mLargeIconImageView = data.largeIconImageView;
                mBig = data.big;
            }

            //-- TITLE ----------------------------------------------------------------

            @Nullable
            private TextView mTitleTextView;

            @NonNull
            public Builder setTitleView(@Nullable TextView titleView) {
                mTitleTextView = titleView;
                return this;
            }

            //-- SUBTITLE -------------------------------------------------------------

            @Nullable
            private TextView mSubtitleTextView;

            @NonNull
            public Builder setSubtitleView(@Nullable TextView subtitleView) {
                mSubtitleTextView = subtitleView;
                return this;
            }

            //-- TIMESTAMP ------------------------------------------------------------

            @Nullable
            private TextView mTimestampTextView;

            @NonNull
            public Builder setTimestampView(@Nullable TextView timestampView) {
                mTimestampTextView = timestampView;
                return this;
            }

            //-- MESSAGE --------------------------------------------------------------

            @Nullable
            private ViewGroup mMessageContainer;
            @LayoutRes
            private int mMessageItemLayoutRes;
            private int mMessageMaxLines;
            private boolean mMessageItemUnderlineFirstLetter;

            @NonNull
            public Builder setMessageContainer(@Nullable ViewGroup container) {
                mMessageContainer = container;
                return this;
            }

            @NonNull
            public Builder setMessageItemLayoutRes(@LayoutRes int layoutRes) {
                mMessageItemLayoutRes = layoutRes;
                return this;
            }

            @NonNull
            public Builder setMessageMaxLines(int maxLines) {
                mMessageMaxLines = maxLines;
                return this;
            }

            @NonNull
            public Builder setMessageItemUnderlineFirstLetter(boolean underlineFirstLetter) {
                mMessageItemUnderlineFirstLetter = underlineFirstLetter;
                return this;
            }

            //-- ACTIONS --------------------------------------------------------------

            @Nullable
            private ViewGroup mActionContainer;
            @Nullable
            private OnActionClick mActionClickCallback;
            @LayoutRes
            private int mActionItemLayoutRes;
            private boolean mActionItemIconShown = true;

            @NonNull
            public Builder setActionContainer(@Nullable ViewGroup container) {
                mActionContainer = container;
                return this;
            }

            @NonNull
            public Builder setActionClickCallback(@Nullable OnActionClick clickCallback) {
                mActionClickCallback = clickCallback;
                return this;
            }

            @NonNull
            public Builder setActionItemLayoutRes(@LayoutRes int layoutRes) {
                mActionItemLayoutRes = layoutRes;
                return this;
            }

            @NonNull
            public Builder setActionItemIconShown(boolean isShown) {
                mActionItemIconShown = isShown;
                return this;
            }

            //-- LARGE ICON -----------------------------------------------------------

            @Nullable
            private ImageView mLargeIconImageView;

            @NonNull
            public Builder setLargeIconView(@Nullable ImageView largeIconView) {
                mLargeIconImageView = largeIconView;
                return this;
            }

            //-- OTHER ----------------------------------------------------------------

            private boolean mBig;

            @NonNull
            public Builder setBig(boolean isBig) {
                mBig = isBig;
                return this;
            }

            @NonNull
            public Data build() {
                Check.getInstance().isTrue(mMessageContainer == null || mMessageItemLayoutRes != 0);
                Check.getInstance().isTrue(mActionContainer == null || mActionItemLayoutRes != 0);

                Data md = new Data(
                        mTitleTextView,
                        mSubtitleTextView,
                        mTimestampTextView,
                        mMessageContainer, mMessageItemLayoutRes,
                        mActionContainer, mActionItemLayoutRes, mActionClickCallback,
                        mLargeIconImageView);
                md.messageMaxLines = mMessageMaxLines;
                md.messageItemUnderlineFirstLetter = mMessageItemUnderlineFirstLetter;
                md.actionItemIconShown = mActionItemIconShown;
                md.big = mBig;
                return md;
            }
        }
    }
}