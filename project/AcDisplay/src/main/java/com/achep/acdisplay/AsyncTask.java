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

package com.achep.acdisplay;

/**
 * A better {@link com.achep.acdisplay.AsyncTask}.
 *
 * @author Artem Chepurnoy
 */
public abstract class AsyncTask<A, B, C> extends android.os.AsyncTask<A, B, C> {

    protected volatile boolean running = true;

    /**
     * Equals to calling: {@code AsyncTask.getStatus().equals(AsyncTask.Status.FINISHED)}
     */
    public boolean isFinished() {
        return getStatus().equals(Status.FINISHED);
    }

    public void cancel() {
        running = false;
        cancel(false);
    }

}
