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
package com.achep.acdisplay.notifications;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * @author Artem Chepurnoy
 */
class NotificationPrTask {

    @Nullable
    public final Context context;
    @NonNull
    public final OpenNotification notification;

    /**
     * {@code true} if it's a {@link NotificationPresenter#postNotification(Context, OpenNotification, int) post}
     * event, {@code false} {@link NotificationPresenter#removeNotification(OpenNotification, int) otherwise}.
     */
    public final boolean posts;

    public volatile int flags;

    NotificationPrTask(@Nullable Context context, @NonNull OpenNotification notification,
                       boolean posts, int flags) {
        this.context = context;
        this.notification = notification;
        this.posts = posts;
        this.flags = flags;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("NotificationPrTask(n=%s, is_post_event=%b, flags=%d)",
                notification.toString(), posts, flags);
    }

}
