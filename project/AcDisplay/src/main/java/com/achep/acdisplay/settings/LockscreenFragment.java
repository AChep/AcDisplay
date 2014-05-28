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

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Switch;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.acdisplay.acdisplay.ColorStateDrawable;
import com.achep.acdisplay.settings.enablers.LockscreenEnabler;
import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.OpacityBar;
import com.larswerkman.holocolorpicker.SVBar;
import com.larswerkman.holocolorpicker.SaturationBar;
import com.larswerkman.holocolorpicker.ValueBar;

/**
 * Created by Artem on 09.02.14.
 */
public class LockscreenFragment extends PreferenceFragment implements Config.OnConfigChangedListener, OnPreferenceClickListener {

    private LockscreenEnabler mLockscreenEnabler;
    private Preference mCircleColorPreference;
    private Preference mIconColorPreference;
    private Config mConfig;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.lockscreen_settings);

        Activity activity = getActivity();
        ActionBar actionBar = activity.getActionBar();
        mConfig = Config.getInstance();

        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.layout_ab_switch);
        Switch switch_ = (Switch) actionBar.getCustomView().findViewById(R.id.switch_);
        mLockscreenEnabler = new LockscreenEnabler(activity, switch_);
        
        mCircleColorPreference = (Preference) findPreference(mConfig.KEY_LOCK_CIRCLE_COLOR);
        mIconColorPreference = (Preference) findPreference(mConfig.KEY_LOCK_ICON_COLOR);
        
        mCircleColorPreference.setOnPreferenceClickListener(this);
        mIconColorPreference.setOnPreferenceClickListener(this);
        
    }

    @Override
    public void onResume() {
        super.onResume();
        mLockscreenEnabler.resume();
        restore();
    }

    @Override
    public void onPause() {
        super.onPause();
        mLockscreenEnabler.pause();
    }

    @Override
    public boolean onPreferenceClick(Preference arg0) {
        // TODO Auto-generated method stub
        showColorDialog(arg0.getKey());
        return false;
    }
    
    private void restore() {
        Config config = Config.getInstance();
        mCircleColorPreference.setIcon(setCircleIconDrawable(config.getLockCircleColor()));
        mIconColorPreference.setIcon(setCircleIconDrawable(config.getLockIconColor()));
    }
    
    private void showColorDialog(final String key) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.preference_clock_color_title);
        final Context context = getActivity();
        final Config config = Config.getInstance();
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.color_selection_dialog_noalpha, null, false);
        final ColorPicker picker = (ColorPicker) v.findViewById(R.id.picker);
        SVBar svBar = (SVBar) v.findViewById(R.id.svbar);
        SaturationBar saturationBar = (SaturationBar) v.findViewById(R.id.saturationbar);
        ValueBar valueBar = (ValueBar) v.findViewById(R.id.valuebar);
        picker.addSVBar(svBar);
        picker.addSaturationBar(saturationBar);
        picker.addValueBar(valueBar);
        if(key.equals(Config.KEY_LOCK_CIRCLE_COLOR)) {
            picker.setColor(config.getLockCircleColor());
        }else if (key.equals(Config.KEY_LOCK_ICON_COLOR)) {
            picker.setColor(config.getLockIconColor());
        }
        builder.setView(v);

        builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                if(key.equals(Config.KEY_LOCK_CIRCLE_COLOR)) {
                    config.setLockCircleColor(context, picker.getColor(), LockscreenFragment.this);
                    mCircleColorPreference.setIcon(setCircleIconDrawable(picker.getColor()));
                }else if (key.equals(Config.KEY_LOCK_ICON_COLOR)) {
                    config.setLockIconColor(context, picker.getColor(), LockscreenFragment.this);
                    mIconColorPreference.setIcon(setCircleIconDrawable(picker.getColor()));
                }
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
     * Create a circle icon drawable for Clock color preference
     */
    public ColorStateDrawable setCircleIconDrawable(int color) {
        Drawable[] colorDrawable = new Drawable[] {
                getActivity().getResources().getDrawable(R.drawable.oval) };
        return new ColorStateDrawable(colorDrawable, color);
    }

    @Override
    public void onConfigChanged(Config config, String key, Object value) {
        // TODO Auto-generated method stub
        
    }
}
