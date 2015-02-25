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
package com.achep.acdisplay.graphics;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.os.AsyncTaskCompat;
import android.util.Log;

import com.achep.acdisplay.R;
import com.achep.acdisplay.notifications.NotificationUtils;
import com.achep.acdisplay.notifications.OpenNotification;
import com.achep.base.async.AsyncTask;

import static com.achep.base.Build.DEBUG;

/**
 * The factory for generating the notification icons.
 *
 * @author Artem Chepurnoy
 */
public class IconFactory {

    private static final String TAG = "IconFactory";

    public interface IconAsyncListener {
        void onGenerated(@NonNull Bitmap bitmap);
    }

    @NonNull
    public static AsyncTask<Void, Void, Bitmap> generateAsync(final @NonNull Context context,
                                                              final @NonNull OpenNotification notification,
                                                              final @NonNull IconAsyncListener listener) {
        return (AsyncTask<Void, Void, Bitmap>) AsyncTaskCompat.executeParallel(
                new AsyncTask<Void, Void, Bitmap>() {

                    @Override
                    protected Bitmap doInBackground(Void... params) {
                        final long start = SystemClock.elapsedRealtime();

                        Bitmap output = generate(context, notification);

                        if (DEBUG) {
                            long delta = SystemClock.elapsedRealtime() - start;
                            Log.d(TAG, "Notification icon created in " + delta + " millis:"
                                    + " width=" + output.getWidth()
                                    + " height=" + output.getHeight());
                        }

                        return output;
                    }

                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        super.onPostExecute(bitmap);
                        listener.onGenerated(bitmap);
                    }

                });
    }

    public static Bitmap generate(final @NonNull Context context,
                                  final @NonNull OpenNotification notification) {
        Resources res = context.getResources();

        int iconRes = notification.getNotification().icon;
        Drawable drawable = NotificationUtils.getDrawable(context, notification, iconRes);

        final int size = res.getDimensionPixelSize(R.dimen.notification_icon_size);
        return drawable == null ? createEmptyIcon(res, size) : createIcon(drawable, size);
    }

    // TODO: Automatically scale the icon.
    @NonNull
    private static Bitmap createIcon(@NonNull Drawable drawable, int size) {
        Bitmap icon = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(icon);

        // Calculate scale ratios
        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();
        float ratioX = Math.min((float) drawableWidth / drawableHeight, 1f);
        float ratioY = Math.min((float) drawableHeight / drawableWidth, 1f);

        // Calculate new width and height
        int width = Math.round(size * ratioX);
        int height = Math.round(size * ratioY);
        int paddingLeft = (size - width) / 2;
        int paddingTop = (size - height) / 2;

        // Apply size and draw
        canvas.translate(paddingLeft, paddingTop);
        drawable = drawable.mutate();
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);

        return icon;
    }

    @NonNull
    private static Bitmap createEmptyIcon(@NonNull Resources res, int size) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(0xDDCCCCCC); // white gray

        final float radius = size / 2f;

        Bitmap icon = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(icon);
        canvas.drawCircle(radius, radius, radius, paint);

        Drawable drawable = res.getDrawable(R.drawable.ic_action_warning_white);
        drawable.setBounds(0, 0, size, size);
        drawable.draw(canvas);

        return icon;
    }

}
