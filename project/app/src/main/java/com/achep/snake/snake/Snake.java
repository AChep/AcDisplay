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
package com.achep.snake.snake;

import android.graphics.Color;
import android.graphics.Paint;

import com.achep.snake.Logic;

/**
 * Created by Artem Chepurnoy on 05.11.2014.
 */
public class Snake extends Animal {

    private static final int COLOR_BG = 0xFF212121;
    private static final int COLOR_HEAD = 0xFF03A9F4;
    private static final int COLOR_BODY = 0xFF607D8B;

    private Paint mPaint;

    public Snake(Logic logic) {
        super(logic);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.WHITE);
    }
/*
    @Override
    public void draw(Canvas canvas) {
       Surface surface = mLogic.getSurface();
        int size = surface.getSize();

        // Draw the background.
        mPaint.setColor(COLOR_BG);
        canvas.drawRect(
                surface.getPaddingLeft(),
                surface.getPaddingTop(),
                surface.getPaddingLeft() + surface.getWidth(),
                surface.getPaddingTop() + surface.getHeight(), mPaint);

        Node node = getHead();
        if (node == null) return;
        int xPrev = surface.calculateRealX(node.x);
        int yPrev = surface.calculateRealY(node.y);

        // Draw the head.
        mPaint.setColor(COLOR_HEAD);
        canvas.drawRect(xPrev, yPrev, xPrev + size, yPrev + size, mPaint);
        node = node.child;

        // Draw the body.
        mPaint.setColor(COLOR_BODY);
        while (node != null) {
            int x = surface.calculateRealX(node.x);
            int y = surface.calculateRealY(node.y);
            canvas.drawRect(x, y, x + size, y + size, mPaint);
            node = node.child;
        }
    }*/

}
