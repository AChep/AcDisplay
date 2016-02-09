/*
 * Copyright (C) 2016 AChep@xda <artemchep@gmail.com>
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
package com.achep.acdisplay.ui.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.View;

import com.achep.acdisplay.R;
import com.achep.base.Device;
import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntroFragment;

/**
 * @author Artem Chepurnoy
 */
public class IntroActivity extends AppIntro2 {

    private static final int SYSTEM_UI_BASIC_FLAGS;

    static {
        final int f = Device.hasKitKatApi() ? View.SYSTEM_UI_FLAG_HIDE_NAVIGATION : 0;
        SYSTEM_UI_BASIC_FLAGS = f
                | View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
    }

    @Override
    public void init(Bundle savedInstanceState) {
        addSlide(AppIntroFragment.newInstance(
                getString(R.string.intro_welcome_title),
                getString(R.string.intro_welcome_summary),
                R.drawable.ic_intro_app, 0xFF37474f));
        addSlide(AppIntroFragment.newInstance(
                getString(R.string.intro_notifications_title),
                getString(R.string.intro_notifications_summary),
                R.drawable.ic_intro_notifications, 0xFF00695C));
        // TODO: Get the color from current theme.
        int color = 0xFF888888;
        addSlide(AppIntroFragment.newInstance(
                getString(R.string.intro_am_title),
                Html.fromHtml(getString(R.string.intro_am_summary_skeleton,
                        getString(R.string.intro_am_summary),
                        getString(R.string.intro_settings_enable),
                        Integer.toHexString(Color.red(color))
                                + Integer.toHexString(Color.green(color))
                                + Integer.toHexString(Color.blue(color)))).toString(),
                R.drawable.ic_intro_active_mode, 0xFF00838F));

        // Update system ui visibility: hide nav bar and the
        // status bar.
        final View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
                    public final void onSystemUiVisibilityChange(int f) {
                        setSystemUiVisibilityFake();
                        decorView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                int visibility = SYSTEM_UI_BASIC_FLAGS
                                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                        | View.SYSTEM_UI_FLAG_FULLSCREEN;
                                decorView.setSystemUiVisibility(visibility);
                            }
                        }, 100);
                    }
                }
        );
    }

    public void setSystemUiVisibilityFake() {
        int visibility = SYSTEM_UI_BASIC_FLAGS
                | View.SYSTEM_UI_FLAG_IMMERSIVE
                | View.SYSTEM_UI_FLAG_FULLSCREEN;

        getWindow().getDecorView().setSystemUiVisibility(visibility);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setSystemUiVisibilityFake();
    }

    @Override
    public void onDonePressed() {
        finish(); // Should be replace this activity with main activity?
    }

    @Override
    public void onSlideChanged() { /* unused */ }

    @Override
    public void onNextPressed() { /* unused */ }

}