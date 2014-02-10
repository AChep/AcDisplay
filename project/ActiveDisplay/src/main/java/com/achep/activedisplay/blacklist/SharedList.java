/*
 * Copyright (C) 2013-2014 AChep@xda <artemchep@gmail.com>
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
 * This is useful for implementing blacklists.
 */
public abstract class SharedList<V, T extends SharedList.Saver<V>> {

    private static final String TAG = "SharedList";

    public static final String KEY_NUMBER = "__n";
    public static final String KEY_USED_ITEM = "__used_";

    private final HashMap<V, Integer> mList;
    private final ArrayList<Integer> mPlaceholder;
    private final T mSaver;

    private ArrayList<OnSharedListChangedListener<V>> mListeners;

    public interface OnSharedListChangedListener<V> {
        public void onPut(V object);

        public void onRemoved(V object);
    }

    public void addOnSharedListChangedListener(OnSharedListChangedListener<V> listener) {
        mListeners.add(listener);
    }

    public void removeOnSharedListChangedListener(OnSharedListChangedListener<V> listener) {
        mListeners.remove(listener);
    }

    protected SharedList(Context context, Class<T> clazz) {
        mList = new HashMap<>();
        mPlaceholder = new ArrayList<>(3);
        mListeners = new ArrayList<>(6);

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

    public synchronized void remove(Context context, V object) {
        Integer value = mList.remove(object);
        if (value == null) {
            Log.w(TAG, "Tried to remove non-existing object from blacklist.");
            return;
        }

        int i = 0;
        int length = mPlaceholder.size();
        for (; i < length; i++)
            if (mPlaceholder.get(i) > value)
                break;
        mPlaceholder.add(i, value);

        getSharedPreferences(context).edit()
                .putBoolean(KEY_USED_ITEM + value, false)
                .apply();

        for (OnSharedListChangedListener<V> listener : mListeners) {
            listener.onRemoved(object);
        }
    }

    public synchronized void put(Context context, V object) {
        if (mList.containsKey(object)) {
            Log.w(TAG, "Trying to put existing object to blacklist.");
            return;
        }

        boolean growUp = mPlaceholder.size() == 0;
        int value = growUp ? mList.size() : mPlaceholder.get(0);
        mList.put(object, value);

        SharedPreferences.Editor editor = mSaver
                .put(object, getSharedPreferences(context).edit(), value)
                .putBoolean(KEY_USED_ITEM + value, true);
        if (growUp) editor.putInt(KEY_NUMBER, value);
        editor.apply();

        for (OnSharedListChangedListener<V> listener : mListeners) {
            listener.onPut(object);
        }
    }

    public synchronized boolean contains(V object) {
        return mList.containsKey(object);
    }

    public synchronized Set<V> valuesSet() {
        return mList.keySet();
    }

    public static abstract class Saver<V> {

        /**
         * Should not overwrite value at {@link SharedList#KEY_NUMBER} or/and
         * has key starts with {@link SharedList#KEY_USED_ITEM}!
         */
        public abstract SharedPreferences.Editor put(V object, SharedPreferences.Editor editor, int position);

        public abstract V get(SharedPreferences prefs, int position);

    }

    public static class StringSaver extends Saver<String> {

        private static final String KEY = "str_";

        @Override
        public SharedPreferences.Editor put(String str, SharedPreferences.Editor editor, int position) {
            return editor.putString(KEY + position, str);
        }

        @Override
        public String get(SharedPreferences prefs, int position) {
            return prefs.getString(KEY + position, null);
        }
    }

}
