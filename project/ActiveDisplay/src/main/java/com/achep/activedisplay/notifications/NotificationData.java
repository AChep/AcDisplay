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

import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import com.achep.activedisplay.Device;
import com.achep.activedisplay.Project;
import com.achep.activedisplay.fragments.AcDisplayFragment;
import com.achep.activedisplay.notifications.parser.IExtractor;
import com.achep.activedisplay.notifications.parser.NativeParser;
import com.achep.activedisplay.notifications.parser.ViewParser;
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
    private Bitmap background;

    /**
     * The number of events that this notification represents. For example, in a new mail
     * notification, this could be the number of unread messages.
     * <p/>
     * The system may or may not use this field to modify the appearance of the notification. For
     * example, before {@link android.os.Build.VERSION_CODES#HONEYCOMB}, this number was
     * superimposed over the icon in the status bar. Starting with
     * {@link android.os.Build.VERSION_CODES#HONEYCOMB}, the template used by
     * {@link Notification.Builder} has displayed the number in the expanded notification view.
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

    public Bitmap getBackground() {
        return background;
    }

    // //////////////////////////////////////////
    // /////////// -- LISTENERS -- //////////////
    // //////////////////////////////////////////

    private ArrayList<OnNotificationDataChangedListener> mListeners = new ArrayList<>(3);

    public interface OnNotificationDataChangedListener {

        public void onNotificationDataChanged(NotificationData data, int changeId);
    }

    public void addOnNotificationDataChangedListener(OnNotificationDataChangedListener listener) {
        if (Project.DEBUG) Log.d(TAG, "add_l_" + mListeners.size() + "=" + listener);
        mListeners.add(listener);
    }

    public void removeOnNotificationDataChangedListener(OnNotificationDataChangedListener listener) {
        if (Project.DEBUG) Log.d(TAG, "remove_l_" + mListeners.size() + "=" + listener);
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

    private static SoftReference<IExtractor> sNativeExtractor = new SoftReference<>(null);
    private static SoftReference<IExtractor> sViewExtractor = new SoftReference<>(null);
    private AcDisplayFragment.BackgroundFactoryThread mBackgroundLoader;
    private IconLoaderThread mIconLoader;

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

    public void loadNotification(Context context, StatusBarNotification sbn, boolean isRead) {
        boolean useViewExtractor = !Device.hasKitKatApi();

        if (!useViewExtractor) {
            IExtractor extractor = sNativeExtractor.get();
            if (extractor == null) {
                extractor = new NativeParser();
                sNativeExtractor = new SoftReference<>(extractor);
            }
            extractor.loadTexts(context, sbn, this);

            // Developer of that notification thinks that he'll be more awesome
            // while using truly custom notifications (90% percents of them
            // sucks a lot).
            useViewExtractor = true
                    && TextUtils.isEmpty(titleText)
                    && TextUtils.isEmpty(getLargeMessage());
        }
        if (useViewExtractor) {
            IExtractor extractor = sViewExtractor.get();
            if (extractor == null) {
                extractor = new ViewParser();
                sViewExtractor = new SoftReference<>(extractor);
            }
            extractor.loadTexts(context, sbn, this);
        }

        number = sbn.getNotification().number;
        markAsRead(isRead);

        if (mIconLoader != null && !mIconLoader.isFinished()) {
            mIconLoader.running = false;
            mIconLoader.cancel(false);
        }
        mIconLoader = new IconLoaderThread(context, sbn, this);
        mIconLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);


        // /////////////////////
        // ~~ LOAD BACKGROUND ~~
        // /////////////////////
        if (mBackgroundLoader != null && !mBackgroundLoader.isFinished()) {
            mBackgroundLoader.cancel();
        }

        Bitmap largeIcon = sbn.getNotification().largeIcon;

        if (largeIcon != null && !BitmapUtils.hasTransparentCorners(largeIcon)) {
            background = sbn.getNotification().largeIcon;

            mBackgroundLoader = new AcDisplayFragment.BackgroundFactoryThread(context, background,
                    new AcDisplayFragment.BackgroundFactoryThread.Callback() {
                @Override
                public void onBackgroundCreated(Bitmap bitmap) {
                    setBackground(bitmap);
                }
            });
            mBackgroundLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            background = null;
        }
    }

    private static class IconLoaderThread extends AsyncTask<Void, Void, Bitmap> {

        private final WeakReference<NotificationData> mNotificationData;
        private final WeakReference<StatusBarNotification> mStatusBarNotification;
        private final WeakReference<Context> mContext;

        public volatile boolean running = true;

        private IconLoaderThread(Context context, StatusBarNotification sbn, NotificationData data) {
            mNotificationData = new WeakReference<>(data);
            mStatusBarNotification = new WeakReference<>(sbn);
            mContext = new WeakReference<>(context);
        }

        /**
         * Current method equals calling:
         * {@code AsyncTask.getStatus().equals(AsyncTask.Status.FINISHED)}
         */
        public boolean isFinished() {
            return getStatus().equals(AsyncTask.Status.FINISHED);
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            final long start = SystemClock.elapsedRealtime();

            StatusBarNotification sbn = mStatusBarNotification.get();
            Context context = mContext.get();

            if (context == null || sbn == null || !running) {
                return null;
            }

            Context contextNotify = NotificationUtils.createContext(context, sbn);
            Bitmap icon = BitmapFactory.decodeResource(
                    contextNotify.getResources(),
                    sbn.getNotification().icon);

            if (icon == null) {
                Log.w(TAG, "No notification icon found.");
                return null;
            }

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

    private static class BackgroundBitmapLoaderThread extends AsyncTask<Void, Void, Bitmap> {

        private final WeakReference<NotificationData> mNotificationData;
        private final WeakReference<StatusBarNotification> mStatusBarNotification;
        private final WeakReference<Context> mContext;

        public volatile boolean running = true;

        private BackgroundBitmapLoaderThread(Context context, StatusBarNotification sbn, NotificationData data) {
            mNotificationData = new WeakReference<>(data);
            mStatusBarNotification = new WeakReference<>(sbn);
            mContext = new WeakReference<>(context);
        }

        /**
         * Current method equals calling:
         * {@code AsyncTask.getStatus().equals(AsyncTask.Status.FINISHED)}
         */
        public boolean isFinished() {
            return getStatus().equals(AsyncTask.Status.FINISHED);
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            final long start = SystemClock.elapsedRealtime();

            StatusBarNotification sbn = mStatusBarNotification.get();
            Context context = mContext.get();

            if (context == null || sbn == null || !running) {
                return null;
            }

            Bitmap largeIcon = sbn.getNotification().largeIcon;
            if (largeIcon == null) {
                return null;
            }

            Bitmap icon = Bitmap.createBitmap(
                    largeIcon.getWidth(),
                    largeIcon.getHeight(),
                    largeIcon.getConfig());

            try {
                RenderScript rs = RenderScript.create(context);
                Allocation overlayAlloc = Allocation.createFromBitmap(rs, largeIcon);
                ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs, overlayAlloc.getElement());

                blur.setInput(overlayAlloc);
                blur.setRadius(3f);
                blur.forEach(overlayAlloc);
                overlayAlloc.copyTo(icon);
            } catch (Exception e) {
                Log.e(TAG, "Failed to blur notification icon.");
            }

            if (Project.DEBUG) {
                long delta = SystemClock.elapsedRealtime() - start;
                Log.d(TAG, "Notification background created in " + delta + " millis:"
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

            data.setBackground(bitmap);
        }
    }

}
