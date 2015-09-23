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
package com.achep.base.ui;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.CompoundButton;

import com.achep.acdisplay.R;
import com.achep.acdisplay.ui.DialogHelper;
import com.achep.base.interfaces.ICheckable;
import com.achep.base.permissions.Permission;
import com.achep.base.ui.widgets.SwitchBar;
import com.achep.base.utils.ViewUtils;

/**
 * @author Artem Chepurnoy
 */
public class SwitchBarPermissible implements Permission.OnPermissionStateChanged, ICheckable {

    @NonNull
    private final AppCompatActivity mActivity;
    @NonNull
    private final SwitchBar mSwitchBar;
    @Nullable
    private final Permission[] mPermissions;

    @Nullable
    private CompoundButton.OnCheckedChangeListener mListener;

    public SwitchBarPermissible(@NonNull AppCompatActivity activity, @NonNull SwitchBar switchBar,
                                @Nullable Permission[] permissions) {
        mActivity = activity;
        mPermissions = permissions;
        mSwitchBar = switchBar;
        if (mPermissions != null) mSwitchBar.setIconResource(R.drawable.ic_action_warning_amber);
        mSwitchBar.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && !hasAccess()) {
                    assert mPermissions != null;
                    buttonView.setChecked(false);
                    DialogHelper.showPermissionsDialog(mActivity, mPermissions);
                } else {
                    if (mListener != null) mListener.onCheckedChanged(buttonView, isChecked);
                }
            }

        });
    }

    /**
     * Called on activity resumed.
     */
    public void resume() {
        if (mPermissions == null) {
            return;
        }

        boolean enabled = true;
        for (Permission permission : mPermissions) {
            permission.registerListener(this);
            if (enabled) enabled = permission.isGranted();
        }

        ViewUtils.setVisible(mSwitchBar.getIconView(), !enabled);
        setChecked(mSwitchBar.isChecked() && enabled);
    }

    /**
     * Called on activity paused.
     */
    public void pause() {
        if (mPermissions == null) {
            return;
        }

        for (Permission permission : mPermissions) {
            permission.unregisterListener(this);
        }
    }

    private boolean hasAccess() {
        if (mPermissions == null) return true;
        for (Permission permission : mPermissions)
            if (!permission.isGranted()) return false;
        return true;
    }

    //-- I-CHECKABLE ----------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOnCheckedChangeListener(
            @Nullable CompoundButton.OnCheckedChangeListener listener) {
        mListener = listener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setChecked(boolean checked) {
        mSwitchBar.setChecked(checked);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isChecked() {
        return mSwitchBar.isChecked();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void toggle() {
        mSwitchBar.toggle();
    }

}
