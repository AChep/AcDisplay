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
package com.achep.acdisplay.notifications;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.util.Log;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.utils.BitmapUtils;
import com.achep.base.Device;
import com.achep.base.async.AsyncTask;
import com.achep.base.interfaces.IOnLowMemory;
import com.achep.base.interfaces.ISubscriptable;
import com.achep.base.tests.Check;
import com.achep.base.utils.Operator;
import com.achep.base.utils.PackageUtils;
import com.achep.base.utils.smiley.SmileyParser;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.achep.base.Build.DEBUG;

/**
 * @author Artem Chepurnoy
 */
public abstract class OpenNotification implements
        ISubscriptable<OpenNotification.OnNotificationDataChangedListener>,
        IOnLowMemory {

    private static final String TAG = "OpenNotification";

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @NonNull
    static OpenNotification newInstance(@NonNull StatusBarNotification sbn) {
        Notification n = sbn.getNotification();
        if (Device.hasLollipopApi()) {
            return new OpenNotificationLollipop(sbn, n);
        } else if (Device.hasKitKatWatchApi()) {
            return new OpenNotificationKitKatWatch(sbn, n);
        }

        return new OpenNotificationJellyBeanMR2(sbn, n);
    }

    @NonNull
    public static OpenNotification newInstance(@NonNull Notification n) {
        if (Device.hasJellyBeanMR2Api()) {
            throw new RuntimeException("You must use the StatusBarNotification!");
        }

        return new OpenNotificationJellyBean(n);
    }

    //-- DEBUG ----------------------------------------------------------------

    /* Only for debug purposes! */
    private final Object dFinalizeWatcher = DEBUG ? new Object() {

        /**
         * Logs the notifications' removal.
         */
        @Override
        protected void finalize() throws Throwable {
            try {
                Log.d(TAG, "Removing the notification[recycled=" + isRecycled()
                        + "] from the heap: " + OpenNotification.this);
            } finally {
                super.finalize();
            }
        }

    } : null;

    //-- BEGIN ----------------------------------------------------------------

    /**
     * Notification visibility: Show this notification in its entirety on all lockscreens.
     * <p>
     * {@see #getVisibility()}
     */
    public static final int VISIBILITY_PUBLIC = 1;

    /**
     * Notification visibility: Show this notification on all lockscreens, but conceal sensitive or
     * private information on secure lockscreens.
     * <p>
     * {@see #getVisibility()}
     */
    public static final int VISIBILITY_PRIVATE = 0;

    /**
     * Notification visibility: Do not reveal any part of this notification on a secure lockscreen.
     * <p>
     * {@see #getVisibility()}
     */
    public static final int VISIBILITY_SECRET = -1;

    // Events
    public static final int EVENT_ICON = 1;
    public static final int EVENT_READ = 2;
    public static final int EVENT_BACKGROUND = 3;
    public static final int EVENT_BRAND_COLOR = 4;

    @Nullable
    private final StatusBarNotification mStatusBarNotification;
    @NonNull
    private final Notification mNotification;
    @Nullable
    private Action[] mActions;
    private boolean mEmoticonsEnabled;
    private boolean mMine;
    private boolean mRead;
    private boolean mRecycled;
    private long mLoadedTimestamp;
    private int mNumber;

    // Extracted
    @Nullable
    public CharSequence titleBigText;
    @Nullable
    public CharSequence titleText;
    @Nullable
    public CharSequence messageBigText;
    private CharSequence messageBigTextOrigin;
    @Nullable
    public CharSequence messageText;
    private CharSequence messageTextOrigin;
    @Nullable
    public CharSequence[] messageTextLines;
    private CharSequence[] messageTextLinesOrigin;
    @Nullable
    public CharSequence infoText;
    @Nullable
    public CharSequence subText;
    @Nullable
    public CharSequence summaryText;

    // Notification icon.
    @Nullable
    private Bitmap mIconBitmap;
    @Nullable
    private static WeakReference<IconFactory> sIconFactoryRef;
    private IconFactory mIconFactory;
    @NonNull
    private final IconFactory.IconAsyncListener mIconCallback =
            new IconFactory.IconAsyncListener() {
                @Override
                public void onGenerated(@NonNull Bitmap bitmap) {
                    mIconFactory = null;
                    setIcon(bitmap);
                }
            };

    // Dynamic background.
    @Nullable
    private Bitmap mBackgroundBitmap;
    @Nullable
    private static WeakReference<IconFactory> sBackgroundFactoryRef;
    private IconFactory mBackgroundFactory;
    @NonNull
    private final BackgroundFactory.BackgroundAsyncListener mBackgroundCallback =
            new BackgroundFactory.BackgroundAsyncListener() {
                @Override
                public void onGenerated(@NonNull Bitmap bitmap) {
                    mBackgroundFactory = null;
                    setBackground(bitmap);
                }
            };

    // Brand color.
    private int mBrandColor = Color.WHITE;
    @Nullable
    private android.os.AsyncTask<Bitmap, Void, Palette> mPaletteWorker;

    // Listeners
    @NonNull
    private final ArrayList<OnNotificationDataChangedListener> mListeners = new ArrayList<>(3);

    protected OpenNotification(@Nullable StatusBarNotification sbn, @NonNull Notification n) {
        mStatusBarNotification = sbn;
        mNotification = n;
    }

    public void load(@NonNull Context context) {
        mLoadedTimestamp = SystemClock.elapsedRealtime();
        mMine = TextUtils.equals(getPackageName(), PackageUtils.getName(context));
        mActions = Action.makeFor(mNotification);
        mNumber = mNotification.number;

        // Load the brand color.
        loadBrandColor(context);

        // Load notification icon.
        if (sIconFactoryRef == null || (mIconFactory = sIconFactoryRef.get()) == null) {
            sIconFactoryRef = new WeakReference<>(mIconFactory = new IconFactory());
        }
        mIconFactory.remove(this);
        mIconFactory.add(context, this, mIconCallback);

        // Load all other things, such as title text, message text
        // and more and more.
        new Extractor().loadTexts(context, this);
        messageText = ensureNotEmpty(messageText);
        messageBigText = ensureNotEmpty(messageBigText);

        messageTextOrigin = messageText;
        messageBigTextOrigin = messageBigText;
        messageTextLinesOrigin = messageTextLines == null ? null : messageTextLines.clone();

        // Initially load emoticons.
        if (mEmoticonsEnabled) {
            mEmoticonsEnabled = false;
            setEmoticonsEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLowMemory() {
    }

    @Nullable
    private CharSequence ensureNotEmpty(@Nullable CharSequence cs) {
        return TextUtils.isEmpty(cs) ? null : cs;
    }

    /**
     * @return The {@link android.service.notification.StatusBarNotification} or
     * {@code null}.
     */
    @Nullable
    public StatusBarNotification getStatusBarNotification() {
        return mStatusBarNotification;
    }

    /**
     * @return The {@link Notification} supplied to
     * {@link android.app.NotificationManager#notify(int, Notification)}.
     */
    @NonNull
    public Notification getNotification() {
        return mNotification;
    }

    /**
     * Array of all {@link Action} structures attached to this notification.
     */
    @Nullable
    public Action[] getActions() {
        return mActions;
    }

    @Nullable
    public Bitmap getBackground() {
        return mBackgroundBitmap;
    }

    @Nullable
    public Bitmap getIcon() {
        return mIconBitmap;
    }

    /**
     * The number of events that this notification represents. For example, in a new mail
     * notification, this could be the number of unread messages.
     * <p>
     * The system may or may not use this field to modify the appearance of the notification. For
     * example, before {@link android.os.Build.VERSION_CODES#HONEYCOMB}, this number was
     * superimposed over the icon in the status bar. Starting with
     * {@link android.os.Build.VERSION_CODES#HONEYCOMB}, the template used by
     * {@link Notification.Builder} has displayed the number in the expanded notification view.
     * <p>
     * If the number is 0 or negative, it is never shown.
     */
    public int getNumber() {
        return mNumber;
    }

    /**
     * Sphere of visibility of this notification, which affects how and when the SystemUI reveals
     * the notification's presence and contents in untrusted situations (namely, on the secure
     * lockscreen).
     * <p>
     * The default level, {@link #VISIBILITY_PRIVATE}, behaves exactly as notifications have always
     * done on Android: The notification's {@link #getIcon()} (if available) are
     * shown in all situations, but the contents are only available if the device is unlocked for
     * the appropriate user.
     * <p>
     * A more permissive policy can be expressed by {@link #VISIBILITY_PUBLIC}; such a notification
     * can be read even in an "insecure" context (that is, above a secure lockscreen).
     * <p>
     * Finally, a notification can be made {@link #VISIBILITY_SECRET}, which will suppress its icon
     * and ticker until the user has bypassed the lockscreen.
     */
    public int getVisibility() {
        return VISIBILITY_PRIVATE;
    }

    /**
     * @return {@code true} if user has seen the notification,
     * {@code false} otherwise.
     * @see #markAsRead()
     * @see #setRead(boolean)
     */
    public boolean isRead() {
        return mRead;
    }

    //-- COMPARING INSTANCES --------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("OpenNotification(pkg=%s, g_key=%s, g_summary=%b, g_child=%b)",
                getPackageName(), getGroupKey(), isGroupSummary(), isGroupChild());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract int hashCode();

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public abstract boolean equals(Object o);

    /**
     * Note, that method does not equals with {@link #equals(Object)} method.
     *
     * @param n notification to compare with.
     * @return {@code true} if notifications are from the same source and will
     * be handled by system as same notifications, {@code false} otherwise.
     */
    @SuppressLint("NewApi")
    @SuppressWarnings("ConstantConditions")
    public abstract boolean hasIdenticalIds(@Nullable OpenNotification n);

    //-- NOTIFICATION DATA ----------------------------------------------------

    /**
     * Interface definition for a callback to be invoked
     * when date of notification is changed.
     */
    public interface OnNotificationDataChangedListener {

        /**
         * @see #EVENT_BACKGROUND
         * @see #EVENT_ICON
         * @see #EVENT_READ
         */
        void onNotificationDataChanged(@NonNull OpenNotification notification, int event);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerListener(@NonNull OnNotificationDataChangedListener listener) {
        mListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterListener(@NonNull OnNotificationDataChangedListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Notifies all listeners about this event.
     *
     * @see com.achep.acdisplay.notifications.OpenNotification.OnNotificationDataChangedListener
     * @see #registerListener(com.achep.acdisplay.notifications.OpenNotification.OnNotificationDataChangedListener)
     */
    private void notifyListeners(int event) {
        for (OnNotificationDataChangedListener listener : mListeners) {
            listener.onNotificationDataChanged(this, event);
        }
    }

    private void setIcon(@Nullable Bitmap bitmap) {
        if (mIconBitmap == (mIconBitmap = bitmap)) return;
        notifyListeners(EVENT_ICON);
    }

    //-- BRAND COLOR ----------------------------------------------------------

    protected void setBrandColor(int color) {
        if (mBrandColor == (mBrandColor = color)) return;
        notifyListeners(EVENT_BRAND_COLOR);
    }

    protected void loadBrandColor(@NonNull Context context) {
        try {
            String packageName = getPackageName();
            Drawable appIcon;
            try {
                appIcon = context.getPackageManager().getApplicationIcon(packageName);
            } catch (OutOfMemoryError e) {
                Log.e(TAG, "Failed to get application\'s icon due to OutOfMemoryError!");
                return;
            }

            final Bitmap bitmap = Bitmap.createBitmap(
                    appIcon.getMinimumWidth(),
                    appIcon.getMinimumHeight(),
                    Bitmap.Config.ARGB_4444);
            if (bitmap == null) {
                // This had happened on somewhat strange
                // chinese phone.
                return;
            }
            appIcon.draw(new Canvas(bitmap));
            AsyncTask.stop(mPaletteWorker);
            mPaletteWorker = new Palette.Builder(bitmap)
                    .maximumColorCount(16)
                    .generate(new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(Palette palette) {
                            setBrandColor(palette.getVibrantColor(Color.WHITE));
                            bitmap.recycle();
                        }
                    });
        } catch (PackageManager.NameNotFoundException e) { /* do nothing */ }
    }

    public int getBrandColor(int defaultColor) {
        if (mBrandColor == Color.BLACK || mBrandColor == Color.WHITE) {
            return defaultColor;
        }
        return mBrandColor;
    }

    //-- BACKGROUND -----------------------------------------------------------

    private void setBackground(@Nullable Bitmap bitmap) {
        if (mBackgroundBitmap == (mBackgroundBitmap = bitmap)) return;
        notifyListeners(EVENT_BACKGROUND);
    }

    /**
     * Asynchronously generates the background of notification. The background is
     * used by {@link com.achep.acdisplay.ui.fragments.AcDisplayFragment}.
     *
     * @see #clearBackground()
     */
    public void loadBackgroundAsync() {
        // Clear old background.
        clearBackground();

        // Generate new background.
        Bitmap bitmap = mNotification.largeIcon;
        if (isBackgroundFine(bitmap)) {
            if (sBackgroundFactoryRef == null || (mBackgroundFactory = sBackgroundFactoryRef.get()) == null) {
                sBackgroundFactoryRef = new WeakReference<>(mBackgroundFactory = new BackgroundFactory());
            }
            //noinspection ConstantConditions
            mBackgroundFactory.add(null, this, mBackgroundCallback);
        }
    }

    /**
     * Stops the {@link #mBackgroundFactory background loader} and sets the background
     * to {@code null}.
     *
     * @see #loadBackgroundAsync()
     */
    public void clearBackground() {
        if (mBackgroundFactory != null) {
            mBackgroundFactory.remove(this);
            mBackgroundFactory = null;
        }
        setBackground(null);
    }

    private boolean isBackgroundFine(@Nullable Bitmap bitmap) {
        return bitmap != null && !BitmapUtils.hasTransparentCorners(bitmap);
    }

    //-- EMOTICONS ------------------------------------------------------------

    public void setEmoticonsEnabled(boolean enabled) {
        if (mEmoticonsEnabled == (mEmoticonsEnabled = enabled)) return;
        reformatTexts();
    }

    //-- BASICS ---------------------------------------------------------------

    private void reformatTexts() {
        messageText = reformatMessage(messageTextOrigin);
        messageBigText = reformatMessage(messageBigTextOrigin);
        if (messageTextLines != null) {
            for (int i = 0; i < messageTextLines.length; i++) {
                messageTextLines[i] = reformatMessage(messageTextLinesOrigin[i]);
            }
        }
    }

    private CharSequence reformatMessage(@Nullable CharSequence cs) {
        if (cs == null) return null;
        if (mEmoticonsEnabled) cs = SmileyParser.getInstance().addSmileySpans(cs);
        return cs;
    }

    /**
     * Marks the notification as read.
     *
     * @see #setRead(boolean)
     */
    public void markAsRead() {
        setRead(true);
    }

    /**
     * Sets the state of the notification.
     *
     * @param isRead {@code true} if user has seen the notification,
     *               {@code false} otherwise.
     * @see #markAsRead()
     */
    void setRead(boolean isRead) {
        List<OpenNotification> list = getGroupNotifications();
        if (list != null) {
            for (OpenNotification n : list) n.setRead(isRead);
        }
        if (mRead == (mRead = isRead)) return;
        notifyListeners(EVENT_READ);
    }

    /**
     * Dismisses this notification from system.
     *
     * @see NotificationUtils#dismissNotification(OpenNotification)
     */
    public void dismiss() {
        NotificationUtils.dismissNotification(this);
    }

    /**
     * Performs a click on notification.<br/>
     * To be clear it is not a real click but launching its content intent.
     *
     * @return {@code true} if succeed, {@code false} otherwise
     * @see NotificationUtils#startContentIntent(OpenNotification)
     */
    public boolean click() {
        return NotificationUtils.startContentIntent(this);
    }

    /**
     * Clears some notification's resources.
     */
    void recycle() {
        Check.getInstance().isFalse(mRecycled);
        mRecycled = true;

        clearBackground();
        AsyncTask.stop(mPaletteWorker);
        if (mIconFactory != null) {
            mIconFactory.remove(this);
            mIconFactory = null;
        }
    }

    /* Only for debug purposes */
    boolean isRecycled() {
        return mRecycled;
    }

    /**
     * @return {@code true} if notification has been posted from my own application,
     * {@code false} otherwise (or the package name can not be get).
     */
    public boolean isMine() {
        return mMine;
    }

    /**
     * @return {@code true} if notification can be dismissed by user, {@code false} otherwise.
     */
    public boolean isDismissible() {
        return isClearable();
    }

    /**
     * Convenience method to check the notification's flags for
     * either {@link Notification#FLAG_ONGOING_EVENT} or
     * {@link Notification#FLAG_NO_CLEAR}.
     */
    public boolean isClearable() {
        return !Operator.bitAnd(mNotification.flags, Notification.FLAG_ONGOING_EVENT)
                && !Operator.bitAnd(mNotification.flags, Notification.FLAG_NO_CLEAR);
    }

    public boolean isContentSecret(@NonNull Context context) {
        return NotificationUtils.isSecret(context, this, VISIBILITY_PRIVATE,
                Config.PRIVACY_HIDE_CONTENT_MASK);
    }

    /**
     * @return the package name of notification, or a random string
     * if not possible to get the package name.
     */
    @NonNull
    public abstract String getPackageName();

    /**
     * Time since notification has been loaded; in {@link android.os.SystemClock#elapsedRealtime()}
     * format.
     */
    public long getLoadTimestamp() {
        return mLoadedTimestamp;
    }

    //-- GROUPS ---------------------------------------------------------------

    /**
     * @return a key that indicates the group with which this message ranks,
     * or a {@code null} on deprecated systems.
     * @see #getGroupNotifications()
     */
    @Nullable
    public String getGroupKey() {
        return null;
    }

    /**
     * @return the list of notifications of this group (without its summary),
     * or a {@code null} on deprecated systems.
     * @see #isGroupChild()
     * @see #isGroupSummary()
     */
    @Nullable
    public List<OpenNotification> getGroupNotifications() {
        return null;
    }

    /**
     * @return {@code true} if this notification is a child of the {@link #getGroupKey() group},
     * {@code false} otherwise.
     */
    public boolean isGroupChild() {
        return false;
    }

    /**
     * @return {@code true} if this notification is the summary (short summary of all notifications)
     * of the {@link #getGroupKey() group}, {@code false} otherwise.
     */
    public boolean isGroupSummary() {
        return false;
    }

}
