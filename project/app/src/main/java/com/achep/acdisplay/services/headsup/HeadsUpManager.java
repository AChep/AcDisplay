package com.achep.acdisplay.services.headsup;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.achep.acdisplay.App;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.acdisplay.compat.TransitionManager;
import com.achep.acdisplay.notifications.NotificationPresenter;
import com.achep.acdisplay.notifications.NotificationUtils;
import com.achep.acdisplay.notifications.OpenNotification;
import com.achep.acdisplay.receiver.Receiver;
import com.achep.acdisplay.utils.PendingIntentUtils;
import com.achep.acdisplay.utils.PowerUtils;
import com.achep.acdisplay.widgets.NotificationWidget;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class that manages notifications and shows them in popups.
 * In best scenario this class should be inside of the {@link android.app.Service}.
 *
 * @author Artem Chepurnoy
 */
public class HeadsUpManager implements
        NotificationPresenter.OnNotificationListChangedListener {

    private static final String TAG = "HeadsUpManager";

    /**
     * Represents how long notification will be shown.
     */
    private static final long DURATION = 5000; // ms.

    private static final int STATE_ADDED = 1;
    private static final int STATE_EXIT_ANIM = 2;
    private static final int STATE_REMOVED = 3;

    private int mWindowState = STATE_REMOVED;

    private final Animation mUpdateAnimation;
    private final Animation mExitAnimation;

    private HeadsUpView mRootView;
    private ViewGroup mContainer;

    private ArrayList<NotificationWidget> mNotificationWidget;
    private HashMap<NotificationWidget, Runnable> mRunnableMap;
    private Handler mHandler;

    private Context mContext;
    private boolean mAttached;
    private boolean mIgnoreShowing;

    /**
     * A runnable that contains of {@link #detach()} method.
     */
    private final Runnable mDetachRunnable =
            new Runnable() {

                @Override
                public void run() {
                    detach();
                }
            };

    private BroadcastReceiver mReceiver =
            new Receiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    super.onReceive(context, intent);
                    switch (intent.getAction()) {
                        case App.ACTION_EAT_HOME_PRESS_START:
                            mIgnoreShowing = true;
                            detach();
                            break;
                        case App.ACTION_EAT_HOME_PRESS_STOP:
                            mIgnoreShowing = false;
                            break;
                    }
                }
            };

    @SuppressLint("InflateParams")
    public HeadsUpManager(@NonNull Context context) {
        mContext = context;

        mNotificationWidget = new ArrayList<>();
        mRunnableMap = new HashMap<>();
        mHandler = new Handler();

        // Load animations.
        mUpdateAnimation = AnimationUtils.loadAnimation(context, R.anim.heads_up_update);
        mExitAnimation = AnimationUtils.loadAnimation(context, R.anim.heads_up_exit);

        // Create root layouts.
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRootView = (HeadsUpView) inflater.inflate(R.layout.heads_up, null, false);
        mRootView.setHeadsUpManager(this);
        mContainer = (ViewGroup) mRootView.findViewById(R.id.content);
    }

    public void start() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(App.ACTION_EAT_HOME_PRESS_START);
        filter.addAction(App.ACTION_EAT_HOME_PRESS_STOP);
        mContext.registerReceiver(mReceiver, filter);
        NotificationPresenter.getInstance().registerListener(this);
    }

    public void stop() {
        mContext.unregisterReceiver(mReceiver);
        NotificationPresenter.getInstance().unregisterListener(this);
        detach();
    }

    @Override
    public void onNotificationListChanged(
            @NonNull NotificationPresenter np,
            @NonNull OpenNotification osbn, int event) {
        if (mIgnoreShowing || !PowerUtils.isScreenOn(mContext)) {
            return;
        }

        switch (event) {
            case NotificationPresenter.EVENT_POSTED:
            case NotificationPresenter.EVENT_CHANGED:
            case NotificationPresenter.EVENT_CHANGED_SPAM:
                if (osbn.getNotification().priority < Notification.PRIORITY_HIGH) {
                    // As said in Android documentation:
                    // < https://developer.android.com/preview/notifications.html >

                    // Use high priority notifications primarily for important communication,
                    // such as message or chat events with content that is particularly
                    // interesting for the user. High priority notifications will get
                    // the Heads-Up Notification display starting in L.
                   // return;
                }
                break;
        }

        NotificationWidget widget;

        switch (event) {
            case NotificationPresenter.EVENT_POSTED:
                postNotification(osbn);
                break;
            case NotificationPresenter.EVENT_CHANGED:
                int i = indexOf(osbn);
                if (i == -1) {
                    postNotification(osbn);
                } else {
                    TransitionManager.beginDelayedTransition(mContainer);

                    widget = mNotificationWidget.get(i);
                    widget.setNotification(osbn);

                    // Delay dismissing this notification.
                    Runnable runnable = mRunnableMap.get(widget);
                    mHandler.removeCallbacks(runnable);
                    mHandler.postDelayed(runnable, DURATION);

                    // widget.startAnimation(mUpdateAnimation);
                    // TODO: Notify user about this change via animation.
                }
                break;
            case NotificationPresenter.EVENT_REMOVED:
                i = indexOf(osbn);
                if (i != -1) {
                    widget = mNotificationWidget.get(i);
                    removeImmediately(widget);
                }
                break;
            case NotificationPresenter.EVENT_BATH:
                // Fortunately there's no need to support bath
                // changing list of notification.
                break;
        }
    }

    /**
     * @return the position of given {@link com.achep.acdisplay.notifications.OpenNotification} in
     * {@link #mNotificationWidget list}, or {@code -1} if not found.
     */
    public int indexOf(@NonNull OpenNotification n) {
        final int size = mNotificationWidget.size();
        for (int i = 0; i < size; i++) {
            OpenNotification n2 = mNotificationWidget.get(i).getNotification();
            if (NotificationUtils.hasIdenticalIds(n, n2)) {
                return i;
            }
        }
        return -1;
    }

    private void removeImmediately(NotificationWidget widget) {
        Runnable runnable = mRunnableMap.get(widget);

        // Run dismissing runnable immediately.
        mHandler.removeCallbacks(runnable);
        runnable.run();
    }

    /**
     * Posts new {@link com.achep.acdisplay.widgets.NotificationWidget widget} with
     * current notification and starts dismissing timer.
     *
     * @param n the notification to show
     */
    private void postNotification(@NonNull OpenNotification n) {

        String style = Config.getInstance().getHeadsUpStyle();
        int styleRes = style.equals("dark")
                ? R.style.HeadsUp_Theme_Dark
                : R.style.HeadsUp_Theme;

        Context themedContext = new ContextThemeWrapper(mContext, styleRes);
        TypedArray typedArray = themedContext.obtainStyledAttributes(new int[]{
                R.styleable.Theme_headsUpNotificationLayout,
        });
        final int layoutRes = typedArray.getInt(0, R.layout.heads_up_notification);
        typedArray.recycle();

        // Inflate notification widget.
        LayoutInflater inflater = (LayoutInflater) themedContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final HeadsUpNotificationView widget = (HeadsUpNotificationView) inflater
                .inflate(layoutRes, mContainer, false);

        // Fill widget and add to container.
        widget.setHeadsUpManager(this);
        widget.setNotification(n);
        widget.setActionButtonsAlignTop(false);
        widget.setAlpha(0);
        widget.setRotationX(-15);
        widget.animate().alpha(1).rotationX(0).setDuration(300);
        widget.setOnClickListener(new NotificationWidget.OnClickListener() {
            @Override
            public void onClick(View v) {
                widget.getNotification().click();
            }

            @Override
            public void onActionButtonClick(View v, PendingIntent intent) {
                PendingIntentUtils.sendPendingIntent(intent);
                widget.getNotification().dismiss();
            }
        });
        mNotificationWidget.add(widget);
        mContainer.addView(widget);

        // Attaches heads-up to window.
        attach();

        // Timed-out runnable.
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mContainer.removeView(widget);
                mNotificationWidget.remove(widget);
                mRunnableMap.remove(widget);

                // Detach view from window, if there's
                // no content.
                if (mContainer.getChildCount() == 0) {
                    // Leave some time for animation.
                    mRootView.animate().alpha(0).setDuration(300).start();
                    mHandler.postDelayed(mDetachRunnable, 300);
                }
            }
        };

        mRunnableMap.put(widget, runnable);
        mHandler.postDelayed(runnable, (long) (DURATION * Math.sqrt(mRunnableMap.size())));
    }

    private void attach() {
        mHandler.removeCallbacks(mDetachRunnable);
        if (mAttached & (mAttached = true)) return;

        WindowManager wm = (WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        wm.addView(mRootView, lp);
    }

    private void detach() {
        if (!mAttached & !(mAttached = false)) return;

        WindowManager wm = (WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE);
        wm.removeView(mRootView);

        // Clean everything.
        mHandler.removeCallbacksAndMessages(null);
        mContainer.removeAllViews();
        mNotificationWidget.clear();
        mRunnableMap.clear();
    }

    public void hideHeadsUp() {
        detach();
    }

    public void resetHeadsUpDecayTimer(HeadsUpNotificationView widget) {

        // Delay dismissing this notification.
        Runnable runnable = mRunnableMap.get(widget);
        mHandler.removeCallbacks(runnable);
        mHandler.postDelayed(runnable, DURATION);
    }

    /**
     * Dismisses given {@link NotificationWidget notification widget} and its notification.
     *
     * @param widget a widget to be dismissed.
     * @see OpenNotification#dismiss()
     */
    public void dismiss(NotificationWidget widget) {
        widget.getNotification().dismiss();
    }
}
