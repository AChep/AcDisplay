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
package com.achep.base.async;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.achep.base.AppHeap;
import com.achep.base.interfaces.IOnLowMemory;
import com.achep.base.utils.IOUtils;
import com.achep.base.utils.NetworkUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import timber.log.Timber;

/**
 * A better {@link AsyncTask}.
 *
 * @author Artem Chepurnoy
 */
public abstract class AsyncTask<A, B, C> extends android.os.AsyncTask<A, B, C> {

    public static void stop(@Nullable AsyncTask asyncTask) {
        if (asyncTask != null && !asyncTask.isFinished()) {
            asyncTask.cancel();
        }
    }

    public static void stop(@Nullable android.os.AsyncTask asyncTask) {
        if (asyncTask != null && !asyncTask.getStatus().equals(Status.FINISHED)) {
            asyncTask.cancel(false);
        }
    }

    /**
     * Equals to calling: {@code AsyncTask.getStatus().equals(AsyncTask.Status.FINISHED)}
     */
    public boolean isFinished() {
        return getStatus().equals(Status.FINISHED);
    }

    /**
     * Equals to calling: {@code AsyncTask.cancel(false)}
     */
    public void cancel() {
        cancel(false);
    }

    /**
     * Downloads text files from internet. Note, that forcing task to stop immediately
     * will likely produce a memory leak.
     *
     * @author Artem Chepurnoy
     */
    public static class DownloadText extends AsyncTask<String, Void, String[]> implements IOnLowMemory {

        private static final String TAG = "DownloadText";

        private static final int MAX_THREAD_NUM = 5;

        private final WeakReference<Callback> mCallback;
        private final ConcurrentHashMap<String, String> mMap;
        private final List<LoaderThread> mThreadList;

        private boolean mReduceThreads = false;

        /**
         * Reduces the number of running download tasks to one.
         * This is not possible to revert.
         */
        @Override
        public void onLowMemory() {
            mReduceThreads = true;
        }

        /**
         * Interface definition for a callback to be invoked
         * when downloading finished or failed.
         */
        public interface Callback {

            /**
             * Called when downloading finished or failed.
             */
            void onDownloaded(@NonNull String[] texts);
        }

        private static class LoaderThread extends Thread {

            private final ConcurrentHashMap<String, String> mMap;
            private final String mUrl;

            public LoaderThread(ConcurrentHashMap<String, String> map, String url) {
                mMap = map;
                mUrl = url;
            }

            @Override
            // TODO: Calculate how much downloading will take
            // to be able to kick threads effectively.
            public void run() {
                Timber.tag(TAG).d("Fetching from " + mUrl);

                InputStream is = null;
                InputStreamReader isr = null;
                BufferedReader br = null;
                try {
                    is = new URL(mUrl).openStream();
                    isr = new InputStreamReader(is);
                    br = new BufferedReader(isr);
                    String result = IOUtils.readTextFromBufferedReader(br);

                    mMap.put(mUrl, result);
                    Timber.tag(TAG).d("Done fetching from " + mUrl);
                } catch (IOException e) {
                    Timber.tag(TAG).d("Failed fetching from " + mUrl);
                } finally {
                    try {
                        if (br != null) {
                            br.close();
                        } else if (isr != null) {
                            isr.close();
                        } else if (is != null) {
                            is.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public DownloadText(@NonNull Callback callback) {
            mCallback = new WeakReference<>(callback);

            int initialCapacity = Math.min(MAX_THREAD_NUM, 5);
            mMap = new ConcurrentHashMap<>(initialCapacity);
            mThreadList = new ArrayList<>(initialCapacity);
        }

        @Override
        protected String[] doInBackground(String... urls) {
            String[] result = new String[urls.length];
            if (!NetworkUtils.isOnline(AppHeap.getContext())) return result;
            for (String url : urls) {
                if (TextUtils.isEmpty(url)) continue;

                // Control the amount of running threads.
                final int threadSize = mThreadList.size();
                if (mReduceThreads ? threadSize > 1 : threadSize > MAX_THREAD_NUM) {

                    // Search for the best candidate to be
                    // finished.
                    LoaderThread thread = null;
                    for (int i = 0; i < threadSize; i++) {
                        thread = mThreadList.get(i);
                        if (!thread.isAlive()) {
                            // No need to search more,
                            // dead thread is a great choice.
                            break;
                        } else if (i == threadSize - 1) {
                            thread = mThreadList.get(0);
                        }
                    }

                    assert thread != null;
                    joinThread(thread);
                    mThreadList.remove(thread);
                }

                LoaderThread thread = new LoaderThread(mMap, url);
                thread.start();
                mThreadList.add(thread);

                if (isCancelled()) {
                    fireThreads();
                    return null;
                }
            }

            // Wait for all threads.
            for (LoaderThread thread : mThreadList) {
                joinThread(thread);
            }
            mThreadList.clear();

            // Extract results to the array.
            for (int i = 0; i < urls.length; i++) {
                result[i] = mMap.get(urls[i]);
            }
            mMap.clear();
            return result;
        }

        /**
         * Joins given thread, if it is alive.
         *
         * @param thread thread to be joined
         */
        private void joinThread(@NonNull LoaderThread thread) {
            if (thread.isAlive()) {
                while (true) {
                    try {
                        thread.join();
                        break;
                    } catch (InterruptedException e) { /* pretty please! */ }
                    // Well, at least it didn't explode.
                }
            }
        }

        private void fireThreads() { // is it correct?
            for (LoaderThread thread : mThreadList) {
                if (thread.isAlive() && !thread.isInterrupted()) {
                    thread.interrupt();
                }
            }
            mThreadList.clear();
        }

        @Override
        protected void onPostExecute(@NonNull String[] s) {
            super.onPostExecute(s);
            Callback callback = mCallback.get();
            if (callback != null) {
                callback.onDownloaded(s);
            } else Timber.tag(TAG).d("Finished loading text, but callback is null!");
        }
    }
}
