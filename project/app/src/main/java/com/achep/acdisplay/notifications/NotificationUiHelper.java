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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.acdisplay.interfaces.INotificatiable;
import com.achep.base.tests.Check;
import com.achep.base.utils.CsUtils;
import com.achep.base.utils.NullUtils;
import com.achep.base.utils.Operator;
import com.achep.base.utils.RefCacheBase;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Arrays;

/**
 * Created by Artem Chepurnoy on 11.02.2015.
 */
public class NotificationUiHelper implements INotificatiable {

    // Callback events
    public static final int EVENT_TITLE_CHANGED = 1;
    public static final int EVENT_MESSAGE_CHANGED = 2;
    public static final int EVENT_TIMESTAMP_CHANGED = 3;
    public static final int EVENT_SUBTITLE_CHANGED = 4;
    public static final int EVENT_ACTIONS_CHANGED = 5;
    public static final int EVENT_SMALL_ICON_CHANGED = 6;
    public static final int EVENT_LARGE_ICON_CHANGED = 7;

    // Pending updates
    private static final int PENDING_UPDATE_TITLE = 1;
    private static final int PENDING_UPDATE_SUBTITLE = 1 << 1;
    private static final int PENDING_UPDATE_TIMESTAMP = 1 << 2;
    private static final int PENDING_UPDATE_MESSAGE = 1 << 3;
    private static final int PENDING_UPDATE_ACTIONS = 1 << 4;
    private static final int PENDING_UPDATE_ICONS = 1 << 5;

    private static SoftReference<CharSequence[]> sSecureContentLabelRef;
    static RefCacheBase<Bitmap> sAppIconCache = new RefCacheBase<Bitmap>() {
        @NonNull
        @Override
        protected Reference<Bitmap> onCreateReference(@NonNull Bitmap bitmap) {
            return new WeakReference<>(bitmap);
        }
    };

    private OpenNotification mNotification;
    private CharSequence[] mMessages;
    private CharSequence mTitle;
    private CharSequence mTimestamp;
    private CharSequence mSubtitle;
    private Action[] mActions;
    private Bitmap mSmallIcon;
    private Bitmap mLargeIcon;

    private boolean mBig;

    private final Context mContext;
    private final OnNotificationContentChanged mListener;
    private final OpenNotification.OnNotificationDataChangedListener mNo =
            new OpenNotification.OnNotificationDataChangedListener() {
                @Override
                public void onNotificationDataChanged(
                        @NonNull OpenNotification notification,
                        int event) {
                    Check.getInstance().isInMainThread();
                    switch (event) {
                        case OpenNotification.EVENT_ICON:
                            updateIcons();
                            break;
                    }
                }
            };

    private int mPendingUpdates;
    private boolean mResumed;

    public interface OnNotificationContentChanged {

        /**
         * @param n
         * @param event
         */
        void onNotificationContentChanged(
                @NonNull NotificationUiHelper helper,
                final int event);

    }

    public NotificationUiHelper(
            @NonNull Context context,
            @NonNull OnNotificationContentChanged listener) {
        mContext = context;
        mListener = listener;
    }

    public void resume() {
        Check.getInstance().isInMainThread();
        mResumed = true;

        synchronized (NotificationPresenter.getInstance().monitor) {
            if (Operator.bitAnd(mPendingUpdates, PENDING_UPDATE_TITLE)) updateTitle();
            if (Operator.bitAnd(mPendingUpdates, PENDING_UPDATE_SUBTITLE)) updateSubtitle();
            if (Operator.bitAnd(mPendingUpdates, PENDING_UPDATE_TIMESTAMP)) updateTimestamp();
            if (Operator.bitAnd(mPendingUpdates, PENDING_UPDATE_MESSAGE)) updateMessage();
            if (Operator.bitAnd(mPendingUpdates, PENDING_UPDATE_ACTIONS)) updateActions();
            if (Operator.bitAnd(mPendingUpdates, PENDING_UPDATE_ICONS)) updateIcons();
        }
        mPendingUpdates = 0;

        registerNotificationListener();
    }

    public void pause() {
        Check.getInstance().isInMainThread();
        unregisterNotificationListener();
        mResumed = false;
    }

    /**
     * @param n
     */
    public void setNotification(@Nullable OpenNotification n) {
        Check.getInstance().isInMainThread();
        unregisterNotificationListener();
        mNotification = n;
        registerNotificationListener();

        synchronized (NotificationPresenter.getInstance().monitor) {
            // Update everything
            updateTitle();
            updateTimestamp();
            updateSubtitle();
            updateMessage();
            updateActions();
            updateIcons();
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

    /**
     * Controls the <i>big</i> and <i>small</i> types of the notification
     * view.
     */
    public void setBig(boolean isBig) {
        mBig = isBig;
    }

    private void registerNotificationListener() {
        if (mNotification != null) mNotification.registerListener(mNo);
    }

    private void unregisterNotificationListener() {
        if (mNotification != null) mNotification.unregisterListener(mNo);
    }

    private boolean isSecret(int minVisibility, int privacyMask) {
        return NotificationUtils.isSecret(mContext, mNotification, minVisibility, privacyMask);
    }

    /**
     * @param mask one of the following:
     *             {@link #PENDING_UPDATE_TITLE}, {@link #PENDING_UPDATE_SUBTITLE},
     *             {@link #PENDING_UPDATE_MESSAGE}, {@link #PENDING_UPDATE_ACTIONS},
     *             {@link #PENDING_UPDATE_TIMESTAMP}, {@link #PENDING_UPDATE_ICONS}.
     * @return {@code true} if the update should be canceled, {@code false} otherwise.
     */
    private boolean isPendingUpdate(int mask) {
        if (!mResumed) {
            mPendingUpdates |= mask;
            return true;
        }
        return false;
    }

    //-- ICONS ----------------------------------------------------------------

    private void updateIcons() {
        if (isPendingUpdate(PENDING_UPDATE_ICONS)) return;
        if (mNotification == null) {
            setLargeIcon(null);
            setSmallIcon(null);
            return;
        }

        final boolean secret = isLargeIconSecret();

        Bitmap bitmap;
        if (secret) {
            // Load application's icon as the large icon.

            // Store the bitmaps in soft-reference cache map, to
            // reduce memory usage and improve performance.
            String packageName = mNotification.getPackageName();
            if ((bitmap = sAppIconCache.get(packageName)) == null) {
                Drawable drawable = getAppIcon(mNotification.getPackageName());
                if (drawable != null) {
                    bitmap = Bitmap.createBitmap(
                            drawable.getIntrinsicWidth(),
                            drawable.getIntrinsicHeight(),
                            Bitmap.Config.ARGB_4444);
                    drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                    drawable.draw(new Canvas(bitmap));
                    sAppIconCache.put(packageName, bitmap);
                } else {
                    bitmap = null;
                    sAppIconCache.remove(packageName);
                }
            }
        } else {
            bitmap = mNotification.getNotification().largeIcon;
        }

        if (bitmap == null) {
            setLargeIcon(mNotification.getIcon());
            setSmallIcon(null);
        } else {
            setLargeIcon(bitmap);
            setSmallIcon(mNotification.getIcon());
        }
    }

    /**
     * @return {@code true} if the large icon is a secret and should not be visible to
     * user, {@code false} otherwise.
     */
    protected final boolean isLargeIconSecret() {
        return isSecret(
                OpenNotification.VISIBILITY_SECRET,
                Config.PRIVACY_HIDE_CONTENT_MASK);
    }

    @Nullable
    private Drawable getAppIcon(@NonNull String packageName) {
        PackageManager pm = mContext.getPackageManager();
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationIcon(appInfo);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private void setSmallIcon(@Nullable Bitmap bitmap) {
        if (sameAs(mSmallIcon, bitmap)) {
            // No need to notify listeners about this
            // change.
            return;
        }

        mSmallIcon = bitmap;
        mListener.onNotificationContentChanged(this, EVENT_SMALL_ICON_CHANGED);
    }

    /**
     * @return the notification's small icon to be displayed.
     * @see #getLargeIcon()
     */
    @Nullable
    public Bitmap getSmallIcon() {
        return mSmallIcon;
    }

    private void setLargeIcon(@Nullable Bitmap bitmap) {
        if (sameAs(mLargeIcon, bitmap)) {
            // No need to notify listeners about this
            // change.
            return;
        }

        mLargeIcon = bitmap;
        mListener.onNotificationContentChanged(this, EVENT_LARGE_ICON_CHANGED);
    }

    /**
     * @return the notification's large icon to be displayed.
     * @see #getSmallIcon()
     */
    @Nullable
    public Bitmap getLargeIcon() {
        return mLargeIcon;
    }

    /**
     * @return {@code true} if both {@link Bitmap bitmaps} are {@code null}
     * or if the {@link Bitmap bitmaps} are equal according to
     * {@link android.graphics.Bitmap#sameAs(android.graphics.Bitmap)}, {@code false} otherwise.
     */
    private boolean sameAs(@Nullable Bitmap bitmap, @Nullable Bitmap bitmap2) {
        return bitmap == bitmap2 || bitmap != null && bitmap2 != null && bitmap.sameAs(bitmap2);
    }

    //-- TITLE ----------------------------------------------------------------

    private void updateTitle() {
        if (isPendingUpdate(PENDING_UPDATE_TITLE)) return;
        if (mNotification == null) {
            setTitle(null);
            return;
        }

        final boolean secret = isTitleSecret();

        CharSequence title;
        if (secret) {
            CharSequence appLabel = getAppLabel(mNotification.getPackageName());
            title = appLabel != null ? appLabel : "Hidden app";
        } else if (mBig) {
            title = NullUtils.whileNotNull(
                    mNotification.titleBigText,
                    mNotification.titleText
            );
        } else {
            title = mNotification.titleText;
        }

        setTitle(title);
    }

    /**
     * @return {@code true} if the title is a secret and should not be visible to
     * user, {@code false} otherwise.
     */
    protected final boolean isTitleSecret() {
        return isSecret(
                OpenNotification.VISIBILITY_SECRET,
                Config.PRIVACY_HIDE_CONTENT_MASK);
    }

    @Nullable
    private CharSequence getAppLabel(@NonNull String packageName) {
        PackageManager pm = mContext.getPackageManager();
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(appInfo);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private void setTitle(@Nullable CharSequence title) {
        if (TextUtils.equals(mTitle, title)) {
            // No need to notify listeners about this
            // change.
            return;
        }

        mTitle = title;
        mListener.onNotificationContentChanged(this, EVENT_TITLE_CHANGED);
    }

    /**
     * @return the notification's title to be displayed.
     * @see #getMessages()
     * @see OpenNotification#getVisibility()
     */
    @Nullable
    public CharSequence getTitle() {
        return mTitle;
    }

    //-- TIMESTAMP ------------------------------------------------------------

    private void updateTimestamp() {
        if (isPendingUpdate(PENDING_UPDATE_TIMESTAMP)) return;
        if (mNotification == null) {
            setTimestamp(null);
            return;
        }

        final long when = mNotification.getNotification().when;
        setTimestamp(DateUtils.formatDateTime(mContext, when, DateUtils.FORMAT_SHOW_TIME));
    }

    private void setTimestamp(@Nullable CharSequence timestamp) {
        if (TextUtils.equals(mTimestamp, timestamp)) {
            // No need to notify listeners about this
            // change.
            return;
        }

        mTimestamp = timestamp;
        mListener.onNotificationContentChanged(this, EVENT_TIMESTAMP_CHANGED);
    }

    /**
     * @return the notification's timestamp to be displayed.
     * @see android.app.Notification#when
     */
    @Nullable
    public CharSequence getTimestamp() {
        return mTimestamp;
    }

    //-- SUBTITLE -------------------------------------------------------------

    protected void updateSubtitle() {
        if (isPendingUpdate(PENDING_UPDATE_SUBTITLE)) return;
        if (mNotification == null) {
            setSubtitle(null);
            return;
        }

        setSubtitle(CsUtils.join(" ", mNotification.subText, mNotification.infoText));
    }

    private void setSubtitle(@Nullable CharSequence subtitle) {
        if (TextUtils.equals(mSubtitle, subtitle)) {
            // No need to notify listeners about this
            // change.
            return;
        }

        mSubtitle = subtitle;
        mListener.onNotificationContentChanged(this, EVENT_SUBTITLE_CHANGED);
    }

    /**
     * @return the notification's subtitle to be displayed.
     */
    @Nullable
    public CharSequence getSubtitle() {
        return mSubtitle;
    }

    //-- MESSAGE --------------------------------------------------------------

    /**
     * Updates message from the current {@link #mNotification notificiation}.
     *
     * @see #setMessages(CharSequence[])
     * @see #isMessageSecret()
     */
    private void updateMessage() {
        if (isPendingUpdate(PENDING_UPDATE_MESSAGE)) return;
        if (mNotification == null) {
            setMessages(null);
            return;
        }

        final boolean secret = isMessageSecret();

        // Get message text
        if (secret) {
            CharSequence[] messages;
            if (sSecureContentLabelRef == null || (messages = sSecureContentLabelRef.get()) == null) {
                final CharSequence cs = mContext.getString(R.string.privacy_mode_hidden_content);
                final SpannableString ss = new SpannableString(cs);
                Check.getInstance().isTrue(ss.length());
                ss.setSpan(new StyleSpan(Typeface.ITALIC),
                        0 /* start */, ss.length() /* end */,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                messages = new CharSequence[]{ss};
                sSecureContentLabelRef = new SoftReference<>(messages);
            }

            setMessages(messages);
        } else {
            CharSequence message;
            if (mBig) {
                if (mNotification.messageTextLines != null) {
                    setMessages(mNotification.messageTextLines);
                    return;
                }

                message = NullUtils.whileNotNull(
                        mNotification.messageBigText,
                        mNotification.messageText
                );
            } else {
                message = mNotification.messageText;
            }

            setMessages(TextUtils.isEmpty(message) ? null : new CharSequence[]{message});
        }

    }

    /**
     * @return {@code true} if the message is a secret and should not be visible to
     * user, {@code false} otherwise.
     */
    protected final boolean isMessageSecret() {
        return mNotification.isContentSecret(mContext);
    }

    private void setMessages(@Nullable CharSequence[] messages) {
        if (Arrays.equals(mMessages, messages)) {
            // No need to notify listeners about this
            // change.
            return;
        }

        mMessages = messages;
        mListener.onNotificationContentChanged(this, EVENT_MESSAGE_CHANGED);
    }

    /**
     * @return the notification's messages to be displayed.
     * @see #getTitle()
     * @see OpenNotification#getVisibility()
     */
    @Nullable
    public CharSequence[] getMessages() {
        return mMessages;
    }

    //-- ACTIONS --------------------------------------------------------------

    private void updateActions() {
        if (isPendingUpdate(PENDING_UPDATE_ACTIONS)) return;
        if (mNotification == null) {
            setActions(null);
            return;
        }

        final boolean secret = isActionsSecret();
        setActions(secret || !mBig ? null : mNotification.getActions());
    }

    /**
     * @return {@code true} if the actions are secret and should not be visible to
     * user, {@code false} otherwise.
     */
    protected final boolean isActionsSecret() {
        return isSecret(
                OpenNotification.VISIBILITY_PRIVATE,
                Config.PRIVACY_HIDE_ACTIONS_MASK);
    }

    private void setActions(@Nullable Action[] actions) {
        if (Arrays.equals(mActions, actions)) {
            // No need to notify listeners about this
            // change.
            return;
        }

        mActions = actions;
        mListener.onNotificationContentChanged(this, EVENT_ACTIONS_CHANGED);
    }

    /**
     * @return the notification's actions to be displayed.
     * @see OpenNotification#getVisibility()
     */
    @Nullable
    public Action[] getActions() {
        return mActions;
    }

}