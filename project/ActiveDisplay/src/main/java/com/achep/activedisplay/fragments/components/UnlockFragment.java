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

package com.achep.activedisplay.fragments.components;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.achep.activedisplay.R;
import com.achep.activedisplay.fragments.AcDisplayFragment;

/**
 * Created by Artem on 02.04.2014.
 */
public class UnlockFragment extends AcDisplayFragment.Widget {

    public UnlockFragment(AcDisplayFragment fragment) {
        super(fragment);
    }

    @Override
    public int getType() {
        return AcDisplayFragment.SCENE_UNLOCK;
    }

    @Override
    public View onCreateCollapsedView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.widget_notification_icon_fake, container, false);
        assert view != null;

        ImageView icon = (ImageView) view.findViewById(R.id.icon);
        icon.setImageResource(R.drawable.stat_unlock);

        return view;
    }

    @Override
    public ViewGroup onCreateExpandedView(LayoutInflater inflater, ViewGroup container, ViewGroup sceneView) {
        return null;
    }
}
