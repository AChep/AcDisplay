/*
 * Copyright (C) 2015 AChep@xda <artemchep@gmail.com>
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
package com.achep.base.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Artem Chepurnoy on 02.03.2015.
 */
public abstract class RefCacheBase<T> {

    @NonNull
    private final Map<CharSequence, Reference<T>> mCache = new ConcurrentHashMap<>();

    public final void put(@NonNull CharSequence key, @NonNull T object) {
        synchronized (this) {
            mCache.put(key, onCreateReference(object));
        }
    }

    @Nullable
    public final T remove(@NonNull CharSequence key) {
        synchronized (this) {
            Reference<T> ref = mCache.remove(key);
            if (ref == null) {
                cleanDeadEntries();
                return null;
            }
            return ref.get();
        }
    }

    @Nullable
    public final T get(@NonNull CharSequence key) {
        synchronized (this) {
            Reference<T> ref = mCache.get(key);
            if (ref == null) {
                cleanDeadEntries();
                return null;
            }
            return ref.get();
        }
    }

    private void cleanDeadEntries() {
        ArrayList<CharSequence> deaths = null;

        // Find empty links
        for (Map.Entry<CharSequence, Reference<T>> entry : mCache.entrySet()) {
            if (entry.getValue().get() == null) {
                if (deaths == null) {
                    deaths = new ArrayList<>();
                }
                deaths.add(entry.getKey());
            }
        }

        // Clean-up
        if (deaths != null) {
            for (CharSequence key : deaths) {
                mCache.remove(key);
            }
        }
    }

    /**
     * Removes all elements from this cache, leaving it empty.
     */
    public void clear() {
        synchronized (this) {
            mCache.clear();
        }
    }

    @NonNull
    protected abstract Reference<T> onCreateReference(@NonNull T object);

}
