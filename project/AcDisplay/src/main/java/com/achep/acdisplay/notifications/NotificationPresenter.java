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

package com.achep.acdisplay.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.achep.acdisplay.App;
import com.achep.acdisplay.Build;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.InactiveHoursHelper;
import com.achep.acdisplay.Operator;
import com.achep.acdisplay.Presenter;
import com.achep.acdisplay.activemode.sensors.ProximitySensor;
import com.achep.acdisplay.blacklist.AppConfig;
import com.achep.acdisplay.blacklist.Blacklist;
import com.achep.acdisplay.utils.PackageUtils;
import com.achep.acdisplay.utils.PowerUtils;

import java.util.ArrayList;

/**
 * Created by Artem on 27.12.13.
 */
public class NotificationPresenter implements NotificationList.Callback {

    private static final String TAG = "NotificationPresenter";

    public static final int EVENT_BATH = 0;
    public static final int EVENT_POSTED = 1;
    public static final int EVENT_CHANGED = 2;
    public static final int EVENT_REMOVED = 3;

    private static final int INITIALIZING_PROCESS_NONE = 0;
    private static final int INITIALIZING_PROCESS_STARTED = 1;
    private static final int INITIALIZING_PROCESS_DONE = 2;

    private static NotificationPresenter sNotificationPresenter;

    private int mInitProcess = INITIALIZING_PROCESS_NONE;

    private final NotificationList mGList;
    private final NotificationList mLList;

    private final ArrayList<OnNotificationListChangedListener> mListeners;
    private final Config mConfig;
    private final Blacklist mBlacklist;

    /**
     * Listens to config to update notification list when needed.
     */
    private class ConfigListener implements Config.OnConfigChangedListener {

        @Override
        public void onConfigChanged(Config config, String key, Object value) {
            boolean enabled;
            switch (key) {
                case Config.KEY_NOTIFY_LOW_PRIORITY:
                    handleLowPriorityNotificationsPreferenceChanged();
                    break;
                case Config.KEY_UI_DYNAMIC_BACKGROUND_MODE:
                    enabled = Operator.bitAnd((int) value, Config.DYNAMIC_BG_NOTIFICATION_MASK);
                    for (OpenStatusBarNotification osbn : mGList.list()) {
                        StatusBarNotification sbn = osbn.getStatusBarNotification();
                        NotificationData data = osbn.getNotificationData();

                        if (enabled) {
                            data.loadBackground(config.getContext(), sbn);
                        } else {
                            data.clearBackground();
                        }
                    }
                    break;
                case Config.KEY_UI_NOTIFY_CIRCLED_ICON:
                    enabled = (boolean) value;
                    for (OpenStatusBarNotification osbn : mGList.list()) {
                        StatusBarNotification sbn = osbn.getStatusBarNotification();
                        NotificationData data = osbn.getNotificationData();

                        if (enabled) {
                            data.loadCircleIcon(sbn);
                        } else {
                            data.clearCircleIcon();
                        }
                    }
                    break;
            }
        }

        private void handleLowPriorityNotificationsPreferenceChanged() {
            rebuildLocalList(new Comparator() {
                @Override
                public boolean needsRebuild(OpenStatusBarNotification osbn) {
                    StatusBarNotification sbn = osbn.getStatusBarNotification();
                    return sbn.getNotification().priority <= Notification.PRIORITY_LOW;
                }
            });
        }
    }

    private class BlacklistListener extends Blacklist.OnBlacklistChangedListener {

        @Override
        public void onBlacklistChanged(final AppConfig configNew, AppConfig configOld, int diff) {
            if (configNew.isHiddenReal() != configOld.isHiddenReal()) {
                handlePackageVisibilityChanged(configNew.packageName);
            }
        }

        private void handlePackageVisibilityChanged(final String packageName) {
            rebuildLocalList(new Comparator() {
                @Override
                public boolean needsRebuild(OpenStatusBarNotification osbn) {
                    StatusBarNotification sbn = osbn.getStatusBarNotification();
                    return sbn.getPackageName().equals(packageName);
                }
            });
        }
    }

    private interface Comparator {
        public boolean needsRebuild(OpenStatusBarNotification osbn);
    }

    private void rebuildLocalList(Comparator comparator) {
        for (OpenStatusBarNotification osbn : mGList.list()) {
            if (comparator.needsRebuild(osbn)) {
                rebuildLocalList();
                break;
            }
        }
    }

    // //////////////////////////////////////////
    // ////////////// -- MAIN -- ////////////////
    // //////////////////////////////////////////

    public interface OnNotificationListChangedListener {

        /**
         * Callback that the list of notifications has changed.
         *
         * @param osbn  an instance of notification.
         * @param event event type:
         *              {@link #EVENT_BATH}, {@link #EVENT_POSTED},
         *              {@link #EVENT_CHANGED}, {@link #EVENT_REMOVED}
         */
        public void onNotificationListChanged(NotificationPresenter np,
                                              OpenStatusBarNotification osbn, int event);

    }

    public void addOnNotificationListChangedListener(OnNotificationListChangedListener listener) {
        mListeners.add(listener);
    }

    public void removeOnNotificationListChangedListener(OnNotificationListChangedListener listener) {
        mListeners.remove(listener);
    }

    private NotificationPresenter() {
        mListeners = new ArrayList<>();
        mGList = new NotificationList(null);
        mLList = new NotificationList(this);

        mConfig = Config.getInstance();
        mConfig.addOnConfigChangedListener(new ConfigListener());

        mBlacklist = Blacklist.getInstance();
        mBlacklist.registerListener(new BlacklistListener());
    }

    public synchronized static NotificationPresenter getInstance() {
        if (sNotificationPresenter == null) {
            sNotificationPresenter = new NotificationPresenter();
        }
        return sNotificationPresenter;
    }

    /**
     * Called on {@link NotificationHandleService#onNotificationPosted(android.service.notification.StatusBarNotification)}
     */
    public void postNotification(Context context, StatusBarNotification n) {
        postNotification(context, n, false);
    }

    public void postNotification(Context context, StatusBarNotification n, boolean silently) {
        logNotification(n, "posted");
        OpenStatusBarNotification osbn = new OpenStatusBarNotification(n);

        boolean globalValid = isValidForGlobal(n);
        boolean localValid = globalValid && isValidForLocal(n);

        // If notification will not be added to the
        // list there's no point of loading its data.
        if (globalValid) {
            osbn.loadData(context);

            StatusBarNotification sbn = osbn.getStatusBarNotification();
            NotificationData data = osbn.getNotificationData();
            Config config = Config.getInstance();

            // Selective load exactly what we need and nothing more.
            // This will reduce RAM consumption for a bit (1% or so.)
            if (config.isCircledLargeIconEnabled())
                data.loadCircleIcon(sbn);
            if (Operator.bitAnd(
                    config.getDynamicBackgroundMode(),
                    Config.DYNAMIC_BG_NOTIFICATION_MASK))
                data.loadBackground(context, sbn);
        }

        mGList.pushOrRemove(osbn, globalValid, silently);
        mLList.pushOrRemove(osbn, localValid, silently);

        if (localValid && !silently) {
            tryStartGui(context, osbn);
        }
    }

    /**
     * Called on {@link NotificationHandleService#onNotificationRemoved(android.service.notification.StatusBarNotification)}
     */
    public void removeNotification(Context context, StatusBarNotification n) {
        logNotification(n, "removed");
        OpenStatusBarNotification osbn = new OpenStatusBarNotification(n);
        mGList.remove(osbn);
        mLList.remove(osbn);
    }

    private void rebuildLocalList() {
        if (Build.DEBUG) Log.d(TAG, "Rebuilding local list of notifications.");

        ArrayList<OpenStatusBarNotification> list = mLList.list();
        list.clear();

        for (OpenStatusBarNotification notification : mGList.list()) {
            if (isValidForLocal(notification.getStatusBarNotification()))
                list.add(notification);
        }

        notifyListeners(null, EVENT_BATH);
    }

    public ArrayList<OpenStatusBarNotification> getList() {
        return mLList.list();
    }

    // //////////////////////////////////////////
    // ///////////// -- EVENTS -- ///////////////
    // //////////////////////////////////////////

    @Override
    public int onNotificationAdded(OpenStatusBarNotification n) {
        notifyListeners(n, EVENT_POSTED);
        return 0;
    }

    @Override
    public int onNotificationChanged(OpenStatusBarNotification n) {
        notifyListeners(n, EVENT_CHANGED);
        return 0;
    }

    @Override
    public int onNotificationRemoved(OpenStatusBarNotification n) {
        notifyListeners(n, EVENT_REMOVED);
        return 0;
    }

    // //////////////////////////////////////////
    // //////// -- NOTIFICATION UTILS -- ////////
    // //////////////////////////////////////////

    private void notifyListeners(OpenStatusBarNotification notification, int event) {
        for (OnNotificationListChangedListener listener : mListeners) {
            listener.onNotificationListChanged(this, notification, event);
        }
    }

    /**
     * Returns {@code false} if the notification doesn't fit
     * the requirements (such as not ongoing and clearable).
     */
    private boolean isValidForLocal(StatusBarNotification n) {
        AppConfig config = mBlacklist.getAppConfig(n.getPackageName());

        boolean hidden = config.enabled && config.isHidden();
        boolean priorityNormal = n.getNotification().priority >= Notification.PRIORITY_LOW;
        boolean lowPriorPassed = priorityNormal || mConfig.isLowPriorityNotificationsAllowed();

        return lowPriorPassed && !hidden;
    }

    private boolean isValidForGlobal(StatusBarNotification n) {
        return !n.isOngoing() && n.isClearable();
    }

    private void logNotification(StatusBarNotification n, String action) {
        Log.d(TAG, "Notification " + action + ": package=" + n.getPackageName()
                + " id=" + n.getId()
                + " user_id=" + n.getUserId()
                + " tag=" + n.getTag()
                + " post_time=" + n.getPostTime()
                + " is_valid_global=" + isValidForGlobal(n)
                + " is_valid_local=" + isValidForLocal(n));
    }

    // //////////////////////////////////////////
    // ///////// -- USER INTERFACE -- ///////////
    // //////////////////////////////////////////

    /**
     * Starts {@link com.achep.acdisplay.acdisplay.AcDisplayActivity activity} if active display
     * is enabled and screen is turned off and...
     */
    private boolean tryStartGui(Context context, OpenStatusBarNotification notification) {
        String packageName = notification.getStatusBarNotification().getPackageName();
        if (ProximitySensor.isNear()
                || mConfig.isEnabled() == false
                || mConfig.isNotifyWakingUp() == false
                || mConfig.isEnabledOnlyWhileCharging() /* show only      */
                && !PowerUtils.isPlugged(context)       /* while charging */
                || mBlacklist.getAppConfig(packageName).isRestrictedReal())
            return false;

        // Inactive time
        if (mConfig.isInactiveTimeEnabled() && InactiveHoursHelper.isInactiveTime(mConfig)) {
            return false;
        }

        Presenter.getInstance().start(context);
        return true;
    }

    // //////////////////////////////////////////
    // ////////// -- INITIALIZING -- ////////////
    // //////////////////////////////////////////

    /**
     * Should be called when notification listener service is ready to receive new notifications.
     */
    // Running on wrong thread
    void tryStartInitProcess() {
        if (mInitProcess != INITIALIZING_PROCESS_NONE) {
            return;
        }

        mInitProcess = INITIALIZING_PROCESS_STARTED;

        // Well I know that handler doesn't work properly on deep sleep.
        // This is okay. It'll send this init notification after waking up.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                NotificationHandleService service = NotificationHandleService.sService;

                if (service == null) {
                    Log.w(TAG, "Tried to send an init-notification but notification service is offline.");
                    return;
                }

                Resources res = service.getResources();
                Notification.Builder builder = new Notification.Builder(service)
                        .setContentTitle(res.getString(R.string.app_name))
                        .setContentText(res.getString(R.string.init_notification_text))
                        .setSmallIcon(R.drawable.stat_notify)
                        .setPriority(Notification.PRIORITY_MIN)
                        .setAutoCancel(true);

                NotificationManager nm = (NotificationManager)
                        service.getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(App.ID_NOTIFY_INIT, builder.build());
            }
        }, 2500);
    }

    @SuppressWarnings("PointlessBooleanExpression")
    void tryInit(NotificationHandleService service, final StatusBarNotification n, StatusBarNotification[] activeNotifications) {
        if (mInitProcess != INITIALIZING_PROCESS_STARTED
                // Is posted notification equals to init notification?
                || n.getId() != App.ID_NOTIFY_INIT
                || n.getPackageName().equals(PackageUtils.getName(service)) == false) {
            return;
        } else {
            mInitProcess = INITIALIZING_PROCESS_DONE;
        }

        if (activeNotifications != null) {
            for (StatusBarNotification notification : activeNotifications) {
                postNotification(service, notification, true);
            }
            notifyListeners(null, EVENT_BATH);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                NotificationHelper.dismissNotification(n);
            }
        }, 1000);
    }

    boolean isInitialized() {
        return mInitProcess == INITIALIZING_PROCESS_DONE;
    }

}


