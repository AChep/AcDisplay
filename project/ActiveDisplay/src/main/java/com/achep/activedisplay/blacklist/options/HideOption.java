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
package com.achep.activedisplay.blacklist.options;

import android.content.Context;
import android.widget.CompoundButton;

import com.achep.activedisplay.Operator;
import com.achep.activedisplay.R;
import com.achep.activedisplay.blacklist.AppConfig;
import com.achep.activedisplay.blacklist.BlacklistEnabler;

/**
 * Created by Artem on 28.02.14.
 */
public class HideOption extends Option {

    public HideOption(Context context, CompoundButton cb, BlacklistEnabler enabler) {
        super(context, cb, enabler,
                context.getResources().getDrawable(R.drawable.ic_settings_hide_notifies),
                context.getResources().getString(R.string.blacklist_app_hide_title),
                context.getResources().getString(R.string.blacklist_app_hide_summary));
    }

    @Override
    public boolean[] extractVariable(AppConfig config) {
        return config.hidden;
    }

    @Override
    public boolean isChanged(int diff) {
        return Operator.bitandCompare(diff, AppConfig.DIFF_HIDDEN);
    }
}
