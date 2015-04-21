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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.achep.base.interfaces.IBackupable;
import com.achep.base.interfaces.IOnLowMemory;
import com.achep.base.interfaces.ISubscriptable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import static com.achep.base.Build.DEBUG;

/**
 * Simple list which automatically saves items to private storage and restores on initialize.
 * This may be useful for implementing blacklists or something fun.
 *
 * @author Artem Chepurnoy
 */
public abstract class SharedList<V, T extends SharedList.Saver<V>> implements
        ISubscriptable<SharedList.OnSharedListChangedListener<V>>,
        IOnLowMemory, Iterable<V>, IBackupable {

    private static final String TAG = "SharedList";

    /**
     * Key's prefix for SharedList's internal usage.
     */
    private static final String KEY_PREFIX = "__";

    private static final String KEY_NUMBER = KEY_PREFIX + "n";
    private static final String KEY_USED_ITEM = KEY_PREFIX + "used_";

    private HashMap<V, Integer> mList;
    private ArrayList<Integer> mPlaceholder;

    private ArrayList<OnSharedListChangedListener<V>> mListeners;

    private boolean mRecyclableCreated;
    private Comparator<V> mComparator;
    private T mSaver;

    /**
     * Interface definition for a callback to be invoked
     * when a shared list changed.
     *
     * @author Artem Chepurnoy
     * @see SharedList
     * @see SharedList#registerListener(SharedList.OnSharedListChangedListener)
     * @see SharedList#unregisterListener(SharedList.OnSharedListChangedListener)
     */
    public interface OnSharedListChangedListener<V> {

        /**
         * Called on object put to / replaced in the list.
         *
         * @param objectNew current object
         * @param objectOld old object from the list
         * @param diff      the difference between old and new objects (provided by {@link SharedList.Comparator})
         */
        void onPut(@NonNull V objectNew, @Nullable V objectOld, int diff);

        /**
         * Called on object removed from the list.
         *
         * @param objectRemoved removed object from the list
         */
        void onRemoved(@NonNull V objectRemoved);
    }

    /**
     * The provider of the diffs between "old" and "new" objects in list.
     *
     * @author Artem Chepurnoy
     * @see OnSharedListChangedListener#onPut(Object, Object, int)
     */
    public static abstract class Comparator<V> {

        /**
         * Compares old and new object and returns the difference between them.
         *
         * @return The difference between old and new objects.
         */
        public abstract int compare(@NonNull V object, @Nullable V old);
    }

    /**
     * Skeleton of the saver class which needed to store and get values
     * into the {@link android.content.SharedPreferences}.
     *
     * @author Artem Chepurnoy
     */
    // I could use Parcelable for that too.
    public static abstract class Saver<V> {

        /**
         * Should put object's data to given shared prefs editor.
         * <b>Note:</b> This should not write any values with
         * a key starting with {@link #KEY_PREFIX}!
         *
         * @param position position of given object in list
         * @see #get(android.content.SharedPreferences, int)
         */
        @NonNull
        public abstract SharedPreferences.Editor put(@NonNull V object,
                                                     @NonNull SharedPreferences.Editor editor,
                                                     int position);

        /**
         * Restores previously save Object from shared preferences.
         *
         * @param position position of given object in list
         * @see #put(Object, android.content.SharedPreferences.Editor, int)
         */
        public abstract V get(@NonNull SharedPreferences prefs, int position);

    }

    /**
     * Note, that you must unregister your listener lately.
     *
     * @see #unregisterListener(SharedList.OnSharedListChangedListener)
     * @see SharedList.OnSharedListChangedListener
     */
    @Override
    public void registerListener(@NonNull OnSharedListChangedListener<V> listener) {
        mListeners.add(listener);
    }

    /**
     * Unregisters previously registered listener.
     *
     * @see #registerListener(SharedList.OnSharedListChangedListener)
     * @see SharedList.OnSharedListChangedListener
     */
    @Override
    public void unregisterListener(@NonNull OnSharedListChangedListener<V> listener) {
        mListeners.remove(listener);
    }

    protected SharedList() { /* You must call #init(Context) later! */ }

    protected SharedList(@NonNull Context context) {
        init(context);
    }

    protected void init(@NonNull Context context) {
        mList = new HashMap<>();
        mPlaceholder = new ArrayList<>(3);
        mListeners = new ArrayList<>(6);

        createRecyclableFields();

        // Restore previously saved list.
        SharedPreferences prefs = getSharedPreferences(context);
        final int n = prefs.getInt(KEY_NUMBER, 0);
        for (int i = 0; i < n; i++) {
            if (prefs.getBoolean(KEY_USED_ITEM + i, false)) {
                // Create previously saved object.
                V object = mSaver.get(prefs, i);
                mList.put(object, i);
            } else {
                // This is an empty place which we can re-use
                // later.
                mPlaceholder.add(i);
            }
        }
    }

    @NonNull
    private SharedPreferences getSharedPreferences(@NonNull Context context) {
        return context.getSharedPreferences(getPreferencesFileName(), Context.MODE_PRIVATE);
    }

    /**
     * @return the name of the shared list's file.
     * @see #getSharedPreferences(android.content.Context)
     */
    @NonNull
    protected abstract String getPreferencesFileName();

    /**
     * @return Instance of saver which will save your Object to shared preferences.
     * @see Saver
     */
    @NonNull
    protected abstract T onCreateSaver();

    /**
     * @return The comparator of this shared list (may be null.)
     * @see OnSharedListChangedListener#onPut(Object, Object, int)
     * @see #put(android.content.Context, Object)
     * @see #put(android.content.Context, Object, OnSharedListChangedListener)
     * @see #getComparator()
     */
    @Nullable
    protected Comparator<V> onCreateComparator() {
        return null;
    }

    /**
     * @return Previously created comparator.
     * @see #onCreateComparator()
     */
    @Nullable
    public Comparator<V> getComparator() {
        return mComparator;
    }

    protected boolean isOverwriteAllowed(@NonNull V object) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLowMemory() {
        mRecyclableCreated = false;
        // This probably won't free a lot, but
        // yes, we can do it.
        mComparator = null;
        mSaver = null;
    }

    private void createRecyclableFields() {
        if (mRecyclableCreated & (mRecyclableCreated = true)) return;
        mComparator = onCreateComparator();
        mSaver = onCreateSaver();
    }

    public void remove(@NonNull Context context, V object) {
        remove(context, object, null);
    }

    public void remove(@NonNull Context context, V object, @Nullable OnSharedListChangedListener l) {
        if (!mList.containsKey(object)) {
            Log.w(TAG, "Tried to remove non-existing object from the list.");
            return;
        }

        V objectRemoved = find(object);
        assert objectRemoved != null; // Defined by the condition above
        int pos = mList.remove(object);

        // Put the position of newly removed object
        // to sorted list (keeping it sorted).
        int i = 0;
        final int size = mPlaceholder.size();
        for (; i < size; i++)
            if (mPlaceholder.get(i) > pos)
                break;
        mPlaceholder.add(i, pos);

        // Mark this item as unused, so we can restore placeholders too.
        getSharedPreferences(context).edit()
                .putBoolean(KEY_USED_ITEM + pos, false)
                .apply();

        notifyOnRemoved(objectRemoved, l);
    }

    @Nullable
    public V put(@NonNull Context context, @NonNull V object) {
        return put(context, object, null);
    }

    @Nullable
    public V put(@NonNull Context context, @NonNull V object, @Nullable OnSharedListChangedListener l) {
        boolean growUp = false;
        int pos;

        V old = null;
        if (contains(object)) {
            // This is completely useless if equality-checking
            // method had been implemented correctly (content truly equals).
            if (!isOverwriteAllowed(object)) {
                if (DEBUG) Log.w(TAG, "Trying to put an existing object to the shared list.");
                return null; // Do nothing.
            }

            // Search for an old object...
            old = find(object);

            // Remember the position of old object
            // and pop it out.
            pos = mList.get(old);
            mList.remove(old);
        } else {

            // Increase the size of the list if there no
            // empty place, that we can use.
            growUp = mPlaceholder.size() == 0;

            // Get where-to-save this object.
            if (!growUp) {
                pos = mPlaceholder.get(0);
                mPlaceholder.remove(0);
            } else {
                pos = mList.size();
            }
        }

        mList.put(object, pos);
        createRecyclableFields();

        // Save object to internal memory.
        SharedPreferences.Editor editor = mSaver
                .put(object, getSharedPreferences(context).edit(), pos)
                .putBoolean(KEY_USED_ITEM + pos, true);
        if (growUp) editor.putInt(KEY_NUMBER, mList.size());
        editor.apply();

        notifyOnPut(object, old, l);
        return old;
    }

    @Nullable
    private V find(@NonNull V object) {
        for (V o : mList.keySet()) {
            if (o.equals(object)) {
                return o;
            }
        }
        return null;
    }

    /**
     * Notifies {@link #registerListener(OnSharedListChangedListener) registered} listeners
     * about removed from list object.
     *
     * @param objectRemoved removed object from the list
     * @param l             Listener that will be ignored while notifying.
     */
    protected void notifyOnRemoved(@NonNull V objectRemoved,
                                   @Nullable OnSharedListChangedListener l) {
        for (OnSharedListChangedListener<V> listener : mListeners) {
            if (listener == l) continue;
            listener.onRemoved(objectRemoved);
        }
    }

    /**
     * Notifies {@link #registerListener(OnSharedListChangedListener) registered} listeners
     * that list got one more item / or one item is overwritten.
     *
     * @param object new object
     * @param old    old object from the list
     * @param l      Listener that will be ignored while notifying.
     */
    protected void notifyOnPut(V object, V old, @Nullable OnSharedListChangedListener l) {
        createRecyclableFields();

        int diff = mComparator != null ? mComparator.compare(object, old) : 0;
        for (OnSharedListChangedListener<V> listener : mListeners) {
            if (listener == l) continue;
            listener.onPut(object, old, diff);
        }
    }

    /**
     * Returns whether this list contains the specified object.
     *
     * @return {@code true} if this list contains the specified object, {@code true} otherwise.
     */
    public boolean contains(@Nullable V object) {
        return mList.containsKey(object);
    }

    @NonNull
    public Set<V> values() {
        return mList.keySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<V> iterator() {
        return values().iterator();
    }

    //-- BACKUP ---------------------------------------------------------------

    @Override
    @Nullable
    public String toBackupText() {
        return null;
    }

    @Override
    public boolean fromBackupText(@NonNull Context context, @NonNull String input) {
        return false;
    }
}
