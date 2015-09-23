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
package com.achep.base.tests;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A class for performing <i>oblivious</i> checks.
 *
 * @author Artem Chepurnoy
 */
@SuppressWarnings("SameParameterValue")
public abstract class Check {

    @Nullable
    private static Check sCheck;

    @NonNull
    public static Check getInstance() {
        if (sCheck == null) {
            sCheck = new CheckImpl();
        }
        return sCheck;
    }

    /**
     * Does nothing if the incoming param is {@code true}, crashes otherwise.
     *
     * @throws java.lang.RuntimeException
     * @see #isTrue(int)
     * @see #isFalse(boolean)
     */
    public abstract void isTrue(boolean bool);

    public abstract void isTrue(boolean bool, @NonNull String message);

    /**
     * Does nothing if the incoming param is not zero, crashes otherwise.
     *
     * @throws java.lang.RuntimeException
     * @see #isTrue(boolean)
     */
    public abstract void isTrue(int value);

    public abstract void isTrue(int value, @NonNull String message);

    /**
     * Does nothing if the incoming param is {@code false}, crashes otherwise. It's an opposite
     * to {@link #isTrue(boolean)}
     *
     * @throws java.lang.RuntimeException
     * @see #isTrue(boolean)
     */
    public abstract void isFalse(boolean bool);

    public abstract void isFalse(boolean bool, @NonNull String message);

    /**
     * Does nothing if the incoming param is {@code null}, crashes otherwise. It's an opposite
     * to {@link #isTrue(boolean)}
     *
     * @throws java.lang.RuntimeException
     * @see #isNonNull(Object)
     */
    public abstract void isNull(@Nullable Object object);

    public abstract void isNull(@Nullable Object object, @NonNull String message);

    /**
     * Does nothing if the incoming param is not {@code null}, crashes otherwise. It's an opposite
     * to {@link #isNull(Object)}
     *
     * @throws java.lang.RuntimeException
     * @see #isNull(Object)
     */
    public abstract void isNonNull(@NonNull Object object);

    public abstract void isNonNull(@NonNull Object object, @NonNull String message);

    /**
     * Does nothing if run on the {@link android.os.Looper#getMainLooper() main thread},
     * crashes otherwise.
     *
     * @throws java.lang.RuntimeException
     */
    public abstract void isInMainThread();

    public abstract void isInMainThread(@NonNull String message);
}
