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

import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * @author Artem Chepurnoy
 */
final class CheckImpl extends Check {

    @Override
    public void isTrue(boolean bool) {
        isTrue(bool, "Should be true!");
    }

    @Override
    public void isTrue(boolean bool, @NonNull String message) {
        if (!bool) throw new RuntimeException(message);
    }

    @Override
    public void isTrue(int value) {
        isTrue(value, "Should be not zero!");
    }

    @Override
    public void isTrue(int value, @NonNull String message) {
        if (value == 0) throw new RuntimeException(message);
    }

    @Override
    public void isFalse(boolean bool) {
        isFalse(bool, "Should be false!");
    }

    @Override
    public void isFalse(boolean bool, @NonNull String message) {
        if (bool) throw new RuntimeException(message);
    }

    @Override
    public void isNull(@Nullable Object object) {
        isNull(object, "Should be null!");
    }

    @Override
    public void isNull(@Nullable Object object, @NonNull String message) {
        if (object != null) throw new RuntimeException(message);
    }

    @Override
    public void isNonNull(@NonNull Object object) {
        isNonNull(object, "Should be not null!");
    }

    @Override
    public void isNonNull(@NonNull Object object, @NonNull String message) {
        //noinspection ConstantConditions
        if (object == null) throw new RuntimeException(message);
    }

    @Override
    public void isInMainThread() {
        isInMainThread("Should be called on the main thread");
    }

    @Override
    public void isInMainThread(@NonNull String message) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new RuntimeException(message);
        }
    }

}
