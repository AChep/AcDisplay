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
 */
public abstract class SharedList<V, T extends SharedList.Saver<V>> {

    private static final String TAG = "SharedList";

    public static final String KEY_NUMBER = "__n";
    public static final String KEY_USED_ITEM = "__used_";

    private final HashMap<V, Integer> mList;
    private final ArrayList<Integer> mPlaceholder;
    private final Comparator<V> mComparator;
    private final T mSaver;

    private ArrayList<OnSharedListChangedListener<V>> mListeners;

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

    public void addOnSharedListChangedListener(OnSharedListChangedListener<V> listener) {
        Log.i(TAG, "Registered new listener: (" + mListeners.size() + ")" + listener);
        mListeners.add(listener);
    }

    public void removeOnSharedListChangedListener(OnSharedListChangedListener<V> listener) {
        Log.i(TAG, "Unregistered listener: (" + mListeners.size() + ")" + listener);
        mListeners.remove(listener);
    }

    protected SharedList(Context context, Class<T> clazz) {
        mList = new HashMap<>();
        mPlaceholder = new ArrayList<>(3);
        mListeners = new ArrayList<>(6);
        mComparator = onCreateComparator();

        try {
            mSaver = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException("Given class doesn't exist or/and I don\'t like you.");
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

    protected abstract String getPreferencesFileName();

    /**
     * May be null.
     */
    protected Comparator<V> onCreateComparator() {
        return null;
    }

    public Comparator<V> getComparator() {
        return mComparator;
    }

    protected boolean isOverwriteAllowed(V object) {
        return false;
    }

    // ///////// -- CORE CODE -- ///////////

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

    /**
     * Notifies listener about Remove event
     */
    protected void notifyOnRemoved(V objectRemoved, OnSharedListChangedListener l) {
        for (OnSharedListChangedListener<V> listener : mListeners) {
            if (listener == l) continue;
            listener.onRemoved(objectRemoved);
        }
    }

    public V put(Context context, V object) {
        return put(context, object, null);
    }

    public V put(Context context, V object, OnSharedListChangedListener l) {
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
     * Notifies listener about Put event
     *
     * @param object new object
     * @param old    old object from the list
     */
    protected void notifyOnPut(V object, V old, OnSharedListChangedListener l) {
        int diff = mComparator != null ? mComparator.compare(object, old) : 0;
        for (OnSharedListChangedListener<V> listener : mListeners) {
            if (listener == l) continue;
            listener.onPut(object, old, diff);
        }
    }

    public boolean contains(V object) {
        return mList.containsKey(object);
    }

    public Set<V> valuesSet() {
        return mList.keySet();
    }

    // ///////// -- CLASSES-- ///////////

    /**
     * Additional class to provide diffs between "old" and "new" objects.
     */
    public static abstract class Comparator<V> {
        public abstract int compare(V object, V old);
    }

    // I could use Parcelable for that too.
    public static abstract class Saver<V> {

        /**
         * Should not overwrite value at {@link com.achep.activedisplay.blacklist.SharedList#KEY_NUMBER} or/and
         * has key which starts with {@link com.achep.activedisplay.blacklist.SharedList#KEY_USED_ITEM}!
         */
        public abstract SharedPreferences.Editor put(V object, SharedPreferences.Editor editor, int position);

        public abstract V get(SharedPreferences prefs, int position);

    }

}
