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

import java.lang.ref.Reference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Artem Chepurnoy on 02.03.2015.
 */
public abstract class RefCacheBase<T> {

    private volatile Map<CharSequence, Reference<T>> mCache = new ConcurrentHashMap<>();

    public final void put(@NonNull CharSequence key, @NonNull T object) {
        mCache.put(key, onCreateReference(object));
    }

    public final T get(@NonNull CharSequence key) {
        Reference<T> ref = mCache.get(key);
        if (ref == null) return null;
        return ref.get();
    }

    @NonNull
    protected abstract Reference<T> onCreateReference(@NonNull T object);

}
