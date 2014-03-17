/*
 * Copyright (C) 2013 AChep@xda <artemchep@gmail.com>
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
package com.achep.activedisplay.fragments.activedisplay;

import android.app.Fragment;

/**
 * Created by Artem on 01.02.14.
 */
public abstract class MyFragment extends Fragment {
    private static final String TAG = "MyFragment";

    private int mTodoOnResumeFragment;

    @Override
    public void onResume() {
        super.onResume();

        handleTodoList(mTodoOnResumeFragment);
        mTodoOnResumeFragment = 0;
    }

    protected abstract void handleTodoList(int v);

    protected boolean tryPutTodo(int todo) {
        if (!isResumed()) {
            mTodoOnResumeFragment |= todo;
        } else
            return false;
        return true;
    }

}
