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
package com.achep.base.interfaces;

/**
 * @author Artem Chepurnoy
 */
public interface IOnLowMemory {

    /**
     * This is called when the overall system is running low on memory, and actively running
     * processes should trim their memory usage. While the exact point at which this
     * will be called is not defined, generally it will happen when all background process
     * have been killed. That is, before reaching the point of killing processes hosting service
     * and foreground UI that we would like to avoid killing.
     * You should implement this method to release any caches or other unnecessary resources you
     * may be holding on to. The system will perform a garbage collection for you after returning
     * from this method.
     */
    void onLowMemory();

}
