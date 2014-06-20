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

import java.util.ArrayList;

/**
 * Created by Artem on 10.04.2014.
 *
 * @author Artem Chepurnoy
 */
final class NotificationList {

    private ArrayList<OpenNotification> mList;
    private Callback mCallback;

    public interface Callback {
        public int onNotificationAdded(OpenNotification n);

        public int onNotificationChanged(OpenNotification n, OpenNotification old);

        public int onNotificationRemoved(OpenNotification n);
    }

    public NotificationList(Callback callback) {
        mCallback = callback;
        mList = new ArrayList<>(10);
    }

    public int pushOrRemove(OpenNotification n, boolean push, boolean silently) {
        Callback cb = mCallback;
        if (silently) mCallback = null;
        final int callback = push ? push(n) : remove(n);
        if (silently) mCallback = cb;
        return callback;
    }

    /**
     * Replace or add notification to the list.
     *
     * @return {@link com.achep.acdisplay.notifications.NotificationList.Callback#onNotificationAdded(OpenNotification n)} or
     * {@link com.achep.acdisplay.notifications.NotificationList.Callback#onNotificationChanged(OpenNotification n)}
     */
    public int push(OpenNotification n) {
        int index = indexOf(n);
        if (index < 0) {
            mList.add(n);
            if (mCallback != null) return mCallback.onNotificationAdded(n);
        } else {
            OpenNotification old = mList.get(index);
            mList.remove(index);
            mList.add(index, n);
            if (mCallback != null) return mCallback.onNotificationChanged(n, old);
        }
        return 0;
    }

    public int remove(OpenNotification n) {
        int index = indexOf(n);
        if (index >= 0) {
            mList.get(index).getNotificationData().stopLoading();
            mList.remove(index);
            if (mCallback != null) return mCallback.onNotificationRemoved(n);
        }
        return 0;
    }

    public ArrayList<OpenNotification> list() {
        return mList;
    }

    public int indexOf(OpenNotification n) {
        int size = mList.size();
        for (int i = 0; i < size; i++) {
            OpenNotification o = mList.get(i);
            if (o == null || o.getStatusBarNotification() == null) {
                throw new RuntimeException("Null-notification found! Notification list is probably corrupted. ");
            } else if (NotificationUtils.equals(n, o)) {
                return i;
            }
        }
        return -1;
    }
}