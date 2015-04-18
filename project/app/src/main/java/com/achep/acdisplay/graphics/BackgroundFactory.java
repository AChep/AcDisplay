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
package com.achep.acdisplay.graphics;

import android.graphics.Bitmap;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.os.AsyncTaskCompat;
import android.util.Log;

import com.achep.base.async.AsyncTask;
import com.enrique.stackblur.StackBlurManager;

import static com.achep.base.Build.DEBUG;

/**
 * The factory for generating the background images.
 *
 * @author Artem Chepurnoy
 */
public class BackgroundFactory {

    private static final String TAG = "BackgroundFactory";

    public interface BackgroundAsyncListener {
        void onGenerated(@NonNull Bitmap bitmap);
    }

    @NonNull
    public static AsyncTask<Void, Void, Bitmap> generateAsync(final @NonNull Bitmap bitmap,
                                                              final @NonNull BackgroundAsyncListener listener) {
        return (AsyncTask<Void, Void, Bitmap>) AsyncTaskCompat.executeParallel(
                new AsyncTask<Void, Void, Bitmap>() {

                    @Override
                    protected Bitmap doInBackground(Void... params) {
                        final long start = SystemClock.elapsedRealtime();


                        Bitmap output;
                        try {
                            output = generate(bitmap);
                        } catch (OutOfMemoryError e) {
                            Log.e(TAG, "Out-of-memory error while blurring the background!");
                            output = bitmap;
                        }

                        if (DEBUG) {
                            long delta = SystemClock.elapsedRealtime() - start;
                            Log.d(TAG, "Dynamic background created in " + delta + " millis:"
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

    public static Bitmap generate(@NonNull Bitmap bitmap) {
        StackBlurManager sbm = new StackBlurManager(bitmap);
        return sbm.process(3);
    }

}
