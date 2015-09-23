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
package com.achep.base.interfaces;

/**
 * Created by Artem Chepurnoy on 08.01.2015.
 */
public interface ITimeout {

    /**
     * Same as calling {@link #set(int, boolean)} with {@code override = false}.
     *
     * @see #set(int, boolean)
     * @see #resume()
     * @see #clear()
     */
    void set(final int delay);

    /**
     * Configures the timeout.
     *
     * @param override {@code true} to rewrite previous timeout\'s time, {@code false} to
     *                 set the nearest one.
     * @see #set(int)
     * @see #resume()
     * @see #clear()
     */
    void set(final int delay, boolean override);

    /**
     * Pauses the timeout.
     *
     * @see #resume()
     */
    void pause();

    /**
     * Resumes the timeout (does nothing if the timeout if cleared).
     *
     * @see #set(int, boolean)
     * @see #pause()
     * @see #clear()
     */
    void resume();

    /**
     * Clears the timeout.
     *
     * @see #set(int, boolean)
     * @see #pause()
     */
    void clear();
}
