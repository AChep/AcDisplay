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

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by achep on 26.05.14.
 *
 * @author Artem Chepurnoy
 */
public class BathService extends Service {

    private static final String TAG = "BathService";

    private static final String ACTION_ADD_SERVICE = TAG + ":add_service";
    private static final String ACTION_REMOVE_SERVICE = TAG + ":remove_service";
    private static final String EXTRA_SERVICE_CLASS = "class";

    public static void startService(Context context, Class<? extends ChildService> clazz) {
        synchronized (monitor) {
            if (sCreated) {
                Intent intent = new Intent(ACTION_ADD_SERVICE);
                intent.putExtra(EXTRA_SERVICE_CLASS, clazz);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } else if (!sServiceMap.containsKey(clazz)) {
                try {
                    // Adding child to parent service.
                    ChildService child = clazz.newInstance();
                    sServiceMap.put(clazz, child);
                } catch (InstantiationException | IllegalAccessException e) {
                    // Should never happen
                    throw new RuntimeException(e.getMessage());
                }

                context.startService(new Intent(context, BathService.class));
            }
        }
    }

    public static void stopService(Context context, Class<? extends ChildService> clazz) {
        synchronized (monitor) {
            if (sCreated) {
                Intent intent = new Intent(ACTION_REMOVE_SERVICE);
                intent.putExtra(EXTRA_SERVICE_CLASS, clazz);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } else if (sServiceMap.containsKey(clazz)) {
                ChildService child = sServiceMap.remove(clazz);
                child.onDestroy();
            }
        }
    }

    private static final ConcurrentHashMap<Class, ChildService> sServiceMap = new ConcurrentHashMap<>(2);
    private static final Object monitor = new Object();
    private static boolean sCreated;

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
                boolean containsClazz = sServiceMap.containsKey(clazz);

                if (containsClazz == add) {
                    if (Build.DEBUG) Log.e(TAG, "Trying to add an existing service.");
                    return;
                }

                if (add) {
                    try {
                        // Adding child to parent service.
                        ChildService child = (ChildService) clazz.newInstance();
                        child.setContext(BathService.this);
                        child.onCreate();
                        sServiceMap.put(clazz, child);
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new RuntimeException(e.getMessage()); // Should never happen
                    }
                    updateNotification();
                    return;
                }

                // Removing child from parent service.
                ChildService child = sServiceMap.remove(clazz);
                child.onDestroy();

                if (sServiceMap.isEmpty()) {
                    BathService.this.stopSelf();
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

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_ADD_SERVICE);
            intentFilter.addAction(ACTION_REMOVE_SERVICE);
            mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
            mLocalBroadcastManager.registerReceiver(mReceiver, intentFilter);

            Enumeration<ChildService> elements = sServiceMap.elements();
            while (elements.hasMoreElements()) {
                ChildService child = elements.nextElement();
                child.setContext(this);
                child.onCreate();
            }

            startForeground(App.ID_NOTIFY_BATH, buildNotification());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        synchronized (monitor) {
            sCreated = false;

            mLocalBroadcastManager.unregisterReceiver(mReceiver);
            mLocalBroadcastManager = null;

            Enumeration<ChildService> elements = sServiceMap.elements();
            while (elements.hasMoreElements()) {
                ChildService child = elements.nextElement();
                child.onDestroy();
            }
            sServiceMap.clear();
        }
    }

    private void updateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(App.ID_NOTIFY_BATH, buildNotification());
    }

    private Notification buildNotification() {
        boolean empty = true;

        StringBuilder builder = new StringBuilder();
        String divider = getString(R.string.settings_multi_list_divider);
        Enumeration<ChildService> elements = sServiceMap.elements();
        while (elements.hasMoreElements()) {
            if (!empty) {
                builder.append(divider);
            }
            ChildService child = elements.nextElement();
            builder.append(child.getName());
            empty = false;
        }

        String contentText = builder.toString();
        contentText = contentText.charAt(0) + contentText.substring(1).toLowerCase();

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
     * An implementation of fake service.
     *
     * @author Artem Chepurnoy
     */
    public abstract static class ChildService {

        private Context mContext;

        public ChildService() {
            if (Build.DEBUG) Log.d(TAG, "Creating child service...");
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

        public abstract String getName();

        public Context getContext() {
            return mContext;
        }

    }

}
