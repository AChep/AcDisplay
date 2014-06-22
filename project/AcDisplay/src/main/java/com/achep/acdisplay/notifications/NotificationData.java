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
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.achep.acdisplay.AsyncTask;
import com.achep.acdisplay.Build;
import com.achep.acdisplay.Device;
import com.achep.acdisplay.R;
import com.achep.acdisplay.acdisplay.BackgroundFactoryThread;
import com.achep.acdisplay.notifications.parser.Extractor;
import com.achep.acdisplay.utils.BitmapUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
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

    public Action[] actions;

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

    /**
     * Wrapper around {@link android.app.Notification.Action} class that supports both
     * Jelly Bean (via reflections) and KitKat Android versions.
     */
    public static class Action {

        public final int icon;
        public final CharSequence title;
        public final PendingIntent intent;

        private Action(int icon, CharSequence title, PendingIntent intent) {
            this.icon = icon;
            this.title = title;
            this.intent = intent;
        }

        @SuppressLint("NewApi")
        private static Action[] create(Notification notification) {
            if (Device.hasKitKatApi()) {
                Notification.Action[] src = notification.actions;

                if (src == null) {
                    return null;
                }

                final int length = src.length;
                final Action[] dst = new Action[src.length];
                for (int i = 0; i < length; i++) {
                    dst[i] = new Action(src[i].icon, src[i].title, src[i].actionIntent);
                }

                return dst;
            }

            // Getting actions from stupid Jelly Bean.
            Object[] src;
            try {
                Field field = Notification.class.getDeclaredField("actions");
                field.setAccessible(true);
                src = (Object[]) field.get(notification);
            } catch (Exception e) {
                Log.w(TAG, "Failed to access actions on Jelly Bean.");
                return null;
            }

            if (src == null) {
                return null;
            }

            final int length = src.length;
            final Action[] dst = new Action[src.length];
            for (int i = 0; i < length; i++) {
                int icon;
                CharSequence title;
                PendingIntent intent;

                Object object = src[i];
                try {
                    Field field = object.getClass().getDeclaredField("icon");
                    field.setAccessible(true);
                    icon = (int) field.get(object);
                } catch (Exception e) {
                    icon = 0;
                }

                try {
                    Field field = object.getClass().getDeclaredField("title");
                    field.setAccessible(true);
                    title = (CharSequence) field.get(object);
                } catch (Exception e) {
                    title = null;
                }

                try {
                    Field field = object.getClass().getDeclaredField("actionIntent");
                    field.setAccessible(true);
                    intent = (PendingIntent) field.get(object);
                } catch (Exception e) {
                    intent = null;
                }

                dst[i] = new Action(icon, title, intent);
            }

            return dst;
        }
    }

    // //////////////////////////////////////////
    // /////////// -- LISTENERS -- //////////////
    // //////////////////////////////////////////

    private final ArrayList<OnNotificationDataChangedListener> mListeners = new ArrayList<>(3);

    public interface OnNotificationDataChangedListener {

        public void onNotificationDataChanged(NotificationData data, int changeId);
    }

    public void registerListener(OnNotificationDataChangedListener listener) {
        mListeners.add(listener);
    }

    public void unregisterListener(OnNotificationDataChangedListener listener) {
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

    private static final Extractor sExtractor = new Extractor();
    private IconLoaderThread mIconLoader;
    private BackgroundFactoryThread mBackgroundLoader;
    private BackgroundFactoryThread.Callback mBackgroundLoaderCallback =
            new BackgroundFactoryThread.Callback() {
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
        if (icon == (icon = bitmap)) return;
        notifyListeners(ICON);
    }

    public void setBackground(Bitmap bitmap) {
        if (background == (background = bitmap)) return;
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
            mBackgroundLoader = new BackgroundFactoryThread(
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
        Notification notification = sbn.getNotification();
        actions = Action.create(notification);
        number = notification.number;
        markAsRead(isRead);

        sExtractor.loadTexts(context, sbn, this);

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

        private volatile long time;

        private IconLoaderThread(Context context, StatusBarNotification sbn, NotificationData data) {
            mNotificationData = new WeakReference<>(data);
            mStatusBarNotification = new WeakReference<>(sbn);
            mContext = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            time = SystemClock.elapsedRealtime();
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            StatusBarNotification sbn = mStatusBarNotification.get();
            Context context = mContext.get();

            if (context == null || sbn == null || !running) {
                return null;
            }

            Drawable drawable = NotificationUtils.getDrawable(
                    context, sbn, sbn.getNotification().icon);

            if (drawable == null) {
                Log.w(TAG, "No notification icon found.");
                return createEmptyIcon();
            }

            Resources res = context.getResources();
            int iconSize = res.getDimensionPixelSize(R.dimen.notification_icon_size);
            drawable.setBounds(0, 0, iconSize, iconSize);

            Bitmap icon = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_4444);
            Canvas canvas = new Canvas(icon);
            drawable.draw(canvas);

            return icon;
        }

        private Bitmap createEmptyIcon() {
            Bitmap icon = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_4444);
            Canvas canvas = new Canvas(icon);
            canvas.drawColor(0x60FF0000);
            return icon;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (Build.DEBUG) {
                long delta = SystemClock.elapsedRealtime() - time;
                Log.d(TAG, "Notification icon loaded in " + delta + " millis:"
                        + " bitmap=" + bitmap);
            }

            NotificationData data = mNotificationData.get();
            if (bitmap == null || data == null) {
                return;
            }

            data.setIcon(bitmap);
        }
    }

}
