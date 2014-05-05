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

package com.achep.activedisplay.providers;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import com.achep.activedisplay.Config;
import com.achep.activedisplay.Project;
import com.achep.activedisplay.R;
import com.achep.activedisplay.receiver.ReceiverActivity;

/**
 * Toggle widget provider.
 *
 * @author Artem Chepurnoy
 */
public class ToggleWidgetProvider extends AppWidgetProvider
        implements Config.OnConfigChangedListener {

    private static final String TAG = "AppWidgetProvider";

    private Config mConfig;
    private Context mContext;

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        onEnabledInternal(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        onDisabledInternal(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        onEnabledInternal(context);
        updateWidgets(context);
    }

    @Override
    public void onConfigChanged(Config config, String key, Object value) {
        switch (key) {
            case Config.KEY_ENABLED:
                updateWidgets(mContext);
                break;
        }
    }

    private void onEnabledInternal(Context context) {
        if (mContext != null) {
            return; // already initialized
        }

        mContext = context;
        mConfig = Config.getInstance();
        mConfig.addOnConfigChangedListener(this);

        if (Project.DEBUG) Log.d(TAG, "Toggle widget enabled");
    }

    private void onDisabledInternal(Context context) {
        if (mContext == null) {
            return; // not initialized
        }

        mConfig.removeOnConfigChangedListener(this);

        if (Project.DEBUG) Log.d(TAG, "Toggle widget disabled");
    }

    private void updateWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        ComponentName cn = new ComponentName(context, ToggleWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(cn);
        for (int appWidgetId : appWidgetIds) {

            // Create an Intent to launch ReceiverActivity to toggle AcDisplay.
            // Probably doing same using BroadcastReceiver would be better solution.
            Intent intent = new Intent(context, ReceiverActivity.class)
                    .setData(Uri.parse("acdisplay://" + ReceiverActivity.HOST_TOGGLE));
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            Resources res = context.getResources();

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.appwidget_toggle_layout);
            rv.setOnClickPendingIntent(R.id.container, pendingIntent);
            rv.setTextViewText(R.id.title, res.getString(
                    mConfig.isEnabled() ? R.string.widget_toggle_enabled : R.string.widget_toggle_disabled));

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, rv);
        }
    }
}
