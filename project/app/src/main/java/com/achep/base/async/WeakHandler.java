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
package com.achep.base.async;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;

import java.lang.ref.WeakReference;

import timber.log.Timber;

/**
 * Created by Artem Chepurnoy on 25.01.2015.
 */
public abstract class WeakHandler<A> extends Handler {

    private static final String TAG = "WeakHandler";
    @NonNull
    private WeakReference<A> mWeakRef;

    public WeakHandler(@NonNull A object) {
        mWeakRef = new WeakReference<>(object);
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        A object = mWeakRef.get();
        if (object == null) {
            Timber.tag(TAG).w("Weak reference has died!"
                    + " class=" + mWeakRef.getClass()
                    + " message=" + msg.toString());
            return;
        }

        onHandleMassage(object, msg);
    }

    protected abstract void onHandleMassage(@NonNull A object, Message msg);

}
