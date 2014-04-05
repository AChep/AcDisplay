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

package com.achep.activedisplay.widgets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.TextView;

import com.achep.activedisplay.R;
import com.achep.activedisplay.utils.PowerUtils;

import java.util.Calendar;

/**
 * Created by Artem on 19.03.14.
 */
public class StatusTextView extends TextView {

    private boolean mShowBattery;
    private int mBatteryLevel;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_DATE_CHANGED:
                case Intent.ACTION_TIMEZONE_CHANGED:
                    break;
                case Intent.ACTION_BATTERY_CHANGED:
                    updateBatteryLevel(intent);
                    break;
                default:
                    return;
            }
            updateText();
        }
    };

    private String mFormat;
    private String mBatteryDateFormat;

    public StatusTextView(Context context) {
        super(context);
        init();
    }

    public StatusTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StatusTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mFormat = getResources().getString(R.string.status_format_date);
        mBatteryDateFormat = getResources().getString(R.string.status_format_battery_plus_date);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        getContext().registerReceiver(mIntentReceiver, filter, null, null);

        Intent batteryStats = getContext().registerReceiver(
                null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        updateBatteryLevel(batteryStats);
        updateText();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(mIntentReceiver);
    }

    private void updateBatteryLevel(Intent intent) {
        if (intent == null) {
            return;
        }

        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        mBatteryLevel = Math.round(level / (float) scale * 100);
        mShowBattery = mBatteryLevel < 15 || PowerUtils.isPlugged(intent);
    }

    private void updateText() {
        final Calendar calendar = Calendar.getInstance();
        final CharSequence format;

        if (mShowBattery) {
            format = String.format(mBatteryDateFormat, mBatteryLevel);
        } else {
            format = mFormat;
        }

        setText(DateFormat.format(format, calendar));
        setContentDescription(DateFormat.format(format, calendar));
    }
}
