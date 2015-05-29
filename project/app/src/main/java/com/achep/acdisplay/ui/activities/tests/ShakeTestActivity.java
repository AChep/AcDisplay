/*
 * Copyright (C) 2015 AChep@xda <artemchep@gmail.com>
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
package com.achep.acdisplay.ui.activities.tests;

import android.content.Context;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import uk.co.jarofgreen.lib.ShakeDetector;

/**
 * An activity for testing and debugging the shake algorithms. The background is
 * red in idle mode and green if shake-d.
 *
 * @author Artem Chepurnoy
 */
public class ShakeTestActivity extends TestActivity implements View.OnClickListener {

    @NonNull
    private final ShakeDetector.Listener mShakeDetectorListener =
            new ShakeDetector.Listener() {
                @Override
                public void onShakeDetected() {
                    mTextView.setBackgroundColor(Color.GREEN);
                }
            };
    @NonNull
    private final ShakeDetector mShakeDetector =
            new ShakeDetector(mShakeDetectorListener);

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        ll.setPadding(30, 30, 30, 30);

        mTextView = onCreateTextView();
        mTextView.setText("Shake detector");

        ll.addView(mTextView);
        setContentView(ll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mShakeDetector.start(sensorManager);
    }

    @Override
    protected void onPause() {
        mShakeDetector.stop();
        super.onPause();
    }

    @NonNull
    private TextView onCreateTextView() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0 /* weight = 1 */);
        lp.weight = 1;
        TextView textView = new TextView(this);
        textView.setLayoutParams(lp);
        textView.setOnClickListener(this);
        textView.setGravity(Gravity.CENTER);
        textView.setBackgroundColor(Color.RED);
        return textView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(View v) {
        v.setBackgroundColor(Color.RED);
    }

}
