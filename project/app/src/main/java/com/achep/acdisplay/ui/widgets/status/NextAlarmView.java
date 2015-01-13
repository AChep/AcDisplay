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
package com.achep.acdisplay.ui.widgets.status;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by achep on 03.08.14.
 */
// TODO: Watch for alarm changes
public class NextAlarmView extends TextView {

    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\s[0-9]");

    private static final String FORMATTER = "%1$s, %2$s"; // Day, time

    public NextAlarmView(Context context) {
        this(context, null);
    }

    public NextAlarmView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NextAlarmView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateText();
    }

    private void updateText() {
        ContentResolver cr = getContext().getContentResolver();
        String nextAlarm = Settings.System.getString(cr, Settings.System.NEXT_ALARM_FORMATTED);
        if (!TextUtils.isEmpty(nextAlarm)) {
            Matcher m = DIGIT_PATTERN.matcher(nextAlarm);
            if (m.find() && m.start() > 0) {
                nextAlarm = String.format(FORMATTER,
                        nextAlarm.substring(0, m.start() - 1),
                        nextAlarm.substring(m.start() + 1));
            }
        }

        setText(nextAlarm);
    }
}
