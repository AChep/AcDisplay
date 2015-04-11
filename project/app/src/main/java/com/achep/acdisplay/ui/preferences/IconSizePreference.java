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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.base.ui.preferences.MaterialDialogPreference;
import com.afollestad.materialdialogs.MaterialDialog;

import java.lang.ref.SoftReference;

/**
 * Preference to configure the size of collapsed views.
 *
 * @author Artem Chepurnoy
 */
public class IconSizePreference extends MaterialDialogPreference implements
        SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "IconSizePreference";

    private final String mValueLabel;
    private SoftReference<String>[] mSoftStoredLabels;

    private int mMin;

    private SeekBar mSeekBar;
    private TextView mValueTextView;
    private LinearLayout mContainer;

    public IconSizePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mValueLabel = context.getResources().getString(R.string.preference_icon_size_dpi);
    }

    @NonNull
    @Override
    public MaterialDialog onBuildDialog(@NonNull MaterialDialog.Builder builder) {
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Resources res = getContext().getResources();
        MaterialDialog md = builder
                .customView(R.layout.preference_dialog_size, false)
                .build();

        final int max = res.getInteger(R.integer.config_icon_size_max_dp);
        mMin = res.getInteger(R.integer.config_icon_size_min_dp);
        mSoftStoredLabels = new SoftReference[max + 1 - mMin];

        Config config = Config.getInstance();

        View root = md.getCustomView();
        assert root != null;
        mContainer = (LinearLayout) root.findViewById(R.id.container);
        mValueTextView = (TextView) root.findViewById(R.id.info);
        mSeekBar = (SeekBar) root.findViewById(R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setMax(max - mMin);
        mSeekBar.setProgress(config.getIconSize(Config.ICON_SIZE_DP) - mMin);

        // Init preview
        for (int i = 0; i < 3; i++) {
            View view = inflater.inflate(R.layout.notification_icon, mContainer, false);
            view.setBackgroundColor(res.getColor(R.color.selector_pressed_dark));
            ImageView icon = (ImageView) view.findViewById(R.id.icon);
            icon.setImageResource(R.drawable.stat_notify);
            TextView text = (TextView) view.findViewById(R.id.number);
            text.setText(Integer.toString(i * 3));
            mContainer.addView(view);
        }

        onStopTrackingTouch(mSeekBar);

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
        config.setIconSizeDp(getContext(), mSeekBar.getProgress() + mMin, null);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean byUser) {

        // Store labels to soft references array
        // to prevent lots of new strings.
        String label;
        SoftReference<String> cached = mSoftStoredLabels[progress];
        if (cached == null || cached.get() == null) {
            label = String.format(mValueLabel, Integer.toString(progress + mMin));
            mSoftStoredLabels[progress] = new SoftReference<>(label);
        } else {
            label = cached.get();
        }

        mValueTextView.setText(label);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) { /* unused */ }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // Update the preview of the collapsed views
        // list.
        float density = getContext().getResources().getDisplayMetrics().density;
        int size = Math.round((seekBar.getProgress() + mMin) * density);
        int length = mContainer.getChildCount();
        for (int i = 0; i < length; i++) {
            View child = mContainer.getChildAt(i);
            ViewGroup.LayoutParams lp = child.getLayoutParams();
            lp.height = size;
            lp.width = size;
            child.setLayoutParams(lp);
        }
    }

}