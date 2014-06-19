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
package com.achep.acdisplay.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.Operator;
import com.achep.acdisplay.R;
import com.achep.acdisplay.acdisplay.ColorStateDrawable;
import com.achep.acdisplay.settings.preferences.IconSizePreference;
import com.achep.acdisplay.widgets.TimeView;
import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.OpacityBar;
import com.larswerkman.holocolorpicker.SVBar;
import com.larswerkman.holocolorpicker.SaturationBar;
import com.larswerkman.holocolorpicker.ValueBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Interface settings fragment.
 *
 * @author Artem Chepurnoy
 */
public class InterfaceFragment extends PreferenceFragment implements
Preference.OnPreferenceChangeListener,
Config.OnConfigChangedListener, OnPreferenceClickListener {

    private CheckBoxPreference mShowWallpaper;
    private CheckBoxPreference mShadowToggle;
    private CheckBoxPreference mMirroredTimeoutToggle;
    private CheckBoxPreference mCircledIconToggle;
    private CheckBoxPreference mBatteryAlwaysVisibleToggle;
    private CheckBoxPreference mImmersiveMode;
    private IconSizePreference mIconSize;
    private MultiSelectListPreference mDynamicBackground;

    //Add clock customization stuff

    private Preference mClockFont;
    private Preference mClockColor;
    private Preference mClockSize;
    private AlertDialog fontDialog;
    final List<String> fonts = new ArrayList<String>();

    private boolean mBroadcasting;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.interface_settings);

        mShowWallpaper = (CheckBoxPreference) findPreference(
                Config.KEY_UI_WALLPAPER_SHOWN);
        mShadowToggle = (CheckBoxPreference) findPreference(
                Config.KEY_UI_SHADOW_TOGGLE);
        mDynamicBackground = (MultiSelectListPreference) findPreference(
                Config.KEY_UI_DYNAMIC_BACKGROUND_MODE);
        mMirroredTimeoutToggle = (CheckBoxPreference) findPreference(
                Config.KEY_UI_MIRRORED_TIMEOUT_BAR);
        mCircledIconToggle = (CheckBoxPreference) findPreference(
                Config.KEY_UI_NOTIFY_CIRCLED_ICON);
        mBatteryAlwaysVisibleToggle = (CheckBoxPreference) findPreference(
                Config.KEY_UI_STATUS_BATTERY_ALWAYS_VISIBLE);
        mImmersiveMode = (CheckBoxPreference) findPreference(
                Config.KEY_UI_IMMERSIVE_MODE);
        mIconSize = (IconSizePreference) findPreference(
                Config.KEY_UI_ICON_SIZE);

        //find clock customization preferences
        mClockFont = (Preference) findPreference(
                Config.KEY_CLOCK_FONT);
        mClockColor = (Preference) findPreference(
                Config.KEY_CLOCK_COLOR);
        mClockSize = (Preference) findPreference(
                Config.KEY_CLOCK_SIZE);

        mShowWallpaper.setOnPreferenceChangeListener(this);
        mShadowToggle.setOnPreferenceChangeListener(this);
        mDynamicBackground.setOnPreferenceChangeListener(this);
        mMirroredTimeoutToggle.setOnPreferenceChangeListener(this);
        mCircledIconToggle.setOnPreferenceChangeListener(this);
        mBatteryAlwaysVisibleToggle.setOnPreferenceChangeListener(this);
        mImmersiveMode.setOnPreferenceChangeListener(this);


        //add listeners
        mClockFont.setOnPreferenceClickListener(this);
        mClockColor.setOnPreferenceClickListener(this);
        mClockSize.setOnPreferenceClickListener(this);

        //set clock color as preference icon
        Config config = Config.getInstance();
        mClockColor.setIcon(setCircleIconDrawable(config.getClockColor()));



        //add fonts from assets to the list
        fonts.add("fonts/Roboto-Bold.ttf");
        fonts.add("fonts/Roboto-BoldCondensed.ttf");
        fonts.add("fonts/Roboto-BoldItalic.ttf");
        fonts.add("fonts/Roboto-Condensed.ttf");
        fonts.add("fonts/Roboto-Italic.ttf");
        fonts.add("fonts/Roboto-Light.ttf");
        fonts.add("fonts/Roboto-LightItalic.ttf");
        fonts.add("fonts/Roboto-Regular.ttf");
        fonts.add("fonts/Roboto-Thin.ttf");
        fonts.add("fonts/Roboto-ThinItalic.ttf");
        fonts.add("fonts/RobotoSlab-Light.ttf");
        fonts.add("fonts/RobotoSlab-LightItalic.ttf");
        fonts.add("fonts/RobotoSlab-Regular.ttf");
        fonts.add("fonts/RobotoSlab-Thin.ttf");		
        fonts.add("fonts/Franks-Regular.otf");
        fonts.add("fonts/Sabo-Filled.otf");
        fonts.add("fonts/VTKSGoodLuckForYou.ttf");
        fonts.add("fonts/BetterThanPixel.ttf");
        fonts.add("fonts/darkforest.ttf");
        fonts.add("fonts/DarkistheNight.ttf");
        fonts.add("fonts/PanicStricken.ttf");
        fonts.add("fonts/Scratch_Regular.ttf");
        fonts.add("fonts/Stroke.ttf");
        fonts.add("fonts/YoungShark.ttf");
    }

    @Override
    public void onResume() {
        super.onResume();
        Config config = Config.getInstance();
        config.addOnConfigChangedListener(this);

        updateShowWallpaperPreference(config);
        updateShowShadowPreference(config);
        updateMirroredTimeoutPreference(config);
        updateCircledIconPreference(config);
        updateBatteryAlwaysVisiblePreference(config);
        updateDynamicBackgroundPreference(config);
        updateImmersiveMode(config);
    }

    @Override
    public void onPause() {
        super.onPause();
        Config config = Config.getInstance();
        config.removeOnConfigChangedListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mBroadcasting) {
            return true;
        }

        Context context = getActivity();
        Config config = Config.getInstance();
        if (preference == mShowWallpaper) {
            config.setWallpaperShown(context, (Boolean) newValue, this);
        } else if (preference == mShadowToggle) {
            config.setShadowEnabled(context, (Boolean) newValue, this);
        } else if (preference == mMirroredTimeoutToggle) {
            config.setMirroredTimeoutProgressBarEnabled(context, (Boolean) newValue, this);
        } else if (preference == mCircledIconToggle) {
            config.setCircledLargeIconEnabled(context, (Boolean) newValue, this);
        } else if (preference == mBatteryAlwaysVisibleToggle) {
            config.setBatteryAlwaysVisible(context, (Boolean) newValue, this);
        } else if (preference == mDynamicBackground) {
            int mode = 0;

            Set<String> values = (Set<String>) newValue;
            for (String v : values) {
                mode |= Integer.parseInt(v);
            }

            config.setDynamicBackgroundMode(context, mode, this);
            updateDynamicBackgroundPreferenceSummary(config);
        } else if (preference == mImmersiveMode){
            config.setImmersiveMode(context, (Boolean) newValue, this);
        }else
            return false;
        return true;
    }

    @Override
    public void onConfigChanged(Config config, String key, Object value) {
        switch (key) {
        case Config.KEY_UI_WALLPAPER_SHOWN:
            updateShowWallpaperPreference(config);
            break;
        case Config.KEY_UI_SHADOW_TOGGLE:
            updateShowShadowPreference(config);
            break;
        case Config.KEY_UI_MIRRORED_TIMEOUT_BAR:
            updateMirroredTimeoutPreference(config);
            break;
        case Config.KEY_UI_NOTIFY_CIRCLED_ICON:
            updateCircledIconPreference(config);
            break;
        case Config.KEY_UI_STATUS_BATTERY_ALWAYS_VISIBLE:
            updateBatteryAlwaysVisiblePreference(config);
            break;
        case Config.KEY_UI_DYNAMIC_BACKGROUND_MODE:
            updateDynamicBackgroundPreference(config);
            break;
        case Config.KEY_UI_IMMERSIVE_MODE:
            updateImmersiveMode(config);
            break;
        }
    }

    /*
     * Clock customization preferences Click listener
     * Show dialogs
     */
    @Override
    public boolean onPreferenceClick(Preference arg0) {
        // TODO Auto-generated method stub
        if(arg0 == mClockFont) {
            showClockFontDialog();
        }else if(arg0 == mClockColor) {
            showClockColorDialog();
        }else if(arg0 == mClockSize) {
            showClockSizeDialog();
        }
        return false;
    }

    private void updateShowWallpaperPreference(Config config) {
        updatePreference(mShowWallpaper, config.isWallpaperShown());
    }

    private void updateShowShadowPreference(Config config) {
        updatePreference(mShadowToggle, config.isShadowEnabled());
    }

    private void updateMirroredTimeoutPreference(Config config) {
        updatePreference(mMirroredTimeoutToggle, config.isMirroredTimeoutProgressBarEnabled());
    }

    private void updateCircledIconPreference(Config config) {
        updatePreference(mCircledIconToggle, config.isCircledLargeIconEnabled());
    }

    private void updateBatteryAlwaysVisiblePreference(Config config) {
        updatePreference(mBatteryAlwaysVisibleToggle, config.isBatteryAlwaysVisible());
    }

    private void updateImmersiveMode(Config config) {
        updatePreference(mImmersiveMode, config.isImmersible());
    }

    private void updatePreference(CheckBoxPreference preference, boolean checked) {
        mBroadcasting = true;
        preference.setChecked(checked);
        mBroadcasting = false;
    }

    private void updateDynamicBackgroundPreference(Config config) {
        mBroadcasting = true;

        int mode = config.getDynamicBackgroundMode();
        String[] values = new String[Integer.bitCount(mode)];
        for (int i = 1, j = 0; j < values.length; i <<= 1) {
            if (Operator.bitAnd(mode, i)) {
                values[j++] = Integer.toString(i);
            }
        }

        Set<String> valuesSet = new HashSet<>();
        Collections.addAll(valuesSet, values);
        mDynamicBackground.setValues(valuesSet);

        mBroadcasting = false;
        updateDynamicBackgroundPreferenceSummary(config);
    }

    private void updateDynamicBackgroundPreferenceSummary(Config config) {
        CharSequence summary;
        if (config.getDynamicBackgroundMode() != 0) {
            CharSequence[] entries = mDynamicBackground.getEntries();
            CharSequence[] values = mDynamicBackground.getEntryValues();
            int mode = config.getDynamicBackgroundMode();

            String divider = getString(R.string.settings_multi_list_divider);
            StringBuilder sb = new StringBuilder();
            boolean empty = true;

            assert entries != null;
            assert values != null;

            // Append selected items.
            for (int i = 0; i < values.length; i++) {
                int a = Integer.parseInt(values[i].toString());
                if (Operator.bitAnd(mode, a)) {
                    if (!empty) {
                        sb.append(divider);
                    }
                    sb.append(entries[i]);
                    empty = false;
                }
            }

            String itemsText = sb.toString().toLowerCase(Locale.getDefault());
            summary = getString(R.string.settings_dynamic_background_summary, itemsText);
        } else {
            summary = getString(R.string.settings_dynamic_background_disabled);
        }
        mDynamicBackground.setSummary(summary);
    }


    /*
     * CLOCK CUSTOMIZATION DIALOGS SECTION START
     */

    /*
     *  Custom clock Font Dialog
     */
    private void showClockFontDialog() {


        final Context context = getActivity();
        final Config config = Config.getInstance();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Custom Clock Font");
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.font_dialog_layout, null, false);
        ListView list = (ListView) v.findViewById(R.id.font_list);
        FontDialogBaseAdapter mAdapter = new FontDialogBaseAdapter(getActivity(), fonts, android.R.layout.simple_list_item_1);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position,
                    long id) {
                // TODO Auto-generated method stub
                config.setClockFont(context, fonts.get(position), InterfaceFragment.this);
                fontDialog.cancel();
            }

        });

        builder.setView(v);
        fontDialog = builder.create();
        fontDialog.show();

    }


    /*
     * Custom Clock color Dialog
     */
    private void showClockColorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.preference_clock_color_title);
        final Context context = getActivity();
        final Config config = Config.getInstance();
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.color_selection_dialog, null, false);
        final ColorPicker picker = (ColorPicker) v.findViewById(R.id.picker);
        SVBar svBar = (SVBar) v.findViewById(R.id.svbar);
        OpacityBar opacityBar = (OpacityBar) v.findViewById(R.id.opacitybar);
        SaturationBar saturationBar = (SaturationBar) v.findViewById(R.id.saturationbar);
        ValueBar valueBar = (ValueBar) v.findViewById(R.id.valuebar);
        picker.addSVBar(svBar);
        picker.addOpacityBar(opacityBar);
        picker.addSaturationBar(saturationBar);
        picker.addValueBar(valueBar);
        picker.setColor(config.getClockColor());
        builder.setView(v);

        builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                config.setClockColor(context, picker.getColor(), InterfaceFragment.this);
                mClockColor.setIcon(setCircleIconDrawable(picker.getColor()));
                dialog.cancel();

            }
        })
        .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                dialog.cancel();
            }
        });
        builder.create().show();
    }


    /*
     * Custom Clock Size Dialog
     */
    private void showClockSizeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.preference_clock_size_title);
        final Context context = getActivity();
        final Config config = Config.getInstance();
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.clock_size_dialog, null, false);
        final TimeView time = (TimeView) v.findViewById(R.id.time);
        final SeekBar sizeBar = (SeekBar) v.findViewById(R.id.size_bar);
        sizeBar.setMax(200);
        Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), config.getClockFont());
        time.setTypeface(tf);
        time.setTextSize((float)config.getClockSize());
        sizeBar.setProgress(config.getClockSize());
        sizeBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
                // TODO Auto-generated method stub
                time.setTextSize((float)progress);

            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub

            }
            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub

            }

        });

        builder.setView(v)
        .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog , int which) {
                // TODO Auto-generated method stub
                config.setClockSize(context, sizeBar.getProgress(), InterfaceFragment.this);
                dialog.cancel();
            }
        })
        .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                dialog.cancel();
            }
        })
        .create()
        .show();

    }


    /*
     * Create a circle icon drawable for Clock color preference
     */
    public ColorStateDrawable setCircleIconDrawable(int color) {
        Drawable[] colorDrawable = new Drawable[] {
                getActivity().getResources().getDrawable(R.drawable.oval) };
        return new ColorStateDrawable(colorDrawable, color);
    }

}
