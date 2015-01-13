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
package com.achep.acdisplay.ui.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.achep.acdisplay.R;
import com.achep.acdisplay.blacklist.Blacklist;
import com.achep.acdisplay.blacklist.options.HideOption;
import com.achep.acdisplay.blacklist.options.NonClearableOption;
import com.achep.acdisplay.blacklist.options.Option;
import com.achep.acdisplay.blacklist.options.RestrictOption;
import com.achep.base.utils.ViewUtils;

/**
 * Created by Artem on 09.02.14.
 */
public class BlacklistAppFragment extends Fragment {

    public static final String ARGS_PACKAGE_NAME = "package_name";

    private static final String SAVE_KEY_PACKAGE_NAME = "package_name";
    private static final String SAVE_KEY_SCROLL_VIEW_X = "scroll_view_x";
    private static final String SAVE_KEY_SCROLL_VIEW_Y = "scroll_view_y";

    private Option[] mOptions;
    private String mPackageName;

    // header
    private ImageView mAppIcon;
    private TextView mAppName;
    private TextView mAppPackageName;

    // options
    private LinearLayout mContainer;
    private ScrollView mScrollView;

    private String extractPackageName(Bundle args, Bundle savedInstanceState) {
        String packageName;
        if (args != null) {
            packageName = args.getString(ARGS_PACKAGE_NAME);
            if (packageName != null) return packageName;
        }
        if (savedInstanceState != null) {
            packageName = savedInstanceState.getString(SAVE_KEY_PACKAGE_NAME);
            if (packageName != null) return packageName;
        }
        return "";
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity activity = getActivity();
        Blacklist blacklist = Blacklist.getInstance();
        mPackageName = extractPackageName(getArguments(), savedInstanceState);
        mOptions = new Option[]{
                new HideOption(activity, new CheckBox(activity), blacklist, mPackageName),
                new RestrictOption(activity, new CheckBox(activity), blacklist, mPackageName),
                new NonClearableOption(activity, new CheckBox(activity), blacklist, mPackageName)
        };
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVE_KEY_PACKAGE_NAME, mPackageName);
        outState.putInt(SAVE_KEY_SCROLL_VIEW_X, mScrollView.getScrollX());
        outState.putInt(SAVE_KEY_SCROLL_VIEW_Y, mScrollView.getScrollY());
    }

    @Override
    public void onResume() {
        super.onResume();
        for (Option option : mOptions) {
            option.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        for (Option option : mOptions) {
            option.pause();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blacklist_app_config, container, false);
        assert view != null;

        // Hide switch from the header
        mAppIcon = (ImageView) view.findViewById(R.id.icon);
        mAppName = (TextView) view.findViewById(R.id.title);
        mAppPackageName = (TextView) view.findViewById(R.id.summary);

        // Options list
        mScrollView = (ScrollView) view.findViewById(R.id.scrollview);
        mContainer = (LinearLayout) view.findViewById(R.id.container);
        buildOptionsList();

        if (savedInstanceState != null) {

            // Restore scroll position
            final int scrollX = savedInstanceState.getInt(SAVE_KEY_SCROLL_VIEW_X);
            final int scrollY = savedInstanceState.getInt(SAVE_KEY_SCROLL_VIEW_Y);
            mScrollView.post(new Runnable() {
                @Override
                public void run() {
                    mScrollView.scrollTo(scrollX, scrollY);
                }
            });
        }

        displayApp();
        return view;
    }

    private void buildOptionsList() {
        mContainer.removeAllViews();

        LayoutInflater inflater = getActivity().getLayoutInflater();
        for (Option option : mOptions) {
            View view = inflater.inflate(R.layout.preference_blacklist_app_config, mContainer, false);

            ImageView icon = (ImageView) view.findViewById(R.id.icon);
            TextView title = (TextView) view.findViewById(android.R.id.title);
            TextView summary = (TextView) view.findViewById(android.R.id.summary);

            icon.setImageDrawable(option.icon);
            ViewUtils.safelySetText(title, option.title);
            ViewUtils.safelySetText(summary, option.summary);

            final CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);
            option.setCompoundButton(checkBox);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkBox.toggle();
                }
            });

            mContainer.addView(view);
        }
    }

    private void displayApp() {
        mScrollView.scrollTo(0, 0);

        // Update header
        final PackageManager pm = getActivity().getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(mPackageName, 0);

            mAppIcon.setImageDrawable(pm.getApplicationIcon(ai));
            mAppName.setText(pm.getApplicationLabel(ai));
            mAppPackageName.setText(mPackageName);
        } catch (PackageManager.NameNotFoundException e) {
            mAppName.setText("Name not found");
        }
    }
}
