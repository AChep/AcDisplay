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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.achep.acdisplay.App;
import com.achep.acdisplay.R;
import com.achep.acdisplay.notifications.NotificationHelper;
import com.achep.acdisplay.ui.activities.MainActivity;
import com.achep.base.AppHeap;
import com.achep.base.interfaces.IOnLowMemory;
import com.achep.base.services.BaseService;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.achep.base.Build.DEBUG;

/**
 * Created by achep on 26.05.14.
 *
 * @author Artem Chepurnoy
 */
public class BathService extends BaseService {

    private static final String TAG = "BathService";

    private static final String ACTION_ADD_SERVICE = TAG + ":add_service";
    private static final String ACTION_REMOVE_SERVICE = TAG + ":remove_service";
    private static final String EXTRA_SERVICE_CLASS = "class";

    public static void startService(Context context, Class<? extends ChildService> clazz) {
        synchronized (monitor) {
            if (sRunning) {
                Intent intent = new Intent(ACTION_ADD_SERVICE);
                intent.putExtra(EXTRA_SERVICE_CLASS, clazz);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } else if (!sServiceMap.containsKey(clazz)) {
                ChildService instance;
                try {
                    instance = clazz.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                sServiceMap.put(clazz, instance);
                if (!sCreated) context.startService(new Intent(context, BathService.class));
            }
        }
    }

    public static void stopService(Context context, Class<? extends ChildService> clazz) {
        synchronized (monitor) {
            if (sRunning) {
                Intent intent = new Intent(ACTION_REMOVE_SERVICE);
                intent.putExtra(EXTRA_SERVICE_CLASS, clazz);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } else {
                sServiceMap.remove(clazz);
            }
        }
    }

    private static final Map<Class, ChildService> sServiceMap = new ConcurrentHashMap<>(2);
    private static final Object monitor = new Object();
    private static boolean sCreated;
    private static boolean sRunning;

    private LocalBroadcastManager mLocalBroadcastManager;
    private NotificationManager mNotificationManager;
    private String mLanguage;

    private final Map<Class, ChildService> mMap = new HashMap<>(2);
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                // Received from a local broadcast receiver.
                case ACTION_ADD_SERVICE:
                case ACTION_REMOVE_SERVICE:
                    synchronized (monitor) {
                        Class clazz = (Class) intent.getSerializableExtra(EXTRA_SERVICE_CLASS);
                        boolean addition = ACTION_ADD_SERVICE.equals(action);
                        boolean exists = mMap.containsKey(clazz);
                        if (addition == exists) return;
                        if (addition) { // Addition
                            ChildService child;
                            try {
                                // Adding child to host service.
                                child = (ChildService) clazz.newInstance();
                            } catch (Exception e) {
                                throw new RuntimeException(e); // Should never happen
                            }
                            child.setContext(BathService.this);
                            child.onCreate();
                            mMap.put(clazz, child);

                            updateNotification();
                        } else { // Removal
                            ChildService child = mMap.remove(clazz);
                            child.onDestroy();
                            child.setContext(null);

                            if (mMap.isEmpty()) {
                                stopMySelf();
                            } else updateNotification();
                        }
                    }
                    break;
                // Received from a system broadcast receiver.
                case Intent.ACTION_CONFIGURATION_CHANGED:
                    String lang = getResources().getConfiguration().locale.getLanguage();
                    if (!TextUtils.equals(mLanguage, lang)) {
                        mLanguage = lang;
                        updateNotification();
                    }
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mLanguage = getResources().getConfiguration().locale.getLanguage();

        // Listen for the config changes to update notification just
        // once locale has changed.
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        registerReceiver(mReceiver, intentFilter);

        synchronized (monitor) {
            sCreated = true;
            sRunning = true;

            // Register for add / remove service events.
            intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_ADD_SERVICE);
            intentFilter.addAction(ACTION_REMOVE_SERVICE);
            mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
            mLocalBroadcastManager.registerReceiver(mReceiver, intentFilter);

            if (sServiceMap.isEmpty()) {
                stopMySelf();
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
    public void onLowMemory() {
        super.onLowMemory();
        synchronized (monitor) {
            for (ChildService child : mMap.values()) {
                child.onLowMemory();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        synchronized (monitor) {
            sCreated = false;
            sRunning = false;
            mLocalBroadcastManager.unregisterReceiver(mReceiver);

            // Kill all children.
            for (ChildService child : mMap.values()) child.onDestroy();
            mMap.clear();

            // TODO: Should I add children back to pending map and then
            // restart the service?
            if (!sServiceMap.isEmpty()) startService(new Intent(this, getClass()));
        }

        unregisterReceiver(mReceiver);
        // Make sure that notification does not exists.
        mNotificationManager.cancel(App.ID_NOTIFY_BATH);

        // Leaks canary
        AppHeap.getRefWatcher().watch(this);
    }

    private void stopMySelf() {
        sRunning = false;
        stopSelf();
    }

    private void updateNotification() {
        mNotificationManager.notify(App.ID_NOTIFY_BATH, buildNotification());
    }

    /**
     * Builds fresh notification with all {@link ChildService children services}'s
     * {@link com.achep.acdisplay.services.BathService.ChildService#getLabel() labels} in.
     */
    @NonNull
    private Notification buildNotification() {
        boolean empty = true;
        StringBuilder sb = new StringBuilder();
        String divider = getString(R.string.settings_multi_list_divider);
        for (ChildService child : mMap.values()) {
            String label = child.getLabel();
            if (TextUtils.isEmpty(label)) {
                if (DEBUG) {
                    label = "[" + child.getClass().getSimpleName() + "]";
                } else continue;
            }
            if (!empty) {
                sb.append(divider);
            }
            sb.append(label);
            empty = false;
        }

        // Format a message text.
        String contentText = sb.toString();
        if (contentText.length() > 0 && !mLanguage.contains("de")) {
            contentText = contentText.charAt(0) + contentText.substring(1).toLowerCase();
        }

        // Get notification intent.
        Intent intent = null;
        for (ChildService child : mMap.values())
            if (!TextUtils.isEmpty(child.getLabel())) {
                if (intent == null) {
                    intent = child.getSettingsIntent();
                } else {
                    intent = null;
                    break;
                }
            }
        if (intent == null) {
            intent = new Intent(this, MainActivity.class);
        }

        return NotificationHelper.buildNotification(this, App.ID_NOTIFY_BATH, contentText, intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //-- CHILD SERVICE --------------------------------------------------------

    /**
     * Base for fake foreground service hosted in {@link com.achep.acdisplay.services.BathService}.
     * Call {@link BathService#startService(android.content.Context, Class)} to start this service,
     * and {@link BathService#stopService(android.content.Context, Class)} to stop.
     *
     * @author Artem Chepurnoy
     */
    public abstract static class ChildService implements IOnLowMemory {

        private Context mContext;

        public ChildService() {
            if (DEBUG) {
                Log.d(TAG, "Creating " + getClass().getSimpleName() + " service...");
            }
        }

        final void setContext(Context context) {
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
         * {@inheritDoc}
         */
        @Override
        public void onLowMemory() { /* placeholder */ }

        /**
         * @return The human-readable label of this service.
         */
        @Nullable
        public String getLabel() {
            return null;
        }

        @Nullable
        public Intent getSettingsIntent() {
            return null;
        }

        public final Context getContext() {
            return mContext;
        }

    }

}
