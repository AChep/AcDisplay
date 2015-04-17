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
import android.support.annotation.NonNull;

/**
 * {@inheritDoc}
 */
class MediaController2Empty extends MediaController2 {

    /**
     * {@inheritDoc}
     */
    protected MediaController2Empty(@NonNull Activity activity) {
        super(activity);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public MediaController2 asyncWrap() {
        return this; // don't wrap
    }

    /**
     * {@inheritDoc}
     */
    public void sendMediaAction(int action) { /* do nothing */ }

    /**
     * {@inheritDoc}
     */
    @Override
    public void seekTo(long position) { /* do nothing */ }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getPlaybackBufferedPosition() {
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getPlaybackPosition() {
        return -1;
    }

}
