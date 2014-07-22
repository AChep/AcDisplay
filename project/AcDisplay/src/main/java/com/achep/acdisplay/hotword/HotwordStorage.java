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

package com.achep.acdisplay.hotword;

import android.content.Context;

import com.achep.acdisplay.blacklist.SharedList;
import com.achep.acdisplay.fragments.HotwordFragment;

/**
* Created by achep on 25.06.14.
*/
public class HotwordStorage extends SharedList<Hotword, Hotword.Saver> {

    private static HotwordStorage sHotwordStorage;

    public static synchronized HotwordStorage getInstance(Context context) {
        if (sHotwordStorage == null) {
            sHotwordStorage = new HotwordStorage(context);
        }
        return sHotwordStorage;
    }

    public HotwordStorage(Context context) {
        super(context);
    }

    @Override
    protected String getPreferencesFileName() {
        return "hotwords";
    }

    @Override
    protected Hotword.Saver onCreateSaver() {
        return new Hotword.Saver();
    }
}
