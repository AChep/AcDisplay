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
package com.achep.activedisplay.legacy;

public class MainActivity {

}
/*
public class MainActivity extends Activity implements
        NotificationPresenter.OnNotificationListChangedListener,
        SensorEventListener, CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "ActiveDisplayActivity";

    private static final int SCREEN_OFF_TIMEOUT = 10000; // ms.
    private static final int SCREEN_OFF_TIMEOUT_SHORT = 6000; // ms.
    private static final int SCREEN_OFF_TIMEOUT_SUPER_SHORT = 3500; // ms.

    private static final int MSG_TURN_SCREEN_OFF = 1;
    private static final int MSG_UNLOCK = 2;

    private OpenStatusBarNotification mCurrentNotification;
    private OpenStatusBarNotification mHandlingNotification;

    private FrameLayout mHandleView;
    private ImageView mHandleActionIcon;
    private ImageView mUnlockImageView;
    private ImageView mLockImageView;
    private LinearLayout mHandleLayout;
    private WaveView mWaveView;

    private AnimatorSet mUnlockViewInAnimation;
    private AnimatorSet mNotificationViewInAnimation;
    private AnimatorSet mLockViewInAnimation;

    private float mTouchDownX, mTouchDownY;

    private float mNotificationDismissProgress;
    private SensorManager mSensorManager;
    private HoloCircularProgressBar mScreenOffProgressBar;
    private boolean mUnlocking;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_TURN_SCREEN_OFF:
                    turnScreenOff();
                    break;
                case MSG_UNLOCK:
                    unlock();
                    break;
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };
    private DebugLayerView mDebugLayerView;
    private boolean mUpdateWaveViewRequired;
    private RadioGroup mHandleNotifiesGroup;
    private boolean mBroadcasting;
    private NotificationWidget mNotificationWidget;

    private View[] mClickableViews;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                            | WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
    }

    private float[] mTouchHyperbola = new float[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);

        setContentView(R.layout.activity_main);

        if (Project.DEBUG) {
            mDebugLayerView = (DebugLayerView) findViewById(R.id.debug);
        }

        mHandleLayout = (LinearLayout) findViewById(R.id.panel);
        mHandleNotifiesGroup = (RadioGroup) mHandleLayout.findViewById(R.id.radios);
        mHandleView = (FrameLayout) mHandleLayout.findViewById(R.id.handle);
        mHandleActionIcon = (ImageView) mHandleView.findViewById(R.id.icon);
        mScreenOffProgressBar = (HoloCircularProgressBar) mHandleView.findViewById(R.id.timeout);
        mHandleView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                View decorView = getWindow().getDecorView();

                // TODO: That doesn't look like the best place for this.
                if (mUpdateWaveViewRequired) {
                    mUpdateWaveViewRequired = false;
                    mWaveView.init(ViewUtils.getBottom(mCurrentNotification == null
                            ? mLockImageView
                            : mNotificationWidget, decorView),
                            ViewUtils.getBottom(view, decorView) + view.getHeight() / 2,
                            ViewUtils.getTop(mUnlockImageView, decorView),
                            view.getHeight() / 2);
                }

                final float localX = event.getX() - view.getWidth() / 2;
                final float localY = event.getY() - view.getHeight() / 2;
                final float x = event.getRawX();
                final float y = event.getRawY();

                switch_case:
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mNotificationDismissProgress = 0f;
                        mHandlingNotification = mCurrentNotification;

                        mHandleLayout.setVisibility(View.INVISIBLE);
                        mUnlockImageView.setVisibility(View.VISIBLE);

                        if (mHandlingNotification == null) {
                            mLockImageView.setVisibility(View.VISIBLE);
                            mLockViewInAnimation.start();
                        } else {
                            mNotificationWidget.setVisibility(View.VISIBLE);
                            mNotificationViewInAnimation.start();
                        }

                        mUnlockViewInAnimation.start();
                        mWaveView.animateExpand();

                        // Remove screen off timeout.
                        cancelTurningScreenOff();

                        mTouchDownX = localX;
                        mTouchDownY = localY;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        mTouchHyperbola[0] = localX - mTouchDownX;
                        mTouchHyperbola[1] = localY - mTouchDownY;
                        CoordsUtils.putIntoHyperbola(mTouchHyperbola, 100);

                        if (Project.DEBUG) {
                            mDebugLayerView.setTouchPoint(x, y);
                            mDebugLayerView.setHyperbolaPoint(
                                    mTouchHyperbola[0] - localX + x,
                                    mTouchHyperbola[1] - localY + y);
                            mDebugLayerView.postInvalidateOnAnimation();
                        }

                        for (View v : mClickableViews) {
                            v.setPressed(ViewUtils.isTouchPointInView(v, x, y, decorView));
                        }

                        mNotificationDismissProgress =
                                Math.abs(mTouchHyperbola[0]) * 2 / mNotificationWidget.getWidth();

                        mNotificationWidget.setAlpha(
                                MathUtils.range(1 - mNotificationDismissProgress, 0, 1));
                        mNotificationWidget.setTranslationX(mNotificationDismissProgress
                                * MathUtils.charge(mTouchHyperbola[0])
                                * mNotificationWidget.getWidth() / 2);
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        for (View v : mClickableViews) {
                            if (ViewUtils.isTouchPointInView(v, x, y, decorView)
                                    && (mHandlingNotification != null || v != mNotificationWidget)) {
                                v.performClick();
                                v.setPressed(false);
                                break switch_case;
                            }
                        }

                        if (mHandlingNotification != null && mNotificationDismissProgress > 0.6) {
                            NotificationHelper.dismissNotification(mHandlingNotification.getStatusBarNotification());
                        }

                        mNotificationWidget.setVisibility(View.INVISIBLE);
                        mHandleLayout.setVisibility(View.VISIBLE);
                        mUnlockImageView.setVisibility(View.INVISIBLE);
                        mLockImageView.setVisibility(View.INVISIBLE);

                        mUnlockViewInAnimation.cancel();
                        mNotificationViewInAnimation.cancel();
                        mWaveView.cancelExpand();

                        setScreenOffTimeout(SCREEN_OFF_TIMEOUT);
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });
        mWaveView = (WaveView) findViewById(R.id.wave);

        // Setup content views
        mNotificationWidget = (NotificationWidget) findViewById(R.id.notification);
        mUnlockImageView = (ImageView) findViewById(R.id.unlock);
        mLockImageView = (ImageView) findViewById(R.id.lock);

        // Load animations
        mUnlockViewInAnimation = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.anim.card_flip_in_from_bottom);
        assert mUnlockViewInAnimation != null;
        mUnlockViewInAnimation.setTarget(mUnlockImageView);
        mLockViewInAnimation = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.anim.card_flip_in_from_top);
        assert mLockViewInAnimation != null;
        mLockViewInAnimation.setTarget(mLockImageView);
        mNotificationViewInAnimation = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.anim.card_flip_in_from_top);
        assert mNotificationViewInAnimation != null;
        mNotificationViewInAnimation.setTarget(mNotificationWidget);

        // Setup active notification
        ArrayList<OpenStatusBarNotification> n = NotificationPresenter.getInstance().getList();
        final int size = n.size();
        if (size != 0) mCurrentNotification = n.get(size - 1);
        refreshUiCurrentNotification();
        refreshUiNotificationsList();

        mClickableViews = new View[]{mLockImageView, mUnlockImageView, mNotificationWidget};

        // Register listeners
        NotificationPresenter.getInstance().addOnNotificationListChangedListener(this);
        registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    private void refreshUiNotificationsList() {
        ArrayList<OpenStatusBarNotification> list = NotificationPresenter.getInstance().getList();
        if (list.size() > 1) {
            mHandleNotifiesGroup.setVisibility(View.VISIBLE);

            final int notifiesCount = list.size();
            final int viewsCount = mHandleNotifiesGroup.getChildCount();
            final boolean[] notifiesUsage = new boolean[notifiesCount];
            final boolean[] viewsUsage = new boolean[viewsCount];

            // Does not need an update
            for (int i = 0; i < viewsCount; i++) {
                NotificationRadioButton nrb = (NotificationRadioButton) mHandleNotifiesGroup.getChildAt(i);
                OpenStatusBarNotification target = nrb.getNotification();
                for (int j = 0; j < notifiesCount; j++) {
                    OpenStatusBarNotification n = list.get(j);
                    if (target == n || NotificationUtils
                            .equals(target, n)) {

                        notifiesUsage[j] = true;
                        viewsUsage[i] = true;

                        if (target != n) {
                            nrb.setNotification(n);
                        }
                        break;
                    }
                }
            }

            // Re-use free views and remove redundant views.
            boolean removeAllAfter = false;
            for (int i = 0; i < viewsCount; i++) {
                if (viewsUsage[i] & (viewsUsage[i] = true))
                    continue;

                if (removeAllAfter) {
                    mHandleNotifiesGroup.removeViewAt(i);
                    continue;
                }

                NotificationRadioButton nrb = (NotificationRadioButton) mHandleNotifiesGroup.getChildAt(i);
                for (int j = 0; j < notifiesCount; j++) {
                    if (j == notifiesCount - 1)
                        removeAllAfter = true;
                    if (notifiesUsage[j] & (notifiesUsage[j] = true))
                        continue;

                    nrb.setNotification(list.get(j));
                }
            }

            // Create new views
            for (int i = 0; i < notifiesCount; i++) {
                if (notifiesUsage[i])
                    continue;

                NotificationRadioButton nrb = (NotificationRadioButton) getLayoutInflater().inflate(
                        R.layout.radio_notification_icon, mHandleNotifiesGroup, false);
                nrb.setNotification(list.get(i));
                nrb.setOnCheckedChangeListener(this);
                mHandleNotifiesGroup.addView(nrb, i);
            }

            // Check current notification
            for (int i = 0; i < notifiesCount; i++) {
                NotificationRadioButton nrb = (NotificationRadioButton) mHandleNotifiesGroup.getChildAt(i);
                if (mCurrentNotification == nrb.getNotification()) {
                    mBroadcasting = true;
                    nrb.setChecked(true);
                    mBroadcasting = false;
                    break;
                }
            }
        } else {
            mHandleNotifiesGroup.setVisibility(View.GONE);
        }
        mUpdateWaveViewRequired = true;
    }

    private void refreshUiCurrentNotification() {
        mNotificationWidget.setNotification(mCurrentNotification);

        Drawable notifyIcon = mCurrentNotification == null
                ? getResources().getDrawable(R.drawable.stat_unlock)
                : mCurrentNotification.getSmallIcon(this);
        mHandleActionIcon.setImageDrawable(notifyIcon);
        mUpdateWaveViewRequired = true;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        if (!checked || mBroadcasting) {
            return;
        }

        setScreenOffTimeout(SCREEN_OFF_TIMEOUT, true);

        NotificationRadioButton nrb = (NotificationRadioButton) compoundButton;
        mCurrentNotification = nrb.getNotification();
        refreshUiCurrentNotification();
    }

    private void turnScreenOff() {
        removeMessageFromHandler(MSG_TURN_SCREEN_OFF);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm.isScreenOn()) {
            DevicePolicyManager dpm = (DevicePolicyManager)
                    getSystemService(Context.DEVICE_POLICY_SERVICE);
            dpm.lockNow();
        }
    }

    private void unlock() {
        removeMessageFromHandler(MSG_UNLOCK | MSG_TURN_SCREEN_OFF);

        mUnlocking = true;
        finish();
        overridePendingTransition(0, 0);
    }

    private void removeMessageFromHandler(int what) {
        if ((what & MSG_UNLOCK) == MSG_UNLOCK)
            mHandler.removeMessages(MSG_UNLOCK);
        if ((what & MSG_TURN_SCREEN_OFF) == MSG_TURN_SCREEN_OFF)
            mHandler.removeMessages(MSG_TURN_SCREEN_OFF);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSensorManager != null)
            mSensorManager.registerListener(this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),
                    SensorManager.SENSOR_DELAY_NORMAL);

        setScreenOffTimeout(SCREEN_OFF_TIMEOUT, true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeMessageFromHandler(MSG_UNLOCK | MSG_TURN_SCREEN_OFF);

        if (mSensorManager != null)
            mSensorManager.unregisterListener(this);
        if (!mUnlocking) {

            // Cause i can't disable navigation bar's
            // buttons - the only thing i can to do is to turn
            // screen off to prevent random calls and porn.
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm.isScreenOn()) {
                mHandler.sendEmptyMessageDelayed(MSG_TURN_SCREEN_OFF, 500);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        NotificationPresenter.getInstance().removeOnNotificationListChangedListener(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mUpdateWaveViewRequired = true;
    }

    // //////////////////////////////////////////
    // ////////// -- NOTIFICATION -- ////////////
    // //////////////////////////////////////////

    @Override
    public void onNotificationInitialized(NotificationPresenter nm) {

    }

    @Override
    // running on wrong ui thread
    public void onNotificationPosted(final NotificationPresenter nm, final OpenStatusBarNotification notification) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mCurrentNotification == null) {
                    mCurrentNotification = notification;
                    refreshUiCurrentNotification();
                }

                refreshUiNotificationsList();
            }
        });
    }

    @Override
    //  running on wrong ui thread
    public void onNotificationChanged(final NotificationPresenter nm, final OpenStatusBarNotification notification) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (NotificationUtils.equals(mCurrentNotification.getStatusBarNotification(),
                        notification.getStatusBarNotification())) {
                    mCurrentNotification = notification;
                    refreshUiCurrentNotification();
                }
                refreshUiNotificationsList();
            }
        });
    }

    @Override
    //  running on wrong ui thread
    public void onNotificationRemoved(final NotificationPresenter nm, final OpenStatusBarNotification notification) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final int size = nm.getList().size();
                if (NotificationUtils.equals(mCurrentNotification, notification)) {
                    mCurrentNotification = size > 0 ? nm.getList().get(size - 1) : null;
                    refreshUiCurrentNotification();
                }

                if (size > 0) {
                    refreshUiNotificationsList();
                } else {
                    refreshUiCurrentNotification();
                    // turnScreenOff();
                }

                setScreenOffTimeout(mScreenOffTimeout, true);
            }
        });
    }

    @Override
    public void onNotificationSelected(NotificationPresenter nm, OpenStatusBarNotification notification, boolean isChanged) {

    }

    // //////////////////////////////////////////
    // /////// -- ADDITIONAL SECURITY -- ////////
    // //////////////////////////////////////////

    private long mScreenOffAtTime = Long.MAX_VALUE;
    private int mScreenOffTimeout;

    private void setScreenOffTimeout(int delayMillis) {
        setScreenOffTimeout(delayMillis, false);
    }

    private void setScreenOffTimeout(int delayMillis, boolean resetOld) {
        if (resetOld) cancelTurningScreenOff();

        // Notify the user that timeout have changed.
        long now = SystemClock.uptimeMillis();
        if (delayMillis + now < mScreenOffAtTime) {
            mHandler.sendEmptyMessageDelayed(MSG_TURN_SCREEN_OFF, delayMillis);
            mScreenOffProgressBar.animateProgressFromOne(delayMillis);
            mScreenOffTimeout = delayMillis;
            mScreenOffAtTime = now + mScreenOffTimeout;
        }
    }

    private void cancelTurningScreenOff() {
        removeMessageFromHandler(MSG_TURN_SCREEN_OFF);
        mScreenOffProgressBar.cancelAnimateProgress();
        mScreenOffAtTime = Long.MAX_VALUE;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_PROXIMITY:

                // This is only needed to determinate
                // proximity level on activity's start.
                // I don't think that reacting on this
                // event after can be useful.
                mSensorManager.unregisterListener(this);
                mSensorManager = null;

                float distance = event.values[0];
                if (distance < 2 ) {

                    // Well, the device is probably somewhere in bag.
                    setScreenOffTimeout(SCREEN_OFF_TIMEOUT_SUPER_SHORT);

                    if (Project.DEBUG)
                        Log.d(TAG, "Device is in pocket[proximity=" + distance
                                + "cm] --> delayed turning screen off.");
                }
                break;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setScreenOffTimeout(SCREEN_OFF_TIMEOUT_SHORT);
                break;
            default:
                return super.onTouchEvent(event);
        }
        return true;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {  }

    @Override
    public void onBackPressed() {   }

    // //////////////////////////////////////////
    // ////////// -- CLICK EVENTS -- ////////////
    // //////////////////////////////////////////

    public void onClickLockView(View view) {
        turnScreenOff();
    }

    public void onClickUnlockView(View view) {
        unlock();
    }

    public void onClickNotificationView(View view) {
        unlock();
        NotificationHelper.startContentIntent(mCurrentNotification.getStatusBarNotification());
    }
}
*/