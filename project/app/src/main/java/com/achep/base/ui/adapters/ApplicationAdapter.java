package com.achep.base.ui.adapters;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.achep.acdisplay.R;

public class ApplicationAdapter extends ArrayAdapter < ApplicationInfo > {
    private List < ApplicationInfo > appsList = null;
    private Context context;
    private PackageManager packageManager;

    public ApplicationAdapter(Context context, int textViewResourceId, List < ApplicationInfo > appsList) {
        super(context, textViewResourceId, appsList);
        this.context = context;
        this.appsList = appsList;
        packageManager = context.getPackageManager();
    }

    @Override
    public int getCount() {
        return ((null != appsList) ? appsList.size() : 0);
    }

    @Override
    public ApplicationInfo getItem(int position) {
        return ((null != appsList) ? appsList.get(position) : null);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (null == view) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.preference_apps_list_row, null);
        }

        ApplicationInfo applicationInfo = appsList.get(position);
        if (null != applicationInfo) {
            TextView appName = (TextView) view.findViewById(R.id.app_name);
            TextView packageName = (TextView) view.findViewById(R.id.app_paackage);
            ImageView iconview = (ImageView) view.findViewById(R.id.app_icon);

            appName.setText(applicationInfo.loadLabel(packageManager));
            packageName.setText(applicationInfo.packageName);
            iconview.setImageDrawable(applicationInfo.loadIcon(packageManager));
        }
        return view;
    }
};