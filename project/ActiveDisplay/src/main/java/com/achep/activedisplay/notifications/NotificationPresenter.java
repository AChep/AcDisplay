/*
 * Copyright (C) 2013-2014 AChep@xda <artemchep@gmail.com>
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
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.achep.activedisplay.Config;
import com.achep.activedisplay.NotificationIds;
import com.achep.activedisplay.Operator;
import com.achep.activedisplay.Project;
import com.achep.activedisplay.R;
import com.achep.activedisplay.activities.ActiveDisplayActivity;
import com.achep.activedisplay.utils.LogUtils;
import com.achep.activedisplay.utils.PowerUtils;

import java.util.ArrayList;

/**
 * Created by Artem on 27.12.13.
 */
public class NotificationPresenter {

    private static final String TAG = "NotificationPresenter";

    private static final String COM_ANDROID_PROVIDERS_DOWNLOADS = "com.android.providers.downloads";

    private static final int UI_MULTI_START_TIMEOUT = 1000; // ms.

    private static final int INITIALIZING_PROCESS_NONE = 0;
    private static final int INITIALIZING_PROCESS_STARTED = 1;
    private static final int INITIALIZING_PROCESS_DONE = 2;

    private static NotificationPresenter mNotificationPresenter;
    public final Object monitor = new Object();

    private final ArrayList<OpenStatusBarNotification> mNotificationList = new ArrayList<>();
    private final ArrayList<OnNotificationListChangedListener> mListeners = new ArrayList<>();

    private long mUIStartTime;
    private int mInitProcess = INITIALIZING_PROCESS_NONE;

    private OpenStatusBarNotification mSelectedNotification;
    private OpenStatusBarNotification mFastSelectedNotification;
    private boolean mSelectedNotificationLocked;

    // //////////////////////////////////////////
    // ////////////// -- MAIN -- ////////////////
    // //////////////////////////////////////////

    private NotificationPresenter() { /* unused */ }

    public synchronized static NotificationPresenter getInstance() {
        if (mNotificationPresenter == null)
            mNotificationPresenter = new NotificationPresenter();
        return mNotificationPresenter;
    }

    /**
     * Should be called when notification listener service is ready to receive events.
     */
    synchronized void tryStartInitProcess(final Context context) {
        boolean doNotStart = context == null || mInitProcess > INITIALIZING_PROCESS_NONE;
        if (context == null) Log.i(TAG, "Tried to start init process but context is null.");
        if (mInitProcess > INITIALIZING_PROCESS_NONE)
            Log.wtf(TAG, "Initializing process is already on the way.");
        if (doNotStart) return;

        mInitProcess = INITIALIZING_PROCESS_STARTED;
        if (Project.DEBUG) LogUtils.d(TAG, "Sending initial notification.");

        // Send initializing notification with a short delay.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Notification n = new Notification.Builder(context)
                        .setContentTitle(context.getString(R.string.app_name))
                        .setSmallIcon(R.drawable.stat_test)
                        .setPriority(Notification.PRIORITY_MIN)
                        .build();

                NotificationManager nm = (NotificationManager)
                        context.getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(NotificationIds.INIT_NOTIFICATION, n);

                if (Project.DEBUG) LogUtils.d(TAG, "Initial notification sent.");
            }
        }, 500);
    }

    private void tryInit(Context context, StatusBarNotification n) {
        if (mInitProcess != INITIALIZING_PROCESS_STARTED
                || !(context instanceof NotificationHandleService)
                || !Project.getPackageName(context).equals(n.getPackageName())) {
            if (mInitProcess == INITIALIZING_PROCESS_STARTED)
                Log.w(TAG, "Initializing notification failed: " + n.toString());
            return;
        }
        mInitProcess = INITIALIZING_PROCESS_DONE;

        final NotificationHandleService nhs = (NotificationHandleService) context;
        for (StatusBarNotification notification : nhs.getActiveNotifications()) {
            if (Project.DEBUG) logNotification(context, notification, "init post");
            if (!isValid(context, notification) || notification == n)
                continue;

            mNotificationList.add(new OpenStatusBarNotification(notification));
        }

        if (mSelectedNotification == null)
            setSelectedNotification(getLastNotification());
        notifyOnInitialized();

        // Init notification isn't needed anymore.
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(n.getId());
    }

    // //////////////////////////////////////////
    // ////////// -- NOTIFICATIONS -- ///////////
    // //////////////////////////////////////////

    /**
     * If notification is {@link #isValid(Context, StatusBarNotification) valid}
     * puts notification to the list and launches gui.
     *
     * @see OnNotificationListChangedListener#onNotificationPosted(NotificationPresenter, OpenStatusBarNotification)
     * @see OnNotificationListChangedListener#onNotificationChanged(NotificationPresenter, OpenStatusBarNotification)
     */
    synchronized void postNotification(Context context, StatusBarNotification notification) {
        synchronized (monitor) {
            tryInit(context, notification);

            if (Project.DEBUG) logNotification(context, notification, "posted");
            if (!isValid(context, notification)) return;

            final OpenStatusBarNotification openNotification;
            final int index = indexOf(notification);
            if (index == -1) {

                openNotification = new OpenStatusBarNotification(notification);
                mNotificationList.add(openNotification);

                notifyOnPosted(openNotification);
            } else if (isNotificationChangeAllowed(notification)) {

                openNotification = new OpenStatusBarNotification(notification);
                mNotificationList.remove(index);
                mNotificationList.add(index, openNotification);

                if (NotificationUtils.equals(mSelectedNotification, openNotification)
                        || mSelectedNotification == null) {
                    setSelectedNotification(openNotification, true);
                }

                // Notify about notification change
                notifyOnChanged(openNotification);

                if (Operator.bitandCompare(
                        notification.getNotification().flags,
                        Notification.FLAG_ONLY_ALERT_ONCE)) {
                    // Don't start UI second time.
                    return;
                }
            } else {
                return;
            }

            setSelectedNotification(openNotification);
            tryStartUi(context, openNotification);
        }
    }

    synchronized void removeNotification(Context context, StatusBarNotification notification) {
        synchronized (monitor) {
            tryInit(context, notification);

            if (Project.DEBUG) logNotification(context, notification, "removed");
            if (!isValid(context, notification)) return;

            final int index = indexOf(notification);
            if (index != -1) {
                OpenStatusBarNotification rmn = mNotificationList.get(index);
                mNotificationList.remove(index);

                if (NotificationUtils.equals(mSelectedNotification, rmn)) {
                    setSelectedNotification(getLastNotification());
                }

                notifyOnRemoved(rmn);
            }
        }
    }

    private int indexOf(StatusBarNotification n) {
        final int size = mNotificationList.size();
        for (int i = 0; i < size; i++)
            if (NotificationUtils.equals(mNotificationList.get(i)
                    .getStatusBarNotification(), n))
                return i;
        return -1;
    }

    private OpenStatusBarNotification getLastNotification() {
        final int size = mNotificationList.size();
        return size > 0 ? mNotificationList.get(size - 1) : null;
    }

    /**
     * Returns {@code false} if the notification doesn't meet
     * the requirements (such as not ongoing and clearable).
     */
    private boolean isValid(Context context, StatusBarNotification n) {
        boolean isInitNotification = n.getId() == NotificationIds.INIT_NOTIFICATION &&
                !Project.getPackageName(context).equals(n.getPackageName());
        return n.getNotification().priority >= Notification.PRIORITY_LOW
                && !n.isOngoing()
                && n.isClearable()
                && !isInitNotification;
    }

    // TODO: Add auto ban for some notifications
    private boolean isNotificationChangeAllowed(StatusBarNotification n) {
        return !n.getPackageName().equals(COM_ANDROID_PROVIDERS_DOWNLOADS);
    }

    // //////////////////////////////////////////
    // ///////// -- USER INTERFACE -- ///////////
    // //////////////////////////////////////////

    /**
     * Starts {@link ActiveDisplayActivity activity} if active display
     * is enabled and screen is turned off and...
     */
    private void tryStartUi(Context context, OpenStatusBarNotification notification) {
        final Config config = Config.getInstance(context);
        if (notification.isBlacklisted(context)
                || !config.getActiveDisplayEnabled()
                || config.getActiveDisplayEnabledOnlyWhenCharging() /* show only     */
                && !PowerUtils.isConnected(context))                /* when charging */
            return;

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        TelephonyManager ts = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (pm.isScreenOn()
                || /* activity is started    */ SystemClock.uptimeMillis() - mUIStartTime < UI_MULTI_START_TIMEOUT
                || /* somebody is calling me */ ts.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
            if (Project.DEBUG)
                LogUtils.d(TAG, "Passed an UI launch:"
                        + " screen_on=" + pm.isScreenOn()
                        + " call_state=" + ts.getCallState());
            return;
        } else if (Project.DEBUG) LogUtils.d(TAG, "Starting UI");

        context.startActivity(new Intent(Intent.ACTION_MAIN, null)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        | Intent.FLAG_ACTIVITY_NO_USER_ACTION
                        | Intent.FLAG_ACTIVITY_NO_ANIMATION
                        | Intent.FLAG_FROM_BACKGROUND)
                .setClass(context, ActiveDisplayActivity.class));

        mUIStartTime = SystemClock.uptimeMillis();
    }

    // //////////////////////////////////////////
    // /////////// -- LOG THINGS -- /////////////
    // //////////////////////////////////////////

    private void logNotification(Context context, StatusBarNotification notification, String action) {
        LogUtils.d(TAG, action + ": owner=" + notification.getPackageName()
                + " id=" + notification.getId()
                + " user_id=" + notification.getUserId()
                + " tag=" + notification.getTag()
                + " post_time=" + notification.getPostTime()
                + " is_valid=" + isValid(context, notification)
                + " is_changeable=" + isNotificationChangeAllowed(notification)
                + " is_blacklisted=" + Blacklist.getInstance(context).contains(notification.getPackageName()));
    }

    // //////////////////////////////////////////
    // // -- TRACKING SELECTED NOTIFICATION -- //
    // //////////////////////////////////////////

    public ArrayList<OpenStatusBarNotification> getList() {
        return mNotificationList;
    }

    public void lockSelectedNotification() {
        mSelectedNotificationLocked = true;
    }

    public void unlockSelectedNotification() {
        mSelectedNotificationLocked = false;
        setSelectedNotification(mFastSelectedNotification);
    }

    public void setSelectedNotification(OpenStatusBarNotification notification) {
        setSelectedNotification(notification, false);
    }

    private void setSelectedNotification(OpenStatusBarNotification notification, boolean isChanged) {
        mFastSelectedNotification = notification;

        if (mSelectedNotificationLocked
                || mSelectedNotification == notification) {
            if (Project.DEBUG) LogUtils.d(TAG, "");
            return;
        }

        mSelectedNotification = notification;
        for (OnNotificationListChangedListener listener : mListeners) {
            listener.onNotificationSelected(this, notification, isChanged);
        }
    }

    public OpenStatusBarNotification getSelectedNotification() {
        return mSelectedNotification;
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

    private void notifyOnInitialized() {
        for (OnNotificationListChangedListener listener : mListeners) {
            listener.onNotificationInitialized(this);
        }
    }

    private void notifyOnPosted(OpenStatusBarNotification notification) {
        for (OnNotificationListChangedListener listener : mListeners) {
            listener.onNotificationPosted(this, notification);
        }
    }

    private void notifyOnChanged(OpenStatusBarNotification notification) {
        for (OnNotificationListChangedListener listener : mListeners) {
            listener.onNotificationChanged(this, notification);
        }
    }

    private void notifyOnRemoved(OpenStatusBarNotification notification) {
        for (OnNotificationListChangedListener listener : mListeners) {
            listener.onNotificationRemoved(this, notification);
        }
    }

    public interface OnNotificationListChangedListener {

        public void onNotificationInitialized(NotificationPresenter nm);

        public void onNotificationPosted(NotificationPresenter nm,
                                         OpenStatusBarNotification notification);

        public void onNotificationChanged(NotificationPresenter nm,
                                          OpenStatusBarNotification notification);

        public void onNotificationRemoved(NotificationPresenter nm,
                                          OpenStatusBarNotification notification);

        public void onNotificationSelected(NotificationPresenter nm,
                                           OpenStatusBarNotification notification,
                                           boolean isChanged);
    }

    public static class SimpleOnNotificationListChangedListener implements OnNotificationListChangedListener {

        public static final int INITIALIZED = 0;
        public static final int POSTED = 1;
        public static final int CHANGED = 2;
        public static final int REMOVED = 3;
        public static final int SELECTED = 4;

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

        @Override
        public void onNotificationSelected(NotificationPresenter nm,
                                           OpenStatusBarNotification notification,
                                           boolean isChanged) {
            onNotificationEvent(nm, notification, SELECTED);
        }

        public void onNotificationEvent(NotificationPresenter nm,
                                        OpenStatusBarNotification notification,
                                        int event) { /* placeholder */ }
    }

}
