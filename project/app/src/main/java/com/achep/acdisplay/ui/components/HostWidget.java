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
package com.achep.acdisplay.ui.components;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.acdisplay.appwidget.MyAppWidgetHost;
import com.achep.acdisplay.appwidget.MyAppWidgetHostView;
import com.achep.acdisplay.ui.fragments.AcDisplayFragment;
import com.achep.base.content.ConfigBase;
import com.achep.base.utils.AppWidgetUtils;
import com.achep.base.utils.MathUtils;
import com.achep.base.utils.ViewUtils;

/**
 * A widget the hosts {@link android.appwidget.AppWidgetHost}.
 *
 * @author Artem Chepurnoy
 */
public class HostWidget extends Widget implements ConfigBase.OnConfigChangedListener {

    private static final String TAG = "HostWidget";

    public static final int HOST_ID = 1;

    private final AppWidgetManager mAppWidgetManager;
    private final MyAppWidgetHost mAppWidgetHost;
    private MyAppWidgetHostView mHostView;
    private ViewGroup mHostContainer;
    private View mEmptyView;

    private boolean mHostViewNeedsReInflate;

    public HostWidget(@NonNull Callback callback, @NonNull AcDisplayFragment fragment) {
        super(callback, fragment);
        Activity activity = fragment.getActivity();
        mAppWidgetManager = AppWidgetManager.getInstance(activity);
        mAppWidgetHost = new MyAppWidgetHost(activity, HOST_ID);
    }

    @Override
    public void onStart() {
        super.onStart();
        mAppWidgetHost.startListening();
        getConfig().registerListener(this);
        updateAppWidgetViewIfNeeded();
    }

    @Override
    public void onViewAttached() {
        super.onViewAttached();
        updateAppWidgetViewIfNeeded();
    }

    @Override
    public void onStop() {
        getConfig().unregisterListener(this);
        mAppWidgetHost.stopListening();
        mHostViewNeedsReInflate = true;
        // Stopping listening removes all active views from it,
        // so we will have to re-inflate them.
        super.onStop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isHomeWidget() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasClock() {
        return true;
    }

    @Override
    protected ViewGroup onCreateView(
            @NonNull LayoutInflater inflater,
            @NonNull ViewGroup container,
            @Nullable ViewGroup sceneView) {
        boolean initialize = sceneView == null;
        if (initialize) {
            sceneView = (ViewGroup) inflater.inflate(R.layout.acdisplay_scene_host, container, false);
            assert sceneView != null;
        }

        mHostContainer = (ViewGroup) sceneView.findViewById(R.id.scene);
        mEmptyView = sceneView.findViewById(R.id.empty);
        return sceneView;
    }

    @Nullable
    @Override
    public Bitmap getBackground() {
        return null;
    }

    @Override
    public int getBackgroundMask() {
        return 0;
    }

    //-- APP WIDGET HOST ------------------------------------------------------

    private void updateAppWidgetViewIfNeeded() {
        if (!isStarted() || !isViewAttached()) return;

        int id = getConfig().getCustomWidgetId();
        if (!AppWidgetUtils.isValidId(id)) {
            if (mHostView != null) {
                ViewUtils.removeViewParent(mHostView);
                mEmptyView.setVisibility(View.VISIBLE);

                mHostViewNeedsReInflate = false;
                mHostView = null;
            }
            return;
        }

        Context context = getFragment().getActivity();
        if (mHostView == null) {
            mHostView = new MyAppWidgetHostView(context);
            updateAppWidgetTouchable();

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_HORIZONTAL);
            mHostContainer.addView(mHostView, lp);
            mEmptyView.setVisibility(View.GONE);
        } else if (!mHostViewNeedsReInflate && mHostView.getAppWidgetId() == id) return;
        AppWidgetProviderInfo appWidget = mAppWidgetManager.getAppWidgetInfo(id);
        mAppWidgetHost.updateView(context, id, appWidget, mHostView);
        mHostViewNeedsReInflate = false;
        updateAppWidgetFrameSize();
    }

    private void updateAppWidgetFrameSize() {
        Resources res = getFragment().getActivity().getResources();
        int h = getConfig().getCustomWidgetHeightDp();
        int hMin = res.getDimensionPixelSize(R.dimen.scene_min_height);
        int hMax = res.getDimensionPixelSize(R.dimen.scene_max_height);
        int w = getConfig().getCustomWidgetWidthDp();
        int wMin = res.getDimensionPixelSize(R.dimen.scene_min_width);
        int wMax = res.getDimensionPixelSize(R.dimen.scene_max_width);
        float density = res.getDisplayMetrics().density;
        w = Math.round(MathUtils.range(w * density, wMin, wMax));
        h = Math.round(MathUtils.range(h * density, hMin, hMax));
        // Update size
        ViewUtils.setSize(mHostView, w, h);
        mHostView.updateAppWidgetSize(null, w, h, w, h);
    }

    private void updateAppWidgetTouchable() {
        mHostView.setTouchable(getConfig().isCustomWidgetTouchable());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConfigChanged(@NonNull ConfigBase config,
                                @NonNull String key,
                                @NonNull Object value) {
        switch (key) {
            case Config.KEY_UI_CUSTOM_WIDGET_ID:
                mHostViewNeedsReInflate = true;
                updateAppWidgetViewIfNeeded();
                break;
            case Config.KEY_UI_CUSTOM_WIDGET_WIDTH_DP:
            case Config.KEY_UI_CUSTOM_WIDGET_HEIGHT_DP:
                if (mHostView != null) updateAppWidgetFrameSize();
                break;
            case Config.KEY_UI_CUSTOM_WIDGET_TOUCHABLE:
                if (mHostView != null) updateAppWidgetTouchable();
                break;
        }
    }
}
