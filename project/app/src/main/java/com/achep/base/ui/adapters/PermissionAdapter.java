/*
 * Copyright (C) 2015 AChep@xda <artemchep@gmail.com>
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
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.achep.acdisplay.R;
import com.achep.base.permissions.Permission;

import java.util.List;

/**
 * @author Artem Chepurnoy
 */
public class PermissionAdapter extends BetterArrayAdapter<Permission> {

    private final List<Permission> mPermissions;

    private static class ViewHolder extends BetterArrayAdapter.ViewHolder {

        final ImageView icon;
        final TextView title;
        final TextView summary;

        public ViewHolder(@NonNull View view) {
            super(view);
            this.icon = (ImageView) view.findViewById(R.id.icon);
            this.title = (TextView) view.findViewById(R.id.title);
            this.summary = (TextView) view.findViewById(R.id.summary);
        }

    }

    public PermissionAdapter(Context context, List<Permission> items) {
        super(context, R.layout.item_blah);
        mPermissions = items;
    }

    @NonNull
    @Override
    protected BetterArrayAdapter.ViewHolder onCreateViewHolder(@NonNull View view) {
        int padding = mContext.getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
        view.setPadding(padding, view.getPaddingTop(), padding, view.getPaddingBottom());
        return new ViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull BetterArrayAdapter.ViewHolder viewHolder, int i) {
        final Permission item = getItem(i);
        final ViewHolder holder = (ViewHolder) viewHolder;

        holder.title.setText(item.getTitleResource());
        holder.summary.setText(item.getSummaryResource());
        holder.icon.setImageResource(item.getIconResource());
    }

    @Override
    public int getCount() {
        return mPermissions.size();
    }

    @Override
    public Permission getItem(int position) {
        return mPermissions.get(position);
    }

    @NonNull
    public List<Permission> getPermissionList() {
        return mPermissions;
    }

}
