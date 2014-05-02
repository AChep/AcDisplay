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

package com.achep.activedisplay.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.achep.activedisplay.Config;
import com.achep.activedisplay.InactiveHoursHelper;
import com.achep.activedisplay.NotificationIds;
import com.achep.activedisplay.Operator;
import com.achep.activedisplay.Presenter;
import com.achep.activedisplay.Project;
import com.achep.activedisplay.R;
import com.achep.activedisplay.activemode.ProximitySensor;
import com.achep.activedisplay.blacklist.AppConfig;
import com.achep.activedisplay.blacklist.Blacklist;
import com.achep.activedisplay.utils.PowerUtils;

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
            switch (key) {
                case Config.KEY_LOW_PRIORITY_NOTIFICATIONS:
                    handleLowPriorityNotificationsPreferenceChanged();
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

    private NotificationPresenter(Context context) {
        mListeners = new ArrayList<>();
        mGList = new NotificationList(null);
        mLList = new NotificationList(this);

        mConfig = Config.getInstance(context);
        mConfig.addOnConfigChangedListener(new ConfigListener());

        mBlacklist = Blacklist.getInstance(context);
        mBlacklist.registerListener(new BlacklistListener());
    }

    public synchronized static NotificationPresenter getInstance(Context context) {
        if (sNotificationPresenter == null) {
            sNotificationPresenter = new NotificationPresenter(context);
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
        OpenStatusBarNotification osbn = OpenStatusBarNotification.wrap(n);

        boolean globalValid = isValidForGlobal(n);
        boolean localValid = globalValid && isValidForLocal(n);

        // If notification will not be added to the
        // list there's no point of loading its data.
        if (globalValid) {
            osbn.loadData(context);
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
        OpenStatusBarNotification osbn = OpenStatusBarNotification.wrap(n);
        mGList.remove(osbn);
        mLList.remove(osbn);
    }

    private void rebuildLocalList() {
        if (Project.DEBUG) Log.d(TAG, "Rebuilding local list of notifications.");

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
     * Starts {@link com.achep.activedisplay.activities.AcDisplayActivity activity} if active display
     * is enabled and screen is turned off and...
     */
    private boolean tryStartGui(Context context, OpenStatusBarNotification notification) {
        String packageName = notification.getStatusBarNotification().getPackageName();
        if (ProximitySensor.isNear()
                || mConfig.isEnabled() == false
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
                nm.notify(NotificationIds.INIT_NOTIFICATION, builder.build());
            }
        }, 2500);
    }

    @SuppressWarnings("PointlessBooleanExpression")
    void tryInit(NotificationHandleService service, StatusBarNotification n, StatusBarNotification[] activeNotifications) {
        if (mInitProcess != INITIALIZING_PROCESS_STARTED
                // Is posted notification equals to init notification?
                || n.getId() != NotificationIds.INIT_NOTIFICATION
                || n.getPackageName().equals(Project.getPackageName(service)) == false) {
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

        NotificationHelper.dismissNotification(n);
    }

}


