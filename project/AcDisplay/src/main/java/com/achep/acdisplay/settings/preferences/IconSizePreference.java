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
package com.achep.acdisplay.settings.preferences;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.DialogHelper;
import com.achep.acdisplay.R;

import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Preference to configure icon sizes.
 * Creates the dialog in settings to change the Icon Size settings.
 *
 * @author Artem Chepurnoy
 */
public class IconSizePreference extends DialogPreference implements
        SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "IconSizePreference";

    private final Drawable mIcon;
    private final CharSequence mTitle;

    private final String mValueLabel;
    private SoftReference<String>[] mSoftStoredLabels;

    private Group[] mGroups;
    private int[] mProgresses = new int[1];
    private int mMin;

    public IconSizePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Get data from default dialog and hide it.
        mIcon = getDialogIcon();
        mTitle = getDialogTitle();
        setDialogTitle(null);

        mValueLabel = getContext().getResources().getString(R.string.preference_resize_dpi);
    }

    @Override
    protected View onCreateDialogView() {
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View root = inflater.inflate(R.layout.preference_dialog_resize, null);
        assert root != null;

        mProgresses = new int[1];
        mGroups = new Group[1];
        mGroups[0] = new Group(
                (SeekBar) root.findViewById(R.id.icon_resize_seekbar),
                (TextView) root.findViewById(R.id.icon_resize_value),
                "setIconSize", "getIconSize");

        Resources res = getContext().getResources();
        final int max = res.getInteger(R.integer.config_maxIconSize);
        mMin = res.getInteger(R.integer.config_minIconSize);
        mSoftStoredLabels = new SoftReference[max + 1];

        Config config = Config.getInstance();


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
            group.seekBar.setProgress(progress);
            Log.e(TAG, "Progress: "+progress);
            Log.e(TAG, "Min: "+mMin);
            Log.e(TAG, "Max: "+max);
        }

        // Build custom dialog.
        return new DialogHelper.Builder(getContext())
                .setIcon(mIcon)
                .setTitle(mTitle)
                .setView(root)
                .createCommonView();
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
                method.invoke(config, getContext(), group.seekBar.getProgress(), null);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
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
            label = String.format(mValueLabel, Float.toString(progress));
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