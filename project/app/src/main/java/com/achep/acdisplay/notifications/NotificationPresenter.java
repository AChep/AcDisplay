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
import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.achep.acdisplay.App;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.blacklist.AppConfig;
import com.achep.acdisplay.blacklist.Blacklist;
import com.achep.base.AppHeap;
import com.achep.base.Device;
import com.achep.base.content.ConfigBase;
import com.achep.base.interfaces.ISubscriptable;
import com.achep.base.tests.Check;
import com.achep.base.utils.Operator;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.achep.base.Build.DEBUG;

/**
 * Created by Artem on 27.12.13.
 */
public class NotificationPresenter implements
        NotificationList.OnNotificationListChangedListener,
        ISubscriptable<NotificationPresenter.OnNotificationListChangedListener> {

    private static final String TAG = "NotificationPresenter";
    private static final String WAKE_LOCK_TAG = "Notification pool post/remove lock.";

    /**
     * {@code true} to filter the noisy flow of same notifications,
     * {@code false} to handle all notifications' updates normally.
     */
    private static final boolean FILTER_NOISY_NOTIFICATIONS = true;

    private static final long POST_REMOVE_POOL_DURATION = 400; // 0.4 sec.

    private static final int FRESH_NOTIFICATION_EXPIRY_TIME = 4000; // 4 sec.

    public static final int FLAG_SILENCE = 1;
    public static final int FLAG_IMMEDIATELY = 1 << 1;

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

    private static NotificationPresenter sNotificationPresenter;

    private final NotificationList mGList;
    private final NotificationList mLList;
    private final Set<String> mGroupsWithSummaries;

    private OnNotificationPostedListener mMainListener;
    private final ArrayList<WeakReference<OnNotificationListChangedListener>> mListenersRefs;
    private final ArrayList<NotificationListChange> mFrozenEvents;
    private int mFreezeLevel;

    private final Config mConfig;
    private final Blacklist mBlacklist;

    // Threading
    private final Handler mHandler;
    private volatile int mDetectingSyncTroubles;

    // Remote posting/removing
    private boolean mProcessingPRQueue;
    private final ConcurrentLinkedQueue<PostRemoveEvent> mPRQueue = new ConcurrentLinkedQueue<>();
    private final Runnable mPRRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (mPRQueue) {
                if (DEBUG) Log.d(TAG, "Processing the p&r queue (" + mPRQueue.size() + " events)");

                mProcessingPRQueue = true;
                // Release all events!
                PostRemoveEvent event;
                while ((event = mPRQueue.poll()) != null) {
                    if (event.posts) {
                        assert event.context != null;
                        postNotification(event.context, event.notification, event.flags);
                    } else {
                        removeNotification(event.notification, event.flags);
                    }
                }
                mProcessingPRQueue = false;
            }
        }
    };

    /**
     * @author Artem Chepurnoy
     */
    private static class PostRemoveEvent {

        @Nullable
        public final Context context;
        @NonNull
        public final OpenNotification notification;

        /**
         * {@code true} if it's a {@link #postNotification(Context, OpenNotification, int) post}
         * event, {@code false} {@link #removeNotification(OpenNotification, int) otherwise}.
         */
        public final boolean posts;

        public int flags;

        PostRemoveEvent(@Nullable Context context, @NonNull OpenNotification notification,
                        boolean posts, int flags) {
            this.context = context;
            this.notification = notification;
            this.posts = posts;
            this.flags = flags;
        }
    }

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
                    for (OpenNotification notification : mLList) {
                        if (enabled) {
                            notification.loadBackgroundAsync();
                        } else {
                            notification.clearBackground();
                        }
                    }
                    break;
                case Config.KEY_UI_EMOTICONS:
                    boolean b = (boolean) value;
                    for (OpenNotification n : mGList) {
                        n.setEmoticonsEnabled(b);
                    }
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
                public boolean needsRebuild(@NonNull OpenNotification n) {
                    int priority = n.getNotification().priority;
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
                public boolean needsRebuild(@NonNull OpenNotification n) {
                    return n.getPackageName().equals(packageName);
                }
            });
        }
    }

    private interface Comparator {
        boolean needsRebuild(@NonNull OpenNotification n);
    }

    private void rebuildLocalList(@NonNull Comparator comparator) {
        for (OpenNotification n : mGList) {
            if (comparator.needsRebuild(n)) {
                rebuildLocalList();
                break;
            }
        }
    }

    //-- LISTENERS ------------------------------------------------------------

    public interface OnNotificationListChangedListener {

        /**
         * Callback that the list of notifications has changed.
         *
         * @param n                     an instance of notification (must be non-null, if the
         *                              event is not a {@link #EVENT_BATH, {@code null} otherwise})
         * @param event                 event type:
         *                              {@link #EVENT_POSTED}, {@link #EVENT_REMOVED},
         *                              {@link #EVENT_CHANGED}, {@link #EVENT_CHANGED_SPAM},
         *                              {@link #EVENT_BATH}
         * @param isLastEventInSequence {@code true} if this is last of bath changes, {@code false}
         *                              otherwise.
         */
        void onNotificationListChanged(@NonNull NotificationPresenter np,
                                       OpenNotification n, int event,
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

        cleanDeadListeners();
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

    /* Ideally this method and the whole weakness thing should not be needed. */
    private void cleanDeadListeners() {
        Iterator<WeakReference<OnNotificationListChangedListener>> i = mListenersRefs.iterator();
        while (i.hasNext()) {
            WeakReference wr = i.next();
            if (wr.get() == null) {
                Log.w(TAG, "Removing the dead listener.");
                i.remove();
            }
        }
    }

    /**
     * @author Artem Chepurnoy
     */
    public interface OnNotificationPostedListener {

        /**
         * @see #postNotificationFromMain(android.content.Context, OpenNotification, int)
         * @see #postNotification(android.content.Context, OpenNotification, int)
         */
        void onNotificationPosted(@NonNull Context context, @NonNull OpenNotification n, int flags);

    }

    /**
     * @see #registerListener(OnNotificationListChangedListener)
     * @see #unregisterListener(OnNotificationListChangedListener)
     */
    public void setOnNotificationPostedListener(@Nullable OnNotificationPostedListener listener) {
        mMainListener = listener;
    }

    //-- MAIN -----------------------------------------------------------------

    private NotificationPresenter() {
        mFrozenEvents = new ArrayList<>();
        mListenersRefs = new ArrayList<>();
        mGList = new NotificationList(null);
        mLList = new NotificationList(this);
        mGroupsWithSummaries = new HashSet<>();
        mHandler = new Handler(Looper.getMainLooper());

        if (!Device.hasJellyBeanMR2Api()) { // pre 4.3 version
            mGList.setMaximumSize(5);
            mLList.setMaximumSize(5);
        }

        mConfig = Config.getInstance();
        mConfigListener = new ConfigListener(mConfig); // because of weak listeners
        mConfig.registerListener(mConfigListener);

        mBlacklistListener = new BlacklistListener();
        mBlacklist = Blacklist.getInstance();
        mBlacklist.registerListener(mBlacklistListener);
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
        synchronized (mPRQueue) {
            if (DEBUG) Log.d(TAG, "Initially posting " + n + " from \'"
                    + Thread.currentThread().getName() + "\' thread.");

            boolean immediately = Operator.bitAnd(flags, FLAG_IMMEDIATELY) || !n.isClearable();
            internalAddPREvent(context, n, flags, true /* post */, immediately);
        }
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
     * @see #FLAG_SILENCE
     */
    void postNotification(
            @NonNull Context context,
            @NonNull OpenNotification n, int flags) {
        Check.getInstance().isInMainThread();
        mDetectingSyncTroubles = 1;

        synchronized (mPRQueue) {
            // Clean-up all previous event regard this
            // notification.
            if (!mProcessingPRQueue) internalRemovePreviousPREvent(n);
        }

        // Check for the test notification.
        if (isInitNotification(context, n)) {
            NotificationUtils.dismissNotification(n);
            // Try with another way, just to be sure.
            String name = Context.NOTIFICATION_SERVICE;
            NotificationManager nm = (NotificationManager) context.getSystemService(name);
            nm.cancel(App.ID_NOTIFY_INIT);
            return;
        }

        freezeListeners();

        boolean globalValid = isValidForGlobal(n);
        boolean localValid = false;
        boolean groupChild = false;

        // If notification will not be added to the
        // list there's no point of loading its data.
        if (globalValid) {
            n.load(context);

            if (n.isGroupSummary()) {
                String groupKey = n.getGroupKey();
                assert groupKey != null;
                mGroupsWithSummaries.add(groupKey);

                // Put all group's children to its summary
                // notification.
                for (int i = mGList.size() - 1; i >= 0; i--) {
                    OpenNotification n2 = mGList.get(i);
                    if (groupKey.equals(n2.getGroupKey())) {
                        assert n.getGroupNotifications() != null;
                        n.getGroupNotifications().add(n2);

                        // Remove this notification from the global list.
                        mGList.removeNotification(n2);
                        mLList.removeNotification(n2);
                    }
                }
            } else if (n.isGroupChild() && mGroupsWithSummaries.contains(n.getGroupKey())) {
                // Artem Chepurnoy: Not sure if this may happen.
                if (DEBUG) Log.d(TAG, "Adding a notification to an existent group.");

                String groupKey = n.getGroupKey();
                assert groupKey != null;
                for (OpenNotification n2 : mGList) {
                    if (groupKey.equals(n2.getGroupKey())) {
                        Check.getInstance().isTrue(n2.isGroupSummary());

                        groupChild = true;

                        assert n2.getGroupNotifications() != null;
                        ((NotificationList) n2.getGroupNotifications()).pushNotification(n);
                        notifyListeners(n2, EVENT_CHANGED);
                        break;
                    }
                }

                Check.getInstance().isTrue(groupChild);
            }

            Config config = Config.getInstance();
            n.setEmoticonsEnabled(config.isEmoticonsEnabled());

            if (groupChild) {
                globalValid = false;
                // I assume that 'localValid' if
                // 'False' here.
            } else {
                localValid = isValidForLocal(n);
            }
        }

        mGList.pushOrRemoveNotification(n, globalValid);
        int result = mLList.pushOrRemoveNotification(n, localValid);
        if (localValid && result == RESULT_SUCCESS && mMainListener != null) {
            if (DEBUG) Log.d(TAG, "Notification posted: notifying the main listener.");
            mMainListener.onNotificationPosted(context, n, flags);
        }

        // Release listeners and send all pending
        // events.
        if (Operator.bitAnd(flags, FLAG_SILENCE)) mFrozenEvents.clear();
        meltListeners();

        // Try to detect the sync troubles
        Check.getInstance().isTrue(mDetectingSyncTroubles == 1);
        mDetectingSyncTroubles = 0;
    }

    public void removeNotificationFromMain(final @NonNull OpenNotification n, final int flags) {
        synchronized (mPRQueue) {
            if (DEBUG) Log.d(TAG, "Initially removing " + n + " from \'"
                    + Thread.currentThread().getName() + "\' thread.");

            boolean immediately = Operator.bitAnd(flags, FLAG_IMMEDIATELY) || !n.isClearable();
            internalAddPREvent(null, n, 0, false /* remove */, immediately);
        }
    }

    /**
     * Removes notification from the presenter and sends
     * this event to followers. Calling his method will not
     * remove notification from system!
     */
    public void removeNotification(@NonNull OpenNotification n, final int flags) {
        Check.getInstance().isInMainThread();
        mDetectingSyncTroubles = 2;

        synchronized (mPRQueue) {
            // Clean-up all previous event regard this
            // notification.
            if (!mProcessingPRQueue) internalRemovePreviousPREvent(n);
        }

        if (n.isGroupSummary()) {
            String groupKey = n.getGroupKey();
            assert groupKey != null;
            mGroupsWithSummaries.remove(groupKey);
        } else if (n.isGroupChild() && mGroupsWithSummaries.contains(n.getGroupKey())) {
            String groupKey = n.getGroupKey();
            assert groupKey != null;
            for (OpenNotification n2 : mGList) {
                if (groupKey.equals(n2.getGroupKey())) {
                    Check.getInstance().isTrue(n2.isGroupSummary());
                    assert n2.getGroupNotifications() != null;

                    NotificationList list = (NotificationList) n2.getGroupNotifications();
                    int i = list.indexOfNotification(n);
                    if (i != -1) {
                        list.get(i).recycle();
                        list.remove(i);
                    }
                    return;
                }
            }
        }

        NotificationList list = mGList;
        int i = list.indexOfNotification(n);
        if (i != -1) {
            list.get(i).recycle();
            list.remove(i);
            mLList.removeNotification(n);
        }

        // Try to detect the sync troubles
        Check.getInstance().isTrue(mDetectingSyncTroubles == 2);
        mDetectingSyncTroubles = 0;
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
        for (int i = 0; i < mLList.size(); i++) {
            OpenNotification n = mLList.get(i);
            if (!isValidForLocal(n)) {
                mLList.remove(i--);
                changes.add(new NotificationListChange(EVENT_REMOVED, n));
            }
        }

        // Add newly valid notifications to local list.
        for (OpenNotification n : mGList) {
            if (isValidForLocal(n) && mLList.indexOfNotification(n) == -1) {
                mLList.add(n);
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

    private void internalAddPREvent(
            @Nullable Context context,
            @NonNull OpenNotification notification,
            int flags, boolean posts,
            boolean immediately) {
        internalRemovePreviousPREvent(notification);
        mPRQueue.add(new PostRemoveEvent(context, notification, posts, flags));

        final long duration = POST_REMOVE_POOL_DURATION;
        if (!immediately) {
            context = AppHeap.getContext(); // that's cheating :(
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).acquire(duration + 100);
        }

        mHandler.removeCallbacks(mPRRunnable);
        mHandler.postDelayed(mPRRunnable, immediately ? 0 : duration);
    }

    private void internalRemovePreviousPREvent(@NonNull OpenNotification notification) {
        // Remove all previous events from the queue, since they're not
        // important at all.
        boolean found = false;
        Iterator<PostRemoveEvent> iterator = mPRQueue.iterator();
        while (iterator.hasNext()) {
            PostRemoveEvent event = iterator.next();
            if (NotificationUtils.hasIdenticalIds(event.notification, notification)) {
                Check.getInstance().isFalse(found);
                iterator.remove();
                found = true;

                // I don't want that test to be run on
                // release build.
                if (!DEBUG) break;
            }
        }
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
        return mLList;
    }

    /**
     * @return the number of notifications in local list of notifications.
     */
    public int size() {
        return getList().size();
    }

    public boolean isEmpty() {
        return getList().isEmpty();
    }

    //-- LOCAL LIST'S EVENTS --------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    // Not an enter point, should not be synchronized.
    public int onNotificationAdded(@NonNull OpenNotification n) {
        loadNotificationBackground(n);
        notifyListeners(n, EVENT_POSTED);
        return RESULT_SUCCESS;
    }

    /**
     * {@inheritDoc}
     */
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

        loadNotificationBackground(n);
        notifyListeners(n, EVENT_CHANGED);
        return RESULT_SUCCESS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // Not an enter point, should not be synchronized.
    public int onNotificationRemoved(@NonNull OpenNotification n) {
        notifyListeners(n, EVENT_REMOVED);
        n.clearBackground();
        return RESULT_SUCCESS;
    }

    private void loadNotificationBackground(@NonNull OpenNotification notification) {
        Config config = Config.getInstance();
        // Selective load exactly what we need and nothing more.
        // This will reduce RAM consumption for a bit (1% or so.)
        if (Operator.bitAnd(
                config.getDynamicBackgroundMode(),
                Config.DYNAMIC_BG_NOTIFICATION_MASK))
            notification.loadBackgroundAsync();
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
        mFreezeLevel++;
    }

    /**
     * Unfreezes all events and sends them.
     *
     * @see #freezeListeners()
     */
    private void meltListeners() {
        Check.getInstance().isTrue(mFreezeLevel > 0);
        if (--mFreezeLevel == 0) {
            notifyListeners(mFrozenEvents);
            mFrozenEvents.clear();
        }
    }

    private void notifyListeners(@Nullable OpenNotification n, int event) {
        notifyListeners(n, event, true);
    }

    private void notifyListeners(@Nullable OpenNotification n, int event,
                                 boolean isLastEventInSequence) {
        Check.getInstance().isInMainThread();

        if (mFreezeLevel > 0) {
            if (mFrozenEvents.size() >= 1 && mFrozenEvents.get(0).event == EVENT_BATH) return;
            if (event == EVENT_BATH) mFrozenEvents.clear();
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
        // I don't want to store more notifications in Android 4.2-'s
        // global list, cause they only eat memory and almost not
        // useful.
        return Device.hasJellyBeanMR2Api() || isValidForLocal(notification);
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
                    postNotification(context, n, FLAG_SILENCE);
                }

                notifyListeners(null, EVENT_BATH);
            }
        });
    }

    void clearFromMain(final boolean notifyListeners) {
        mHandler.post(new Runnable() {
            @SuppressLint("NewApi")
            @Override
            public void run() {
                clear(notifyListeners);
            }
        });
    }

    void clear(final boolean notifyListeners) {
        Check.getInstance().isInMainThread();
        if (DEBUG) Log.d(TAG, "Clearing the notifications list... notify_listeners="
                + notifyListeners);

        synchronized (mPRQueue) {
            mPRQueue.clear();
            mHandler.removeCallbacks(mPRRunnable);
        }

        mGroupsWithSummaries.clear();
        mGList.clear();
        mLList.clear();
        if (notifyListeners) notifyListeners(null, EVENT_BATH);
    }

}