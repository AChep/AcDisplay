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
package com.achep.activedisplay.notifications;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import com.achep.activedisplay.AsyncTask;
import com.achep.activedisplay.Device;
import com.achep.activedisplay.Project;
import com.achep.activedisplay.R;
import com.achep.activedisplay.fragments.AcDisplayFragment;
import com.achep.activedisplay.notifications.parser.Extractor;
import com.achep.activedisplay.notifications.parser.NativeExtractor;
import com.achep.activedisplay.notifications.parser.ViewExtractor;
import com.achep.activedisplay.utils.BitmapUtils;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by Artem on 13.01.14.
 */
public class NotificationData {

    private static final String TAG = "NotificationData";

    public static final int ICON = 1;
    public static final int READ = 2;
    public static final int BACKGROUND = 3;

    public CharSequence titleText;
    public CharSequence messageText;
    public CharSequence messageTextLarge;
    public CharSequence infoText;
    public CharSequence subText;
    public CharSequence summaryText;

    private Bitmap icon;
    private Bitmap circleIcon;
    private Bitmap background;

    /**
     * The number of events that this notification represents. For example, in a new mail
     * notification, this could be the number of unread messages.
     * <p/>
     * The system may or may not use this field to modify the appearance of the notification. For
     * example, before {@link android.os.Build.VERSION_CODES#HONEYCOMB}, this number was
     * superimposed over the icon in the status bar. Starting with
     * {@link android.os.Build.VERSION_CODES#HONEYCOMB}, the template used by
     * {@link android.app.Notification.Builder} has displayed the number in the expanded notification view.
     * <p/>
     * If the number is 0 or negative, it is never shown.
     */
    public int number;
    public boolean isRead;

    /**
     * @return {@link #messageTextLarge large message} if not null,
     * otherwise returns {@link #messageText short message}.
     */
    public CharSequence getLargeMessage() {
        return messageTextLarge == null ? messageText : messageTextLarge;
    }

    /**
     * @return small notification icon with corrected size and color.
     * This may return null if icon is still loading.
     */
    public Bitmap getIcon() {
        return icon;
    }

    public Bitmap getCircleIcon() {
        return circleIcon;
    }

    public Bitmap getBackground() {
        return background;
    }

    /**
     * Stops running threads.
     */
    public void stopLoading() {
        stopAsyncTask(mBackgroundLoader);
        stopAsyncTask(mIconLoader);
    }

    // //////////////////////////////////////////
    // /////////// -- LISTENERS -- //////////////
    // //////////////////////////////////////////

    private final ArrayList<OnNotificationDataChangedListener> mListeners = new ArrayList<>(3);

    public interface OnNotificationDataChangedListener {

        public void onNotificationDataChanged(NotificationData data, int changeId);
    }

    public void addOnNotificationDataChangedListener(OnNotificationDataChangedListener listener) {
        mListeners.add(listener);
    }

    public void removeOnNotificationDataChangedListener(OnNotificationDataChangedListener listener) {
        mListeners.remove(listener);
    }

    private void notifyListeners(int changeId) {
        for (OnNotificationDataChangedListener listener : mListeners) {
            listener.onNotificationDataChanged(this, changeId);
        }
    }

    // //////////////////////////////////////////
    // ///////////// -- MAIN -- /////////////////
    // //////////////////////////////////////////

    private static SoftReference<Extractor> sNativeExtractor = new SoftReference<>(null);
    private static SoftReference<Extractor> sViewExtractor = new SoftReference<>(null);
    private IconLoaderThread mIconLoader;
    private AcDisplayFragment.BackgroundFactoryThread mBackgroundLoader;
    private AcDisplayFragment.BackgroundFactoryThread.Callback mBackgroundLoaderCallback =
            new AcDisplayFragment.BackgroundFactoryThread.Callback() {
        @Override
        public void onBackgroundCreated(Bitmap bitmap) {
            setBackground(bitmap);
        }
    };

    public void markAsRead(boolean value) {
        if (isRead == (isRead = value)) return;
        notifyListeners(READ);
    }

    private void setIcon(Bitmap bitmap) {
        icon = bitmap;
        notifyListeners(ICON);
    }

    public void setBackground(Bitmap bitmap) {
        background = bitmap;
        notifyListeners(BACKGROUND);
    }

    /**
     * Asynchronously loads the background of notification.
     *
     * @param sbn Notification to load from.
     * @see #clearBackground()
     */
    public void loadBackground(Context context, StatusBarNotification sbn) {
        // Stop previous thread if it is still
        // running.
        stopAsyncTask(mBackgroundLoader);

        Bitmap bitmapIcon = sbn.getNotification().largeIcon;
        if (bitmapIcon != null && !BitmapUtils.hasTransparentCorners(bitmapIcon)) {
            mBackgroundLoader = new AcDisplayFragment.BackgroundFactoryThread(
                    context, bitmapIcon, mBackgroundLoaderCallback);
            mBackgroundLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    /**
     * Frees the background of this notification.
     *
     * @see #loadBackground(Context, StatusBarNotification)
     */
    public void clearBackground() {
        if (background != null) {
            background.recycle();
            setBackground(null);
        }
    }

    /**
     * Loads the circle icon of this notification.
     *
     * @see #clearCircleIcon()
     */
    public void loadCircleIcon(StatusBarNotification sbn) {
        Bitmap bitmapIcon = sbn.getNotification().largeIcon;
        if (bitmapIcon != null && !BitmapUtils.hasTransparentCorners(bitmapIcon)) {
            circleIcon = BitmapUtils.createCircleBitmap(bitmapIcon);
        }
    }

    /**
     * Frees the circle icon of this notification.
     *
     * @see #loadBackground(Context, StatusBarNotification)
     */
    public void clearCircleIcon() {
        if (circleIcon != null) {
            circleIcon.recycle();
            circleIcon = null;
        }
    }

    public void loadNotification(Context context, StatusBarNotification sbn, boolean isRead) {
        boolean useViewExtractor = !Device.hasKitKatApi();

        if (!useViewExtractor) {
            Extractor extractor = sNativeExtractor.get();
            if (extractor == null) {
                extractor = new NativeExtractor();
                sNativeExtractor = new SoftReference<>(extractor);
            }
            extractor.loadTexts(context, sbn, this);

            // Developer of that notification thinks that he'll be more awesome
            // while using truly custom notifications (90% percents of them
            // sucks a lot).
            //noinspection PointlessBooleanExpression
            useViewExtractor = true
                    && TextUtils.isEmpty(titleText)
                    && TextUtils.isEmpty(getLargeMessage());
        }
        if (useViewExtractor) {
            Extractor extractor = sViewExtractor.get();
            if (extractor == null) {
                extractor = new ViewExtractor();
                sViewExtractor = new SoftReference<>(extractor);
            }
            extractor.loadTexts(context, sbn, this);
        }

        number = sbn.getNotification().number;
        markAsRead(isRead);

        stopAsyncTask(mIconLoader);
        mIconLoader = new IconLoaderThread(context, sbn, this);
        mIconLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void stopAsyncTask(AsyncTask asyncTask) {
        if (asyncTask != null && !asyncTask.isFinished()) {
            asyncTask.cancel();
        }
    }

    private static class IconLoaderThread extends AsyncTask<Void, Void, Bitmap> {

        private final WeakReference<NotificationData> mNotificationData;
        private final WeakReference<StatusBarNotification> mStatusBarNotification;
        private final WeakReference<Context> mContext;

        private IconLoaderThread(Context context, StatusBarNotification sbn, NotificationData data) {
            mNotificationData = new WeakReference<>(data);
            mStatusBarNotification = new WeakReference<>(sbn);
            mContext = new WeakReference<>(context);
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            final long start = SystemClock.elapsedRealtime();

            StatusBarNotification sbn = mStatusBarNotification.get();
            Context context = mContext.get();

            if (context == null || sbn == null || !running) {
                return null;
            }

            Drawable drawable = NotificationUtils.getDrawable(
                    context, sbn, sbn.getNotification().icon);

            if (drawable == null) {
                Log.w(TAG, "No notification icon found.");
                Bitmap icon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_4444);
                Canvas canvas = new Canvas(icon);
                canvas.drawColor(0x60000000);
                return icon;
            }

            Resources res = context.getResources();
            int iconSize = res.getDimensionPixelSize(R.dimen.notification_icon_size);
            drawable.setBounds(0, 0, iconSize, iconSize);

            Bitmap icon = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_4444);
            Canvas canvas = new Canvas(icon);
            drawable.draw(canvas);

            if (Project.DEBUG) {
                long delta = SystemClock.elapsedRealtime() - start;
                Log.d(TAG, "Notification icon loaded in " + delta + " millis:"
                        + " width=" + icon.getWidth()
                        + " height=" + icon.getHeight());
            }

            return icon;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            NotificationData data = mNotificationData.get();
            if (bitmap == null || data == null) {
                return;
            }

            data.setIcon(bitmap);
        }
    }

}
