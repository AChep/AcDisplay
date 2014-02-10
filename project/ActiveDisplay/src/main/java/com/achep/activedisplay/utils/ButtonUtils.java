/*
 * Copyright (C) 2013-2014 AChep@xda <artemchep@gmail.com>
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
package com.achep.activedisplay.utils;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.Button;

/**
 * Created by Artem on 06.02.14.
 */
public class ButtonUtils {

    public static void centerIcon(Button button) {
        Drawable[] drawables = button.getCompoundDrawables();

        if ((drawables != null) && (drawables.length > 0) && (drawables[0] != null)) {
            BitmapDrawable drawableLeft = (BitmapDrawable) drawables[0];
            int width = drawableLeft.getIntrinsicWidth();

            // Set the padding on the button to match the previous padding
            // but add the image width to the right to center the text
            button.setPadding(button.getPaddingLeft(), button.getPaddingTop(),
                    button.getPaddingRight() + width, button.getPaddingBottom());
        }
    }

}
