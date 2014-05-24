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
package com.achep.activedisplay.settings;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
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
import android.widget.TextView;

import com.achep.activedisplay.Config;
import com.achep.activedisplay.Operator;
import com.achep.activedisplay.R;
import com.achep.activedisplay.widgets.TimeView;
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
	private MultiSelectListPreference mDynamicBackground;
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
				Config.KEY_INTERFACE_WALLPAPER_SHOWN);
		mShadowToggle = (CheckBoxPreference) findPreference(
				Config.KEY_INTERFACE_SHADOW_TOGGLE);
		mDynamicBackground = (MultiSelectListPreference) findPreference(
				Config.KEY_INTERFACE_DYNAMIC_BACKGROUND_MODE);
		mMirroredTimeoutToggle = (CheckBoxPreference) findPreference(
				Config.KEY_INTERFACE_MIRRORED_TIMEOUT_PROGRESS_BAR);
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
		
		mClockFont.setOnPreferenceClickListener(this);
		mClockColor.setOnPreferenceClickListener(this);
		mClockSize.setOnPreferenceClickListener(this);
		
		Config config = Config.getInstance(getActivity());
		mClockColor.setIcon(setIconDrawable(config.getClockColor()));
		
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
		Config config = Config.getInstance(getActivity());
		config.addOnConfigChangedListener(this);

		updateShowWallpaperPreference(config);
		updateShowShadowPreference(config);
		updateMirroredTimeoutPreference(config);
		updateDynamicBackgroundPreference(config);
	}

	@Override
	public void onPause() {
		super.onPause();
		Config config = Config.getInstance(getActivity());
		config.removeOnConfigChangedListener(this);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (mBroadcasting) {
			return true;
		}

		Context context = getActivity();
		Config config = Config.getInstance(context);
		if (preference == mShowWallpaper) {
			config.setWallpaperShown(context, (Boolean) newValue, this);
		} else if (preference == mShadowToggle) {
			config.setShadowEnabled(context, (Boolean) newValue, this);
		} else if (preference == mMirroredTimeoutToggle) {
			config.setMirroredTimeoutProgressBarEnabled(context, (Boolean) newValue, this);
		} else if (preference == mDynamicBackground) {
			int mode = 0;

			Set<String> values = (Set<String>) newValue;
			for (String v : values) {
				mode |= Integer.parseInt(v);
			}

			config.setDynamicBackgroundMode(context, mode, this);
			updateDynamicBackgroundPreferenceSummary(config);
		} else
			return false;
		return true;
	}

	@Override
	public boolean onPreferenceClick(Preference arg0) {
		// TODO Auto-generated method stub
		if(arg0 == mClockFont) {
			showClockFontDialog();
		}
		if(arg0 == mClockColor) {
			showClockColorDialog();
		}
		if(arg0 == mClockSize) {
			showClockSizeDialog();
		}
		return false;
	}

	@Override
	public void onConfigChanged(Config config, String key, Object value) {
		switch (key) {
		case Config.KEY_INTERFACE_WALLPAPER_SHOWN:
			updateShowWallpaperPreference(config);
			break;
		case Config.KEY_INTERFACE_SHADOW_TOGGLE:
			updateShowShadowPreference(config);
			break;
		case Config.KEY_INTERFACE_MIRRORED_TIMEOUT_PROGRESS_BAR:
			updateMirroredTimeoutPreference(config);
			break;
		case Config.KEY_INTERFACE_DYNAMIC_BACKGROUND_MODE:
			updateDynamicBackgroundPreference(config);
			break;
		}
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
			if (Operator.bitandCompare(mode, i)) {
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
				if (Operator.bitandCompare(mode, a)) {
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

	private void showClockFontDialog() {
		

		final Context context = getActivity();
		final Config config = Config.getInstance(context);

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
	
	private void showClockColorDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle("Clock Color");
		final Context context = getActivity();
		final Config config = Config.getInstance(context);
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
		
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				config.setClockColor(context, picker.getColor(), InterfaceFragment.this);
				mClockColor.setIcon(setIconDrawable(picker.getColor()));
				dialog.cancel();
				
			}
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				dialog.cancel();
			}
		});
		builder.create().show();
	}
	
	
	private void showClockSizeDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle("Clock Size");
		final Context context = getActivity();
		final Config config = Config.getInstance(context);
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
		.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog , int which) {
				// TODO Auto-generated method stub
				config.setClockSize(context, sizeBar.getProgress(), InterfaceFragment.this);
				dialog.cancel();
			}
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				dialog.cancel();
			}
		})
		.create()
		.show();
		
	}
	
	public ColorStateDrawable setIconDrawable(int color) {
        Drawable[] colorDrawable = new Drawable[] {
                getActivity().getResources().getDrawable(R.drawable.oval) };
        return new ColorStateDrawable(colorDrawable, color);
    }
}
