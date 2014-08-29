package com.achep.acdisplay.notifications;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.achep.acdisplay.Device;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;

/**
 * Wrapper around {@link android.app.Notification.Action} class that supports both
 * Jelly Bean (via reflections) and KitKat Android versions.
 */
public class Action {

    private static final String TAG = "Action";

    @NonNull
    private static SoftReference<Factory> sFactoryRef = new SoftReference<>(null);

    /**
     * Small icon representing the action.
     */
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
    @NonNull
    public final PendingIntent intent;

    Action(int icon, @NonNull CharSequence title, @NonNull PendingIntent intent) {
        this.icon = icon;
        this.title = title;
        this.intent = intent;
    }

    /**
     * @return Instance of action factory.
     */
    @NonNull
    public static Factory getFactory() {
        Factory factory = sFactoryRef.get();
        if (factory == null) {
            factory = Device.hasKitKatApi()
                    ? new FactoryNative()
                    : new FactoryReflective();
            sFactoryRef = new SoftReference<>(factory);
            return factory;
        }
        return factory;
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
        public abstract Action[] create(@NonNull Notification notification);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static class FactoryNative extends Factory {

        /**
         * {@inheritDoc}
         */
        @Nullable
        @Override
        public Action[] create(@NonNull Notification notification) {
            Notification.Action[] src = notification.actions;

            if (src == null) {
                return null;
            }

            final int length = src.length;
            final Action[] dst = new Action[src.length];
            for (int i = 0; i < length; i++) {
                dst[i] = new Action(src[i].icon, src[i].title, src[i].actionIntent);
            }

            return dst;
        }
    }

    private static class FactoryReflective extends Factory {

        /**
         * {@inheritDoc}
         */
        @Nullable
        @Override
        public Action[] create(@NonNull Notification notification) {

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

                    dst[i] = new Action(icon, title, intent);
                } catch (Exception e) {
                    Log.wtf(TAG, "Failed to access fields of the Action.");
                    return null;
                }
            }

            return dst;
        }
    }
}
