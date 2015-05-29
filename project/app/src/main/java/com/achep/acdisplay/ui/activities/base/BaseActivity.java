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
package com.achep.acdisplay.ui.activities.base;

import android.support.annotation.NonNull;

import com.achep.acdisplay.Config;
import com.achep.base.ui.activities.ActivityBase;

/**
 * A base of all and every activities.
 *
 * @author Artem Chepurnoy
 */
public abstract class BaseActivity extends ActivityBase {

    /**
     * @return a config instance used for storing different
     * options and triggers.
     */
    @NonNull
    public Config getConfig() {
        return Config.getInstance();
    }

}
