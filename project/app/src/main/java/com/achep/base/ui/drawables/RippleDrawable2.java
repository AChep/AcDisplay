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
package com.achep.base.ui.drawables;

import android.annotation.TargetApi;
import android.content.res.ColorStateList;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.support.annotation.NonNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Artem Chepurnoy on 09.01.2015.
 *
 * @author Artem Chepurnoy
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class RippleDrawable2 extends RippleDrawable {

    /**
     * Creates a new ripple drawable with the specified ripple color and
     * optional content and mask drawables.
     *
     * @param color   The ripple color
     * @param content The content drawable, may be {@code null}
     * @param mask    The mask drawable, may be {@code null}
     */
    public RippleDrawable2(ColorStateList color, Drawable content, Drawable mask) {
        super(color, content, mask);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setColorFilter(int color, @NonNull PorterDuff.Mode mode) {
        setColorFilter(new PorterDuffColorFilter(color, mode));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        super.setColorFilter(colorFilter);
        getRipplePaint().setColorFilter(colorFilter);
        // FIXME: Ripple backfires with original color.
    }

    private Paint getRipplePaint() {
        try {
            Method method = RippleDrawable.class.getDeclaredMethod("getRipplePaint");
            method.setAccessible(true);
            return (Paint) method.invoke(this);
        } catch (NoSuchMethodException
                | InvocationTargetException
                | IllegalAccessException ignored) {
        }
        // Normally should never happen.
        return new Paint();
    }

}
