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
package com.achep.activedisplay.utils;

import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Artem on 21.01.14.
 */
public class ViewUtils {

    private static final int[] coordinates = new int[3];

    public static boolean isTouchPointInView(View view, float x, float y) {
        view.getLocationInWindow(coordinates);
        int left = coordinates[0];
        int top = coordinates[1];
        return x >= left && x <= left + view.getWidth() &&
                y >= top && y <= top + view.getHeight();
    }

    public static int getTop(View view, View decorView) {
        view.getLocationInWindow(coordinates);
        int top = coordinates[1];
        decorView.getLocationInWindow(coordinates);
        return top - coordinates[1];
    }

    public static int getBottom(View view, View decorView) {
        view.getLocationInWindow(coordinates);
        int bottom = coordinates[1] + view.getHeight();
        decorView.getLocationInWindow(coordinates);
        return bottom - coordinates[1];
    }

    // //////////////////////////////////////////
    // //////////// -- VISIBILITY -- ////////////
    // //////////////////////////////////////////

    public static void setVisible(View view, boolean visible) {
        setVisible(view, visible, View.GONE);
    }

    public static void setVisible(View view, boolean visible, int notVisible) {
        view.setVisibility(visible ? View.VISIBLE : notVisible);
    }

    public static void safelySetText(TextView textView, CharSequence text) {
        final boolean visible = text != null;
        if (visible) textView.setText(text);
        ViewUtils.setVisible(textView, visible);
    }

    // //////////////////////////////////////////
    // /////////// -- TOUCH EVENTS -- ///////////
    // //////////////////////////////////////////

    public static boolean toGlobalMotionEvent(View view, MotionEvent ev) {
        return toMotionEvent(view, ev, "toGlobalMotionEvent");
    }

    public static boolean toLocalMotionEvent(View view, MotionEvent ev) {
        return toMotionEvent(view, ev, "toLocalMotionEvent");
    }

    private static boolean toMotionEvent(View view, MotionEvent ev, String methodName) {
        try {
            Method method = View.class.getDeclaredMethod(methodName, MotionEvent.class);
            method.setAccessible(true);
            return (boolean) method.invoke(view, ev);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return false;
    }

}
