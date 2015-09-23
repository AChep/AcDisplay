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

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Created by Artem Chepurnoy on 27.01.2015.
 */
public interface IPermission extends IOnLowMemory {

    /**
     * @return {@code true} if the permission is granted and you may use its power right now,
     * {@code false} otherwise.
     */
    boolean isGranted();

    /**
     * @return {@code true} if the permission is possible to give,
     * {@code false} otherwise.
     */
    boolean exists(@NonNull Context context);

}
