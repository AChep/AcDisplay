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
package com.achep.base.ui.adapters;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

/**
 * Created by Artem Chepurnoy on 27.12.2014.
 */
public abstract class BetterArrayAdapter<T> extends ArrayAdapter<T> {

    @NonNull
    protected final LayoutInflater mInflater;

    @NonNull
    protected final Context mContext;

    @LayoutRes
    private final int mResource;

    public abstract static class ViewHolder {

        @NonNull
        public final View view;

        public ViewHolder(@NonNull View view) {
            this.view = view;
        }

    }

    protected BetterArrayAdapter(@NonNull Context context, @LayoutRes int resource) {
        super(context, 0);
        mInflater = LayoutInflater.from(context);
        mContext = context;
        mResource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view;
        final ViewHolder vh;
        if (convertView == null) {
            view = mInflater.inflate(mResource, parent, false);
            vh = onCreateViewHolder(view);
            view.setTag(vh);
        } else {
            view = convertView;
            vh = (ViewHolder) view.getTag();
        }

        onBindViewHolder(vh, position);
        return view;
    }

    @NonNull
    protected abstract ViewHolder onCreateViewHolder(@NonNull View view);

    protected abstract void onBindViewHolder(@NonNull ViewHolder viewHolder, int i);

}
