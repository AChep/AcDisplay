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
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.achep.acdisplay.App;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.Presenter;
import com.achep.acdisplay.blacklist.AppConfig;
import com.achep.acdisplay.blacklist.Blacklist;
import com.achep.base.Device;
import com.achep.base.content.ConfigBase;
import com.achep.base.interfaces.ISubscriptable;
import com.achep.base.tests.Check;
import com.achep.base.utils.Operator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static com.achep.base.Build.DEBUG;

/**
 * Created by Artem on 27.12.13.
 */
public class NotificationPresenter implements
        NotificationList.OnNotificationListChangedListener,
        ISubscriptable<NotificationPresenter.OnNotificationListChangedListener> {

    private static final String TAG = "NotificationPresenter";

    /**
     * {@code true} to use an additional {@link com.achep.acdisplay.notifications.NotificationList list}
     * to store all current notifications, not depending on the preferences, {@code false} to
     * use local list only. Using global list allows to change local list in-time after
     * any of preferences change, which is useful for <b>AcDisplay</b>, but useless for <b>HeadsUp</b>.
     */
    private static final boolean KEEP_GLOBAL_LIST = true;

    /**
     * {@code true} to filter the noisy flow of same notifications,
     * {@code false} to handle all notifications' updates normally.
     */
    private static final boolean FILTER_NOISY_NOTIFICATIONS = true;

    private static final int FRESH_NOTIFICATION_EXPIRY_TIME = 4000; // 4 sec.

    public static final int EVENT_BATH = 0;
    public static final int EVENT_POSTED = 1;
    public static final int EVENT_CHANGED = 2;
    public static final int EVENT_CHANGED_SPAM = 3;
    public static final int EVENT_REMOVED = 4;

    @NonNull
    public static String getEventName(int event) {
        switch (event) {
            case EVENT_POSTED:
                return "EVENT_POSTED";
            case EVENT_CHANGED:
                return "EVENT_CHANGED";
            case EVENT_CHANGED_SPAM:
                return "EVENT_CHANGED_SPAM";
            case EVENT_REMOVED:
                return "EVENT_REMOVED";
            case EVENT_BATH:
                return "EVENT_BATH";
            default:
                return "UNKNOWN_VALUE";
        }
    }

    private static final int RESULT_SUCCESS = 1;
    private static final int RESULT_SPAM = -1;

    private static final int FLAG_DONT_NOTIFY_FOLLOWERS = 1;
    private static final int FLAG_DONT_WAKE_UP = 2;

    private static NotificationPresenter sNotificationPresenter;

    private final NotificationList mGList;
    private final NotificationList mLList;

    private final ArrayList<WeakReference<OnNotificationListChangedListener>> mListenersRefs;
    private final ArrayList<NotificationListChange> mFrozenEvents;
    private boolean mFrozen;

    private final Config mConfig;
    private final Blacklist mBlacklist;
    private final Presenter mPresenter;

    // Threading
    private final Handler mHandler;

    // Threading
    private final Formatter mFormatter;

    //-- HANDLING CONFIG & BLACKLIST CHANGES ----------------------------------

    // Do not make local!
    private final ConfigListener mConfigListener;
    private final BlacklistListener mBlacklistListener;

    /**
     * Listens to config to update notification list when needed.
     */
    private class ConfigListener implements ConfigBase.OnConfigChangedListener {

        private int mMinPriority;
        private int mMaxPriority;

        public ConfigListener(@NonNull Config config) {
            mMinPriority = config.getNotifyMinPriority();
            mMaxPriority = config.getNotifyMaxPriority();
        }

        @Override
        public void onConfigChanged(@NonNull ConfigBase configBase,
                                    @NonNull String key,
                                    @NonNull Object value) {
            boolean enabled;
            int v;
            switch (key) {
                case Config.KEY_NOTIFY_MIN_PRIORITY:
                    v = (int) value;
                    handleNotifyPriorityChanged(v, mMinPriority);
                    mMinPriority = v;
                    break;
                case Config.KEY_NOTIFY_MAX_PRIORITY:
                    v = (int) value;
                    handleNotifyPriorityChanged(v, mMaxPriority);
                    mMaxPriority = v;
                    break;
                case Config.KEY_UI_DYNAMIC_BACKGROUND_MODE:
                    enabled = Operator.bitAnd((int) value, Config.DYNAMIC_BG_NOTIFICATION_MASK);
                    for (OpenNotification notification : getLargeList().list()) {
                        if (enabled) {
                            notification.loadBackgroundAsync();
                        } else {
                            notification.clearBackground();
                        }
                    }
                    break;
                case Config.KEY_UI_EMOTICONS:
                    boolean b = (boolean) value;
                    for (OpenNotification n : mGList.list()) {
                        n.setEmoticonsEnabled(b);
                    }
                    break;
                case Config.KEY_PRIVACY:
                    v = (int) value;
                    mFormatter.setPrivacyMode(v);
                    break;
            }
        }

        private void handleNotifyPriorityChanged(int a, int b) {
            if (a > b) {
                // This is here to remind me the great times
                // of programming in the school. Sorry for that :p
                a -= b *= -1;
                a -= b += a;
                // FIXME: Those two codes must do the same thing (proved by gcc).
                // But definitely Java compiler is broken. I'm scared
                // now... How can I not trust the compiler?
                // a -= b += a -= b *= -1;
            }

            final int lower = a, higher = b;
            rebuildLocalList(new Comparator() {
                @Override
                public boolean needsRebuild(@NonNull OpenNotification osbn) {
                    int priority = osbn.getNotification().priority;
                    return priority >= lower && priority <= higher;
                }
            });
        }

    }

    private class BlacklistListener extends Blacklist.OnBlacklistChangedListener {

        @Override
        public void onBlacklistChanged(
                @NonNull AppConfig configNew,
                @NonNull AppConfig configOld, int diff) {
            boolean hiddenNew = configNew.isHidden();
            boolean hiddenOld = configOld.isHidden();
            boolean nonClearableEnabledNew = configNew.isNonClearableEnabled();
            boolean nonClearableEnabledOld = configOld.isNonClearableEnabled();

            // Check if something important has changed.
            if (hiddenNew != hiddenOld || nonClearableEnabledNew != nonClearableEnabledOld) {
                handlePackageVisibilityChanged(configNew.packageName);
            }
        }

        private void handlePackageVisibilityChanged(@NonNull final String packageName) {
            rebuildLocalList(new Comparator() {
                @Override
                public boolean needsRebuild(@NonNull OpenNotification osbn) {
                    return osbn.getPackageName().equals(packageName);
                }
            });
        }
    }

    private interface Comparator {
        public boolean needsRebuild(@NonNull OpenNotification osbn);
    }

    private void rebuildLocalList(@NonNull Comparator comparator) {
        for (OpenNotification n : getLargeList().list()) {
            if (comparator.needsRebuild(n)) {
                rebuildLocalList();
                break;
            }
        }
    }

    @NonNull
    private NotificationList getLargeList() {
        return KEEP_GLOBAL_LIST ? mGList : mLList;
    }

    //-- LISTENERS ------------------------------------------------------------

    public interface OnNotificationListChangedListener {

        /**
         * Callback that the list of notifications has changed.
         *
         * @param osbn                  an instance of notification (must be non-null, if the
         *                              event is not a {@link #EVENT_BATH, {@code null} otherwise})
         * @param event                 event type:
         *                              {@link #EVENT_POSTED}, {@link #EVENT_REMOVED},
         *                              {@link #EVENT_CHANGED}, {@link #EVENT_CHANGED_SPAM},
         *                              {@link #EVENT_BATH}
         * @param isLastEventInSequence {@code true} if this is last of bath changes, {@code false}
         *                              otherwise.
         */
        public void onNotificationListChanged(@NonNull NotificationPresenter np,
                                              OpenNotification osbn, int event,
                                              boolean isLastEventInSequence);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerListener(@NonNull OnNotificationListChangedListener listener) {
        // Make sure to register listener only once.
        for (WeakReference<OnNotificationListChangedListener> ref : mListenersRefs) {
            if (ref.get() == listener) {
                Log.w(TAG, "Tried to register already registered listener!");
                return;
            }
        }

        mListenersRefs.add(new WeakReference<>(listener));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterListener(@NonNull OnNotificationListChangedListener listener) {
        for (WeakReference<OnNotificationListChangedListener> ref : mListenersRefs) {
            if (ref.get() == listener) {
                mListenersRefs.remove(ref);
                return;
            }
        }

        Log.w(TAG, "Tried to unregister non-existent listener!");
    }

    //-- MAIN -----------------------------------------------------------------

    private NotificationPresenter() {
        mFrozenEvents = new ArrayList<>();
        mListenersRefs = new ArrayList<>();
        mGList = new NotificationList(null);
        mLList = new NotificationList(this);
        mHandler = new Handler(Looper.getMainLooper());
        mFormatter = new Formatter();

        if (!Device.hasJellyBeanMR2Api()) { // pre 4.3 version
            mGList.setMaximumSize(5);
            mLList.setMaximumSize(5);
        }

        mConfig = Config.getInstance();
        mConfigListener = new ConfigListener(mConfig); // because of weak listeners
        mConfig.registerListener(mConfigListener);
        mFormatter.setPrivacyMode(mConfig.getPrivacyMode());

        mBlacklistListener = new BlacklistListener();
        mBlacklist = Blacklist.getInstance();
        mBlacklist.registerListener(mBlacklistListener);

        mPresenter = Presenter.getInstance();
    }

    @NonNull
    public synchronized static NotificationPresenter getInstance() {
        if (sNotificationPresenter == null) {
            sNotificationPresenter = new NotificationPresenter();
        }
        return sNotificationPresenter;
    }

    public void postNotificationFromMain(
            @NonNull final Context context,
            @NonNull final OpenNotification n, final int flags) {
        if (DEBUG) Log.d(TAG, "Initially posting " + n + " from \'"
                + Thread.currentThread().getName() + "\' thread.");

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                //noinspection deprecation
                postNotification(context, n, flags);
            }
        });
    }

    /**
     * Posts notification to global list, notifies every follower
     * about this change, and tries to launch
     * {@link com.achep.acdisplay.ui.activities.AcDisplayActivity}.
     * <p><i>
     * To create {@link OpenNotification}, use
     * {@link OpenNotification#newInstance(StatusBarNotification)} or
     * {@link OpenNotification#newInstance(android.app.Notification)}
     * method.
     * </i></p>
     *
     * @see #FLAG_DONT_NOTIFY_FOLLOWERS
     * @see #FLAG_DONT_WAKE_UP
     */
    public void postNotification(
            @NonNull Context context,
            @NonNull OpenNotification n, int flags) {
        Check.getInstance().isInMainThread();

        // Check for the test notification.
        if (isInitNotification(context, n)) {
            NotificationUtils.dismissNotification(n);
            return;
        }

        boolean globalValid = isValidForGlobal(n);
        boolean localValid = false;

        // If notification will not be added to the
        // list there's no point of loading its data.
        if (globalValid) {
            n.load(context);

            Config config = Config.getInstance();
            n.setEmoticonsEnabled(config.isEmoticonsEnabled());
            // Selective load exactly what we need and nothing more.
            // This will reduce RAM consumption for a bit (1% or so.)
            if (Operator.bitAnd(
                    config.getDynamicBackgroundMode(),
                    Config.DYNAMIC_BG_NOTIFICATION_MASK))
                n.loadBackgroundAsync();

            localValid = isValidForLocal(n);
        }

        // Extract flags.
        boolean flagIgnoreFollowers = Operator.bitAnd(
                flags, FLAG_DONT_NOTIFY_FOLLOWERS);
        boolean flagWakeUp = !Operator.bitAnd(
                flags, FLAG_DONT_WAKE_UP);

        freezeListeners(); // we should handle the event first
        if (KEEP_GLOBAL_LIST) mGList.pushOrRemove(n, globalValid, flagIgnoreFollowers);
        int result = mLList.pushOrRemove(n, localValid, flagIgnoreFollowers);

        if (flagWakeUp && result == RESULT_SUCCESS) {
            // Try start gui
            mPresenter.tryStartGuiCauseNotification(context, n);
        }

        // Release listeners and send all pending
        // events.
        meltListeners();
    }

    public void removeNotificationFromMain(final @NonNull OpenNotification n) {
        if (DEBUG) Log.d(TAG, "Initially removing " + n + " from \'"
                + Thread.currentThread().getName() + "\' thread.");

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                //noinspection deprecation
                removeNotification(n);
            }
        });
    }

    /**
     * Removes notification from the presenter and sends
     * this event to followers. Calling his method will not
     * remove notification from system!
     *
     * @deprecated use {@link #removeNotificationFromMain(OpenNotification)} instead!
     */
    @Deprecated
    public void removeNotification(@NonNull OpenNotification n) {
        Check.getInstance().isInMainThread();

        if (KEEP_GLOBAL_LIST) mGList.remove(n);
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
        ArrayList<NotificationListChange> changes = new ArrayList<>();

        // Remove not valid notifications
        // from local list.
        ArrayList<OpenNotification> list = mLList.list();
        for (int i = 0; i < list.size(); i++) {
            OpenNotification n = list.get(i);
            if (!isValidForLocal(n)) {
                list.remove(i--);
                changes.add(new NotificationListChange(EVENT_REMOVED, n));
            }
        }

        // Add newly valid notifications to local list.
        for (OpenNotification n : mGList.list()) {
            if (isValidForLocal(n) && mLList.indexOf(n) == -1) {
                list.add(n);
                changes.add(new NotificationListChange(EVENT_POSTED, n));
            }
        }

        int size = changes.size();
        if (size > 4) {
            notifyListeners(null, EVENT_BATH);
        } else if (size > 0) {
            notifyListeners(changes);
        }
    }

    @NonNull
    public Formatter getFormatter() {
        return mFormatter;
    }

    @Nullable
    public OpenNotification getFreshNotification() {
        for (OpenNotification n : getList()) {
            long delta = Math.max(n.getNotification().priority, 1) * FRESH_NOTIFICATION_EXPIRY_TIME;
            long past = SystemClock.elapsedRealtime() - delta;
            if (!n.isRead() && n.getLoadTimestamp() > past) return n;
        }
        return null;
    }

    @NonNull
    public ArrayList<OpenNotification> getList() {
        return mLList.list();
    }

    public int size() {
        return getList().size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    //-- LOCAL LIST'S EVENTS --------------------------------------------------

    @Override
    // Not an enter point, should not be synchronized.
    public int onNotificationAdded(@NonNull OpenNotification n) {
        notifyListeners(n, EVENT_POSTED);
        return RESULT_SUCCESS;
    }

    @Override
    // Not an enter point, should not be synchronized.
    public int onNotificationChanged(@NonNull OpenNotification n, @NonNull OpenNotification old) {
        if (!FILTER_NOISY_NOTIFICATIONS) return RESULT_SUCCESS;

        // Prevent god damn notification spam by
        // checking texts' equality.

        // An example of notification spammer is a well-known
        // DownloadProvider (seriously, Google?)

        if (n.getNumber() == old.getNumber()
                && TextUtils.equals(n.titleText, old.titleText)
                && TextUtils.equals(n.titleBigText, old.titleBigText)
                && TextUtils.equals(n.messageText, old.messageText)
                && TextUtils.equals(n.infoText, old.infoText)) {
            // Technically notification was changed, but it was a fault
            // of dumb developer. Mark notification as read, if old one was.
            n.setRead(old.isRead());

            if (!n.isMine()) {
                notifyListeners(n, EVENT_CHANGED_SPAM);
                return RESULT_SPAM; // Don't wake up.
            }
        }

        notifyListeners(n, EVENT_CHANGED);
        return RESULT_SUCCESS;
    }

    @Override
    // Not an enter point, should not be synchronized.
    public int onNotificationRemoved(@NonNull OpenNotification n) {
        notifyListeners(n, EVENT_REMOVED);
        n.recycle(); // Free all resources
        return RESULT_SUCCESS;
    }

    //-- NOTIFICATION UTILS ---------------------------------------------------

    @SuppressLint("NewApi")
    public boolean isTestNotification(@NonNull Context context, @NonNull OpenNotification n) {
        StatusBarNotification sbn = n.getStatusBarNotification();
        return n.isMine() && sbn != null && sbn.getId() == App.ID_NOTIFY_TEST;
    }

    @SuppressLint("NewApi")
    public boolean isInitNotification(@NonNull Context context, @NonNull OpenNotification n) {
        StatusBarNotification sbn = n.getStatusBarNotification();
        return n.isMine() && sbn != null && sbn.getId() == App.ID_NOTIFY_INIT;
    }

    /**
     * Freezes the listeners notification process and
     * stores all events to list.
     *
     * @see #meltListeners()
     */
    private void freezeListeners() {
        mFrozen = true;
    }

    /**
     * Unfreezes all events and sends them.
     *
     * @see #freezeListeners()
     */
    private void meltListeners() {
        mFrozen = false;
        notifyListeners(mFrozenEvents);
        mFrozenEvents.clear();
    }

    private void notifyListeners(@Nullable OpenNotification n, int event) {
        notifyListeners(n, event, true);
    }

    private void notifyListeners(@Nullable OpenNotification n, int event,
                                 boolean isLastEventInSequence) {
        Check.getInstance().isInMainThread();

        if (mFrozen) {
            mFrozenEvents.add(new NotificationListChange(event, n));
            return;
        }

        for (int i = mListenersRefs.size() - 1; i >= 0; i--) {
            WeakReference<OnNotificationListChangedListener> ref = mListenersRefs.get(i);
            OnNotificationListChangedListener l = ref.get();

            if (l == null) {
                // There were no links to this listener except
                // our class.
                Log.w(TAG, "Deleting an unused listener!");
                mListenersRefs.remove(i);
            } else {
                l.onNotificationListChanged(this, n, event, isLastEventInSequence);
            }
        }
    }

    private void notifyListeners(@NonNull ArrayList<NotificationListChange> changes) {
        int size = changes.size();
        for (int i = 0; i < size; i++) {
            NotificationListChange change = changes.get(i);
            notifyListeners(change.notification, change.event, i + 1 == size);
        }
    }

    /**
     * @return {@code true} if notification may be shown to user,
     * {@code false} otherwise.
     */
    private boolean isValidForLocal(@NonNull OpenNotification notification) {
        AppConfig config = mBlacklist.getAppConfig(notification.getPackageName());

        if (config.isHidden()) {
            // Do not display any notifications from this app.
            return false;
        }

        if (!notification.isClearable() && !config.isNonClearableEnabled()) {
            // Do not display non-clearable notification.
            return false;
        }

        if (notification.getNotification().priority < mConfig.getNotifyMinPriority()) {
            // Do not display too low-priority notification.
            return false;
        }

        if (notification.getNotification().priority > mConfig.getNotifyMaxPriority()) {
            // Do not display too high-priority notification.
            return false;
        }

        // Do not allow notifications with no content.
        return !(TextUtils.isEmpty(notification.titleText)
                && TextUtils.isEmpty(notification.titleBigText)
                && TextUtils.isEmpty(notification.messageText)
                && TextUtils.isEmpty(notification.messageBigText)
                && notification.messageTextLines == null);
    }

    // Here we filter completely wrong
    // notifications.
    @SuppressLint("NewApi")
    private boolean isValidForGlobal(@NonNull OpenNotification notification) {
        // Ignore children of notifications that have a summary, since we're not
        // going to show them anyway.
        return !notification.isGroupChild();
    }

    //-- INITIALIZING ---------------------------------------------------------

    void init(final @NonNull Context context,
              final @NonNull StatusBarNotification[] activeNotifications) {
        mHandler.post(new Runnable() {
            @SuppressLint("NewApi")
            @Override
            public void run() {
                clear(false);

                if (DEBUG) Log.d(TAG, "Initializing the notifications list...");
                for (StatusBarNotification sbn : activeNotifications) {
                    OpenNotification n = OpenNotification.newInstance(sbn);
                    postNotification(context, n, FLAG_DONT_NOTIFY_FOLLOWERS | FLAG_DONT_WAKE_UP);
                }

                notifyListeners(null, EVENT_BATH);
            }
        });
    }

    void clear(final boolean notifyListeners) {
        mHandler.post(new Runnable() {
            @SuppressLint("NewApi")
            @Override
            public void run() {
                if (DEBUG) Log.d(TAG, "Clearing the notifications list... notify_listeners="
                        + notifyListeners);

                mGList.list().clear();
                mLList.list().clear();
                if (notifyListeners) notifyListeners(null, EVENT_BATH);
            }
        });
    }

}

