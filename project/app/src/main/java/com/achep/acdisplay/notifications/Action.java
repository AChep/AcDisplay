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

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.RemoteInput;
import android.util.Log;

import com.achep.base.Device;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;

/**
 * Structure to encapsulate a named action that can be shown as part of this notification.
 * It must include an icon, a label, and a {@link PendingIntent} to be fired when the action is
 * selected by the user.
 * <p>
 * This is actually a wrapper around {@link android.app.Notification.Action} class that supports both
 * Jelly Bean (via reflections) and KitKat Android versions.
 *
 * @author Artem Chepurnoy
 */
public class Action {

    private static final String TAG = "Action";

    @NonNull
    private static SoftReference<Factory> sFactoryRef = new SoftReference<>(null);

    @NonNull
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static Factory getFactory() {
        Factory factory = sFactoryRef.get();
        if (factory == null) {
            if (Device.hasKitKatWatchApi()) {
                factory = new FactoryKitKatWatch();
            } else {
                factory = Device.hasKitKatApi()
                        ? new FactoryKitKat()
                        : new FactoryJellyBean();
            }
            sFactoryRef = new SoftReference<>(factory);
            return factory;
        }
        return factory;
    }

    /**
     * Creates a list of actions based on given {@link android.app.Notification notification}
     * instance.
     *
     * @param notification the notification to create from
     * @return array of {@link com.achep.acdisplay.notifications.Action actions} or {@code null}
     */
    @Nullable
    static Action[] makeFor(@NonNull Notification notification) {
        return getFactory().makeFor(notification);
    }

    /**
     * Small icon representing the action.
     */
    @DrawableRes
    public final int icon;

    /**
     * Title of the action.
     */
    @NonNull
    public final CharSequence title;

    /**
     * Intent to send when the user invokes this action. May be null, in which case the action
     * may be rendered in a disabled presentation by the system UI.
     */
    @Nullable
    public final PendingIntent intent;

    /**
     * The list of inputs to be collected from the user when this action is sent.
     * May be null if no remote inputs were added.
     */
    @Nullable
    public final RemoteInput[] remoteInputs;

    private Action(@DrawableRes int icon, @NonNull CharSequence title,
                   @Nullable PendingIntent intent, @Nullable RemoteInput[] remoteInputs) {
        this.icon = icon;
        this.title = title;
        this.intent = intent;
        this.remoteInputs = remoteInputs;
    }

    /**
     * Base definition of {@link com.achep.acdisplay.notifications.Action} creator.
     *
     * @author Artem Chepurnoy
     */
    public static abstract class Factory {

        /**
         * Creates a list of actions based on given {@link android.app.Notification notification}
         * instance.
         *
         * @param notification notification to create from
         * @return array of {@link com.achep.acdisplay.notifications.Action actions} or {@code null}
         */
        @Nullable
        public abstract Action[] makeFor(@NonNull Notification notification);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    private static class FactoryKitKatWatch extends FactoryKitKat {

        /**
         * {@inheritDoc}
         */
        @Nullable
        public RemoteInput[] getRemoteInputs(@NonNull Notification.Action action) {
            return RemoteInputUtils.toCompat(action.getRemoteInputs());
        }

    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static class FactoryKitKat extends Factory {

        /**
         * {@inheritDoc}
         */
        @Nullable
        @Override
        public Action[] makeFor(@NonNull Notification notification) {
            Notification.Action[] src = notification.actions;

            if (src == null) {
                return null;
            }

            final int length = src.length;
            final Action[] dst = new Action[src.length];
            for (int i = 0; i < length; i++) {
                RemoteInput[] remoteInputs = getRemoteInputs(src[i]);
                dst[i] = new Action(src[i].icon, src[i].title, src[i].actionIntent, remoteInputs);
            }

            return dst;
        }

        /**
         * Get the list of inputs to be collected from the user when this action is sent.
         * May return null if no remote inputs were added.
         */
        @Nullable
        public RemoteInput[] getRemoteInputs(@NonNull Notification.Action action) {
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static class FactoryJellyBean extends Factory {

        /**
         * {@inheritDoc}
         */
        @Nullable
        @Override
        public Action[] makeFor(@NonNull Notification notification) {

            // Getting actions from stupid Jelly Bean.
            Object[] src;
            try {
                Field field = Notification.class.getDeclaredField("actions");
                field.setAccessible(true);
                src = (Object[]) field.get(notification);
            } catch (Exception e) {
                Log.w(TAG, "Failed to access actions field!");
                return null;
            }

            if (src == null) {
                return null;
            }

            final int length = src.length;
            final Action[] dst = new Action[src.length];
            for (int i = 0; i < length; i++) {
                Object object = src[i];
                try {
                    Field field = object.getClass().getDeclaredField("icon");
                    field.setAccessible(true);
                    final int icon = (int) field.get(object);

                    field = object.getClass().getDeclaredField("title");
                    field.setAccessible(true);
                    final CharSequence title = (CharSequence) field.get(object);

                    field = object.getClass().getDeclaredField("actionIntent");
                    field.setAccessible(true);
                    final PendingIntent intent = (PendingIntent) field.get(object);

                    dst[i] = new Action(icon, title, intent, null);
                } catch (Exception e) {
                    Log.wtf(TAG, "Failed to access fields of the Action.");
                    return null;
                }
            }

            return dst;
        }
    }
}
