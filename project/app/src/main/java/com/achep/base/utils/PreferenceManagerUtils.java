/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.achep.base.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by Artem Chepurnoy on 05.01.2015.
 */
public final class PreferenceManagerUtils {

    /**
     * Interface definition for a callback to be invoked when a
     * {@link android.preference.Preference} in the hierarchy rooted at this {@link android.preference.PreferenceScreen} is
     * clicked.
     */
    public interface OnPreferenceTreeClickListener {

        /**
         * Called when a preference in the tree rooted at this
         * {@link android.preference.PreferenceScreen} has been clicked.
         *
         * @param preferenceScreen The {@link android.preference.PreferenceScreen} that the
         *                         preference is located in.
         * @param preference       The preference that was clicked.
         * @return Whether the click was handled.
         */
        boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference);

    }

    @NonNull
    public static PreferenceManager newInstance(@NonNull Activity activity, int firstRequestCode) {
        try {
            Constructor<PreferenceManager> c = PreferenceManager.class
                    .getDeclaredConstructor(Activity.class, int.class);
            c.setAccessible(true);
            return c.newInstance(activity, firstRequestCode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the callback to be invoked when a {@link android.preference.Preference} in the
     * hierarchy rooted at this {@link android.preference.PreferenceManager} is clicked.
     *
     * @param listener The callback to be invoked.
     */
    public static void setOnPreferenceTreeClickListener(@NonNull PreferenceManager manager,
                                                        final PreferenceManagerUtils.OnPreferenceTreeClickListener listener) {
        try {
            Field onPreferenceTreeClickListener = PreferenceManager.class
                    .getDeclaredField("mOnPreferenceTreeClickListener");
            onPreferenceTreeClickListener.setAccessible(true);
            if (listener != null) {
                Object proxy = Proxy.newProxyInstance(
                        onPreferenceTreeClickListener.getType().getClassLoader(),
                        new Class[]{onPreferenceTreeClickListener.getType()},
                        new InvocationHandler() {
                            public Object invoke(Object proxy, Method method, Object[] args) {
                                if (method.getName().equals("onPreferenceTreeClick")) {
                                    PreferenceScreen ps = (PreferenceScreen) args[0];
                                    Preference p = (Preference) args[1];
                                    return listener.onPreferenceTreeClick(ps, p);
                                } else {
                                    return null;
                                }
                            }
                        });
                onPreferenceTreeClickListener.set(manager, proxy);
            } else {
                onPreferenceTreeClickListener.set(manager, null);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Inflates a preference hierarchy from the preference hierarchies of
     * {@link android.app.Activity Activities} that match the given {@link android.content.Intent}. An
     * {@link android.app.Activity} defines its preference hierarchy with meta-data using
     * the {@link android.preference.PreferenceManager#METADATA_KEY_PREFERENCES} key.
     * <p/>
     * If a preference hierarchy is given, the new preference hierarchies will
     * be merged in.
     *
     * @param queryIntent     The intent to match activities.
     * @param rootPreferences Optional existing hierarchy to merge the new
     *                        hierarchies into.
     * @return The root hierarchy (if one was not provided, the new hierarchy's
     * root).
     */
    public static PreferenceScreen inflateFromIntent(@NonNull PreferenceManager manager,
                                                     Intent queryIntent,
                                                     PreferenceScreen rootPreferences) {
        try {
            Method m = PreferenceManager.class.getDeclaredMethod("inflateFromIntent",
                    Intent.class,
                    PreferenceScreen.class);
            m.setAccessible(true);
            return (PreferenceScreen) m.invoke(manager, queryIntent, rootPreferences);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Inflates a preference hierarchy from XML. If a preference hierarchy is
     * given, the new preference hierarchies will be merged in.
     *
     * @param context         The context of the resource.
     * @param resId           The resource ID of the XML to inflate.
     * @param rootPreferences Optional existing hierarchy to merge the new
     *                        hierarchies into.
     * @return The root hierarchy (if one was not provided, the new hierarchy's
     * root).
     */
    public static PreferenceScreen inflateFromResource(@NonNull PreferenceManager manager,
                                                       Context context, int resId,
                                                       PreferenceScreen rootPreferences) {
        try {
            Method m = PreferenceManager.class.getDeclaredMethod("inflateFromResource",
                    Context.class, int.class, PreferenceScreen.class);
            m.setAccessible(true);
            return (PreferenceScreen) m.invoke(manager, context, resId, rootPreferences);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the root of the preference hierarchy managed by this class.
     *
     * @return The {@link android.preference.PreferenceScreen} object that is at the root of the hierarchy.
     */
    public static PreferenceScreen getPreferenceScreen(@NonNull PreferenceManager manager) {
        try {
            Method m = PreferenceManager.class.getDeclaredMethod("getPreferenceScreen");
            m.setAccessible(true);
            return (PreferenceScreen) m.invoke(manager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Called by the {@link android.preference.PreferenceManager} to dispatch a subactivity result.
     */
    public static void dispatchActivityResult(@NonNull PreferenceManager manager,
                                              int requestCode, int resultCode,
                                              Intent data) {
        try {
            Method m = PreferenceManager.class.getDeclaredMethod("dispatchActivityResult",
                    int.class, int.class, Intent.class);
            m.setAccessible(true);
            m.invoke(manager, requestCode, resultCode, data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Called by the {@link android.preference.PreferenceManager} to dispatch the activity stop
     * event.
     */
    public static void dispatchActivityStop(@NonNull PreferenceManager manager) {
        try {
            Method m = PreferenceManager.class.getDeclaredMethod("dispatchActivityStop");
            m.setAccessible(true);
            m.invoke(manager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Called by the {@link android.preference.PreferenceManager} to dispatch the activity destroy
     * event.
     */
    public static void dispatchActivityDestroy(@NonNull PreferenceManager manager) {
        try {
            Method m = PreferenceManager.class.getDeclaredMethod("dispatchActivityDestroy");
            m.setAccessible(true);
            m.invoke(manager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the root of the preference hierarchy.
     *
     * @param preferenceScreen The root {@link android.preference.PreferenceScreen} of the preference hierarchy.
     * @return Whether the {@link android.preference.PreferenceScreen} given is different than the previous.
     */
    public static boolean setPreferences(@NonNull PreferenceManager manager,
                                         PreferenceScreen preferenceScreen) {
        try {
            Method m = PreferenceManager.class
                    .getDeclaredMethod("setPreferences", PreferenceScreen.class);
            m.setAccessible(true);
            return ((Boolean) m.invoke(manager, preferenceScreen));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
