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
package com.achep.acdisplay.ui.preferences;

import android.app.TimePickerDialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.TimePicker;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.base.ui.preferences.MaterialDialogPreference;
import com.achep.base.utils.DateUtils;
import com.afollestad.materialdialogs.MaterialDialog;

/**
 * Preference to configure timeouts.
 *
 * @author Artem Chepurnoy
 */
public class InactiveTimePreference extends MaterialDialogPreference implements View.OnClickListener {

    private static final String TAG = "InactiveHoursPreference";

    private CheckBox mEnabled;

    private Data mFrom = new Data();
    private Data mTo = new Data();

    public static class Data {
        public int hours;
        public int minutes;
        public String labelSource;
        public TextView labelTextView;

        public void setTime(Context context, int timeInMinutes) {
            setTime(context, timeInMinutes / 60, timeInMinutes % 60);
        }

        public void setTime(Context context, int hours, int minutes) {
            this.hours = hours;
            this.minutes = minutes;

            String labelTime = DateUtils.formatTime(context, hours, minutes);
            labelTextView.setText(Html.fromHtml(String.format(labelSource, labelTime)));
        }
    }

    public InactiveTimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @NonNull
    @Override
    public MaterialDialog onBuildDialog(@NonNull MaterialDialog.Builder builder) {
        Context context = getContext();
        MaterialDialog md = builder
                .customView(R.layout.preference_dialog_inactive_hours, false)
                .build();

        View root = md.getCustomView();
        assert root != null;
        TextView fromTextView = (TextView) root.findViewById(R.id.from);
        TextView toTextView = (TextView) root.findViewById(R.id.to);
        mEnabled = (CheckBox) root.findViewById(R.id.checkbox);

        fromTextView.setOnClickListener(this);
        toTextView.setOnClickListener(this);

        mFrom.labelTextView = fromTextView;
        mFrom.labelSource = context.getString(R.string.preference_inactive_hours_from);
        mTo.labelTextView = toTextView;
        mTo.labelSource = context.getString(R.string.preference_inactive_hours_to);

        Config config = Config.getInstance();
        mFrom.setTime(context, config.getInactiveTimeFrom());
        mTo.setTime(context, config.getInactiveTimeTo());
        mEnabled.setChecked(config.isInactiveTimeEnabled());
        return md;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (!positiveResult) {
            return;
        }

        Context context = getContext();
        Config config = Config.getInstance();
        config.setInactiveTimeFrom(context, mFrom.hours * 60 + mFrom.minutes, null);
        config.setInactiveTimeTo(context, mTo.hours * 60 + mTo.minutes, null);
        config.setInactiveTimeEnabled(context, mEnabled.isChecked(), null);
    }

    @Override
    public void onClick(View v) {
        final Data data = v == mFrom.labelTextView ? mFrom : mTo;

        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        data.setTime(getContext(), selectedHour, selectedMinute);
                    }
                }, data.hours, data.minutes, true
        );
        timePickerDialog.show();
    }
}
