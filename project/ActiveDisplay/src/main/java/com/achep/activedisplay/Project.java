/*
 * Copyright (C) 2013 AChep@xda <artemchep@gmail.com>
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
package com.achep.activedisplay;

import android.content.Context;

/**
 * Created by Artem on 30.12.13.
 */
public class Project {

    @SuppressWarnings("PointlessBooleanExpression")
    public static final boolean DEBUG = BuildConfig.MY_DEBUG && true;

    public static final String EMAIL = "support@artemchep.com";

    public static final String SUFFIX = Project.class.getPackage().getName() + ":";

    public static String getPackageName(Context context) {
        return context.getApplicationInfo().packageName;
    }

}
