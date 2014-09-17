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

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;

import com.achep.acdisplay.AsyncTask;
import com.achep.acdisplay.Build;
import com.achep.acdisplay.R;
import com.achep.acdisplay.acdisplay.BackgroundFactoryThread;
import com.achep.acdisplay.notifications.parser.Extractor;
import com.achep.acdisplay.utils.BitmapUtils;
import com.achep.acdisplay.utils.StringUtils;

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
    public CharSequence[] messageTextLines;
    public CharSequence infoText;
    public CharSequence subText;
    public CharSequence summaryText;

    public Action[] actions;

    private Bitmap icon;
    private Bitmap circleIcon;
    private Bitmap background;

    public int dominantColor;

    public CharSequence getMergedMessage() {
        if (messageTextLines == null) {
            return messageText;
        } else {
            CharSequence[] messages = messageTextLines;

            boolean isFirstMessage = true;

            StringBuilder sb = new StringBuilder();
            for (CharSequence message : messages) {
                // Start every new message from new line
                if (!isFirstMessage & !(isFirstMessage = false)) {
                    sb.append('\n');
                }

                sb.append(message);
            }

            return sb;
        }
    }

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

    public void recycle() {
        AsyncTask.stop(mBackgroundLoader);
        AsyncTask.stop(mIconLoader);

        // All those bitmaps can be displayed at this moment.
        // BitmapUtils.safelyRecycle(background);
        // BitmapUtils.safelyRecycle(circleIcon);
        // BitmapUtils.safelyRecycle(icon);
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
     * @param n Notification to load from.
     * @see #clearBackground()
     */
    public void loadBackground(Context context, OpenNotification n) {
        // Stop previous thread if it is still
        // running.
        AsyncTask.stop(mBackgroundLoader);

        Bitmap bitmapIcon = n.getNotification().largeIcon;
        if (bitmapIcon != null && !BitmapUtils.hasTransparentCorners(bitmapIcon)) {
            mBackgroundLoader = new BackgroundFactoryThread(
                    context, bitmapIcon, mBackgroundLoaderCallback);
            mBackgroundLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    /**
     * Frees the background of this notification.
     *
     * @see #loadBackground(Context, OpenNotification)
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
    public void loadCircleIcon(OpenNotification n) {
        Bitmap bitmapIcon = n.getNotification().largeIcon;
        if (bitmapIcon != null && !BitmapUtils.hasTransparentCorners(bitmapIcon)) {
            circleIcon = BitmapUtils.createCircleBitmap(bitmapIcon);
        }
    }

    /**
     * Frees the circle icon of this notification.
     *
     * @see #loadBackground(Context, com.achep.acdisplay.notifications.OpenNotification)
     */
    public void clearCircleIcon() {
        if (circleIcon != null) {
            circleIcon.recycle();
            circleIcon = null;
        }
    }

    public void loadNotification(Context context, OpenNotification sbn, boolean isRead) {
        Notification notification = sbn.getNotification();
        actions = Action.getFactory().create(notification);
        number = notification.number;
        markAsRead(isRead);

        try {
            String packageName = sbn.getPackageName();
            Drawable appIcon = context.getPackageManager().getApplicationIcon(packageName);

            Bitmap bitmap = Bitmap.createBitmap(
                    appIcon.getMinimumWidth(),
                    appIcon.getMinimumHeight(),
                    Bitmap.Config.ARGB_4444);
            appIcon.draw(new Canvas(bitmap));
            dominantColor = BitmapUtils.getDominantColor(bitmap);
            bitmap.recycle();
        } catch (PackageManager.NameNotFoundException e) { /* do nothing */ }

        sExtractor.loadTexts(context, sbn, this);

        AsyncTask.stop(mIconLoader);
        mIconLoader = new IconLoaderThread(context, sbn);
        mIconLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Task to load an icon from {@link StatusBarNotification notification}.
     *
     * @author Artem Chepurnoy
     */
    private static class IconLoaderThread extends AsyncTask<Void, Void, Bitmap> {

        private final WeakReference<OpenNotification> mOpenNotification;
        private final WeakReference<Context> mContext;

        private volatile long startTime;

        private IconLoaderThread(Context context, OpenNotification sbn) {
            mOpenNotification = new WeakReference<>(sbn);
            mContext = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            startTime = elapsedRealtime(); // to get elapsed time
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            OpenNotification openNotification = mOpenNotification.get();
            Context context = mContext.get();

            if (context == null || openNotification == null || isCancelled()) {
                return null;
            }

            Resources res = context.getResources();
            Drawable drawable = NotificationUtils
                    .getDrawable(context, openNotification, openNotification.getNotification().icon);

            if (isCancelled()) {
                return null;
            }

            final int size = res.getDimensionPixelSize(R.dimen.notification_icon_size);
            return drawable == null ? createEmptyIcon(res, size) : createIcon(drawable, size);
        }

        // TODO: Automatically scale the icon.
        private Bitmap createIcon(Drawable drawable, int size) {
            Bitmap icon = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_4444);
            Canvas canvas = new Canvas(icon);

            drawable = drawable.mutate();
            drawable.setBounds(0, 0, size, size);
            drawable.draw(canvas);

            return icon;
        }

        private Bitmap createEmptyIcon(Resources res, int size) {
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(0xDDCCCCCC); // white gray

            final float radius = size / 2f;

            Bitmap icon = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_4444);
            Canvas canvas = new Canvas(icon);
            canvas.drawCircle(radius, radius, radius, paint);

            Drawable drawable = res.getDrawable(R.drawable.ic_dialog_bug);
            drawable.setBounds(0, 0, size, size);
            drawable.draw(canvas);

            return icon;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (Build.DEBUG) {
                long delta = elapsedRealtime() - startTime;
                Log.d(TAG, "Notification icon loaded in " + delta + " millis:" + " bitmap=" + bitmap);
            }

            OpenNotification data = mOpenNotification.get();
            if (bitmap != null && data != null) {
                data.getNotificationData().setIcon(bitmap);
            }
        }

        private long elapsedRealtime() {
            return SystemClock.elapsedRealtime();
        }

    }

}
