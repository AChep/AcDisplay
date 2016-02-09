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
package com.achep.base.ui.fragments.dialogs;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.achep.acdisplay.R;
import com.achep.base.permissions.Permission;
import com.achep.base.ui.adapters.PermissionAdapter;
import com.achep.base.utils.ToastUtils;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog fragment that helps user to give all needed permissions
 * to the app.
 *
 * @author Artem Chepurnoy
 */
public class PermissionsDialog extends DialogFragment {

    private static final String KEY_PERMISSIONS = "permissions";

    private PermissionAdapter mAdapter;
    private Permission[] mPermissions;

    public static PermissionsDialog newInstance(@NonNull Permission[] permissions) {
        String[] p = new String[permissions.length];
        for (int i = 0; i < p.length; i++) {
            p[i] = permissions[i].getClass().getSimpleName();
        }

        Bundle bundle = new Bundle();
        bundle.putStringArray(KEY_PERMISSIONS, p);

        PermissionsDialog fragment = new PermissionsDialog();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get permissions array
        Bundle args = getArguments();
        assert args != null;
        String[] p = args.getStringArray(KEY_PERMISSIONS);
        mPermissions = new Permission[p == null ? 0 : p.length];
        for (int i = 0; i < p.length; i++) {
            final String name = p[i];
            mPermissions[i] = Permission.newInstance(getActivity(), name);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        assert context != null;

        MaterialDialog md = new MaterialDialog.Builder(context)
                .title(R.string.permissions_dialog_title)
                .items(new CharSequence[]{"", ""})
                .negativeText(R.string.later)
                .build();

        // Make title more red
        TextView title = md.getTitleView();
        title.setTextColor(title.getCurrentTextColor() & 0xFFFF3333 | 0xFF << 16);

        ListView listView = md.getListView();
        assert listView != null;
        mAdapter = new PermissionAdapter(context, new ArrayList<Permission>());
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PermissionAdapter adapter = (PermissionAdapter) parent.getAdapter();
                Permission item = adapter.getItem(position);

                try {
                    startActivity(item.getIntentSettings());
                } catch (ActivityNotFoundException e) {
                    int msg = item.getErrorResource();
                    if (msg != 0) ToastUtils.showLong(getActivity(), msg);
                }
            }
        });

        return md;
    }

    @Override
    public void onResume() {
        super.onResume();

        List<Permission> data = mAdapter.getPermissionList();
        data.clear();

        for (Permission item : mPermissions) {
            if (!item.isGranted()) {
                data.add(item);
            }
        }

        // Dismiss permission dialog if there's no work for it.
        if (data.isEmpty()) {
            dismiss();
        }

        mAdapter.notifyDataSetChanged();
    }

}
