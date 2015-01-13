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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.achep.acdisplay.R;
import com.achep.snake.snake.Animal;
import com.achep.snake.snake.GameObject;

/**
 * Created by Artem on 13.10.13.
 */
public class GameView extends View implements IDrawable {

    private static final boolean SNAKE = false;

    private Logic mLogic;
    private IDirectionChangeListener mListener;

    /**
     * {@inheritDoc}
     */
    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setLogic(Logic logic) {
        Resources res = getResources();
        mLogic = logic;
        mLogic.getSurface().setDividerSize(res.getDimensionPixelSize(R.dimen.snake_divider_size));
    }

    public void setCallback(IDirectionChangeListener listener) {
        mListener = listener;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            mLogic.resume();
        } else {
            mLogic.pause();
        }
    }

    /*
        Changes the direction of the snake on touch down.
     */
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                byte direction;

                float centerX;
                float centerY;
                if (SNAKE) {
                    Surface surface = mLogic.getSurface();
                    GameObject.Node head = mLogic.getSnake().getHead();
                    centerX = surface.calculateRealX(head.x);
                    centerY = surface.calculateRealY(head.y);
                } else {
                    centerX = getWidth() / 2;
                    centerY = getHeight() / 2;
                }

                float deltaX = event.getX() - centerX;
                float deltaY = event.getY() - centerY;
                if (Math.abs(deltaX) > Math.abs(deltaY)) {
                    direction = deltaX > 0
                            ? Animal.DIRECTION_RIGHT
                            : Animal.DIRECTION_LEFT;
                } else {
                    direction = deltaY > 0
                            ? Animal.DIRECTION_DOWN
                            : Animal.DIRECTION_UP;
                }

                if (mListener != null) {
                    mListener.onDirectionChange(direction);
                }
                break;
            default:
                return super.onTouchEvent(event);
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isInEditMode()) {
            return;
        }

        mLogic.draw(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (isInEditMode()) {
            return;
        }

        mLogic.getSurface().setSize(w, h);
    }

    @Override
    public void tweetRedrawCall() {
        postInvalidate();
    }

}
