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
package com.achep.acdisplay.services.media;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;

import com.achep.base.Device;

/**
 * {@inheritDoc}
 */
class MediaController2JellyBean extends MediaController2KitKat {

    /**
     * {@inheritDoc}
     */
    protected MediaController2JellyBean(@NonNull Activity activity) {
        super(activity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendMediaAction(int action) {
        broadcastMediaAction(mContext, action);
    }

    /**
     * Emulates hardware buttons' click via broadcast system.
     *
     * @see android.view.KeyEvent
     */
    public static void broadcastMediaAction(Context context, int action) {
        int keyCode;
        switch (action) {
            case ACTION_PLAY_PAUSE:
                keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
                break;
            case ACTION_STOP:
                keyCode = KeyEvent.KEYCODE_MEDIA_STOP;
                break;
            case ACTION_SKIP_TO_NEXT:
                keyCode = KeyEvent.KEYCODE_MEDIA_NEXT;
                break;
            case ACTION_SKIP_TO_PREVIOUS:
                keyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS;
                break;
            default:
                Log.d(TAG, "Received unknown media action(" + action + ").");
                return;
        }

        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        KeyEvent keyDown = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        KeyEvent keyUp = new KeyEvent(KeyEvent.ACTION_UP, keyCode);

        if (Device.hasKitKatApi()) Log.i(TAG, "Broadcasting this (" + action + ") media action.");
        context.sendOrderedBroadcast(intent.putExtra(Intent.EXTRA_KEY_EVENT, keyDown), null);
        context.sendOrderedBroadcast(intent.putExtra(Intent.EXTRA_KEY_EVENT, keyUp), null);
    }

}
