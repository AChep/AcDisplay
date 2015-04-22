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
package com.achep.base.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.base.Device;
import com.drivemode.android.typeface.TypefaceHelper;

/**
 * Created by Artem on 29.01.14.
 */
public class TextView extends AppCompatTextView {

    private static final String TAG = "TextView";

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

        if (Config.getInstance().isOverridingFontsEnabled()) {
            String fontName = a.getString(R.styleable.TextView_font);
            if (fontName != null) {
                int maximumSdkVersion = a.getInt(
                        R.styleable.TextView_font_beforeApi,
                        Build.VERSION_CODES.LOLLIPOP);

                if (!Device.hasTargetApi(maximumSdkVersion)) {
                    if (fontName.indexOf('.') == -1) fontName += ".ttf";
                    TypefaceHelper.getInstance().setTypeface(this, "fonts/" + fontName);
                }
            }
        }

        a.recycle();
    }

}
