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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.achep.base.interfaces.IOnLowMemory;

import java.util.ArrayList;

/**
 * Is a list of {@link OpenNotification notifications} with
 * an ability to easily add / replace / remove item from the list.
 *
 * @author Artem Chepurnoy
 */
final class NotificationList extends ArrayList<OpenNotification> implements IOnLowMemory {

    /**
     * Default return value of {@link #pushNotification(OpenNotification)}
     * or {@link #removeNotification(OpenNotification)} methods.
     */
    static final int RESULT_DEFAULT = 0;

    private static final int EVENT_ADDED = 0;
    private static final int EVENT_CHANGED = 1;
    private static final int EVENT_REMOVED = 2;

    @Nullable
    private OnNotificationListChangedListener mListener;

    /**
     * @see #setMaximumSize(int)
     */
    private volatile int mMaximumSize = Integer.MAX_VALUE;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLowMemory() {
        for (OpenNotification n : this) n.onLowMemory();
    }

    /**
     * Interface definition for a callback to be invoked
     * when a list of notifications has changed.
     */
    public interface OnNotificationListChangedListener {

        /**
         * Called when new notification was added to list.
         *
         * @param n newly added notification
         */
        int onNotificationAdded(@NonNull OpenNotification n);

        /**
         * Called when old notification was replaced with new one.
         *
         * @param n   newly added notification
         * @param old removed notification
         */
        int onNotificationChanged(@NonNull OpenNotification n, @NonNull OpenNotification old);

        /**
         * Called when notification was removed from list.
         *
         * @param n removed notification
         */
        int onNotificationRemoved(@NonNull OpenNotification n);

    }

    /**
     * Creates new {@link com.achep.acdisplay.notifications.NotificationList} with initial capacity
     * equals to {@code 10}.
     *
     * @param listener Listener to which all events will be send.
     */
    public NotificationList(@Nullable OnNotificationListChangedListener listener) {
        mListener = listener;
    }

    /**
     * Sets the maximum size of this list.
     *
     * @param maxSize the maximum size.
     */
    public void setMaximumSize(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("Maximum size must be greater than zero!");
        }

        mMaximumSize = maxSize;
    }

    int pushOrRemoveNotification(@NonNull OpenNotification n, boolean push) {
        return push ? pushNotification(n) : removeNotification(n);
    }

    /**
     * Adds or replaces existent notification to/of the list.
     *
     * @return {@link NotificationList.OnNotificationListChangedListener#onNotificationAdded(OpenNotification n)} or
     * {@link NotificationList.OnNotificationListChangedListener#onNotificationChanged(OpenNotification n, OpenNotification old)}
     */
    int pushNotification(@NonNull OpenNotification n) {
        return pushNotification(n, true);
    }

    int pushNotification(@NonNull OpenNotification n, boolean www) {
        final int index = indexOfNotification(n);
        if (index == -1) {
            if (size() > mMaximumSize) {
                remove(0);
            }

            // Add new notification to the list.
            add(n);
            return notifyListener(EVENT_ADDED, n, null);
        } else if (www) {
            // Replace old notification with new one.
            OpenNotification old = remove(index);
            add(index, n);
            return notifyListener(EVENT_CHANGED, n, old);
        }
        return RESULT_DEFAULT;
    }

    /**
     * Removes notification from the list.
     *
     * @return {@link NotificationList.OnNotificationListChangedListener#onNotificationRemoved(OpenNotification n)}
     * @see #pushNotification(OpenNotification n)
     */
    int removeNotification(@NonNull OpenNotification n) {
        final int index = indexOfNotification(n);
        return index != -1 ? removeNotification(index) : RESULT_DEFAULT;
    }

    int removeNotification(int index) {
        OpenNotification old = remove(index);
        return notifyListener(EVENT_REMOVED, old, null);
    }

    /**
     * @return the position of given {@link OpenNotification} in list, or {@code -1} if not found.
     * @see NotificationUtils#hasIdenticalIds(OpenNotification, OpenNotification)
     */
    int indexOfNotification(@NonNull OpenNotification n) {
        final int size = size();
        for (int i = 0; i < size; i++) {
            if (NotificationUtils.hasIdenticalIds(n, get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Notifies listener about this event.
     *
     * @see #EVENT_ADDED
     * @see #EVENT_CHANGED
     * @see #EVENT_REMOVED
     */
    private int notifyListener(final int event, @NonNull OpenNotification n, OpenNotification old) {
        if (mListener == null) return RESULT_DEFAULT;
        switch (event) {
            case EVENT_ADDED:
                return mListener.onNotificationAdded(n);
            case EVENT_CHANGED:
                return mListener.onNotificationChanged(n, old);
            case EVENT_REMOVED:
                return mListener.onNotificationRemoved(n);
            default:
                throw new IllegalArgumentException();
        }
    }
}
