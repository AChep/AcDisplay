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

import android.graphics.Paint;

import com.achep.snake.Logic;

/**
 * Created by Artem Chepurnoy on 23.11.2014.
 */
public class Apple extends GameObject {

    private final Paint mPaint;

    public Apple(Logic logic) {
        super(logic);
        setHead(new Node(logic));

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(0xFF4CAF50);
    }

    @Override
    public void tick() {
        getHead().move(getHead().xp, getHead().yp);
    }

   /*@Override
    public void draw(Canvas canvas) {
        Surface surface = mLogic.getSurface();
        int size = surface.getSize();

        Node node = getHead();
        do {
            int x = surface.calculateRealX(node.x);
            int y = surface.calculateRealY(node.y);
            canvas.drawRect(x, y, x + size, y + size, mPaint);
            node = node.child;
        } while (node != null);
    }*/

}
