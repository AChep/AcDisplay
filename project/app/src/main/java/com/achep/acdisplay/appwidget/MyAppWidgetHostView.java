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
package com.achep.acdisplay.appwidget;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.RemoteViews;

/**
 * Created by Artem Chepurnoy on 19.04.2015.
 */
public class MyAppWidgetHostView extends AppWidgetHostView {

    @NonNull
    LayoutInflater mInflater;

    private Context mContext;
    private int mPreviousOrientation;

    private boolean mTouchable;

    public MyAppWidgetHostView(@NonNull Context context) {
        super(context);
        mContext = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public void updateAppWidget(RemoteViews remoteViews) {
        // Store the orientation in which the widget was inflated
        mPreviousOrientation = mContext.getResources().getConfiguration().orientation;
        super.updateAppWidget(remoteViews);
    }

    /**
     * Set whether this view can pass touches to the {@link RemoteViews}.
     */
    public void setTouchable(boolean touchable) {
        mTouchable = touchable;
    }

    public boolean isReinflateRequired() {
        // Re-inflate is required if the orientation has changed since last inflated.
        int orientation = mContext.getResources().getConfiguration().orientation;
        return mPreviousOrientation != orientation;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return !mTouchable || super.onInterceptTouchEvent(ev); // eat all events
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        return mTouchable && super.onTouchEvent(event);
    }

    @Override
    public int getDescendantFocusability() {
        return ViewGroup.FOCUS_BLOCK_DESCENDANTS;
    }
}
