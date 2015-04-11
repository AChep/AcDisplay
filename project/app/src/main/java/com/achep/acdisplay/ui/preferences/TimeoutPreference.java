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

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.base.content.ConfigBase;
import com.achep.base.ui.preferences.MaterialDialogPreference;
import com.afollestad.materialdialogs.MaterialDialog;

import java.lang.ref.SoftReference;

/**
 * Preference to configure timeouts.
 * Creates the dialog in settings to change the TimeOut settings.
 *
 * @author Artem Chepurnoy
 */
public class TimeoutPreference extends MaterialDialogPreference implements
        SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "TimeoutPreference";

    private static final int MULTIPLIER = 500;

    private final String mValueLabel;
    private SoftReference<String>[] mSoftStoredLabels;

    private Group[] mGroups;
    private int[] mProgresses = new int[3];
    private int mMin;

    public TimeoutPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mValueLabel = context.getResources().getString(R.string.preference_timeout_sec);
    }

    @NonNull
    @Override
    public MaterialDialog onBuildDialog(@NonNull MaterialDialog.Builder builder) {
        Resources res = getContext().getResources();
        MaterialDialog md = builder
                .customView(R.layout.preference_dialog_timeout, false)
                .build();

        Config config = Config.getInstance();
        View root = md.getCustomView();
        assert root != null;

        mProgresses = new int[2];
        mGroups = new Group[mProgresses.length];
        mGroups[0] = new Group(
                (SeekBar) root.findViewById(R.id.normal_timeout_seekbar),
                (TextView) root.findViewById(R.id.normal_timeout_value),
                config.getOption(Config.KEY_TIMEOUT_NORMAL));
        mGroups[1] = new Group(
                (SeekBar) root.findViewById(R.id.short_timeout_seekbar),
                (TextView) root.findViewById(R.id.short_timeout_value),
                config.getOption(Config.KEY_TIMEOUT_SHORT));

        final int max = res.getInteger(R.integer.config_timeout_maxDurationMillis) / MULTIPLIER;
        mMin = res.getInteger(R.integer.config_timeout_minDurationMillis) / MULTIPLIER;
        mSoftStoredLabels = new SoftReference[max + 1];

        for (Group group : mGroups) {
            int progress = (int) group.option.read(config);
            group.seekBar.setOnSeekBarChangeListener(this);
            group.seekBar.setMax(max);
            group.seekBar.setProgress(progress / MULTIPLIER);
        }

        return md;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (!positiveResult) {
            return;
        }

        // Save changes to config.
        Config config = Config.getInstance();
        for (Group group : mGroups) {
            int value = group.seekBar.getProgress() * MULTIPLIER;
            group.option.write(config, getContext(), value, null);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean byUser) {
        int i = 0;
        Group group = null;
        for (; i < mGroups.length; i++)
            if (mGroups[i].seekBar == seekBar) {
                group = mGroups[i];
                break;
            }
        assert group != null;

        // Store labels to soft references array
        // to prevent lots of new strings.
        String label;
        SoftReference<String> cached = mSoftStoredLabels[progress];
        if (cached == null || cached.get() == null) {
            label = String.format(mValueLabel, Float.toString(progress * MULTIPLIER / 1000f));
            mSoftStoredLabels[progress] = new SoftReference<>(label);
        } else {
            label = cached.get();
        }

        group.textView.setText(label);

        if (!byUser) {
            return;
        }

        if (progress < mMin) {
            seekBar.setProgress(mMin);
            return;
        }

        for (int j = i - 1; j >= 0; j--) {
            int old = mGroups[j].seekBar.getProgress();
            int current = Math.max(mProgresses[j], progress);
            if (old != current) {
                mGroups[j].seekBar.setProgress(current);
            }
        }

        for (++i; i < mGroups.length; i++) {
            int old = mGroups[i].seekBar.getProgress();
            int current = Math.min(mProgresses[i], progress);
            if (old != current) {
                mGroups[i].seekBar.setProgress(current);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        for (int i = 0; i < mProgresses.length; i++) {
            mProgresses[i] = mGroups[i].seekBar.getProgress();
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) { /* unused */ }

    /**
     * An object to store the seekbars and variables in that are used in the dialog
     */
    private static class Group {
        SeekBar seekBar;
        TextView textView;
        ConfigBase.Option option;

        public Group(SeekBar seekBar, TextView textView, ConfigBase.Option option) {
            this.seekBar = seekBar;
            this.textView = textView;
            this.option = option;
        }
    }
}