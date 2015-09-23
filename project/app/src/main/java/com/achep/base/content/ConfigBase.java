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
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.achep.base.Device;
import com.achep.base.interfaces.IBackupable;
import com.achep.base.interfaces.IOnLowMemory;
import com.achep.base.interfaces.ISubscriptable;
import com.achep.base.tests.Check;
import com.achep.base.utils.GzipUtils;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import timber.log.Timber;

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
        IOnLowMemory, IBackupable {

    private static final String TAG = "Config";

    protected static final String PREFERENCES_FILE_NAME = "config";

    private final ArrayList<WeakReference<OnConfigChangedListener>> mListenersRefs = new ArrayList<>(6);
    private volatile SoftReference<Map<String, Option>> mMapRef = new SoftReference<>(null);
    private volatile Context mContext;
    private volatile Object mPreviousValue;

    // Threading
    protected final Handler mHandler = new Handler(Looper.getMainLooper());

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
        mMapRef.clear(); // it will be recreated in #getMap().
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
                Timber.tag(TAG).w("Tried to register already registered listener!");
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

        Timber.tag(TAG).w("Tried to unregister non-existent listener!");
    }

    /**
     * @return the {@link java.util.HashMap HashMap} with option's keys as the keys, and
     * its {@link Option data} as the values.
     * @see #onCreateMap(java.util.Map)
     */
    @NonNull
    public final Map<String, Option> getMap() {
        Map<String, Option> map = mMapRef.get();
        if (map == null) {
            map = new HashMap<>();
            onCreateMap(map);
            mMapRef = new SoftReference<>(map);
        }
        return map;
    }

    /**
     * @param key The unique key of the option.
     * @throws RuntimeException if failed to find the corresponding option.
     * @see #getMap()
     */
    @NonNull
    public final Option getOption(@NonNull String key) {
        Option option = getMap().get(key);
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

    /**
     * You may get the previous value from here only on
     * {@link ConfigBase.OnConfigChangedListener#onConfigChanged(ConfigBase, String, Object) config change}.
     */
    @Nullable
    public Object getPreviousValue() {
        return mPreviousValue;
    }

    //-- INTERNAL METHODS -----------------------------------------------------

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
     * @see #getMap()
     */
    protected abstract void onCreateMap(@NonNull Map<String, Option> map);

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

        // Read the current value from an option.
        mPreviousValue = option.read(this);

        // Set the current value to the field.
        try {
            Field field = getClass().getDeclaredField(option.fieldName);
            field.setAccessible(true);
            field.set(this, value);
        } catch (Exception e) {
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
        mPreviousValue = null;
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

    //-- BACKUP ---------------------------------------------------------------

    /**
     * Stores all the values to a JSON string and compresses it
     * using {@link GzipUtils Gzip}.
     *
     * @return the backup string or {@code null} if failed to generate the one.
     * @see #fromBackupText(Context, String)
     */
    @Override
    @Nullable
    public String toBackupText() {
        JSONObject json;
        try {
            json = new JSONObject();
            /*
            // TODO: Should I protect it somehow?
            json.put("__package__", "");
            json.put("__version__", "");
            */
            // Fill the json with key/value pairs
            for (Map.Entry<String, Option> entry : getMap().entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }
        } catch (JSONException e) {
            Log.w(TAG, "Failed to generate JSON: " + e.getMessage());
            return null;
        }

        // We compress the result to protect it from noobs' changes
        // and to reduce its size. This is still easy to extract if
        // you know what to do.
        return GzipUtils.compress(json.toString());
    }

    /**
     * Loads all the settings from previously {@link #toBackupText() generated} backup string.
     * Technically this may broke current settings, so it's kinda dangerous.
     *
     * @return {@code true} if the config was successfully restored, {@code false} otherwise.
     * @see #toBackupText()
     */
    @Override
    public boolean fromBackupText(@NonNull Context context, @NonNull String input) {
        String json = GzipUtils.decompress(input);
        if (json == null) return false;
        String fallback = toBackupText(); // We can't risk
        return fallback != null && fromBackupText(context, json, fallback);
    }

    private boolean fromBackupText(@NonNull Context context,
                                   @NonNull String str, @NonNull String fallback) {
        try {
            JSONObject json = new JSONObject(str);
            Iterator<String> i = json.keys();
            while (i.hasNext()) {
                String key = i.next();
                Object value = json.get(key);
                // Apply the value
                Option option = getMap().get(key);
                if (option != null) {
                    option.write(this, context, value, null);
                } else {
                    Log.w(TAG, "Passed loading an unknown item[" + key + "] from plain text.");
                }
            }
        } catch (Exception e) {
            // Try to fallback to original settings.
            if (!TextUtils.equals(str, fallback)) fromBackupText(context, fallback, fallback);
            // At this point current config may be partially corrupted and un-recoverable.
            return false;
        }
        return true;
    }

    //-- OTHER ----------------------------------------------------------------

    protected void initInternal(@NonNull Context context) {
        try {
            Resources res = context.getResources();
            SharedPreferences prefs = getSharedPreferences(context);
            for (Map.Entry<String, Option> entry : getMap().entrySet()) {
                final String key = entry.getKey();
                final Option option = entry.getValue();

                // Get the current value.
                Object value = option.getDefault(res);
                if (boolean.class.isAssignableFrom(option.clazz)) {
                    value = prefs.getBoolean(key, (Boolean) value);
                } else if (int.class.isAssignableFrom(option.clazz)) {
                    value = prefs.getInt(key, (Integer) value);
                } else if (float.class.isAssignableFrom(option.clazz)) {
                    value = prefs.getFloat(key, (Float) value);
                } else if (String.class.isAssignableFrom(option.clazz)) {
                    value = prefs.getString(key, (String) value);
                } else throw new IllegalArgumentException("Unknown option\'s type.");

                // Set the current value.
                Field field = getClass().getDeclaredField(option.fieldName);
                field.setAccessible(true);
                field.set(this, value);
            }
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    protected void resetInternal(@NonNull Context context) {
        // Reset all values.
        Resources res = context.getResources();
        for (Option option : getMap().values()) {
            Object value = option.getDefault(res);
            option.write(this, context, value, null);
        }
        // Clean the storage.
        SharedPreferences prefs = getSharedPreferences(context);
        prefs.edit().clear().apply();
    }

    //-- SYNCER ---------------------------------------------------------------

    /**
     * A class that syncs {@link android.preference.Preference} with its
     * value in config. Sample class can be found here:
     * {@link com.achep.base.ui.fragments.PreferenceFragment}
     *
     * @author Artem Chepurnoy
     */
    public static class Syncer {

        private final ArrayList<Item> mItems;
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

                        Item item = null;
                        for (Item c : mItems) {
                            if (preference == c.preference) {
                                item = c;
                                break;
                            }
                        }

                        assert item != null;

                        newValue = item.setter.getValue(newValue);
                        item.option.write(mConfig, mContext, newValue, mConfigListener);
                        item.setter.updateSummary(item.preference, item.option, newValue);
                        return true;
                    }

                };

        private final OnConfigChangedListener mConfigListener =
                new OnConfigChangedListener() {

                    @Override
                    public void onConfigChanged(@NonNull ConfigBase config, @NonNull String key,
                                                @NonNull Object value) {
                        Item item = null;
                        for (Item c : mItems) {
                            if (key.equals(c.preference.getKey())) {
                                item = c;
                                break;
                            }
                        }

                        if (item == null) {
                            return;
                        }

                        setPreferenceValue(item, value);
                    }

                };

        private void setPreferenceValue(@NonNull Item item, @NonNull Object value) {
            mBroadcasting = true;
            item.setter.setValue(item.preference, item.option, value);
            item.setter.updateSummary(item.preference, item.option, value);
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
         * and its {@link ConfigBase.Option} and its {@link ConfigBase.Syncer.Setter}.
         *
         * @author Artem Chepurnoy
         */
        private final static class Item {
            final Preference preference;
            final Setter setter;
            final Option option;

            public Item(@NonNull ConfigBase config,
                        @NonNull Preference preference,
                        @NonNull Setter setter) {
                this.preference = preference;
                this.setter = setter;
                this.option = config.getOption(preference.getKey());
            }
        }

        public Syncer(@NonNull Context context, @NonNull ConfigBase config) {
            mItems = new ArrayList<>(10);
            mContext = context;
            mConfig = config;
        }

        public void syncPreference(@Nullable PreferenceScreen ps,
                                   @NonNull Preference preference,
                                   @NonNull Setter setter) {
            Item item = new Item(mConfig, preference, setter);
            if (ps != null) {
                // Remove preference from preference screen
                // if needed.
                boolean fitsMax = !Device.hasTargetApi(item.option.maxSdkVersion + 1);
                boolean fitsMin = Device.hasTargetApi(item.option.minSdkVersion);
                if (!fitsMax || !fitsMin) {
                    ps.removePreference(preference);
                    return;
                }
            }
            // Add preference.
            mItems.add(item);
            // Immediately start listening if needed.
            if (mStarted) startListeningToItem(item);
        }

        /**
         * Updates all preferences and starts to listen to the changes.
         * Don't forget to call {@link #stop()} later!
         *
         * @see #stop()
         * @see #syncPreference(PreferenceScreen, Preference, Setter)
         */
        public void start() {
            mStarted = true;
            mConfig.registerListener(mConfigListener);
            for (Item item : mItems) startListeningToItem(item);
        }

        private void startListeningToItem(@NonNull Item item) {
            item.preference.setOnPreferenceChangeListener(mPreferenceListener);
            setPreferenceValue(item, item.option.read(mConfig));
        }

        /**
         * Stops to listen to the changes.
         *
         * @see #start()
         */
        public void stop() {
            mStarted = false;
            mConfig.unregisterListener(mConfigListener);
            for (Item item : mItems) item.preference.setOnPreferenceChangeListener(null);
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

        private volatile int minSdkVersion = Integer.MIN_VALUE + 1;
        private volatile int maxSdkVersion = Integer.MAX_VALUE - 1;

        private volatile int mDefaultRes = -1;
        private volatile Object mDefault;

        public Option(@NonNull String fieldName,
                      @Nullable String setterName,
                      @Nullable String getterName,
                      @NonNull Class clazz) {
            this.fieldName = fieldName;
            this.setterName = setterName;
            this.getterName = getterName;
            this.clazz = clazz;
        }

        @NonNull
        public Option setDefault(Object value) {
            mDefault = value;
            return this;
        }

        @NonNull
        public Option setDefaultRes(int resource) {
            mDefaultRes = resource;
            return this;
        }

        /**
         * Sets minimum {@link android.os.Build.VERSION#SDK_INT sdk version} of this
         * option. This option won't be shown on older systems.
         *
         * @see #setMaxSdkVersion(int)
         */
        @NonNull
        public Option setMinSdkVersion(int version) {
            minSdkVersion = version;
            return this;
        }

        /**
         * Sets maximum {@link android.os.Build.VERSION#SDK_INT sdk version} of this
         * option. This option won't be shown on newer systems.
         *
         * @see #setMinSdkVersion(int)
         */
        @NonNull
        public Option setMaxSdkVersion(int version) {
            maxSdkVersion = version;
            return this;
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

        /**
         * Extracts and returns the default option's value specified by
         * {@link #setDefault(Object)} or {@link #setDefaultRes(int)}.
         *
         * @see #setDefault(Object)
         * @see #setDefaultRes(int)
         */
        @Nullable
        public final Object getDefault(@NonNull Resources resources) {
            if (mDefaultRes != -1) {
                if (boolean.class.isAssignableFrom(clazz)) {
                    return resources.getBoolean(mDefaultRes);
                } else if (int.class.isAssignableFrom(clazz)) {
                    return resources.getInteger(mDefaultRes);
                } else if (float.class.isAssignableFrom(clazz)) {
                    // Assuming it's a dimension, but not a fraction.
                    return resources.getDimension(mDefaultRes);
                } else if (String.class.isAssignableFrom(clazz)) {
                    return resources.getString(mDefaultRes);
                } else throw new IllegalArgumentException("Unknown option\'s type.");
            }
            return mDefault;
        }

        @NonNull
        public final String getKey(@NonNull ConfigBase config) {
            for (Map.Entry<String, Option> entry : config.getMap().entrySet()) {
                if (entry.getValue().equals(this)) {
                    return entry.getKey();
                }
            }
            throw new RuntimeException();
        }

        //-- READING & WRITING ----------------------------------------------------

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
            } catch (Exception e) {
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
            } catch (Exception e) {
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
            } catch (Exception e) {
                throw new RuntimeException("Failed to access " + clazz.getName() + "#" + setterName + "(***) method.");
            }
        }

    }

}
