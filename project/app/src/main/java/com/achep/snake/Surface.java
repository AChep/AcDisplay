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
package com.achep.snake;

/**
 * Created by Artem Chepurnoy on 05.11.2014.
 */
public class Surface {

    private int mWidth;
    private int mHeight;
    private int mPaddingLeft;
    private int mPaddingTop;
    private int mDividerSize;

    private int mCeil = 15;
    private int mCeilWidth;
    private int mCeilHeight;

    private final Logic mLogic;
    private int mSize;

    public Surface(Logic logic) {
        mLogic = logic;
    }

    public int calculateRealX(float x) {
        return mPaddingLeft + (int) (x * mSize) + (int) x * mDividerSize;
    }

    public int calculateRealY(float y) {
        return mPaddingTop + (int) (y * mSize) + (int) y * mDividerSize;
    }

    public interface OnSurfaceChangedListener {

        void onSurfaceSizeChanged(int n, int m);

    }

    public void registerListener(OnSurfaceChangedListener listener) {

    }

    public void unregisterListener(OnSurfaceChangedListener listener) {

    }

    public void setDividerSize(int dividerSize) {
        mDividerSize = dividerSize;
    }

    public void setSize(int width, int height) {
        int dw = -mDividerSize * (mLogic.getColumnsNumber() - 1);
        int dh = -mDividerSize * (mLogic.getRowsNumber() - 1);
        int cw = (width + dw) / mLogic.getColumnsNumber();
        int ch = (height + dh) / mLogic.getRowsNumber();

        mSize = Math.min(cw, ch);
        mWidth = (mSize + mDividerSize) * mLogic.getColumnsNumber();
        mHeight = (mSize + mDividerSize) * mLogic.getRowsNumber();
        mPaddingLeft = (width - mWidth) / 2;
        mPaddingTop = (height - mHeight) / 2;
    }

    public int getPaddingLeft() {
        return mPaddingLeft;
    }

    public int getPaddingTop() {
        return mPaddingTop;
    }

    public int getSize() {
        return mSize;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

}
