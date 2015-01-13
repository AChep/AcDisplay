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
import android.util.AttributeSet;
import android.view.View;
import android.widget.GridLayout;

import com.achep.acdisplay.R;
import com.achep.snake.snake.Animal;

/**
 * Created by Artem Chepurnoy on 25.11.2014.
 */
public class ControllerView extends GridLayout implements View.OnClickListener {

    private IDirectionChangeListener mCallback;

    private View mUpView;
    private View mDownView;
    private View mLeftView;
    private View mRightView;

    public ControllerView(Context context) {
        super(context);
    }

    public ControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mUpView = findViewById(R.id.up);
        mDownView = findViewById(R.id.down);
        mLeftView = findViewById(R.id.left);
        mRightView = findViewById(R.id.right);

        mUpView.setOnClickListener(this);
        mDownView.setOnClickListener(this);
        mLeftView.setOnClickListener(this);
        mRightView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (mCallback == null) return;

        if (v == mUpView) {
            mCallback.onDirectionChange(Animal.DIRECTION_UP);
        } else if (v == mDownView) {
            mCallback.onDirectionChange(Animal.DIRECTION_DOWN);
        } else if (v == mLeftView) {
            mCallback.onDirectionChange(Animal.DIRECTION_LEFT);
        } else if (v == mRightView) {
            mCallback.onDirectionChange(Animal.DIRECTION_RIGHT);
        }
    }

    public void setCallback(IDirectionChangeListener callback) {
        mCallback = callback;
    }

}
