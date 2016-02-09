package com.achep.base.content;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.achep.base.Device;
import com.achep.base.interfaces.IOnLowMemory;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Config class that stores all application's options and provides an easy way
 * to sync them with a GUI.
 *
 * @author Artem Chepurnoy
 */
public abstract class CfgBase implements IOnLowMemory {

    private static final boolean DEBUG = true;
    private static final String TAG = "cfg_base";

    /**
     * @return the filename of this config
     */
    @NonNull
    public abstract String getPreferencesFileName();

    /**
     * Loads all settings from the storage. Should be called on the begin
     * of app's lifecycle.
     */
    public final void load(@NonNull Context context) {
        synchronized (this) {
            Resources res = context.getResources();
            SharedPreferences prefs = getSharedPreferences(context);
            for (Option option : getMap().values()) {
                // Get the current value.
                Object value = option.getDefault(res);
                if (boolean.class.isAssignableFrom(option.clazz)) {
                    value = prefs.getBoolean(option.key, (Boolean) value);
                } else if (int.class.isAssignableFrom(option.clazz)) {
                    value = prefs.getInt(option.key, (Integer) value);
                } else if (float.class.isAssignableFrom(option.clazz)) {
                    value = prefs.getFloat(option.key, (Float) value);
                } else if (String.class.isAssignableFrom(option.clazz)) {
                    value = prefs.getString(option.key, (String) value);
                } else if (long.class.isAssignableFrom(option.clazz)) {
                    value = prefs.getLong(option.key, (Long) value);
                } else throw new IllegalArgumentException("Unknown option\'s type.");

                Log.d(TAG, "Init option=" + option.key + "  with " + value);

                // Set the current value.
                option.setValue(value);
            }
        }
    }

    /**
     * Resets all options to their default values.
     */
    public final void reset(@NonNull Context context) {
        synchronized (this) {
            Transaction transaction = new Transaction(true /* fake putting */);
            transaction.beginTransaction(context);
            // Reset all values.
            Resources res = context.getResources();
            for (Option option : getMap().values()) {
                Object value = option.getDefault(res);
                transaction.put(option, value, null /* notify everyone */);
            }
            // Clean the storage.
            transaction.clear(); // not faked
            transaction.endTransaction();
        }
    }

    public final void put(@NonNull Context context, @NonNull String key, @NonNull Object obj) {
        put(context, key, obj, null);
    }

    public final void put(@NonNull Context context, @NonNull String key, @NonNull Object obj,
                          @Nullable OnConfigChangedListener listenerIgnored) {
        synchronized (this) {
            put(context, new Change(key, obj, listenerIgnored));
        }
    }

    public final void put(@NonNull Context context, @NonNull Change change) {
        synchronized (this) {
            put(context, new Change[]{change});
        }
    }

    public final void put(@NonNull Context context,
                          @NonNull Change[] changes) {
        synchronized (this) {
            Transaction transaction = new Transaction();
            transaction.beginTransaction(context);
            for (Change change : changes) transaction.put(change);
            transaction.endTransaction();
        }
    }

    //-- BEGIN ----------------------------------------------------------------

    @NonNull
    private final ArrayList<Reference<OnConfigChangedListener>> mListenersRefs = new ArrayList<>(6);
    private final ArrayList<HandlerHolder> mListenersHolders = new ArrayList<>();
    private Map<String, Option> mMap;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLowMemory() { /* unused */ }

    /**
     * Gets an instance of the shared preferences.
     */
    @NonNull
    protected SharedPreferences getSharedPreferences(@NonNull Context context) {
        return context.getSharedPreferences(getPreferencesFileName(), Context.MODE_PRIVATE);
    }

    protected abstract void onConfigChanged(@NonNull Transaction transaction, @NonNull Option option);

    /**
     * Here you should create and add all options.
     */
    protected abstract void onCreateMap(@NonNull Map<String, Option> map);

    protected final void putOption(@NonNull Map<String, Option> map, @NonNull Option option) {
        map.put(option.key, option);
    }

    /**
     * @return the {@link java.util.HashMap HashMap} with option's keys as the keys, and
     * its {@link Option data} as the values.
     * @see #onCreateMap(Map)
     */
    @NonNull
    public final Map<String, Option> getMap() {
        if (mMap == null) {
            mMap = new ConcurrentHashMap<>();
            onCreateMap(mMap);
        }
        return mMap;
    }

    /**
     * @param key The unique key of the option.
     * @throws IllegalArgumentException if failed to find the corresponding option.
     * @see #getMap()
     */
    @NonNull
    public final Option getOption(@NonNull String key) {
        Option option = getMap().get(key);
        if (option == null) {
            throw new IllegalArgumentException("No existent option for " + key + " not found!");
        }
        return option;
    }

    public final boolean contains(@NonNull String key) {
        return getMap().containsKey(key);
    }

    //-- READ -----------------------------------------------------------------

    public boolean getBoolean(@NonNull String key) {
        return (boolean) getObject(key);
    }

    public float getFloat(@NonNull String key) {
        return (float) getObject(key);
    }

    public int getInt(@NonNull String key) {
        return (int) getObject(key);
    }

    public long getLong(@NonNull String key) {
        return (long) getObject(key);
    }

    @NonNull
    public String getString(@NonNull String key) {
        return (String) getObject(key);
    }

    @NonNull
    public Object getObject(@NonNull String key) {
        return getOption(key).getValue();
    }

    //-- LISTENER -------------------------------------------------------------

    /**
     * Adds new {@link WeakReference weak} listener to the config. Make sure you call
     * {@link #unregisterListener(OnConfigChangedListener)} later!
     *
     * @param listener a listener to register to config changes.
     * @see #unregisterListener(OnConfigChangedListener)
     */
    public final void registerListener(@NonNull OnConfigChangedListener listener) {
        synchronized (mListenersRefs) {
            // Make sure to register listener only once.
            for (Reference<OnConfigChangedListener> ref : mListenersRefs) {
                if (ref.get() == listener) {
                    Log.w(TAG, "Tried to register already registered listener!");
                    return;
                }
            }

            addListenerRef(new WeakReference<>(listener), listener);
        }
    }

    /**
     * Un-registers listener is there's one.
     *
     * @param listener a listener to unregister from config changes.
     * @see #registerListener(OnConfigChangedListener)
     */
    public final void unregisterListener(@NonNull OnConfigChangedListener listener) {
        synchronized (mListenersRefs) {
            for (Reference<OnConfigChangedListener> ref : mListenersRefs) {
                if (ref.get() == listener) {
                    removeListenerRef(ref);
                    return;
                }
            }

            Log.w(TAG, "Tried to unregister non-existent listener!");
        }
    }

    private void addListenerRef(@NonNull Reference<OnConfigChangedListener> ref,
                                @NonNull OnConfigChangedListener listener) {
        mListenersRefs.add(ref);

        if (!(listener instanceof UiThreadedConfigChangedListener)) return;
        UiThreadedConfigChangedListener uil = (UiThreadedConfigChangedListener) listener;
        // Yes, it looks weird that I check for a Looper here, but
        // if you check the HandlerHolder class you'll see why it
        // will work normally.

        //noinspection SuspiciousMethodCalls
        int index = mListenersHolders.indexOf(uil.mLooper);
        if (index != -1) {
            if (DEBUG) Log.d(TAG, "Adding a new ref=" + ref + " to looper=" + uil.mLooper);
            mListenersHolders.get(index).list.add(ref);
        } else {
            if (DEBUG) Log.d(TAG, "Creating a new ref=" + ref + " of looper=" + uil.mLooper);
            HandlerHolder hh = new HandlerHolder(uil.mLooper);
            hh.list.add(ref);
            mListenersHolders.add(hh);
        }
    }

    private void removeListenerRef(@NonNull Reference<OnConfigChangedListener> ref) {
        mListenersRefs.remove(ref);

        final OnConfigChangedListener listener = ref.get();
        if (listener != null && !(listener instanceof UiThreadedConfigChangedListener)) return;
        final int length = mListenersHolders.size();
        for (int i = 0; i < length; i++) {
            final HandlerHolder hh = mListenersHolders.get(i);
            if (hh.list.contains(ref)) {
                hh.list.remove(ref);
                if (DEBUG) Log.d(TAG, "Removed ref=" + ref + " from looper=" + hh);
                if (hh.list.isEmpty()) {
                    if (DEBUG) Log.d(TAG, "Removed looper=" + hh);
                    mListenersHolders.remove(i);
                }
                break;
            }
        }
    }

    /**
     * Interface definition for a callback to be invoked
     * when an option is changed.
     *
     * @author Artem Chepurnoy
     */
    public interface OnConfigChangedListener {

        /**
         * Callback that an option has changed.
         */
        void onConfigChanged(@NonNull Transaction transaction, @NonNull Option option);
    }

    /**
     * @author Artem Chepurnoy
     */
    public static abstract class UiThreadedConfigChangedListener implements
            OnConfigChangedListener {

        @NonNull
        private final Looper mLooper;

        public UiThreadedConfigChangedListener(@NonNull Looper looper) {
            if (looper == null) looper = Looper.getMainLooper();
            mLooper = looper;
        }

        /**
         * {@inheritDoc}
         */
        // This may be called on a wrong looper.
        @Override
        public void onConfigChanged(@NonNull final Transaction transaction,
                                    @NonNull final Option option) { /* empty */ }

        public abstract boolean onKeyCheck(@NonNull String key);

        /**
         * Same as {@link #onConfigChanged(Transaction, Option)} but may be called only on
         * a set {@link Looper looper} when the option fits {@link #onKeyCheck(String)}.
         */
        public abstract void onUiThreadedConfigChanged(
                @NonNull Transaction transaction,
                @NonNull Option option);
    }

    /**
     * A simple {@link Handler}. class that checks equality by only its
     * internal {@link Looper looper}. Note that {@link #equals(Object)}
     * methods works if you pass a {@link Looper looper} to it.
     *
     * @author Artem Chepurnoy
     */
    private static class HandlerHolder extends Handler {

        @NonNull
        final List<Reference<OnConfigChangedListener>> list;

        public HandlerHolder(@NonNull Looper looper) {
            super(looper);
            list = new ArrayList<>();
        }

        @Override
        public int hashCode() {
            return getLooper().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (o == this) return true;

            final Looper looper;
            if (o instanceof HandlerHolder) {
                HandlerHolder hh = (HandlerHolder) o;
                looper = hh.getLooper();
            } else if (o instanceof Looper) {
                looper = (Looper) o;
            } else return false;

            return getLooper().equals(looper);
        }
    }

    //-- OTHER ----------------------------------------------------------------

    /**
     * @author Artem Chepurnoy
     */
    public final static class Change {

        @NonNull
        public final String key;
        @NonNull
        public final Object value;
        @Nullable
        public final OnConfigChangedListener listenerIgnored;

        public Change(@NonNull String key, @NonNull Object value,
                      @Nullable OnConfigChangedListener listenerIgnored) {
            this.key = key;
            this.value = value;
            this.listenerIgnored = listenerIgnored;
        }
    }

    //-- TRANSACTION ----------------------------------------------------------

    /**
     * @author Artem Chepurnoy
     */
    public final class Transaction {

        private Context mContext;
        private SharedPreferences.Editor mEditor;

        private final boolean mFake;

        public Transaction() {
            this(false);
        }

        public Transaction(boolean fake) {
            mFake = fake;
        }

        @SuppressLint("CommitPrefEdits")
        @NonNull
        public Transaction beginTransaction(@NonNull Context context) {
            mContext = context.getApplicationContext();
            mEditor = getSharedPreferences(mContext).edit();
            return this;
        }

        @NonNull
        public Transaction put(@NonNull Change change) {
            return put(getOption(change.key), change.value, change.listenerIgnored);
        }

        @NonNull
        public Transaction put(final @NonNull Option option, @NonNull Object value,
                               final @Nullable OnConfigChangedListener listenerIgnored) {
            if (option.getValue().equals(value)) return this; // No need to put an equal value
            // Set the value
            Log.d(TAG, "Saving `" + option.key + "`=`" + value + "`");
            option.setValue(value);
            onConfigChanged(this, option);
            // Notify listeners
            synchronized (mListenersRefs) {
                for (int i = mListenersRefs.size() - 1; i >= 0; i--) {
                    Reference<OnConfigChangedListener> ref = mListenersRefs.get(i);
                    OnConfigChangedListener l = ref.get();

                    if (l == null) {
                        // There were no links to this listener except
                        // our class.
                        Log.w(TAG, "Deleting an addled listener..!");
                        removeListenerRef(ref);
                    } else if (l != listenerIgnored) {
                        l.onConfigChanged(this, option);
                    }
                }

                final int li = mListenersHolders.size();
                for (int i = 0; i < li; i++) {
                    final HandlerHolder hh = mListenersHolders.get(i);
                    final List<Reference<OnConfigChangedListener>> list = hh.list;

                    // Check if we really need to notify about this event
                    // somebody there.
                    boolean post = false;
                    for (Reference<OnConfigChangedListener> ref : list) {
                        OnConfigChangedListener l = ref.get();
                        if (l != null) {
                            UiThreadedConfigChangedListener uil = (UiThreadedConfigChangedListener) l;
                            if (uil.onKeyCheck(option.key)) {
                                post = true;
                                break; // No need to check more.
                            }
                        }
                    }

                    if (!post) continue;
                    hh.post(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (mListenersRefs) {
                                // You may have unregistered it.
                                for (Reference<OnConfigChangedListener> ref : list) {
                                    OnConfigChangedListener l = ref.get();
                                    if (l == null || l == listenerIgnored) continue;
                                    UiThreadedConfigChangedListener uil = (UiThreadedConfigChangedListener) l;
                                    if (!uil.onKeyCheck(option.key)) continue;
                                    uil.onUiThreadedConfigChanged(Transaction.this, option);
                                    Log.d(TAG, "Notifying from looper=" + uil.mLooper + " l=" + Looper.myLooper());
                                }
                            }
                        }
                    });
                }
            }

            if (mFake) return this;
            if (value instanceof Boolean) {
                mEditor.putBoolean(option.key, (Boolean) value);
            } else if (value instanceof Integer) {
                mEditor.putInt(option.key, (Integer) value);
            } else if (value instanceof Float) {
                mEditor.putFloat(option.key, (Float) value);
            } else if (value instanceof String) {
                mEditor.putString(option.key, (String) value);
            } else if (value instanceof Long) {
                mEditor.putLong(option.key, (Long) value);
            } else throw new IllegalArgumentException("Unknown value\'s type.");
            return this;
        }

        @NonNull
        public Transaction clear() {
            mEditor.clear();
            return this;
        }

        @NonNull
        public Transaction endTransaction() {
            mEditor.apply();
            return this;
        }

        // ////////////////////
        // Additional stuff
        // ////////////////////

        @NonNull
        public Context getContext() {
            return mContext;
        }
    }

    //-- OPTION ---------------------------------------------------------------

    /**
     * One single option that may be synced with preference.
     *
     * @author Artem Chepurnoy
     */
    public static class Option {

        @NonNull
        public final Class clazz;
        @NonNull
        public final String key;
        // Defaults
        public final Object defaultValue;
        public final int defaultRes;
        // Sdk
        public final int minSdkVersion;
        public final int maxSdkVersion;

        private volatile Object mValue;

        public Option(@NonNull Class clazz, @NonNull String key,
                      Object defaultValue, int defaultRes,
                      int minSdkVersion, int maxSdkVersion) {
            this.clazz = clazz;
            this.key = key;
            this.defaultValue = defaultValue;
            this.defaultRes = defaultRes;
            this.minSdkVersion = minSdkVersion;
            this.maxSdkVersion = maxSdkVersion;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return new HashCodeBuilder(11, 31)
                    .append(clazz)
                    .append(key)
                    .append(defaultValue)
                    .append(defaultRes)
                    .append(minSdkVersion)
                    .append(maxSdkVersion)
                    .append(mValue)
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
                    .append(clazz, option.clazz)
                    .append(key, option.key)
                    .append(defaultValue, option.defaultValue)
                    .append(defaultRes, option.defaultRes)
                    .append(minSdkVersion, option.minSdkVersion)
                    .append(maxSdkVersion, option.maxSdkVersion)
                    .append(mValue, option.mValue)
                    .isEquals();
        }

        public void setValue(@NonNull Object value) {
            mValue = value;
        }

        @NonNull
        public Object getValue() {
            return mValue;
        }

        // ////////////////////
        // Additional stuff
        // ////////////////////

        /**
         * Extracts and returns the default option's value.
         */
        @NonNull
        public final Object getDefault(@NonNull Resources resources) {
            // check if defaultValue already set
            if (defaultValue == null) {
                if (defaultRes != -1) {
                    if (boolean.class.isAssignableFrom(clazz)) {
                        return resources.getBoolean(defaultRes);
                    } else if (int.class.isAssignableFrom(clazz)) {
                        return resources.getInteger(defaultRes);
                    } else if (float.class.isAssignableFrom(clazz)) {
                        // Assuming it's a dimension, but not a fraction.
                        return resources.getDimension(defaultRes);
                    } else if (String.class.isAssignableFrom(clazz)) {
                        return resources.getString(defaultRes);
                    } else throw new IllegalArgumentException("Unknown option\'s type.");
                } else throw new IllegalStateException();
            }
            return defaultValue;
        }

        /**
         * @author Artem Chepurnoy
         */
        public static class Builder {

            private final Class mClass;
            private final String mKey;
            // Defaults
            private Object mDefaultValue;
            private int mDefaultRes = -1;
            // Sdk
            private int mMinSdkVersion = Integer.MIN_VALUE + 1;
            private int mMaxSdkVersion = Integer.MAX_VALUE - 1;

            public Builder(@NonNull Class clazz, @NonNull String key) {
                mClass = clazz;
                mKey = key;
            }

            @NonNull
            public Builder setDefault(@NonNull Object value) {
                mDefaultValue = value;
                return this;
            }

            @NonNull
            public Builder setDefaultRes(int resource) {
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
            public Builder setMinSdkVersion(int version) {
                mMinSdkVersion = version;
                return this;
            }

            /**
             * Sets maximum {@link android.os.Build.VERSION#SDK_INT sdk version} of this
             * option. This option shouldn't be shown on newer systems.
             *
             * @see #setMinSdkVersion(int)
             */
            @NonNull
            public Builder setMaxSdkVersion(int version) {
                mMaxSdkVersion = version;
                return this;
            }

            /**
             * Bakes this builder into an option.
             */
            @NonNull
            public Option build() {
                return new Option(mClass, mKey,
                        mDefaultValue, mDefaultRes,
                        mMinSdkVersion, mMaxSdkVersion);
            }

        }

    }

    //-- SYNCER ---------------------------------------------------------------

    /**
     * A class for syncing an {@link CfgBase.Option} with
     * corresponding {@link Preference}.
     *
     * @author Artem Chepurnoy
     */
    public final static class Syncer {

        private final CfgBase mCfg;
        private final ArrayList<Item> mItems;
        private final Context mContext;
        private boolean mBroadcastingPref;
        private boolean mStarted;

        @NonNull
        private final Handler mHandler = new Handler(Looper.getMainLooper());

        @NonNull
        private final Preference.OnPreferenceChangeListener mPreferenceListener =
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (mBroadcastingPref) {
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
                        Change change = new Change(item.option.key, newValue, mConfigListener);
                        item.cfg.put(mContext, change);
                        // Update preference's summary
                        item.setter.updateSummary(item.preference, item.option, newValue);
                        return true;
                    }
                };

        @NonNull
        private final OnConfigChangedListener mConfigListener =
                new OnConfigChangedListener() {
                    @Override
                    public void onConfigChanged(@NonNull Transaction transaction,
                                                @NonNull Option option) {
                        Item item = null;
                        for (Item c : mItems) {
                            if (option.key.equals(c.preference.getKey())) {
                                item = c;
                                break;
                            }
                        }

                        if (item == null) {
                            return;
                        }

                        setPreferenceValue(item, option.getValue());
                    }
                };

        public Syncer(@NonNull Context context, @NonNull CfgBase cfg) {
            mCfg = cfg;
            mItems = new ArrayList<>(10);
            mContext = context;
        }

        private void setPreferenceValue(final @NonNull Item item, final @NonNull Object value) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mBroadcastingPref = true;
                    item.setter.setValue(item.preference, item.option, value);
                    item.setter.updateSummary(item.preference, item.option, value);
                    mBroadcastingPref = false;
                }
            });
        }

        public void sync(
                @Nullable PreferenceScreen ps,
                @NonNull Preference preference,
                @NonNull Setter setter) {

            Item item = new Item(preference, setter, mCfg);
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
         * @see #sync(PreferenceScreen, Preference, Setter)
         */
        public void start() {
            mStarted = true;
            if (mItems.size() > 0) {
                mItems.get(0).cfg.registerListener(mConfigListener);
                for (Item item : mItems) startListeningToItem(item);
            }
        }

        private void startListeningToItem(@NonNull Item item) {
            item.preference.setOnPreferenceChangeListener(mPreferenceListener);
            setPreferenceValue(item, item.option.getValue());
        }

        /**
         * Stops to listen to the changes.
         *
         * @see #start()
         */
        public void stop() {
            mStarted = false;
            mCfg.unregisterListener(mConfigListener);
            for (Item item : mItems) item.preference.setOnPreferenceChangeListener(null);
        }

        // ////////////////////
        // Additional stuff
        // ////////////////////

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
         * A class-merge of {@link Preference}
         * and its {@link Option} and its {@link Syncer.Setter}.
         *
         * @author Artem Chepurnoy
         */
        private final static class Item {

            @NonNull
            final CfgBase cfg;
            @NonNull
            final Preference preference;
            @NonNull
            final Setter setter;
            @NonNull
            final Option option;

            public Item(@NonNull Preference preference,
                        @NonNull Setter setter,
                        @NonNull CfgBase cfg) {
                this.preference = preference;
                this.setter = setter;
                this.cfg = cfg;
                this.option = cfg.getOption(preference.getKey());
            }
        }
    }

}