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
package com.achep.base.timber;

import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import com.achep.base.Build;
import com.achep.base.tests.Check;
import com.achep.base.utils.EncryptionUtils;
import com.achep.base.utils.FileUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import timber.log.Timber;

import static com.achep.base.Build.ENCRYPT_LOGS;

/**
 * Async-ly writes all logs to a file.
 *
 * @author Artem Chepurnoy
 */
public class WritingTree extends Timber.DebugTree {

    public static final String FILENAME = "acdisplay_logs.txt";

    @NonNull
    private final SimpleDateFormat mFormat = new SimpleDateFormat("dd HH:mm:ss", Locale.ENGLISH);

    private T mThread;

    /**
     * @author Artem Chepurnoy
     */
    private static class T extends Thread {

        private static final String TAG = "WritingTree.Thread";
        @SuppressWarnings("PointlessBooleanExpression")
        private final boolean DEBUG = false && Build.DEBUG;
        /**
         * How long should the thread sleep after getting a pending
         * log line.
         */
        private static final long SLEEP = 10000; // 10s
        /**
         * Max number of symbols in a log buffer.
         */
        private static final int MAX_LENGTH = 30000; // 30k symbols

        private final Object mMonitor = new Object();
        private final StringBuilder mBuilder = new StringBuilder();
        private final File mFile;

        private volatile boolean mLocked = false;
        private volatile boolean mRunning = true;
        private volatile boolean mSleepy;

        private long mTime;

        public T() {
            mFile = new File(Environment.getExternalStorageDirectory() + "/" + FILENAME);
            FileUtils.deleteRecursive(mFile);
            setPriority(Thread.MIN_PRIORITY);
        }

        @Override
        public void run() {
            while (mRunning) {
                synchronized (mMonitor) {
                    final int length = mBuilder.length();
                    if (length > 0) {
                        if (DEBUG) {
                            Log.d(TAG, "Writing " + length + "-symbols log to a file.");
                        }

                        //noinspection ConstantConditions
                        CharSequence log = ENCRYPT_LOGS
                                ? EncryptionUtils.x(mBuilder.toString(), Build.LOG_KEY_SALT) + "\n"
                                : mBuilder;

                        //noinspection PointlessBooleanExpression,ConstantConditions
                        if (DEBUG && ENCRYPT_LOGS) {
                            // Check if we can decrypt it
                            String encrypted = log.toString().substring(0, log.length() - 2);
                            Check.getInstance().isTrue(EncryptionUtils
                                    .fromX(encrypted, Build.LOG_KEY_SALT)
                                    .equals(mBuilder.toString()));
                        }

                        try {
                            final boolean succeed = FileUtils.writeToFileAppend(mFile, log);
                            if (succeed) mBuilder.delete(0, length - 1);
                        } catch (SecurityException e) {
                            // TODO: Add the real permission check here.
                        }
                    }

                    try {
                        mLocked = true;
                        mMonitor.wait();
                    } catch (InterruptedException e) { /* unused */ } finally {
                        mLocked = false;
                    }
                }

                if (mSleepy) try {
                    Thread.sleep(SLEEP);
                } catch (InterruptedException e) { /* unused */ }
                mSleepy = true;
            }
        }

        public void requestWrite(int priority, @NonNull CharSequence cs) {
            synchronized (mMonitor) {
                mBuilder.append(cs);

                // Cut the log
                final int length = mBuilder.length();
                if (length > MAX_LENGTH) {
                    Log.w(TAG, "Cutting down the log: length=" + length);
                    mBuilder.delete(0, length - MAX_LENGTH);
                }

                // Write the important logs immediately
                mSleepy &= priority < Log.WARN;
                if (mLocked) mMonitor.notifyAll();
            }
        }
    }

    public WritingTree() {
        super();
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        synchronized (this) {
            Log.println(priority, tag, message);
            // Append a log to the pool.
            if (mThread == null) {
                mThread = new T();
                mThread.start();
            }
            String log = mFormat.format(Calendar.getInstance().getTime())
                    + "/" + priority
                    + "/" + tag
                    + ": " + message + "\n";
            mThread.requestWrite(priority, log);
        }
    }
}