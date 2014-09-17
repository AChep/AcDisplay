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
package com.achep.acdisplay.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;

import com.achep.acdisplay.Device;
import com.achep.acdisplay.R;

/**
 * Created by Artem on 29.01.14.
 */
public class TextView extends android.widget.TextView {

    public TextView(Context context) {
        this(context, null);
    }

    public TextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        if (isInEditMode()) return;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TextView);
        String font = a.getString(R.styleable.TextView_font);

        int sdk = a.getInt(R.styleable.TextView_font_sdk, 20);

        if (!Device.hasTargetApi(sdk) && font != null) {
            if (!font.contains(".")) font += ".ttf"; // Add default font's extension.
            setTypeface(Typeface.createFromAsset(context.getAssets(), font));
        }
        
        a.recycle();
    }
}
