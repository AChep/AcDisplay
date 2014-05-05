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
package com.achep.activedisplay.settings.preferences;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.achep.activedisplay.Config;
import com.achep.activedisplay.DialogHelper;
import com.achep.activedisplay.R;

import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Preference to configure timeouts.
 * Creates the dialog in settings to change the TimeOut settings.
 *
 * @author Artem Chepurnoy
 */
public class TimeoutPreference extends DialogPreference implements
        SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "TimeoutPreference";

    private static final int MULTIPLIER = 500;

    private final Drawable mIcon;
    private final CharSequence mTitle;

    private final String mValueLabel;
    private SoftReference<String>[] mSoftStoredLabels;

    private Group[] mGroups;
    private int[] mProgresses = new int[3];
    private int mMin;

    private CheckBox mDisabled;

    public TimeoutPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Get data from default dialog and hide it.
        mIcon = getDialogIcon();
        mTitle = getDialogTitle();
        setDialogTitle(null);

        mValueLabel = getContext().getResources().getString(R.string.preference_timeout_sec);
    }

    @Override
    protected View onCreateDialogView() {
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View root = inflater.inflate(R.layout.preference_dialog_timeout, null);
        assert root != null;

        mProgresses = new int[2];
        mGroups = new Group[mProgresses.length];
        mGroups[0] = new Group(
                (SeekBar) root.findViewById(R.id.normal_timeout_seekbar),
                (TextView) root.findViewById(R.id.normal_timeout_value),
                "setTimeoutNormal", "getTimeoutNormal");
        mGroups[1] = new Group(
                (SeekBar) root.findViewById(R.id.short_timeout_seekbar),
                (TextView) root.findViewById(R.id.short_timeout_value),
                "setTimeoutShort", "getTimeoutShort");

        Resources res = getContext().getResources();
        final int max = res.getInteger(R.integer.config_timeout_maxDurationMillis) / MULTIPLIER;
        mMin = res.getInteger(R.integer.config_timeout_minDurationMillis) / MULTIPLIER;
        mSoftStoredLabels = new SoftReference[max + 1];

        Config config = Config.getInstance();

        mDisabled = (CheckBox) root.findViewById(R.id.no_timeout_checkbox);
        mDisabled.setChecked(!config.isTimeoutEnabled());
        mDisabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                for (Group group : mGroups) {
                    group.seekBar.setEnabled(!isChecked);
                }
            }
        });

        for (Group group : mGroups) {
            int progress = 0;
            try {
                Method method = Config.class.getDeclaredMethod(group.getterName);
                method.setAccessible(true);
                progress = (int) method.invoke(config);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }

            group.seekBar.setOnSeekBarChangeListener(this);
            group.seekBar.setMax(max);
            group.seekBar.setProgress(progress / MULTIPLIER);
            group.seekBar.setEnabled(!mDisabled.isChecked());
        }

        // Build custom dialog.
        return new DialogHelper.Builder(getContext())
                .setIcon(mIcon)
                .setTitle(mTitle)
                .setView(root)
                .create();
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
            try {
                Method method = Config.class.getDeclaredMethod(group.setterName,
                        Context.class, int.class,
                        Config.OnConfigChangedListener.class);
                method.setAccessible(true);
                method.invoke(config, getContext(), group.seekBar.getProgress() * MULTIPLIER, null);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        config.setTimeoutEnabled(getContext(), !mDisabled.isChecked(), null);
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
        String setterName;
        String getterName;

        public Group(SeekBar seekBar, TextView textView, String setterName, String getterName) {
            this.seekBar = seekBar;
            this.textView = textView;
            this.setterName = setterName;
            this.getterName = getterName;
        }
    }
}