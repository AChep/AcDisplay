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
import android.app.FragmentManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.achep.activedisplay.ActiveDisplayPresenter;
import com.achep.activedisplay.Config;
import com.achep.activedisplay.Device;
import com.achep.activedisplay.R;
import com.achep.activedisplay.fragments.AcDisplayFragment;

/**
 * Created by Artem on 25.01.14.
 */
public class AcDisplayActivity extends KeyguardActivity {

    private static final String TAG = "AcDisplayActivity";

    private static final int PENDING_FINISH_MAX_TIME = 1000; // ms.
    private static final int PENDING_FINISH_DELAY = 600; // ms.

    private AcDisplayFragment mAcDisplayFragment;
    private ImageView mBackgroundView;
    private boolean mCustomBackgroundShown;

    private long mPendingFinishTime;
    private Runnable mPendingFinishRunnable = new Runnable() {
        @Override
        public void run() {
            unlock(null, true);
        }
    };

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        handleWindowFocusChanged(hasFocus);
        mAcDisplayFragment.onWindowFocusChanged(hasFocus);
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
            if (Device.hasKitKatApi()) visibility |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            window.getDecorView().setSystemUiVisibility(visibility);
            window.addFlags(windowFlags);
        } else {
            window.clearFlags(windowFlags);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Config config = Config.getInstance(this);
        if (config.isWallpaperShown()) {
            if (config.isShadowEnabled()) {
                setTheme(R.style.AcDisplayTheme_Wallpaper_WithShadow);
            } else {
                setTheme(R.style.AcDisplayTheme_Wallpaper);
            }
        }

        setContentView(R.layout.activity_acdisplay);
        FragmentManager fm = getFragmentManager();

        mAcDisplayFragment = (AcDisplayFragment) fm.findFragmentById(R.id.acdisplay_fragment);
        mBackgroundView = (ImageView) findViewById(R.id.background);

        ActiveDisplayPresenter.getInstance().attachActivity(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleWindowFocusChanged(true);
        mHandler.removeCallbacks(mPendingFinishRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (SystemClock.uptimeMillis() - mPendingFinishTime < PENDING_FINISH_MAX_TIME) {
            mHandler.postDelayed(mPendingFinishRunnable, PENDING_FINISH_DELAY);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActiveDisplayPresenter.getInstance().detachActivity();
    }

    @Override
    public void unlock(Runnable runnable, boolean finish) {
        super.unlock(runnable, finish);
        if (!finish) {
            mPendingFinishTime = SystemClock.uptimeMillis();
        }
    }

    public void dispatchSetBackground(Bitmap bitmap) {
        if (bitmap == null) {
            if (mCustomBackgroundShown) {
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
