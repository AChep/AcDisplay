/*
 * Copyright (C) 2013-2014 AChep@xda <artemchep@gmail.com>
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
package com.achep.activedisplay.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.View;

import com.achep.activedisplay.Project;
import com.achep.activedisplay.utils.LogUtils;

/**
 * Created by Artem on 01.02.14.
 */
public abstract class MyFragment extends Fragment {
    private static final String TAG = "MyFragment";

    private int mTodoOnCreateView;
    private int mTodoOnResumeFragment;

    @Override
    public void onResume() {
        super.onResume();

        handleTodoList(mTodoOnResumeFragment);
        mTodoOnResumeFragment = 0;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        handleTodoList(mTodoOnCreateView);
        mTodoOnCreateView = 0;
    }

    protected abstract void handleTodoList(int v);

    protected boolean tryPutTodo(int todo) {
        if (getView() == null) {
            mTodoOnCreateView |= todo;
        } else if (!isResumed()) {
            mTodoOnResumeFragment |= todo;
        } else
            return false;
        if (Project.DEBUG)
            LogUtils.d(TAG, "Todo list infos: view_is_null=" + (getView() == null) + " is_paused=" + isResumed());
        return true;
    }

}
