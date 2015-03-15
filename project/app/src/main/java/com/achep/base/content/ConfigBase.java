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
package com.achep.base.content;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.achep.base.Device;
import com.achep.base.interfaces.IOnLowMemory;
import com.achep.base.interfaces.ISubscriptable;
import com.achep.base.tests.Check;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.achep.base.Build.DEBUG;

/**
 * Saves all the configurations for the app.
 *
 * @author Artem Chepurnoy
 * @since 21.01.14
 */
@SuppressWarnings("ConstantConditions")
public abstract class ConfigBase implements
        ISubscriptable<ConfigBase.OnConfigChangedListener>,
        IOnLowMemory {

    private static final String TAG = "Config";

    protected static final String PREFERENCES_FILE_NAME = "config";

    private SoftReference<HashMap<String, Option>> mHashMapRef = new SoftReference<>(null);
    private final ArrayList<WeakReference<OnConfigChangedListener>> mListenersRefs = new ArrayList<>(6);
    private Context mContext;

    // Threading
    protected Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * Interface definition for a callback to be invoked
     * when a config is changed.
     */
    public interface OnConfigChangedListener {

        /**
         * Callback that the config has changed.
         *
         * @param config an instance of config
         * @param value  a new value of changed option
         */
        void onConfigChanged(
                @NonNull ConfigBase config,
                @NonNull String key,
                @NonNull Object value);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLowMemory() {
        mHashMapRef.clear(); // it will be recreated in #getHashMap().
    }

    /**
     * Adds new {@link java.lang.ref.WeakReference weak} listener to the config. Make sure you call
     * {@link #unregisterListener(ConfigBase.OnConfigChangedListener)} later!
     *
     * @param listener a listener to register to config changes.
     * @see #unregisterListener(ConfigBase.OnConfigChangedListener)
     */
    @Override
    public final void registerListener(@NonNull OnConfigChangedListener listener) {
        // Make sure to register listener only once.
        for (WeakReference<OnConfigChangedListener> ref : mListenersRefs) {
            if (ref.get() == listener) {
                Log.w(TAG, "Tried to register already registered listener!");
                return;
            }
        }

        mListenersRefs.add(new WeakReference<>(listener));
    }

    /**
     * Un-registers listener is there's one.
     *
     * @param listener a listener to unregister from config changes.
     * @see #registerListener(ConfigBase.OnConfigChangedListener)
     */
    @Override
    public final void unregisterListener(@NonNull OnConfigChangedListener listener) {
        for (WeakReference<OnConfigChangedListener> ref : mListenersRefs) {
            if (ref.get() == listener) {
                mListenersRefs.remove(ref);
                return;
            }
        }

        Log.w(TAG, "Tried to unregister non-existent listener!");
    }

    /**
     * @return the {@link java.util.HashMap HashMap} with option's keys as the keys, and
     * its {@link Option data} as the values.
     * @see #onCreateHashMap(java.util.HashMap)
     */
    @NonNull
    public final HashMap<String, Option> getHashMap() {
        HashMap<String, Option> hashMap = mHashMapRef.get();
        if (hashMap == null) {
            hashMap = new HashMap<>();
            onCreateHashMap(hashMap);
            mHashMapRef = new SoftReference<>(hashMap);
        }
        return hashMap;
    }

    @NonNull
    public final Option getOption(@NonNull String key) {
        Option option = getHashMap().get(key);
        if (option != null) return option;

        throw new RuntimeException("You have forgotten to put #"
                + key + " to the hash map of config.");
    }

    /**
     * You may get a context from here only on
     * {@link ConfigBase.OnConfigChangedListener#onConfigChanged(ConfigBase, String, Object) config change}.
     */
    public Context getContext() {
        return mContext;
    }

    //-- INTERNAL METHODS -----------------------------------------------------

    protected void reset(@NonNull Context context) {
        SharedPreferences prefs = getSharedPreferences(context);
        prefs.edit().clear().apply();
        // TODO: Notify about those changes.
    }

    /**
     * Gets an instance of the shared preferences of {@link #PREFERENCES_FILE_NAME}. By
     * default, the name is {@link #PREFERENCES_FILE_NAME}.
     */
    @NonNull
    protected SharedPreferences getSharedPreferences(@NonNull Context context) {
        return context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Fills the {@link java.util.HashMap hash map} with config's options.
     *
     * @see #getHashMap()
     */
    protected abstract void onCreateHashMap(@NonNull HashMap<String, Option> hashMap);

    protected abstract void onOptionChanged(@NonNull Option option, @NonNull String key);

    protected void writeFromMain(final @NonNull Context context,
                                 final @NonNull Option option, final @NonNull Object value,
                                 final @Nullable OnConfigChangedListener listenerToBeIgnored) {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                write(context, option, value, listenerToBeIgnored);
            }

        });
    }

    protected void write(final @NonNull Context context,
                         final @NonNull Option option,
                         final @NonNull Object value,
                         final @Nullable OnConfigChangedListener listenerToBeIgnored) {
        Check.getInstance().isInMainThread();

        if (option.read(ConfigBase.this).equals(value)) return;
        String key = option.getKey(ConfigBase.this);

        if (DEBUG) Log.d(TAG, "Writing \"" + key + "=" + value + "\" to config.");

        // Set the current value to the field.
        try {
            Field field = getClass().getDeclaredField(option.fieldName);
            field.setAccessible(true);
            field.set(this, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("");
        }

        // Set the current value to the preferences file.
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof String) {
            editor.putString(key, (String) value);
        } else throw new IllegalArgumentException("Unknown option\'s type.");
        editor.apply();

        mContext = context;
        onOptionChanged(option, key);
        notifyConfigChanged(key, value, listenerToBeIgnored);
        mContext = null;
    }

    /**
     * @param key   the key of the option
     * @param value the new value
     * @see ConfigBase.OnConfigChangedListener#onConfigChanged(ConfigBase, String, Object)
     */
    private void notifyConfigChanged(@NonNull String key, @NonNull Object value,
                                     @Nullable OnConfigChangedListener listenerToBeIgnored) {
        Check.getInstance().isInMainThread();

        for (int i = mListenersRefs.size() - 1; i >= 0; i--) {
            WeakReference<OnConfigChangedListener> ref = mListenersRefs.get(i);
            OnConfigChangedListener l = ref.get();

            if (l == null) {
                // There were no links to this listener except
                // our class.
                Log.w(TAG, "Deleting an addled listener..!");
                mListenersRefs.remove(i);
            } else if (l != listenerToBeIgnored) {
                l.onConfigChanged(this, key, value);
            }
        }
    }

    //-- SYNCER ---------------------------------------------------------------

    /**
     * A class that syncs {@link android.preference.Preference} with its
     * value in config.
     *
     * @author Artem Chepurnoy
     */
    public static class Syncer {

        private final ArrayList<Group> mGroups;
        private final Context mContext;
        private final ConfigBase mConfig;

        private boolean mBroadcasting;
        private boolean mStarted;

        private final Preference.OnPreferenceChangeListener mPreferenceListener =
                new Preference.OnPreferenceChangeListener() {

                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (mBroadcasting) {
                            return true;
                        }

                        Group group = null;
                        for (Group c : mGroups) {
                            if (preference == c.preference) {
                                group = c;
                                break;
                            }
                        }

                        assert group != null;

                        newValue = group.setter.getValue(newValue);
                        group.option.write(mConfig, mContext, newValue, mConfigListener);
                        group.setter.updateSummary(group.preference, group.option, newValue);
                        return true;
                    }

                };

        private final OnConfigChangedListener mConfigListener =
                new OnConfigChangedListener() {

                    @Override
                    public void onConfigChanged(@NonNull ConfigBase config, @NonNull String key,
                                                @NonNull Object value) {
                        Group group = null;
                        for (Group c : mGroups) {
                            if (key.equals(c.preference.getKey())) {
                                group = c;
                                break;
                            }
                        }

                        if (group == null) {
                            return;
                        }

                        setPreferenceValue(group, value);
                    }

                };

        private void setPreferenceValue(@NonNull Group group, @NonNull Object value) {
            mBroadcasting = true;
            group.setter.setValue(group.preference, group.option, value);
            group.setter.updateSummary(group.preference, group.option, value);
            mBroadcasting = false;
        }

        public interface Setter {

            void updateSummary(@NonNull Preference preference,
                               @NonNull Option option,
                               @NonNull Object value);

            /**
             * Sets new value to the preference.
             *
             * @param preference preference to set to
             * @param option     the changed option
             * @param value      new value to set
             */
            void setValue(@NonNull Preference preference,
                          @NonNull Option option,
                          @NonNull Object value);

            @NonNull
            Object getValue(@NonNull Object value);

        }

        /**
         * A class-merge of {@link android.preference.Preference}
         * and its {@link ConfigBase.Option}.
         *
         * @author Artem Chepurnoy
         */
        private final static class Group {
            final Preference preference;
            final Setter setter;
            final Option option;

            public Group(@NonNull ConfigBase config,
                         @NonNull Preference preference,
                         @NonNull Setter setter) {
                this.preference = preference;
                this.setter = setter;
                this.option = config.getOption(preference.getKey());
            }
        }

        public Syncer(@NonNull Context context, @NonNull ConfigBase config) {
            mGroups = new ArrayList<>(10);
            mContext = context;
            mConfig = config;
        }

        @NonNull
        public Syncer addPreference(@Nullable PreferenceScreen preferenceScreen,
                                    @NonNull Preference preference,
                                    @NonNull Setter setter) {
            Group group = new Group(mConfig, preference, setter);
            addPreference(preferenceScreen, preference, group);
            return this;
        }

        private void addPreference(@Nullable PreferenceScreen preferenceScreen,
                                   @NonNull Preference preference,
                                   Group group) {

            // Remove preference from preference screen
            // if needed.
            if (preferenceScreen != null) {
                if (Device.hasTargetApi(group.option.maxSdkVersion + 1)
                        || !Device.hasTargetApi(group.option.minSdkVersion)) {
                    preferenceScreen.removePreference(preference);
                    return;
                }
            }

            mGroups.add(group);

            if (mStarted) {
                startListeningGroup(group);
            }
        }

        /**
         * Updates all preferences and starts to listen to the changes.
         */
        public void start() {
            mStarted = true;
            mConfig.registerListener(mConfigListener);
            for (Group group : mGroups) {
                startListeningGroup(group);
            }
        }

        private void startListeningGroup(@NonNull Group group) {
            group.preference.setOnPreferenceChangeListener(mPreferenceListener);
            setPreferenceValue(group, group.option.read(mConfig));
        }

        /**
         * Stops to listen to the changes.
         */
        public void stop() {
            mStarted = false;
            mConfig.unregisterListener(mConfigListener);
            for (Group group : mGroups) {
                group.preference.setOnPreferenceChangeListener(null);
            }
        }
    }

    /**
     * @author Artem Chepurnoy
     */
    public static class Option {

        @NonNull
        private final String fieldName;
        @Nullable
        private final String setterName;
        @Nullable
        private final String getterName;
        @NonNull
        private final Class clazz;

        private final int minSdkVersion;
        private final int maxSdkVersion;

        public Option(@NonNull String fieldName,
                      @Nullable String setterName,
                      @Nullable String getterName,
                      @NonNull Class clazz) {
            this(fieldName, setterName, getterName, clazz, 0, Integer.MAX_VALUE - 1);
        }

        public Option(@NonNull String fieldName,
                      @Nullable String setterName,
                      @Nullable String getterName,
                      @NonNull Class clazz, int minSdkVersion, int maxSdkVersion) {
            this.fieldName = fieldName;
            this.setterName = setterName;
            this.getterName = getterName;
            this.clazz = clazz;
            this.minSdkVersion = minSdkVersion;
            this.maxSdkVersion = maxSdkVersion;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return new HashCodeBuilder(11, 31)
                    .append(fieldName)
                    .append(setterName)
                    .append(getterName)
                    .append(clazz)
                    .toHashCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if (o == null)
                return false;
            if (o == this)
                return true;
            if (!(o instanceof Option))
                return false;

            Option option = (Option) o;
            return new EqualsBuilder()
                    .append(fieldName, option.fieldName)
                    .append(setterName, option.setterName)
                    .append(getterName, option.getterName)
                    .append(clazz, option.clazz)
                    .isEquals();
        }

        @NonNull
        public final String getKey(@NonNull ConfigBase config) {
            for (Map.Entry<String, Option> entry : config.getHashMap().entrySet()) {
                if (entry.getValue().equals(this)) {
                    return entry.getKey();
                }
            }
            throw new RuntimeException();
        }

        /**
         * Reads an option from given config instance.</br>
         * Reading is done using reflections!
         *
         * @param config a config to read from.
         * @throws RuntimeException if failed to read given config.
         */
        @NonNull
        public final Object read(@NonNull ConfigBase config) {
            return getterName != null ? readFromGetter(config) : readFromField(config);
        }

        @NonNull
        private Object readFromField(@NonNull ConfigBase config) {
            assert fieldName != null;
            try {
                Field field = config.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(config);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("Failed to access the " + clazz.getName() + "#" + fieldName + " field.");
            }
        }

        @NonNull
        private Object readFromGetter(@NonNull ConfigBase config) {
            assert getterName != null;
            try {
                Method method = config.getClass().getDeclaredMethod(getterName);
                method.setAccessible(true);
                return method.invoke(config);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException("Failed to access the " + clazz.getName() + "#" + getterName + " method.");
            }
        }

        /**
         * Writes new value to the option to given config instance.</br>
         * Writing is done using reflections!
         *
         * @param config a config to write to.
         * @throws RuntimeException if failed to read given config.
         */
        public final void write(@NonNull ConfigBase config, @NonNull Context context,
                                @NonNull Object newValue, @Nullable OnConfigChangedListener listener) {
            if (setterName != null) {
                // Setter must be calling #writeFromMain by itself.
                writeBySetter(config, context, newValue, listener);
                return;
            }

            config.writeFromMain(context, this, newValue, listener);
        }

        private void writeBySetter(@NonNull ConfigBase config, @NonNull Context context,
                                   @NonNull Object newValue, @Nullable OnConfigChangedListener listener) {
            assert setterName != null;
            try {
                Method method = config.getClass().getDeclaredMethod(setterName,
                        Context.class, clazz,
                        ConfigBase.OnConfigChangedListener.class);
                method.setAccessible(true);
                method.invoke(config, context, newValue, listener);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException("Failed to access " + clazz.getName() + "#" + setterName + "(***) method.");
            }
        }

    }

}
