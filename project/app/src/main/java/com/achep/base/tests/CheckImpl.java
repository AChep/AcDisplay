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
 * Created by Artem Chepurnoy on 21.01.2015.
 */
final class CheckImpl extends Check {

    @Override
    public void isTrue(boolean bool) {
        if (!bool) {
            throw new RuntimeException("Should be true!");
        }
    }

    @Override
    public void isTrue(int value) {
        if (value == 0) {
            throw new RuntimeException("Should be not zero!");
        }
    }

    @Override
    public void isFalse(boolean bool) {
        if (bool) {
            throw new RuntimeException("Should be false!");
        }
    }

    @Override
    public void isNull(@Nullable Object object) {
        if (object != null) {
            throw new RuntimeException("Should be null!");
        }
    }

    @Override
    public void isNonNull(@NonNull Object object) {
        if (object == null) {
            throw new RuntimeException("Should be not null!");
        }
    }

    @Override
    public void isInMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new RuntimeException("Should be called on the main thread");
        }
    }

}
