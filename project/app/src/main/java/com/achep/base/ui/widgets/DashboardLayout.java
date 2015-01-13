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
package com.achep.base.ui.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.achep.acdisplay.R;
import com.achep.base.Device;

public class DashboardLayout extends ViewGroup {

    private final float mCellGapX;
    private final float mCellGapY;

    private int mNumRows;
    private final int mNumColumns;

    public DashboardLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        final Resources res = context.getResources();
        mCellGapX = res.getDimension(R.dimen.dashboard_cell_gap_x);
        mCellGapY = res.getDimension(R.dimen.dashboard_cell_gap_y);
        mNumColumns = res.getInteger(R.integer.dashboard_num_columns);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int childCount = getChildCount();
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int availableWidth = (int) (width - getPaddingLeft() - getPaddingRight() -
                (mNumColumns - 1) * mCellGapX);
        float cellWidth = (float) Math.ceil(((float) availableWidth) / mNumColumns);

        int cellHeight = 0;
        int cursor = 0;

        for (int i = 0; i < childCount; i++) {
            DashboardTileView v = (DashboardTileView) getChildAt(i);
            if (v.getVisibility() == View.GONE) continue;

            LayoutParams lp = v.getLayoutParams();
            int colSpan = v.getColumnSpan();
            lp.width = (int) ((colSpan * cellWidth) + (colSpan - 1) * mCellGapX);

            // Measure the child
            int newWidthSpec = getChildMeasureSpec(widthMeasureSpec, 0, lp.width);
            int newHeightSpec = getChildMeasureSpec(heightMeasureSpec, 0, lp.height);
            v.measure(newWidthSpec, newHeightSpec);

            // Save the cell height
            if (cellHeight <= 0) {
                cellHeight = v.getMeasuredHeight();
            }

            lp.height = cellHeight;

            cursor += colSpan;
        }

        mNumRows = (int) Math.ceil((float) cursor / mNumColumns);
        final int newHeight = (int) ((mNumRows * cellHeight) + ((mNumRows - 1) * mCellGapY)) +
                getPaddingTop() + getPaddingBottom();

        setMeasuredDimension(width, newHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int childCount = getChildCount();
        final boolean isLayoutRtl = isLayoutRtl();
        final int width = getWidth();

        int x = getPaddingStartLeft();
        int y = getPaddingTop();
        int cursor = 0;

        for (int i = 0; i < childCount; i++) {
            final DashboardTileView v = (DashboardTileView) getChildAt(i);
            final LayoutParams lp = v.getLayoutParams();
            if (v.getVisibility() == GONE) continue;

            final int col = cursor % mNumColumns;
            final int colSpan = v.getColumnSpan();

            final int childWidth = lp.width;
            final int childHeight = lp.height;

            int row = cursor / mNumColumns;
            v.setDividerVisibility(row != mNumRows - 1);

            // Push the item to the next row if it can't fit on this one
            if ((col + colSpan) > mNumColumns) {
                x = getPaddingStartLeft();
                y += childHeight + mCellGapY;
                row++;
            }

            final int childLeft = (isLayoutRtl) ? width - x - childWidth : x;
            final int childRight = childLeft + childWidth;

            final int childTop = y;
            final int childBottom = childTop + childHeight;

            // Layout the container
            v.layout(childLeft, childTop, childRight, childBottom);

            // Offset the position by the cell gap or reset the position and cursor when we
            // reach the end of the row
            cursor += v.getColumnSpan();
            if (cursor < (((row + 1) * mNumColumns))) {
                x += childWidth + mCellGapX;
            } else {
                x = getPaddingStartLeft();
                y += childHeight + mCellGapY;
            }
        }
    }

    /**
     * @return {@code true} if this layout is Right-to-left, otherwise,
     * or if this device's Android version doesn't support RTL, it
     * returns {@code false}
     */
    @SuppressLint("NewApi")
    private boolean isLayoutRtl() {
        return Device.hasJellyBeanMR1Api() && getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }

    @SuppressLint("NewApi")
    private int getPaddingStartLeft() {
        return Device.hasJellyBeanMR1Api()
                ? getPaddingStart()
                : getPaddingLeft();
    }

}
