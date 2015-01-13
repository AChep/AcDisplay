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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.achep.base.Build;
import com.achep.snake.Logic;
import com.achep.snake.Surface;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * @author Artem Chepurnoy
 */
public abstract class GameObject {

    private static final String TAG = "GameObject";
    private static final boolean DEBUG = Build.DEBUG && true;

    public static final int ACTION_SIZE_CHANGED = 0;
    public static final int ACTION_KILLED = 1;

    protected final Logic mLogic;
    protected ColorScheme mColorScheme;
    protected Node mNode;

    private ArrayList<WeakReference<OnGameObjectChangedListener>> mListenersRefs;

    public interface OnGameObjectChangedListener {
        void onGameObjectChanged(GameObject object, int action);
    }

    /**
     * Adds new {@link java.lang.ref.WeakReference weak} listener to the object. Make sure you call
     * {@link #unregisterListener(OnGameObjectChangedListener)} later!
     *
     * @param listener a listener to register to config changes.
     * @see #unregisterListener(OnGameObjectChangedListener)
     */
    public void registerListener(@NonNull OnGameObjectChangedListener listener) {
        // Make sure to register listener only once.
        for (WeakReference<OnGameObjectChangedListener> ref : mListenersRefs) {
            if (ref.get() == listener) {
                Log.w(TAG, "Tried to register already registered listener!");
                return;
            }
        }

        mListenersRefs.add(new WeakReference<>(listener));
    }

    /**
     * Un-registers listener is there's one.
     *
     * @param listener a listener to unregister from object's changes.
     * @see #registerListener(OnGameObjectChangedListener)
     */
    public void unregisterListener(@NonNull OnGameObjectChangedListener listener) {
        for (WeakReference<OnGameObjectChangedListener> ref : mListenersRefs) {
            if (ref.get() == listener) {
                mListenersRefs.remove(ref);
                return;
            }
        }

        Log.w(TAG, "Tried to unregister non-existent listener!");
    }

    private void notifyGameObjectChanged(int action) {
        for (int i = mListenersRefs.size() - 1; i >= 0; i--) {
            WeakReference<OnGameObjectChangedListener> ref = mListenersRefs.get(i);
            OnGameObjectChangedListener l = ref.get();

            if (l == null) {
                // There were no links to this listener except
                // our class.
                Log.w(TAG, "Deleting unused listener!");
                mListenersRefs.remove(i);
            } else {
                l.onGameObjectChanged(this, action);
            }
        }
    }

    public GameObject(Logic logic) {
        mLogic = logic;
        mListenersRefs = new ArrayList<>(3);
    }

    public void setColorScheme(@NonNull ColorScheme colorScheme) {
        mColorScheme = colorScheme;
        updateColors();
    }

    public abstract void tick();

    public void draw(Canvas canvas) {
        Node node = getHead();
        while (node != null) {
            node.draw(canvas);
            node = node.child;
        }
    }

    protected void onSizeChanged() {
        notifyGameObjectChanged(ACTION_SIZE_CHANGED);
    }

    /**
     * Sets the new head of this object.
     *
     * @see #getHead()
     */
    public void setHead(@NonNull Node node) {
        mNode = node;
        mNode.owner = this;
        mNode.color = getColor(node);
    }

    /**
     * @return the main {@link Node node} of this object, or
     * {@code null} if this object is dead.
     * @see #setHead(Node)
     */
    @Nullable
    public Node getHead() {
        return mNode;
    }

    public boolean hasHead() {
        return mNode != null;
    }

    /**
     * @see #getHead()
     * @see #setHead(Node)
     */
    void detachHead(Node node) {
        if (node == mNode) {
            if (DEBUG) Log.d(TAG, "Detaching head of " + this);
            mNode = null;

            notifyGameObjectChanged(ACTION_KILLED);
        }
    }

    int getColor(Node node) {
        if (mColorScheme == null) return Color.RED;
        return node.isHeadNode()
                ? mColorScheme.colorPrimary
                : mColorScheme.colorSecondary;
    }

    void updateColors() {
        Node node = getHead();
        while (node != null) {
            node.color = getColor(node);
            node = node.child;
        }
    }

    public int getSize() {
        Node head = getHead();
        return head == null ? 0 : head.getChildCount() + 1;
    }

    /**
     * Main component of the Snake game. The node is inside of apples
     * or snakes or anything and itself node is a two-side-linked list.
     *
     * @author Artem Chepurnoy
     */
    public static class Node extends Drawable {

        private static final String LOG = "GameObject.Node";

        private final Logic mLogic;
        private final Paint mPaint;
        private int color;

        public float x;
        public float y;

        public int xp; // Never change it directly!
        public int yp; // Never change it directly!

        public GameObject owner;
        public Node child;
        public Node parent;

        public boolean isMeal;
        public boolean isPredator;

        public Node(@NonNull Logic logic) {
            this(logic, false);
        }

        public Node(@NonNull Logic logic, boolean isPredator) {
            mLogic = logic;
            this.isMeal = !isPredator;
            this.isPredator = isPredator;

            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setColor(Color.RED);
        }

        public void move(int[] pos) {
            move(pos[0], pos[1]);
        }

        /**
         * Moves this node and all its children.
         *
         * @param xp absolute {@code x} coordinate
         * @param yp absolute {@code y} coordinate
         */
        public void move(int xp, int yp) {
            if (child != null) {
                child.move(this.xp, this.yp);
            }

            int oldX = this.xp;
            int oldY = this.yp;
            this.xp = xp;
            this.yp = yp;
            performMove(xp, yp);

            // Please note, that this is called
            // after Logic#begin()
            mLogic.commitMove(this, oldX, oldY);
        }

        private void performMove(float x, float y) {
            this.x = x;
            this.y = y;
            mLogic.tweetRedrawCall();
        }

        public void kill() {
            if (parent != null) {
                parent.child = null;
                parent = null;
            }

            if (owner != null) {
                owner.detachHead(this);
                owner.onSizeChanged();
                owner = null;
            }
        }

        @SuppressWarnings("ConstantConditions")
        public void reparent(@NonNull Node node) {
            node.kill(); // Detach from any owner.

            GameObject.Node parent = this;
            parent.owner.onSizeChanged();
            parent.child = node;
            node.parent = parent;

            do {
                node.x = parent.x;
                node.y = parent.y;
                node.owner = parent.owner;
                node.isMeal = parent.isMeal;
                node.isPredator = false; // By design only init node may be a predator
                node = node.child;
            } while (node != null);

            // Recalculate all colors.
            owner.updateColors();
        }

        /**
         * @return the count of children of this node.
         */
        public int getChildCount() {
            int n = 0;

            Node node = child;
            while (node != null) {
                node = node.child;
                n++;
            }
            return n;
        }

        /**
         * @return the latest linked node.
         * @see #getInitNode()
         */
        @NonNull
        public GameObject.Node getFinalNode() {
            GameObject.Node node = this;
            while (node.child != null) node = node.child;
            return node;
        }

        /**
         * @return the first node in the sequence
         * @see #getFinalNode()
         */
        @NonNull
        public GameObject.Node getInitNode() {
            GameObject.Node node = this;
            while (node.parent != null) node = node.parent;
            return node;
        }

        /**
         * @return {@code true} if the node is hungry and able to eat other nodes
         * by {@link #reparent(com.achep.snake.snake.GameObject.Node) reparenting}
         * or simple {@link #kill() killing}.
         * @see com.achep.snake.snake.GameObject
         * @see com.achep.snake.snake.Animal
         * @see com.achep.snake.Logic
         * @see #isMeal()
         */
        public boolean isPredator() {
            return isPredator;
        }

        /**
         * Defines if the node can be
         * {@link #reparent(com.achep.snake.snake.GameObject.Node) eated}
         * or no.
         */
        public boolean isMeal() {
            return isMeal;
        }

        public boolean isUnbeatable() {
            return false;
        }

        public boolean isInitNode() {
            return this == getInitNode();
        }

        public boolean isHeadNode() {
            return owner != null && owner.getHead() == this;
        }

        //-- DRAWABLE INTERFACE ---------------------------------------------------

        @Override
        public void draw(Canvas canvas) {
            Surface surface = mLogic.getSurface();
            int size = surface.getSize();

            int x = surface.calculateRealX(this.x);
            int y = surface.calculateRealY(this.y);

            mPaint.setColor(color);
            canvas.drawRect(x, y, x + size, y + size, mPaint);
        }

        @Override
        public void setAlpha(int alpha) {
            mPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            mPaint.setColorFilter(cf);
        }

        @Override
        public int getOpacity() {
            return mPaint.getAlpha();
        }

    }
}
