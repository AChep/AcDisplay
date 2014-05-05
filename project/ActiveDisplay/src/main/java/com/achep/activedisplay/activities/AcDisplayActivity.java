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
package com.achep.activedisplay.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.achep.activedisplay.Config;
import com.achep.activedisplay.Device;
import com.achep.activedisplay.Operator;
import com.achep.activedisplay.Presenter;
import com.achep.activedisplay.R;
import com.achep.activedisplay.Timeout;
import com.achep.activedisplay.activemode.ActiveModeSensor;
import com.achep.activedisplay.activemode.ActiveModeService;
import com.achep.activedisplay.widgets.CircleView;

/**
 * Created by Artem on 25.01.14.
 */
public class AcDisplayActivity extends KeyguardActivity implements
        Timeout.OnTimeoutEventListener, CircleView.Callback {

    private static final String TAG = "AcDisplayActivity";

    // pending finish
    private static final int PENDING_FINISH_MAX_TIME = 1000; // ms.
    private static final int PENDING_FINISH_DELAY = 600; // ms.

    // timeout
    private static final int TIMEOUT_PANIC_MIN_TIME = 1000; // ms.

    private CircleView mCircleView;
    private ImageView mBackgroundView;

    private boolean mCustomBackgroundShown;
    private boolean mImmersiveMode;

    private GestureDetector mGestureDetector;
    private Handler mHandler = new Handler();
    private Timeout mTimeout = new T();
    private Config mConfig;

    private ActiveModeSensor[] mSensors;
    private ActiveModeSensor.Callback mSensorCallback = new ActiveModeSensor.Callback() {
        @Override
        public void show(ActiveModeSensor sensor) { /* unused */ }

        @Override
        public void hide(ActiveModeSensor sensor) {
            if (isCloseableBySensor()) {
                lock();
            }
        }
    };

    private long mPendingFinishTime;
    private Runnable mPendingFinishRunnable = new Runnable() {
        @Override
        public void run() {
            unlock(null, true);
        }
    };

    private class T extends Timeout {

        @Override
        public void setTimeoutDelayed(long delayMillis, boolean resetOld) {
            // This is a "workaround" solution for the
            // never time out option.
            if (mConfig.isTimeoutEnabled()) {
                super.setTimeoutDelayed(delayMillis, resetOld);
            }
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return lock();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        handleWindowFocusChanged(hasFocus);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void handleWindowFocusChanged(boolean hasFocus) {
        Window window = getWindow();

        int windowFlags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        if (hasFocus) {
            int visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
            if (mImmersiveMode) visibility |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            window.getDecorView().setSystemUiVisibility(visibility);
            window.addFlags(windowFlags);

            // Start ticking.
            mTimeout.resume();
        } else {
            window.clearFlags(windowFlags);

            mTimeout.setTimeoutDelayed(mConfig.getTimeoutNormal(), true);
            mTimeout.pause();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConfig = Config.getInstance();
        if (mConfig.isWallpaperShown()) {
            if (mConfig.isShadowEnabled()) {
                setTheme(R.style.AcDisplayTheme_Wallpaper_WithShadow);
            } else {
                setTheme(R.style.AcDisplayTheme_Wallpaper);
            }
        }

        setContentView(R.layout.activity_acdisplay);
        mBackgroundView = (ImageView) findViewById(R.id.background);
        mCircleView = (CircleView) findViewById(R.id.circle);
        mCircleView.setCallback(this);

        mImmersiveMode = Device.hasKitKatApi();
        mGestureDetector = new GestureDetector(this, new GestureListener());
        mSensors = ActiveModeService.buildAvailableSensorsList(this);

        mTimeout.registerListener(this);
        mTimeout.setTimeoutDelayed(mConfig.getTimeoutNormal());

        Presenter.getInstance().attachActivity(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleWindowFocusChanged(true);
        mHandler.removeCallbacks(mPendingFinishRunnable);

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        for (ActiveModeSensor sensor : mSensors) {
            sensor.registerCallback(mSensorCallback);
            sensor.onAttached(sensorManager, this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mTimeout.setTimeoutDelayed(mConfig.getTimeoutNormal(), true);
        mTimeout.pause();

        if (SystemClock.elapsedRealtime() - mPendingFinishTime < PENDING_FINISH_MAX_TIME) {
            mPendingFinishTime = 0;
            mHandler.postDelayed(
                    mPendingFinishRunnable,
                    PENDING_FINISH_DELAY);
        }

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        for (ActiveModeSensor sensor : mSensors) {
            sensor.onDetached(sensorManager);
            sensor.unregisterCallback(mSensorCallback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTimeout.unregisterListener(this);
        mTimeout.clear();
        Presenter.getInstance().detachActivity();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (Operator.bitAnd(event.getFlags(), KeyEvent.FLAG_LONG_PRESS)) {

                    AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    if (am.isMusicActive()) {
                        handled = true;

                        // sendMediaButtonClick(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                        //         ? KeyEvent.KEYCODE_MEDIA_NEXT
                        //         : KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                        // TODO: Go to previous / next track on long press of volume keys (if music is playing).
                    }
                }
                break;
            default:
                return super.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO: Skip "Leave immersive mode swipes".

        mCircleView.onTouchEvent2(event);
        mGestureDetector.onTouchEvent(event);

        return super.onTouchEvent(event);
    }

    @Override
    public void onTimeoutEvent(Timeout timeout, int event) {
        switch (event) {
            case Timeout.EVENT_TIMEOUT:
                lock();
                break;
        }
    }

    @Override
    public void onCircleEvent(float radius, float ratio, int event) {
        switch (event) {
            case CircleView.ACTION_START:
                mTimeout.pause();
                break;
            case CircleView.ACTION_UNLOCK:
                unlock(null, true);
                break;
            case CircleView.ACTION_CANCELED:
                mTimeout.resume();

                // If remaining time is very low - increase
                // it to provide user a bit more time to fap
                // on features.
                if (mTimeout.getRemainingTime() < TIMEOUT_PANIC_MIN_TIME) {
                    mTimeout.delay(TIMEOUT_PANIC_MIN_TIME);
                }
                break;
        }
    }

    @Override
    public void unlock(Runnable runnable, boolean finish) {
        super.unlock(runnable, finish);
        if (!finish) {
            mPendingFinishTime = SystemClock.elapsedRealtime();
        }
    }

    /**
     * Sends media button's click with given key code.
     *
     * @param keyCode May be one of media key events.
     * @see KeyEvent#KEYCODE_MEDIA_NEXT
     * @see KeyEvent#KEYCODE_MEDIA_PLAY_PAUSE
     * @see KeyEvent#KEYCODE_MEDIA_PREVIOUS
     */
    private void sendMediaButtonClick(int keyCode) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        KeyEvent keyDown = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        KeyEvent keyUp = new KeyEvent(KeyEvent.ACTION_UP, keyCode);

        sendOrderedBroadcast(intent.putExtra(Intent.EXTRA_KEY_EVENT, keyDown), null);
        sendOrderedBroadcast(intent.putExtra(Intent.EXTRA_KEY_EVENT, keyUp), null);
    }

    /**
     * @return an instance of timeout handler.
     */
    public Timeout getTimeout() {
        return mTimeout;
    }

    /**
     * @return an instance of timeout config.
     */
    public Config getConfig() {
        return mConfig;
    }

    /**
     * @return True is this activity may be closed by
     * {@link com.achep.activedisplay.activemode.ActiveModeSensor active sensors}.
     */
    // TODO: Write something better
    public boolean isCloseableBySensor() {
        return !mTimeout.isPaused() && hasWindowFocus();
    }

    public void dispatchSetBackground(Bitmap bitmap) {
        if (bitmap == null) {
            if (mCustomBackgroundShown) {
                mBackgroundView.animate().cancel();
                mBackgroundView.animate().alpha(0f).setListener(
                        new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                mBackgroundView.setImageBitmap(null);
                                mBackgroundView.setVisibility(View.GONE);
                            }
                        }
                );
            }
            mCustomBackgroundShown = false;
            return;
        }

        float alphaStart = mBackgroundView.getVisibility() == View.GONE ? 0f : 0.4f;

        mCustomBackgroundShown = true;
        mBackgroundView.setAlpha(alphaStart);
        mBackgroundView.setImageBitmap(bitmap);
        mBackgroundView.setVisibility(View.VISIBLE);

        mBackgroundView.animate().cancel();
        mBackgroundView.animate().alpha(0.8f).setListener(null);
    }

}
