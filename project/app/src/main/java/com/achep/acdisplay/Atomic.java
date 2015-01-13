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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import static com.achep.base.Build.DEBUG;

/**
 * A class for atomic handling start & stop events.
 *
 * @author Artem Chepurnoy
 */
public final class Atomic {

    private final Callback mCallback;
    private final String mTag;

    private volatile boolean mStarted;

    public interface Callback {

        void onStart(Object... objects);

        void onStop(Object... objects);
    }

    public Atomic(@NonNull Callback callback) {
        this(callback, null);
    }

    public Atomic(@NonNull Callback callback, @Nullable String tag) {
        mCallback = callback;
        mTag = tag == null ? getClass().getSimpleName() : tag;
    }

    public void react(boolean start, Object... objects) {
        if (start) {
            start(objects);
        } else {
            stop(objects);
        }
    }

    public void start(Object... objects) {
        synchronized (this) {
            if (!mStarted) {
                mStarted = true;
                mCallback.onStart(objects);
            } else {
                if (DEBUG) Log.d(mTag, "Starting already started.");
            }
        }
    }

    public void stop(Object... objects) {
        synchronized (this) {
            if (mStarted) {
                mStarted = false;
                mCallback.onStop(objects);
            } else {
                if (DEBUG) Log.d(mTag, "Stopping already stopped.");
            }
        }
    }

    public boolean isRunning() {
        synchronized (this) {
            return mStarted;
        }
    }

}
