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
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.achep.acdisplay.R;
import com.achep.acdisplay.appwidget.MyAppWidgetHost;
import com.achep.acdisplay.ui.fragments.AcDisplayFragment;

/**
 * A widget the hosts {@link android.appwidget.AppWidgetHost}.
 *
 * @author Artem Chepurnoy
 */
public class HostWidget extends Widget {

    private static final String TAG = "HostWidget";

    public static final int HOST_ID = 1;

    private final AppWidgetManager mAppWidgetManager;
    private final AppWidgetHost mAppWidgetHost;
    private AppWidgetHostView mHostView;
    private ViewGroup mHostContainer;

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
    }

    @Override
    public void onStop() {
        mAppWidgetHost.stopListening();
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

        if (!initialize) {
            return sceneView;
        }

        updateUi();
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

    private void updateUi() {
        int id = getConfig().getCustomWidgetId();
        if (id < 0) {
            mHostContainer.removeAllViews();
            return;
        }

        // Create the App Widget and get its remote
        // views.
        AppWidgetProviderInfo appWidget = mAppWidgetManager.getAppWidgetInfo(id);
        mHostContainer.removeAllViews();
        mHostView = mAppWidgetHost.createView(getFragment().getActivity(), id, appWidget);

        Resources res = getFragment().getActivity().getResources();
        final int widthMax = res.getDimensionPixelSize(R.dimen.scene_max_width);
        final int width = Math.min((appWidget.minWidth + appWidget.minResizeWidth) / 2, widthMax);
        // Add it to the container.
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        mHostContainer.addView(mHostView, lp);
    }

}
