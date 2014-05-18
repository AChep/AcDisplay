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

package com.achep.acdisplay.acdisplay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.Log;

import com.achep.acdisplay.AsyncTask;
import com.achep.acdisplay.Build;
import com.achep.acdisplay.utils.BitmapUtils;

/**
 * Factory to prepare your background for
 * {@link com.achep.acdisplay.acdisplay.AcDisplayFragment#dispatchSetBackground(android.graphics.Bitmap)}.
 */
public class BackgroundFactoryThread extends AsyncTask<Void, Void, Bitmap> {

    private static final String TAG = "DynamicBackgroundFactory";

    public interface Callback {
        void onBackgroundCreated(Bitmap bitmap);
    }

    private final int mForegroundColor;
    private final Bitmap mBitmapOriginal;
    private final Callback mCallback;

    public BackgroundFactoryThread(Context context, Bitmap original, Callback callback) {
        mForegroundColor = context.getResources().getColor(R.color.keyguard_background_semi);
        mBitmapOriginal = original;
        mCallback = callback;

        if (original == null) throw new IllegalArgumentException("Bitmap may not be null!");
        if (callback == null) throw new IllegalArgumentException("Callback may not be null!");
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        final long start = SystemClock.elapsedRealtime();

        Bitmap origin = mBitmapOriginal;
        Bitmap bitmap = BitmapUtils.doBlur(origin, 3, false);

        if (!running) {
            return null;
        }

        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(mForegroundColor);

        if (Build.DEBUG) {
            long delta = SystemClock.elapsedRealtime() - start;
            Log.d(TAG, "Dynamic background created in " + delta + " millis:"
                    + " width=" + bitmap.getWidth()
                    + " height=" + bitmap.getHeight());
        }

        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        mCallback.onBackgroundCreated(bitmap);
    }
}
