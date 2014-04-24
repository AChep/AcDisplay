package com.achep.activedisplay.providers;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.Switch;

import com.achep.activedisplay.Config;
import com.achep.activedisplay.R;
import com.achep.activedisplay.activities.MainActivity;

/**
 * @author SMillerNL
 * @since 24-03-14
 */
public class MyWidgetProvider extends AppWidgetProvider implements Config.OnConfigChangedListener{

    private static final String ACTION_CLICK = "ACTION_CLICK";
    private boolean mBroadcasting;
    private Switch mSwitch;
    private Config mConfig;
    private ImageView icon;


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Get all ids
        mConfig = Config.getInstance(new MainActivity());
        ComponentName thisWidget = new ComponentName(context, MyWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            Log.w("WidgetExample", "Enabled: "+mConfig.isActiveDisplayEnabled());

            // Set the text
            remoteViews.setTextViewText(R.id.update, "Enabled: "+mConfig.isActiveDisplayEnabled());

            // Register an onClickListener
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://acdisplay.artemchep.com/toggle"));
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            remoteViews.setOnClickPendingIntent(R.id.update, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    @Override
    public void onConfigChanged(Config config, String key, Object value) {
        if (key.equals(Config.KEY_ENABLED)) {
            if (!mBroadcasting) {
                mBroadcasting = true;
                mSwitch.setChecked((Boolean) value);
                mBroadcasting = false;
            }
        }
    }
}