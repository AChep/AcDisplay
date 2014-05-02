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
package com.achep.activedisplay.blacklist;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Simple list which automatically saves items to private storage and restores on initialize.
 * This may be useful for implementing blacklists or something fun.
 *
 * @author Artem Chepurnoy
 */
public abstract class SharedList<V, T extends SharedList.Saver<V>> {

    private static final String TAG = "SharedList";

    /**
     * Key's prefix for SharedList's internal usage.
     */
    public static final String KEY_PREFIX = "__";
    public static final String KEY_NUMBER = KEY_PREFIX + "n";
    public static final String KEY_USED_ITEM = KEY_PREFIX + "used_";

    private final HashMap<V, Integer> mList;
    private final ArrayList<Integer> mPlaceholder;
    private final Comparator<V> mComparator;
    private final T mSaver;

    private ArrayList<OnSharedListChangedListener<V>> mListeners;

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
         * @param diff      the difference between old and new objects (provided by {@link com.achep.activedisplay.blacklist.SharedList.Comparator})
         */
        public void onPut(V objectNew, V objectOld, int diff);

        /**
         * Called on object removed from the list.
         *
         * @param objectRemoved removed object from the list
         */
        public void onRemoved(V objectRemoved);
    }

    /**
     * The provider of the diffs between "old" and "new" objects in list.
     *
     * @see OnSharedListChangedListener#onPut(Object, Object, int)
     * @author Artem Chepurnoy
     */
    public static abstract class Comparator<V> {

        /**
         * Compares old and new object and returns the difference between them.
         *
         * @return The difference between old and new objects.
         */
        public abstract int compare(V object, V old);
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
        public abstract SharedPreferences.Editor put(V object, SharedPreferences.Editor editor, int position);

        /**
         * Restores previously save Object from shared preferences.
         *
         * @param position position of given object in list
         * @see #put(Object, android.content.SharedPreferences.Editor, int)
         */
        public abstract V get(SharedPreferences prefs, int position);

    }

    /**
     * Note, that you must unregister your listener lately.
     *
     * @see #unregisterListener(SharedList.OnSharedListChangedListener)
     * @see SharedList.OnSharedListChangedListener
     */
    public void registerListener(OnSharedListChangedListener<V> listener) {
        mListeners.add(listener);
    }

    /**
     * Unregisters previously registered listener.
     *
     * @see #registerListener(SharedList.OnSharedListChangedListener)
     * @see SharedList.OnSharedListChangedListener
     */
    public void unregisterListener(OnSharedListChangedListener<V> listener) {
        mListeners.remove(listener);
    }

    protected SharedList(Context context) {
        mList = new HashMap<>();
        mPlaceholder = new ArrayList<>(3);
        mListeners = new ArrayList<>(6);
        mComparator = onCreateComparator();
        mSaver = onCreateSaver();

        if (mSaver == null) {
            throw new NullPointerException("The saver of SharedList may not be null!");
        }

        SharedPreferences prefs = getSharedPreferences(context);
        final int n = prefs.getInt(KEY_NUMBER, 0) + 1;
        for (int i = 0; i < n; i++) {
            boolean used = prefs.getBoolean(KEY_USED_ITEM + i, false);
            if (used) {
                mList.put(mSaver.get(prefs, i), i);
            } else {
                mPlaceholder.add(i);
            }
        }
    }

    private SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(getPreferencesFileName(), Context.MODE_PRIVATE);
    }

    /**
     * @return the name of the shared list's file.
     * @see #getSharedPreferences(android.content.Context)
     */
    protected abstract String getPreferencesFileName();

    /**
     * @return Instance of saver which will save your Object to shared preferences.
     * @see Saver
     */
    protected abstract T onCreateSaver();

    /**
     * @return The comparator of this shared list (may be null.)
     * @see OnSharedListChangedListener#onPut(Object, Object, int)
     * @see #put(android.content.Context, Object)
     * @see #put(android.content.Context, Object, OnSharedListChangedListener)
     * @see #getComparator()
     */
    protected Comparator<V> onCreateComparator() {
        return null;
    }

    /**
     * @return Previously created comparator.
     * @see #onCreateComparator()
     */
    public Comparator<V> getComparator() {
        return mComparator;
    }

    protected boolean isOverwriteAllowed(V object) {
        return false;
    }

    public void remove(Context context, V object) {
        remove(context, object, null);
    }

    public void remove(Context context, V object, OnSharedListChangedListener l) {
        if (!mList.containsKey(object)) {
            Log.w(TAG, "Tried to remove non-existing object from the list.");
            return;
        }

        V objectRemoved = null;
        for (V o : mList.keySet()) {
            if (o.equals(object)) {
                objectRemoved = o;
                break;
            }
        }

        Integer value = mList.remove(object);

        int i = 0;
        int length = mPlaceholder.size();
        for (; i < length; i++)
            if (mPlaceholder.get(i) > value)
                break;
        mPlaceholder.add(i, value);

        getSharedPreferences(context).edit()
                .putBoolean(KEY_USED_ITEM + value, false)
                .apply();

        notifyOnRemoved(objectRemoved, l);
    }

    public V put(Context context, V object) {
        return put(context, object, null);
    }

    public V put(Context context, V object, OnSharedListChangedListener l) {
        if (object == null) {
            throw new IllegalArgumentException();
        }

        boolean growUp = mPlaceholder.size() == 0;
        int value = growUp ? mList.size() : mPlaceholder.get(0);

        V old = null;
        if (mList.containsKey(object)) {
            if (!isOverwriteAllowed(object)) {
                Log.w(TAG, "Trying to put an existing object to the list.");
                return null;
            }

            for (V o : mList.keySet()) {
                if (o.equals(object)) {
                    old = o;
                    break;
                }
            }
            value = mList.get(object);
            mList.remove(object);
        }

        mList.put(object, value);

        SharedPreferences.Editor editor = mSaver
                .put(object, getSharedPreferences(context).edit(), value)
                .putBoolean(KEY_USED_ITEM + value, true);
        if (growUp) editor.putInt(KEY_NUMBER, value);
        editor.apply();

        notifyOnPut(object, old, l);
        return old;
    }

    /**
     * Notifies {@link #registerListener(OnSharedListChangedListener) registered} listeners
     * about removed from list object.
     *
     * @param objectRemoved removed object from the list
     * @param l             Listener that will be ignored while notifying.
     */
    protected void notifyOnRemoved(V objectRemoved, OnSharedListChangedListener l) {
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
    protected void notifyOnPut(V object, V old, OnSharedListChangedListener l) {
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
    public boolean contains(V object) {
        return mList.containsKey(object);
    }

    public Set<V> valuesSet() {
        return mList.keySet();
    }

}
