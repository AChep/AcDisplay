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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.achep.snake.snake.Apple;
import com.achep.snake.snake.Brick;
import com.achep.snake.snake.ColorScheme;
import com.achep.snake.snake.GameObject;
import com.achep.snake.snake.Snake;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Artem Chepurnoy on 23.11.2014.
 */
public class Logic implements
        IDirectionChangeListener,
        IDrawable {

    private static final String TAG = "Logic";
    private static final int TICK = 1;
    private static final double PERIOD_MAX = 400;

    private static int COLLISION_MAP_DEPTH = 5;

    private static GameObject.Node sEmptyNode;

    private ArrayList<GameObject.Node> mCollisionNodes;
    private GameObject.Node[][][] mCollisionMap;
    private int[] mTempArray = new int[2];

    private final IDrawable mDrawable;
    private final Surface mSurface;
    private final int mColumnsNumber;
    private final int mRowsNumber;
    private int mScoreMax;
    private Paint mPaint;

    private final Snake mSnake;
    private final Apple mApple;
    private final Brick[] mBricks;

    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case TICK:
                    begin();
                    apply();

                    sendEmptyMessageDelayed(TICK, getFramePeriod());
                    break;
            }
        }

    };

    public Logic(IDrawable drawable, int n, int m, ColorScheme... colorSchemes) {
        mColumnsNumber = n;
        mRowsNumber = m;
        mDrawable = drawable;

        mSurface = new Surface(this);
        mPaint = new Paint();
        mPaint.setColor(0xFF555555);

        // Initialize map.
        mCollisionMap = new GameObject.Node[n][m][COLLISION_MAP_DEPTH];
        mCollisionNodes = new ArrayList<>();

        if (sEmptyNode == null)
            sEmptyNode = new GameObject.Node(null);

        // Init snake.
        GameObject.Node node = new GameObject.Node(this, true);
        node.move(n / 2, m / 2);
        mSnake = new Snake(this);
        mSnake.setHead(node);
        mSnake.setColorScheme(colorSchemes[0]);

        // Init apple.
        mApple = new Apple(this);
        mApple.setColorScheme(colorSchemes[1]);
        mApple.getHead().move(generateRandomPosition());

        // Init unbeatable bricks.
        int length = (int) (Math.sqrt(n * m) / 2);
        mBricks = new Brick[length];
        for (int i = 0; i < length; i++) {
            Brick brick = new Brick(this);
            brick.getHead().move(generateRandomPosition());
            brick.setColorScheme(colorSchemes[2]);
            mBricks[i] = brick;
        }
    }

    private int[] generateRandomPosition() {
        int x, y;
        do { // Pick new position
            x = (int) (Math.random() * mColumnsNumber);
            y = (int) (Math.random() * mRowsNumber);
        } while (mCollisionMap[x][y][0] != null /* then it's free */);
        mTempArray[0] = x;
        mTempArray[1] = y;
        return mTempArray;
    }

    /**
     * Changes the direction of the snake to given one.
     *
     * @param direction one of the following
     *                  {@link com.achep.snake.snake.Animal#DIRECTION_NONE} to not move,
     *                  {@link com.achep.snake.snake.Animal#DIRECTION_LEFT} to move left,
     *                  {@link com.achep.snake.snake.Animal#DIRECTION_RIGHT} to move right,
     *                  {@link com.achep.snake.snake.Animal#DIRECTION_UP} to move up,
     *                  {@link com.achep.snake.snake.Animal#DIRECTION_DOWN} to move down.
     */
    @Override
    public void onDirectionChange(byte direction) {
        mSnake.setDirection(direction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void tweetRedrawCall() {
        mDrawable.tweetRedrawCall();
    }

    /**
     *
     */
    public void resume() {
        mHandler.sendEmptyMessage(TICK);
    }

    /**
     *
     */
    public void pause() {
        mHandler.removeMessages(TICK);
    }

    public void draw(Canvas canvas) {
        canvas.drawRect(
                mSurface.getPaddingLeft(),
                mSurface.getPaddingTop(),
                mSurface.getPaddingLeft() + mSurface.getWidth(),
                mSurface.getPaddingTop() + mSurface.getHeight(), mPaint);

        mSnake.draw(canvas);
        mApple.draw(canvas);
        for (Brick brick : mBricks) brick.draw(canvas);
    }

    public void begin() {
        mSnake.tick();
        mApple.tick();
        for (Brick brick : mBricks) brick.tick();
    }

    public void commitMove(GameObject.Node node, int oldX, int oldY) {
        mCollisionNodes.add(node);
        int x = node.xp;
        int y = node.yp;

        for (int i = 0; i < COLLISION_MAP_DEPTH; i++) {
            GameObject.Node[] stack = mCollisionMap[oldX][oldY];

            if (stack[i] == node) {
                int start = i + 1;
                System.arraycopy(stack, start, stack, start - 1, COLLISION_MAP_DEPTH - start);
                stack[COLLISION_MAP_DEPTH - 1] = null;
                break;
            }
        }

        for (int i = 0; i < COLLISION_MAP_DEPTH; i++) {
            if (mCollisionMap[x][y][i] == null) {
                mCollisionMap[x][y][i] = node;
                break;
            }
        }
    }

    public void apply() {
        for (GameObject.Node node : mCollisionNodes) {
            GameObject.Node[] stack = mCollisionMap[node.xp][node.yp];

            if (stack[0] == sEmptyNode) {
                continue;
            }

            // Count the number of predators.
            int predatorsCount = 0;
            GameObject.Node predator = null;
            for (GameObject.Node n : stack) {
                if (n == null) break;
                if (n.isPredator()) {
                    predator = n;
                    predatorsCount++;
                }
            }

            // Handle the collision.
            if (predator != null) {
                GameObject.Node predatorInit = null;
                if (predatorsCount > 0) {
                    predatorInit = predator.getInitNode();
                }

                for (GameObject.Node n : stack) {
                    if (n == null) break;
                    if (predatorsCount > 1) {
                        n.kill();
                    } else if (n != predator) {
                        if (n.getInitNode() != predatorInit) {
                            Log.d(TAG, "Reparenting node.");
                            predator.getFinalNode().reparent(n);
                        } else {
                            Log.d(TAG, "Killing node." + predatorInit);
                            n.kill();
                        }
                    }
                }
            } else if (stack[1] != null) {
                Log.i(TAG, "Collision between non-predators happened!");
            }

            // Clean collision map.
            stack[0] = sEmptyNode;
        }
        mCollisionNodes.clear();

        // Generate new stuff.
        if (!mApple.hasHead()) {

            // Put new apple on map.
            GameObject.Node node = new GameObject.Node(this);
            node.move(generateRandomPosition());
            mApple.setHead(node);
        }

        // Clean-up collision map.
        for (GameObject.Node[][] n : mCollisionMap) {
            for (GameObject.Node[] m : n) {
                Arrays.fill(m, null);
            }
        }

        // Max score.
        int score = mSnake.getSize();
        mScoreMax = Math.max(mScoreMax, score);
    }

    /**
     * Defines the speed of the game.
     *
     * @return the time in millis between two frames and
     * {@link com.achep.snake.snake.GameObject#tick() moves}.
     * @see #mScoreMax
     * @see #PERIOD_MAX
     */
    private long getFramePeriod() {
        double scoreMaxPossible = mColumnsNumber * mRowsNumber / 3;
        double score = Math.min(mScoreMax, scoreMaxPossible);
        return Math.round(PERIOD_MAX * (1 - score / scoreMaxPossible));
    }

    public int getScore() {
        return mSnake.getSize();
    }

    public Snake getSnake() {
        return mSnake;
    }

    public Surface getSurface() {
        return mSurface;
    }

    public int getColumnsNumber() {
        return mColumnsNumber;
    }

    public int getRowsNumber() {
        return mRowsNumber;
    }

}
