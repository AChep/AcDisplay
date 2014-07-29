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
package com.achep.acdisplay.acdisplay;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.achep.acdisplay.App;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.Device;
import com.achep.acdisplay.Operator;
import com.achep.acdisplay.Presenter;
import com.achep.acdisplay.R;
import com.achep.acdisplay.Timeout;
import com.achep.acdisplay.activities.KeyguardActivity;
import com.achep.acdisplay.fragments.PocketFragment;
import com.achep.acdisplay.services.media.MediaController;

/**
 * Created by Artem on 25.01.14.
 */
public class AcDisplayActivity extends KeyguardActivity implements
        Timeout.OnTimeoutEventListener,
        PocketFragment.OnSleepRequestListener {

    private static final String TAG = "AcDisplayActivity";

    private ImageView mBackgroundView;
    private boolean mCustomBackgroundShown;

    private Timeout mTimeout;
    private Config mConfig = Config.getInstance();

    private MediaController mMediaController;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        populateFlags(hasFocus);
    }

    @SuppressLint("NewApi")
    private void populateFlags(boolean windowHasFocus) {
        Window window = getWindow();
        View decorView = window.getDecorView();

        int windowFlags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

        if (windowHasFocus) {
            int visibilityUi = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LOW_PROFILE;

            if (mConfig.isFullScreen()) {
                // Hide status bar if fullscreen mode is enabled.
                visibilityUi = visibilityUi
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN;
            }

            if (Device.hasKitKatApi()) {
                // Hide navigation bar and flag sticky.
                visibilityUi = visibilityUi
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }

            decorView.setSystemUiVisibility(visibilityUi);
            window.addFlags(windowFlags);

            mTimeout.resume();
            mTimeout.setTimeoutDelayed(mConfig.getTimeoutNormal(), true);
        } else {
            int visibilityUi = decorView.getSystemUiVisibility();
            if (Device.hasKitKatApi()) {
                // Clear immersive sticky flag.
                // Hopefully it will fix annoying Android feature: IMMERSIVE_PANIC
                visibilityUi ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }

            decorView.setSystemUiVisibility(visibilityUi);
            window.clearFlags(windowFlags);

            mTimeout.setTimeoutDelayed(mConfig.getTimeoutNormal(), true);
            mTimeout.pause();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mMediaController = new MediaController();
        mMediaController.onCreate(this);

        mTimeout = new Timeout();
        mTimeout.registerListener(this);

        super.onCreate(savedInstanceState);

        if (mConfig.isWallpaperShown()) {
            if (mConfig.isShadowEnabled()) {
                setTheme(R.style.AcDisplayTheme_Wallpaper_WithShadow);
            } else {
                setTheme(R.style.AcDisplayTheme_Wallpaper);
            }
        }

        setContentView(R.layout.acdisplay);
        mBackgroundView = (ImageView) findViewById(R.id.background);

        initInternalFragments();

        Presenter.getInstance().attachActivity(this);
    }

    /**
     * Initializes non-UI fragments such as {@link com.achep.acdisplay.fragments.PocketFragment}.
     */
    private void initInternalFragments() {
        Fragment fragment;
        FragmentTransaction ft = getFragmentManager().beginTransaction();

        // Turns screen off inside of your pocket.
        if (mConfig.isActiveModeEnabled()) {
            fragment = new PocketFragment().setListener(this);
            ft.add(fragment, PocketFragment.TAG);
        }

        ft.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();

        populateFlags(true);
        mMediaController.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();

        populateFlags(false);
        mMediaController.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaController.onDestroy();

        mTimeout.unregisterListener(this);
        mTimeout.clear();
        mTimeout = null;

        Presenter.getInstance().detachActivity();
    }

    @Override
    public void onTimeoutEvent(Timeout timeout, int event) {
        switch (event) {
            case Timeout.EVENT_TIMEOUT:
                final boolean lockedSuccessful = lock();

                if (lockedSuccessful) {
                    LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
                    manager.sendBroadcast(new Intent(App.ACTION_INTERNAL_TIMEOUT));

                    // TODO: Detect if user has really missed this wake-up or no.
                    manager.sendBroadcast(new Intent(App.ACTION_INTERNAL_PING_SENSORS));
                }
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSleepRequest() {
        // Probably it's not the best solution, but not worst too.
        // Check if user does not interact with app before locking.
        if (!mTimeout.isPaused()) {

            lock();
        }
    }

    /**
     * Clears background.
     *
     * @see #dispatchSetBackground(android.graphics.Bitmap, int)
     */
    public void dispatchClearBackground() {
        dispatchSetBackground(null);
    }

    /**
     * Smoothly sets the background. This feature is known as "Dynamic background".
     *
     * @param bitmap the bitmap to display, or {@code null} to hide previous background.
     * @param mask   one of the following:
     *               {@link Config#DYNAMIC_BG_ARTWORK_MASK},
     *               {@link Config#DYNAMIC_BG_NOTIFICATION_MASK} or
     *               {@code 0} to bypass mask checking.
     * @see #dispatchClearBackground()
     */
    public void dispatchSetBackground(Bitmap bitmap, int mask) {
        if (mask == 0 || Operator.bitAnd(mConfig.getDynamicBackgroundMode(), mask)) {
            dispatchSetBackground(bitmap);
        }
    }

    /**
     * Smoothly sets the background.
     */
    private void dispatchSetBackground(Bitmap bitmap) {
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

        // TODO: Crossfade background change animation would be really nice.
        float alphaStart = mBackgroundView.getVisibility() == View.GONE ? 0f : 0.4f;

        mCustomBackgroundShown = true;
        mBackgroundView.setAlpha(alphaStart);
        mBackgroundView.setImageBitmap(bitmap);
        mBackgroundView.setVisibility(View.VISIBLE);

        mBackgroundView.animate().cancel();
        mBackgroundView.animate().alpha(1f).setListener(null);
    }

    public Config getConfig() {
        return mConfig;
    }

    public Timeout getTimeout() {
        return mTimeout;
    }

    public MediaController getMediaController() {
        return mMediaController;
    }

}
