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
package com.achep.acdisplay.ui.preferences;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.base.content.ConfigBase;
import com.achep.base.ui.preferences.MaterialDialogPreference;
import com.achep.base.utils.ViewUtils;
import com.afollestad.materialdialogs.MaterialDialog;
import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.SaturationBar;
import com.larswerkman.holocolorpicker.ValueBar;

/**
 * Preference to configure the size of collapsed views.
 *
 * @author Artem Chepurnoy
 */
public class ColorPickerPreference extends MaterialDialogPreference {

    /**
     * @return the original color if {@link #isRandomEnabled(int)} is {@code false},
     * random one otherwise.
     */
    public static int getColor(int color) {
        if (isRandomEnabled(color)) {
            int i = (int) (Math.random() * RANDOM_COLORS.length);
            return RANDOM_COLORS[i];
        }
        return color;
    }

    /**
     * @return {@code true} if you should generate random colors instead
     * of this one, {@code false} otherwise.
     * @see #getColor(int)
     */
    public static boolean isRandomEnabled(int color) {
        return Color.alpha(color) == RANDOM_COLOR_ALPHA_MASK;
    }

    /**
     * A constant of alpha color that defines the 'random color' option.
     */
    private static final int RANDOM_COLOR_ALPHA_MASK = 0xFE;

    /**
     * Main color from the material palette, to be in the
     * random colors list.
     */
    private static final int[] RANDOM_COLORS = {
            0xFFF44336, // Red
            0xFFE91E63, // Pink
            0xFF9C27B0, // Purple
            0xFF673AB7, // Deep purple
            0xFF3F51B5, // Indigo
            0xFF2196F3, // Blue
            0xFF03A9F4, // Light blue
            0xFF00BCD4, // Cyan
            0xFF009688, // Teal
            0xFF4CAF50, // Green
            0xFF8BC34A, // Light green
            0xFFCDDC39, // Lime
            0xFFFFEB3B, // Yellow
            0xFFFFC107, // Amber
            0xFFFF9800, // Orange
            0xFFFF5722, // Deep orange
            0xFF607D8B, // Blue grey
    };

    private static final String TAG = "ColorPickerPreference";

    private final ConfigBase.Option mOption;
    private final Config mConfig;

    private ViewGroup mColorPanel;
    private ColorPicker mColorPicker;

    private RadioButton mRadioCustomColor;
    private RadioButton mRadioRandomColor;

    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mConfig = Config.getInstance();
        mOption = mConfig.getOption(getKey());
    }

    @NonNull
    @Override
    public MaterialDialog onBuildDialog(@NonNull MaterialDialog.Builder builder) {
        MaterialDialog md = builder
                .customView(R.layout.dialog_preference_colorpicker, true)
                .build();

        int color = (int) mOption.read(mConfig);
        boolean randomColor = Color.alpha(color) == RANDOM_COLOR_ALPHA_MASK;
        if (randomColor) color |= Color.argb(255, 0, 0, 0);

        RadioGroup rg = (RadioGroup) md.getCustomView().findViewById(R.id.radios);
        mColorPanel = (ViewGroup) rg.findViewById(R.id.custom_color_panel);
        mColorPicker = (ColorPicker) mColorPanel.findViewById(R.id.picker);
        mColorPicker.addSaturationBar((SaturationBar) mColorPanel.findViewById(R.id.saturationbar));
        mColorPicker.addValueBar((ValueBar) mColorPanel.findViewById(R.id.valuebar));
        mColorPicker.setColor(color);
        mColorPicker.setOldCenterColor(color);

        // Setup radio things
        mRadioCustomColor = (RadioButton) rg.findViewById(R.id.custom_color);
        mRadioRandomColor = (RadioButton) rg.findViewById(R.id.random_color);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                ViewUtils.setVisible(mColorPanel, mRadioCustomColor.isChecked());
            }
        });
        rg.check(randomColor
                ? mRadioRandomColor.getId()
                : mRadioCustomColor.getId());
        return md;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (!positiveResult) {
            return;
        }

        // Save changes to config.
        boolean randomColor = mRadioRandomColor.isChecked();
        int color = mColorPicker.getColor();
        if (randomColor) color &= Color.argb(RANDOM_COLOR_ALPHA_MASK, 255, 255, 255);
        mOption.write(mConfig, getContext(), color, null);
    }

}