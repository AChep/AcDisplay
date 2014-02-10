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

import android.app.ListFragment;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.achep.activedisplay.R;

/**
 * Created by Artem on 09.02.14.
 */
public class BlacklistFragment extends ListFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private static class Package {

        private String packageName;
        private ApplicationInfo info;

        public Package(String packageName, ApplicationInfo info) {
            this.packageName = packageName;
            this.info = info;
        }
    }

    private static class PackageAdapter extends ArrayAdapter<Package> {

        private final Context mContext;
        private final Package[] mPackages;
        private final PackageManager mPackageManager;

        public PackageAdapter(Context context, Package[] packages) {
            super(context, R.layout.list_blacklist_item, packages);
            this.mContext = context;
            this.mPackages = packages;

            mPackageManager = context.getPackageManager();
        }

        static class ViewHolder {
            public ImageView imageView;
            public TextView textView;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder;
            View rowView = convertView;
            if (rowView == null) {
                LayoutInflater inflater = (LayoutInflater) mContext
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = inflater.inflate(R.layout.list_blacklist_item, null, true);

                holder = new ViewHolder();
                holder.imageView = (ImageView) rowView.findViewById(R.id.icon);
                holder.textView = (TextView) rowView.findViewById(R.id.title);

                rowView.setTag(holder);
            } else {
                holder = (ViewHolder) rowView.getTag();
            }

            Package pkg = mPackages[position];
            if (pkg.info != null) {
                holder.textView.setText(pkg.info.name);
                holder.imageView.setImageDrawable(pkg.info.loadIcon(mPackageManager));
            } else {
                holder.textView.setText(pkg.packageName);
            }

            return rowView;
        }
    }

}
