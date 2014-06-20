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

package com.achep.acdisplay;

import android.util.Log;

import com.achep.acdisplay.utils.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URL;

/**
 * A better {@link com.achep.acdisplay.AsyncTask}.
 *
 * @author Artem Chepurnoy
 */
public abstract class AsyncTask<A, B, C> extends android.os.AsyncTask<A, B, C> {

    protected volatile boolean running = true;

    /**
     * Equals to calling: {@code AsyncTask.getStatus().equals(AsyncTask.Status.FINISHED)}
     */
    public boolean isFinished() {
        return getStatus().equals(Status.FINISHED);
    }

    public void cancel() {
        running = false;
        cancel(false);
    }

    public static void stop(AsyncTask asyncTask) {
        if (asyncTask != null && !asyncTask.isFinished()) {
            asyncTask.cancel();
        }
    }

    /**
     * Downloads text file from internet.
     *
     * @author Artem Chepurnoy
     */
    public static class DownloadText extends AsyncTask<String, Void, String> {

        private static final String TAG = "DownloadText";
        private WeakReference<Callback> mCallback;

        /**
         * Interface definition for a callback to be invoked
         * when downloading finished or failed.
         */
        public interface Callback {

            /**
             * Called when downloading finished or failed.
             *
             * @param text downloaded text, or {@code null} if failed.
             */
            void onDownloaded(String text);
        }

        public DownloadText(Callback callback) {
            mCallback = new WeakReference<>(callback);
        }

        @Override
        protected String doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                InputStreamReader isr = new InputStreamReader(url.openStream());
                BufferedReader br = new BufferedReader(isr);
                return FileUtils.readTextFromBufferedReader(br);
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            // Notify listener that downloading done.
            Callback callback = mCallback.get();
            if (callback != null) {
                callback.onDownloaded(s);
            } else {
                if (Build.DEBUG) Log.w(TAG, "Finished loading text, but callback is null!");
            }
        }
    }
}
