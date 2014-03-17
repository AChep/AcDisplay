/*
 * Copyright (C) 2013 AChep@xda <artemchep@gmail.com>
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

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Artem on 20.02.14.
 */
class NotificationList {

    private static final String TAG = "NotificationList";

    private ArrayList<OpenStatusBarNotification> mList;

    private Callback mCallback;

    public interface Callback {
        public int onNotificationAdded(OpenStatusBarNotification n);

        public int onNotificationChanged(OpenStatusBarNotification n);

        public int onNotificationRemoved(OpenStatusBarNotification n);
    }

    public NotificationList(Callback callback) {
        mCallback = callback;
        mList = new ArrayList<>(10);
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public int pushOrRemove(OpenStatusBarNotification n, boolean push, boolean silently) {
        Callback cb = mCallback;
        if (silently) mCallback = null;
        final int callback = push ? push(n) : remove(n);
        if (silently) mCallback = cb;
        return callback;
    }

    /**
     * Replace or add notification to the list.
     *
     * @return {@link Callback#onNotificationAdded(OpenStatusBarNotification n)} or
     * {@link Callback#onNotificationChanged(OpenStatusBarNotification n)}
     */
    public int push(OpenStatusBarNotification n) {
        int index = indexOf(n);
        if (index < 0) {
            mList.add(n);
            if (mCallback != null) return mCallback.onNotificationAdded(n);
        } else {
            mList.remove(index);
            mList.add(index, n);
            if (mCallback != null) return mCallback.onNotificationChanged(n);
        }
        return 0;
    }

    public int remove(OpenStatusBarNotification n) {
        int index = indexOf(n);
        if (index >= 0) {
            mList.remove(index);
            if (mCallback != null) return mCallback.onNotificationRemoved(n);
        }
        return 0;
    }

    public ArrayList<OpenStatusBarNotification> list() {
        return mList;
    }

    public int indexOf(OpenStatusBarNotification n) {
        for (int i = mList.size() - 1; i >= 0; i--) {
            OpenStatusBarNotification o = mList.get(i);
            if (o == null || o.getStatusBarNotification() == null) {
                Log.wtf(TAG, "Null-notification found!");
            } else if (NotificationUtils.equals(n, o)) {
                return i;
            }
        }
        return -1;
    }
}
