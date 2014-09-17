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

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.achep.acdisplay.App;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.InactiveTimeHelper;
import com.achep.acdisplay.Operator;
import com.achep.acdisplay.Presenter;
import com.achep.acdisplay.R;
import com.achep.acdisplay.blacklist.AppConfig;
import com.achep.acdisplay.blacklist.Blacklist;
import com.achep.acdisplay.services.MediaService;
import com.achep.acdisplay.services.activemode.sensors.ProximitySensor;
import com.achep.acdisplay.utils.PackageUtils;
import com.achep.acdisplay.utils.PowerUtils;

import java.util.ArrayList;

/**
 * Created by Artem on 27.12.13.
 */
public class NotificationPresenter implements NotificationList.OnNotificationListChangedListener {

    private static final String TAG = "NotificationPresenter";

    public static final int EVENT_BATH = 0;
    public static final int EVENT_POSTED = 1;
    public static final int EVENT_CHANGED = 2;
    public static final int EVENT_CHANGED_SPAM = 3;
    public static final int EVENT_REMOVED = 4;

    private static final int RESULT_SUCCESS = 1;
    private static final int RESULT_SPAM = -1;

    private static final int INITIALIZING_PROCESS_NONE = 0;
    private static final int INITIALIZING_PROCESS_STARTED = 1;
    private static final int INITIALIZING_PROCESS_DONE = 2;

    private static final int FLAG_DONT_NOTIFY_FOLLOWERS = 1;
    private static final int FLAG_DONT_WAKE_UP = 2;

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
                    for (OpenNotification n : mGList.list()) {
                        NotificationData data = n.getNotificationData();

                        if (enabled) {
                            data.loadBackground(config.getContext(), n);
                        } else {
                            data.clearBackground();
                        }
                    }
                    break;
                case Config.KEY_UI_NOTIFY_CIRCLED_ICON:
                    enabled = (boolean) value;
                    for (OpenNotification n : mGList.list()) {
                        NotificationData data = n.getNotificationData();

                        if (enabled) {
                            data.loadCircleIcon(n);
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
                public boolean needsRebuild(OpenNotification osbn) {
                    return osbn.getNotification().priority <= Notification.PRIORITY_LOW;
                }
            });
        }
    }

    private class BlacklistListener extends Blacklist.OnBlacklistChangedListener {

        @Override
        public void onBlacklistChanged(
                @NonNull AppConfig configNew,
                @NonNull AppConfig configOld, int diff) {
            boolean hiddenNew = configNew.forReal(configNew.isHidden());
            boolean hiddenOld = configOld.forReal(configOld.isHidden());
            boolean nonClearableEnabledNew = configNew.forReal(configNew.isNonClearableEnabled());
            boolean nonClearableEnabledOld = configOld.forReal(configOld.isNonClearableEnabled());

            // Check if something important has changed.
            if (hiddenNew != hiddenOld || nonClearableEnabledNew != nonClearableEnabledOld) {
                handlePackageVisibilityChanged(configNew.packageName);
            }
        }

        private void handlePackageVisibilityChanged(final String packageName) {
            rebuildLocalList(new Comparator() {
                @Override
                public boolean needsRebuild(OpenNotification osbn) {
                    return osbn.getPackageName().equals(packageName);
                }
            });
        }
    }

    private interface Comparator {
        public boolean needsRebuild(OpenNotification osbn);
    }

    private void rebuildLocalList(Comparator comparator) {
        for (OpenNotification n : mGList.list()) {
            if (comparator.needsRebuild(n)) {
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
         *              {@link #EVENT_CHANGED}, {@link #EVENT_CHANGED_SPAM},
         *              {@link #EVENT_REMOVED}
         */
        public void onNotificationListChanged(@NonNull NotificationPresenter np,
                                              @Nullable OpenNotification osbn, int event);

    }

    public void registerListener(OnNotificationListChangedListener listener) {
        mListeners.add(listener);
    }

    public void unregisterListener(OnNotificationListChangedListener listener) {
        mListeners.remove(listener);
    }

    private NotificationPresenter() {
        mListeners = new ArrayList<>();
        mGList = new NotificationList(null);
        mLList = new NotificationList(this);

        mConfig = Config.getInstance();
        mConfig.registerListener(new ConfigListener());

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
     * Posts notification to global list, notifies every follower
     * about this change, and tries to launch
     * {@link com.achep.acdisplay.acdisplay.AcDisplayActivity}.
     * <p><i>
     *     To create {@link OpenNotification}, use
     *     {@link OpenNotification#newInstance(StatusBarNotification)} or
     *     {@link OpenNotification#newInstance(android.app.Notification)}
     *     method.
     * </i></p>
     *
     * @see #FLAG_DONT_NOTIFY_FOLLOWERS
     * @see #FLAG_DONT_WAKE_UP
     */
    public void postNotification(
            @NonNull Context context,
            @NonNull OpenNotification n, int flags) {
        boolean globalValid = isValidForGlobal(n);
        boolean localValid = false;

        // If notification will not be added to the
        // list there's no point of loading its data.
        if (globalValid) {
            n.loadData(context);

            NotificationData data = n.getNotificationData();
            data.loadCircleIcon(n);
            Config config = Config.getInstance();

            // Selective load exactly what we need and nothing more.
            // This will reduce RAM consumption for a bit (1% or so.)
            if (Operator.bitAnd(
                    config.getDynamicBackgroundMode(),
                    Config.DYNAMIC_BG_NOTIFICATION_MASK))
                data.loadBackground(context, n);

            localValid = isValidForLocal(n);
        }

        // Extract flags.
        boolean flagIgnoreFollowers = Operator.bitAnd(
                flags, FLAG_DONT_NOTIFY_FOLLOWERS);
        boolean flagWakeUp = !Operator.bitAnd(
                flags, FLAG_DONT_WAKE_UP);

        mGList.pushOrRemove(n, globalValid, flagIgnoreFollowers);
        int result = mLList.pushOrRemove(n, localValid, flagIgnoreFollowers);

        if (flagWakeUp && result == RESULT_SUCCESS) {
            tryStartGui(context, n);
        }
    }

    /**
     * Removes notification from the presenter and sends
     * this event to followers. Calling his method will not
     * remove notification from system!
     */
    public void removeNotification(@NonNull OpenNotification n) {
        mGList.remove(n);
        mLList.remove(n);
    }

    /**
     * Re-validates all notifications from {@link #mGList global list}
     * and sends {@link #EVENT_BATH bath} event after.
     *
     * @see #isValidForLocal(OpenNotification)
     * @see #isValidForGlobal(OpenNotification)
     */
    private void rebuildLocalList() {
        boolean changed = false;

        // Remove not valid notifications
        // from local list.
        ArrayList<OpenNotification> list = mLList.list();
        for (int i = 0; i < list.size(); i++) {
            OpenNotification n = list.get(i);
            if (!isValidForLocal(n)) {
                changed = true;
                list.remove(i--);
            }
        }

        // Add newly valid notifications to local list.
        for (OpenNotification n : mGList.list()) {
            if (isValidForLocal(n) && mLList.indexOf(n) == -1) {
                changed = true;
                list.add(n);
            }
        }

        if (changed) {
            notifyListeners(null, EVENT_BATH);
        }
    }

    public ArrayList<OpenNotification> getList() {
        return mLList.list();
    }

    // //////////////////////////////////////////
    // ///////////// -- EVENTS -- ///////////////
    // //////////////////////////////////////////

    @Override
    public int onNotificationAdded(@NonNull OpenNotification n) {
        notifyListeners(n, EVENT_POSTED);
        return RESULT_SUCCESS;
    }

    @Override
    public int onNotificationChanged(@NonNull OpenNotification n, @NonNull OpenNotification old) {
        // Prevent god damn notification spam by
        // checking texts' equality.

        // An example of notification spammer is well-known
        // DownloadProvider (seriously, Google?)
        NotificationData dataOld = old.getNotificationData();
        NotificationData dataNew = n.getNotificationData();

        if (dataNew.number == dataOld.number
                && TextUtils.equals(dataNew.titleText, dataOld.titleText)
                && TextUtils.equals(dataNew.messageText, dataOld.messageText)
                && TextUtils.equals(dataNew.infoText, dataOld.infoText)) {
            // Technically notification was changed, but it was a fault
            // of dumb developer. Mark notification as read, if old one was.
            n.getNotificationData().markAsRead(old.getNotificationData().isRead);

            if (!n.isMine()) {
                notifyListeners(n, EVENT_CHANGED_SPAM);
                return RESULT_SPAM; // Don't wake up.
            }
        }

        notifyListeners(n, EVENT_CHANGED);
        return RESULT_SUCCESS;
    }

    @Override
    public int onNotificationRemoved(@NonNull OpenNotification n) {
        notifyListeners(n, EVENT_REMOVED);
        n.recycle(); // Free all resources
        return RESULT_SUCCESS;
    }

    // //////////////////////////////////////////
    // //////// -- NOTIFICATION UTILS -- ////////
    // //////////////////////////////////////////

    private void notifyListeners(@Nullable OpenNotification n, int event) {
        for (OnNotificationListChangedListener listener : mListeners) {
            listener.onNotificationListChanged(this, n, event);
        }
    }

    /**
     * Returns {@code false} if the notification doesn't fit
     * the requirements (such as not ongoing and clearable).
     */
    private boolean isValidForLocal(@NonNull OpenNotification o) {
        AppConfig config = mBlacklist.getAppConfig(o.getPackageName());

        if (config.forReal(config.isHidden())) {
            // Do not display any notifications from this app.
            return false;
        }

        if (!o.isClearable() && !config.forReal(config.isNonClearableEnabled())) {
            // Do not display non-clearable notification.
            return false;
        }

        if (o.getNotification().priority <= Notification.PRIORITY_LOW
                && !mConfig.isLowPriorityNotificationsAllowed()) {
            // Do not display low-priority notification.
            return false;
        }

        // Do not allow notifications without any content.
        NotificationData data = o.getNotificationData();
        return !(TextUtils.isEmpty(data.titleText)
                && TextUtils.isEmpty(data.getMergedMessage())
                && TextUtils.isEmpty(data.infoText));
    }

    private boolean isValidForGlobal(@NonNull OpenNotification n) {
        return true;
    }

    // //////////////////////////////////////////
    // ///////// -- USER INTERFACE -- ///////////
    // //////////////////////////////////////////

    @SuppressLint("NewApi")
    private boolean isTestNotification(Context context, OpenNotification n) {
        StatusBarNotification sbn = n.getStatusBarNotification();
        return sbn != null
                && sbn.getId() == App.ID_NOTIFY_INIT
                && n.getPackageName().equals(PackageUtils.getName(context));
    }

    /**
     * Starts {@link com.achep.acdisplay.acdisplay.AcDisplayActivity activity} if active display
     * is enabled and screen is turned off and...
     */
    private boolean tryStartGui(Context context, OpenNotification n) {
        if (!isTestNotification(context, n)) { // force test notification to be shown
            if (!mConfig.isEnabled() || !mConfig.isNotifyWakingUp()
                    // Inactive time
                    || mConfig.isInactiveTimeEnabled()
                    && InactiveTimeHelper.isInactiveTime(mConfig)
                    // Only while charging
                    || mConfig.isEnabledOnlyWhileCharging()
                    && !PowerUtils.isPlugged(context)) {
                // Don't turn screen on due to user settings.
                return false;
            }

            if (ProximitySensor.isNear()) {
                // Don't display while device is face down.
                return false;
            }

            String packageName = n.getPackageName();
            AppConfig config = mBlacklist.getAppConfig(packageName);
            if (config.forReal(config.isRestricted())) {
                // Don't display due to app settings.
                return false;
            }
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
    public void tryStartInitProcess() {
        if (mInitProcess != INITIALIZING_PROCESS_NONE) {
            return;
        }

        mInitProcess = INITIALIZING_PROCESS_STARTED;

        // Well I know that handler doesn't work properly on deep sleep.
        // This is okay. It'll send this init notification after waking up.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                MediaService service = MediaService.sService;

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
    public void tryInit(MediaService service, final StatusBarNotification n, StatusBarNotification[] activeNotifications) {
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
                OpenNotification n1 = OpenNotification.newInstance(notification);
                postNotification(service, n1, FLAG_DONT_NOTIFY_FOLLOWERS | FLAG_DONT_WAKE_UP);
            }
            notifyListeners(null, EVENT_BATH);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                NotificationUtils.dismissNotification(OpenNotification.newInstance(n));
            }
        }, 500);
    }

    public boolean isInitialized() {
        return mInitProcess == INITIALIZING_PROCESS_DONE;
    }

}

