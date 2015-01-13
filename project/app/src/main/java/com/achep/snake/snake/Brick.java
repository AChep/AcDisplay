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

import android.support.annotation.NonNull;

import com.achep.snake.Logic;

/**
 * Created by Artem Chepurnoy on 30.11.2014.
 */
public class Brick extends GameObject {

    public Brick(Logic logic) {
        super(logic);
        setHead(new UnbeatableNode(logic, true));
    }

    @Override
    public void tick() {
        getHead().move(getHead().xp, getHead().yp);
    }

    @Override
    public void setHead(@NonNull Node node) {
        if (mNode != null) {
            throw new RuntimeException("You can not kill The Chosen One.");
        }
        super.setHead(node);
    }

    @NonNull
    @Override
    public Node getHead() {
        assert super.getHead() != null;
        return super.getHead();
    }

    @Override
    void detachHead(Node node) {
        throw new RuntimeException("You can not kill The Chosen One.");
    }

    private static class UnbeatableNode extends Node {

        public UnbeatableNode(@NonNull Logic logic) {
            super(logic);
        }

        public UnbeatableNode(@NonNull Logic logic, boolean isPredator) {
            super(logic, isPredator);
        }

        @Override
        public void kill() { /* do not die. */ }

        @Override
        public boolean isUnbeatable() {
            return true;
        }
    }

}
