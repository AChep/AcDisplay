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
import android.graphics.drawable.Drawable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.base.content.ConfigBase;
import com.achep.base.ui.DialogBuilder;
import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.SaturationBar;
import com.larswerkman.holocolorpicker.ValueBar;

/**
 * Preference to configure the size of collapsed views.
 *
 * @author Artem Chepurnoy
 */
public class ColorPickerPreference extends DialogPreference {

    private static final String TAG = "ColorPickerPreference";

    private final Drawable mIcon;
    private final CharSequence mTitle;
    private final ConfigBase.Option mOption;
    private final Config mConfig;

    private ColorPicker mColorPicker;

    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mConfig = Config.getInstance();
        mOption = mConfig.getOption(getKey());

        // Get data from default dialog and hide it.
        mIcon = getDialogIcon();
        mTitle = getDialogTitle();
        setDialogTitle(null);
    }

    @Override
    protected View onCreateDialogView() {
        final View root = new DialogBuilder(getContext())
                .setIcon(mIcon)
                .setTitle(mTitle)
                .setContentView(R.layout.dialog_preference_colorpicker)
                .createView();

        int color = (int) mOption.read(mConfig);
        mColorPicker = (ColorPicker) root.findViewById(R.id.picker);
        mColorPicker.addSaturationBar((SaturationBar) root.findViewById(R.id.saturationbar));
        mColorPicker.addValueBar((ValueBar) root.findViewById(R.id.valuebar));
        mColorPicker.setColor(color);
        mColorPicker.setOldCenterColor(color);

        return root;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (!positiveResult) {
            return;
        }

        // Save changes to config.
        mOption.write(mConfig, getContext(), mColorPicker.getColor(), null);
    }

}