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

package com.achep.acdisplay.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.achep.acdisplay.App;
import com.achep.acdisplay.Build;
import com.achep.acdisplay.R;
import com.achep.acdisplay.activities.MainActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by achep on 26.05.14.
 *
 * @author Artem Chepurnoy
 */
// THIS CAN BE NOT SAFE!!!
public class BathService extends Service {

    private static final String TAG = "BathService";

    private static final String ACTION_ADD_SERVICE = TAG + ":add_service";
    private static final String ACTION_REMOVE_SERVICE = TAG + ":remove_service";
    private static final String EXTRA_SERVICE_CLASS = "class";

    public static void startService(Context context, Class<? extends ChildService> clazz) {
        synchronized (monitor) {
            if (sCreated) {

                // Send broadcast intent to notify BathService
                // to start this child.
                Intent intent = new Intent(ACTION_ADD_SERVICE);
                intent.putExtra(EXTRA_SERVICE_CLASS, clazz);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } else if (!sServiceMap.containsKey(clazz)) {
                try {
                    // Put empty child to host service and start host.
                    sServiceMap.put(clazz, clazz.newInstance());
                    context.startService(new Intent(context, BathService.class));
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e.getMessage()); // Should never happen
                }
            }
        }
    }

    public static void stopService(Context context, Class<? extends ChildService> clazz) {
        synchronized (monitor) {
            if (sCreated) {

                // Send broadcast intent to notify BathService
                // to stop this child.
                Intent intent = new Intent(ACTION_REMOVE_SERVICE);
                intent.putExtra(EXTRA_SERVICE_CLASS, clazz);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } else {

                // Service is not created, so I can handle hash map manually.
                sServiceMap.remove(clazz);
            }
        }
    }

    private static final ConcurrentHashMap<Class, ChildService> sServiceMap = new ConcurrentHashMap<>(2);
    private static final Object monitor = new Object();
    private static boolean sCreated;

    private final HashMap<Class, ChildService> mMap = new HashMap<>(2);
    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean add = false;
            switch (intent.getAction()) {
                case ACTION_ADD_SERVICE:
                    add = true;
                case ACTION_REMOVE_SERVICE:
                    break;
                default:
                    return;
            }

            synchronized (monitor) {
                Class clazz = (Class) intent.getSerializableExtra(EXTRA_SERVICE_CLASS);
                boolean containsClazz = mMap.containsKey(clazz);

                if (containsClazz == add) {
                    return;
                }

                if (add) {
                    try {
                        // Adding child to host service.
                        ChildService child = (ChildService) clazz.newInstance();
                        child.setContext(BathService.this);
                        child.onCreate();
                        mMap.put(clazz, child);
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new RuntimeException(e.getMessage()); // Should never happen
                    }
                    updateNotification();
                    return;
                }

                // Removing child from parent service.
                ChildService child = mMap.remove(clazz);
                child.onDestroy();

                if (mMap.isEmpty()) {
                    stopSelf();
                } else {
                    updateNotification();
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        synchronized (monitor) {
            sCreated = true;

            // Register for add / remove service events.
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_ADD_SERVICE);
            intentFilter.addAction(ACTION_REMOVE_SERVICE);
            mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
            mLocalBroadcastManager.registerReceiver(mReceiver, intentFilter);

            if (sServiceMap.isEmpty()) {
                stopSelf();
            } else {
                // Init all children
                Set<Map.Entry<Class, ChildService>> set = sServiceMap.entrySet();
                for (Map.Entry<Class, ChildService> entry : set) {
                    ChildService child = entry.getValue();
                    child.setContext(this);
                    child.onCreate();

                    mMap.put(entry.getKey(), child);
                }
                sServiceMap.clear();

                startForeground(App.ID_NOTIFY_BATH, buildNotification());
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        synchronized (monitor) {
            sCreated = false;

            mLocalBroadcastManager.unregisterReceiver(mReceiver);
            mLocalBroadcastManager = null;

            // Kill all children
            for (ChildService child : mMap.values()) {
                child.onDestroy();
            }
            mMap.clear();
        }
    }

    private void updateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(App.ID_NOTIFY_BATH, buildNotification());
    }

    /**
     * <p>NOT SYNCHRONIZED!</p>
     * Builds fresh notification with all {@link ChildService children services}'s
     * {@link com.achep.acdisplay.services.BathService.ChildService#getLabel() labels} in.
     * Content intent starts {@link com.achep.acdisplay.activities.MainActivity}.
     */
    private Notification buildNotification() {
        boolean empty = true;

        StringBuilder builder = new StringBuilder();
        String divider = getString(R.string.settings_multi_list_divider);
        for (ChildService child : mMap.values()) {
            if (!empty) {
                builder.append(divider);
            }
            builder.append(child.getLabel());
            empty = false;
        }

        String contentText = builder.toString();
        if (contentText.length() > 0) {
            contentText = contentText.charAt(0) + contentText.substring(1).toLowerCase();
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                App.ID_NOTIFY_BATH, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this)
                .setContentTitle(getString(R.string.service_bath))
                .setContentText(contentText)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.stat_acdisplay)
                .setPriority(Notification.PRIORITY_MIN)
                .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Base for fake foreground service hosted in {@link com.achep.acdisplay.services.BathService}.
     * Call {@link BathService#startService(android.content.Context, Class)} to start this service,
     * and {@link BathService#stopService(android.content.Context, Class)} to stop.
     *
     * @author Artem Chepurnoy
     */
    public abstract static class ChildService {

        private Context mContext;

        public ChildService() {
            if (Build.DEBUG) {
                Log.d(TAG, "Creating " + getClass().getSimpleName() + " service...");
            }
        }

        void setContext(Context context) {
            mContext = context;
        }

        /**
         * Called when fake-service is attached to main one.
         *
         * @see android.app.Service#onCreate()
         */
        public abstract void onCreate();

        /**
         * Called when fake-service is detached from main one.
         *
         * @see android.app.Service#onDestroy()
         */
        public abstract void onDestroy();

        /**
         * @return The label of this service.
         */
        public abstract String getLabel();

        public Context getContext() {
            return mContext;
        }

    }

}
