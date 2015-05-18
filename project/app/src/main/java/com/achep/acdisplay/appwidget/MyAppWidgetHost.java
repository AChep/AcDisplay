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
package com.achep.acdisplay.appwidget;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.os.TransactionTooLargeException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.achep.base.Device;

/**
 * Specific {@link AppWidgetHost} that creates our {@link MyAppWidgetHostView}
 * which eats all touch events. This ensures that users can not
 * bypass the keyguard.
 *
 * @author Artem Chepurnoy
 */
public class MyAppWidgetHost extends AppWidgetHost {

    @Nullable
    private AppWidgetHostView mTempView;

    public MyAppWidgetHost(@NonNull Context context, int hostId) {
        super(Device.hasLollipopMR1Api()
                // Up to Android 5.1 app widget host has a bug, that
                // holds the context reference.
                // See the fix: https://github.com/android/platform_frameworks_base/commit/7a96f3c917e0001ee739b65da37b2fadec7d7765
                ? context
                : context.getApplicationContext(), hostId);
    }

    /**
     * Create the AppWidgetHostView for the given widget.
     * The AppWidgetHost retains a pointer to the newly-created View.
     */
    @NonNull
    public final AppWidgetHostView updateView(@NonNull Context context, int appWidgetId,
                                              @NonNull AppWidgetProviderInfo appWidget,
                                              @Nullable AppWidgetHostView view) {
        mTempView = view;
        view = createView(context, appWidgetId, appWidget);
        mTempView = null;
        return view;
    }

    @NonNull
    @Override
    protected AppWidgetHostView onCreateView(@NonNull Context context, int appWidgetId,
                                             @NonNull AppWidgetProviderInfo appWidget) {
        return mTempView != null ? mTempView : new MyAppWidgetHostView(context);
    }

    @Override
    public void startListening() {
        try {
            super.startListening();
        } catch (Exception e) {
            //noinspection StatementWithEmptyBody
            if (e.getCause() instanceof TransactionTooLargeException) {
                // We're willing to let this slide. The exception is being caused by the list of
                // RemoteViews which is being passed back. The startListening relationship will
                // have been established by this point, and we will end up populating the
                // widgets upon bind anyway. See issue 14255011 for more context.
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void stopListening() {
        super.stopListening();
        clearViews();
    }
}