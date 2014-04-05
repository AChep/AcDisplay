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
import android.content.Intent;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.achep.activedisplay.ActiveDisplayPresenter;
import com.achep.activedisplay.Config;
import com.achep.activedisplay.InactiveHoursHelper;
import com.achep.activedisplay.NotificationIds;
import com.achep.activedisplay.Operator;
import com.achep.activedisplay.Project;
import com.achep.activedisplay.R;
import com.achep.activedisplay.activemode.ProximitySensor;
import com.achep.activedisplay.blacklist.AppConfig;
import com.achep.activedisplay.blacklist.Blacklist;
import com.achep.activedisplay.services.SendNotificationService;
import com.achep.activedisplay.utils.PowerUtils;

import java.util.ArrayList;

/**
 * Created by Artem on 27.12.13.
 */
public class NotificationPresenter implements NotificationList.Callback {

    private static final String TAG = "NotificationPresenter";

    private static final int INITIALIZING_PROCESS_NONE = 0;
    private static final int INITIALIZING_PROCESS_STARTED = 1;
    private static final int INITIALIZING_PROCESS_DONE = 2;

    private static NotificationPresenter sNotificationPresenter;

    private int mInitProcess = INITIALIZING_PROCESS_NONE;

    private NotificationList mGList = new NotificationList(null);
    private NotificationList mLList = new NotificationList(this);

    private final ArrayList<OnNotificationListChangedListener> mListeners = new ArrayList<>();
    private final Config mConfig;
    private final Blacklist mBlacklist;

    private class ConfigListener implements Config.OnConfigChangedListener {

        @Override
        public void onConfigChanged(Config config, String key, Object value) {
            if (key.equals(Config.KEY_LOW_PRIORITY_NOTIFICATIONS)) {

                // Check if the change touches our notifications.
                for (OpenStatusBarNotification osbn : mGList.list()) {
                    StatusBarNotification notification = osbn.getStatusBarNotification();

                    if (notification.getNotification().priority <= Notification.PRIORITY_LOW) {
                        rebuildLocalList();
                        break;
                    }
                }
            }
        }
    }

    private class BlacklistListener extends Blacklist.OnBlacklistChangedListener {

        @Override
        public void onBlacklistChanged(AppConfig configNew, AppConfig configOld, int diff) {
            if (Operator.bitandCompare(diff, AppConfig.DIFF_HIDDEN_REAL)) {

                // Check if the change touches our notifications.
                for (OpenStatusBarNotification osbn : mGList.list()) {
                    StatusBarNotification notification = osbn.getStatusBarNotification();

                    if (notification.getPackageName().equals(configNew.packageName)) {
                        rebuildLocalList();
                        break;
                    }
                }
            }
        }
    }

    // //////////////////////////////////////////
    // ////////////// -- MAIN -- ////////////////
    // //////////////////////////////////////////

    private NotificationPresenter(Context context) {
        mConfig = Config.getInstance(context);
        mConfig.addOnConfigChangedListener(new ConfigListener());
        mBlacklist = Blacklist.getInstance(context);
        mBlacklist.addOnSharedListChangedListener(new BlacklistListener());
    }

    public synchronized static NotificationPresenter getInstance(Context context) {
        if (sNotificationPresenter == null)
            sNotificationPresenter = new NotificationPresenter(context);
        return sNotificationPresenter;
    }

    /**
     * Should be called when notification listener service is ready to receive events.
     */
    // Running on wrong thread
    void tryStartInitProcess(final Context context) {
        if (context == null || mInitProcess > INITIALIZING_PROCESS_NONE) return;
        mInitProcess = INITIALIZING_PROCESS_STARTED;

        // Send initializing notification with a short delay.
        Intent notificationIntent = new Intent(context, SendNotificationService.class)
                .putExtra(SendNotificationService.EXTRA_TITLE, context.getString(R.string.app_name))
                .putExtra(SendNotificationService.EXTRA_ID, NotificationIds.INIT_NOTIFICATION)
                .putExtra(SendNotificationService.EXTRA_ICON_RESOURCE, R.drawable.stat_test)
                .putExtra(SendNotificationService.EXTRA_PRIORITY, Notification.PRIORITY_MIN);
        SendNotificationService.notify(context, notificationIntent, 500);
    }

    void tryInit(Context context, StatusBarNotification n, StatusBarNotification[] activeNotifications) {
        if (mInitProcess != INITIALIZING_PROCESS_STARTED
                || !(context instanceof NotificationHandleService)
                || !Project.getPackageName(context).equals(n.getPackageName())) {
            return;
        }
        mInitProcess = INITIALIZING_PROCESS_DONE;

        if (activeNotifications != null) {
            for (StatusBarNotification notification : activeNotifications) {
                postNotification(context, notification, true);
            }

            for (OnNotificationListChangedListener listener : mListeners)
                listener.onNotificationInitialized(this);
        } else {
            Log.w(TAG, "Failed to get current active notifications!");
        }

        // Init notification isn't needed anymore.
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NotificationIds.INIT_NOTIFICATION);
    }

    // //////////////////////////////////////////
    // //////// -- POST NOTIFICATION -- /////////
    // //////////////////////////////////////////

    public ArrayList<OpenStatusBarNotification> getList() {
        return mLList.list();
    }

    public void postNotification(Context context, StatusBarNotification n) {
        postNotification(context, n, false);
    }

    public void postNotification(Context context, StatusBarNotification n, boolean internal) {
        logNotification(n, "Post");
        OpenStatusBarNotification osbn = OpenStatusBarNotification.wrap(n);

        boolean globalValid = isValidForGlobal(n);
        boolean localValid = globalValid && isValidForLocal(n);

        if (globalValid) {
            osbn.loadData(context);
        }

        mGList.pushOrRemove(osbn, globalValid, internal);
        mLList.pushOrRemove(osbn, localValid, internal);

        if (localValid && !internal) {
            tryStartGui(context, osbn);
        }
    }

    public void removeNotification(Context context, StatusBarNotification n) {
        logNotification(n, "Remove");
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

        for (OnNotificationListChangedListener listener : mListeners)
            listener.onNotificationInitialized(this);
    }

    // //////////////////////////////////////////
    // ///////////// -- EVENTS -- ///////////////
    // //////////////////////////////////////////

    @Override
    public int onNotificationAdded(OpenStatusBarNotification n) {
        for (OnNotificationListChangedListener listener : mListeners)
            listener.onNotificationPosted(this, n);
        return 0;
    }

    @Override
    public int onNotificationChanged(OpenStatusBarNotification n) {
        for (OnNotificationListChangedListener listener : mListeners)
            listener.onNotificationChanged(this, n);
        return 0;
    }

    @Override
    public int onNotificationRemoved(OpenStatusBarNotification n) {
        for (OnNotificationListChangedListener listener : mListeners)
            listener.onNotificationRemoved(this, n);
        return 0;
    }

    // //////////////////////////////////////////
    // //////// -- NOTIFICATION UTILS -- ////////
    // //////////////////////////////////////////

    /**
     * Returns {@code false} if the notification doesn't fit
     * the requirements (such as not ongoing and clearable).
     */
    private boolean isValidForLocal(StatusBarNotification n) {
        AppConfig config = AppConfig.wrap(n.getPackageName());
        mBlacklist.fill(config);

        boolean hidden = config.enabled && config.isHidden();

        return (n.getNotification().priority >= Notification.PRIORITY_LOW
                || mConfig.isLowPriorityNotificationsAllowed())
                && !hidden;
    }

    private boolean isValidForGlobal(StatusBarNotification n) {
        return !n.isOngoing() && n.isClearable();
    }

    // //////////////////////////////////////////
    // ///////// -- USER INTERFACE -- ///////////
    // //////////////////////////////////////////

    /**
     * Starts {@link com.achep.activedisplay.activities.AcDisplayActivity activity} if active display
     * is enabled and screen is turned off and...
     */
    private boolean tryStartGui(Context context, OpenStatusBarNotification notification) {
        if (notification.isRestricted(mBlacklist)
                || !mConfig.isActiveDisplayEnabled()
                || ProximitySensor.isNear()
                || mConfig.isEnabledOnlyWhileCharging() /* show only      */
                && !PowerUtils.isPlugged(context))     /* while charging */
            return false;

        // Inactive time
        if (mConfig.isInactiveTimeEnabled() && InactiveHoursHelper.isInactiveTime(mConfig)) {
            return false;
        }

        ActiveDisplayPresenter.getInstance().start(context);
        return true;
    }

    // //////////////////////////////////////////
    // /////////// -- LOG THINGS -- /////////////
    // //////////////////////////////////////////

    private void logNotification(StatusBarNotification n, String action) {
        Log.d(TAG, action + ": package=" + n.getPackageName()
                + " id=" + n.getId()
                + " user_id=" + n.getUserId()
                + " tag=" + n.getTag()
                + " post_time=" + n.getPostTime()
                + " is_valid_global=" + isValidForGlobal(n)
                + " is_valid_local=" + isValidForLocal(n));
    }

    // //////////////////////////////////////////
    // /////////// -- LISTENERS -- //////////////
    // //////////////////////////////////////////

    public void addOnNotificationListChangedListener(OnNotificationListChangedListener listener) {
        mListeners.add(listener);
    }

    public void removeOnNotificationListChangedListener(OnNotificationListChangedListener listener) {
        mListeners.remove(listener);
    }

    public interface OnNotificationListChangedListener {

        public void onNotificationInitialized(NotificationPresenter nm);

        public void onNotificationPosted(NotificationPresenter nm,
                                         OpenStatusBarNotification notification);

        public void onNotificationChanged(NotificationPresenter nm,
                                          OpenStatusBarNotification notification);

        public void onNotificationRemoved(NotificationPresenter nm,
                                          OpenStatusBarNotification notification);

    }

    public static class SimpleOnNotificationListChangedListener implements OnNotificationListChangedListener {

        public static final int INITIALIZED = 0;
        public static final int POSTED = 1;
        public static final int CHANGED = 2;
        public static final int REMOVED = 3;

        @Override
        public void onNotificationInitialized(NotificationPresenter nm) {
            onNotificationEvent(nm, null, INITIALIZED);
        }

        @Override
        public void onNotificationPosted(NotificationPresenter nm,
                                         OpenStatusBarNotification notification) {
            onNotificationEvent(nm, notification, POSTED);
        }

        @Override
        public void onNotificationChanged(NotificationPresenter nm,
                                          OpenStatusBarNotification notification) {
            onNotificationEvent(nm, notification, CHANGED);
        }

        @Override
        public void onNotificationRemoved(NotificationPresenter nm,
                                          OpenStatusBarNotification notification) {
            onNotificationEvent(nm, notification, REMOVED);
        }

        public void onNotificationEvent(NotificationPresenter nm,
                                        OpenStatusBarNotification notification,
                                        int event) {
        }
    }
}
