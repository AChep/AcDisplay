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
package com.achep.base.ui.fragments;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.achep.acdisplay.R;
import com.achep.base.async.WeakHandler;
import com.achep.base.dashboard.DashboardCategory;
import com.achep.base.dashboard.DashboardTile;
import com.achep.base.ui.activities.SettingsActivity;
import com.achep.base.ui.fragments.leakcanary.LeakWatchFragment;
import com.achep.base.ui.widgets.DashboardTileView;

import java.util.List;

import static com.achep.base.Build.DEBUG;

public class DashboardFragment extends LeakWatchFragment {

    private static final String TAG = "DashboardFragment";

    private LayoutInflater mLayoutInflater;
    private ViewGroup mDashboardContainer;

    private static final int MSG_REBUILD_UI = 1;
    private final H mHandler = new H(this);

    private static class H extends WeakHandler<DashboardFragment> {

        public H(@NonNull DashboardFragment object) {
            super(object);
        }

        @Override
        protected void onHandleMassage(@NonNull DashboardFragment df, Message msg) {
            switch (msg.what) {
                case MSG_REBUILD_UI: {
                    final Context context = df.getActivity();
                    df.rebuildUI(context);
                }
                break;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        sendRebuildUI();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mLayoutInflater = inflater;

        final View rootView = inflater.inflate(R.layout.dashboard, container, false);
        mDashboardContainer = (ViewGroup) rootView.findViewById(R.id.dashboard_container);
        return rootView;
    }

    private void rebuildUI(Context context) {
        if (!isAdded()) {
            Log.w(TAG, "Cannot build the DashboardSummary UI yet as the Fragment is not added");
            return;
        }

        final long start = System.currentTimeMillis();
        final Resources res = getResources();

        mDashboardContainer.removeAllViews();

        SettingsActivity activity = (SettingsActivity) context;
        List<DashboardCategory> categories = activity.getDashboardCategories(true);

        final int count = categories.size();
        for (int i = 0; i < count; i++) {
            DashboardCategory category = categories.get(i);

            View view = mLayoutInflater.inflate(R.layout.dashboard_category, mDashboardContainer, false);
            TextView labelView = (TextView) view.findViewById(R.id.category_title);
            labelView.setText(category.getTitle(res));
            mDashboardContainer.addView(view);

            ViewGroup categoryContent = (ViewGroup) view.findViewById(R.id.category_content);

            for (DashboardTile tile : category) {
                // Create, fill and add new tile to the category.
                DashboardTileView tileView = (DashboardTileView)
                        mLayoutInflater.inflate(R.layout.dashboard_tile, categoryContent, false);
                tileView.setDashboardTile(tile);
                categoryContent.addView(tileView);
            }
        }

        if (DEBUG) {
            long delta = System.currentTimeMillis() - start;
            Log.d(TAG, "Rebuilding GUI took " + delta + "ms.");
        }
    }

    private void sendRebuildUI() {
        if (!mHandler.hasMessages(MSG_REBUILD_UI)) {
            mHandler.sendEmptyMessage(MSG_REBUILD_UI);
        }
    }
}
