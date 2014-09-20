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
package com.achep.acdisplay.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.Device;
import com.achep.acdisplay.DialogHelper;
import com.achep.acdisplay.R;
import com.achep.acdisplay.admin.AdminReceiver;
import com.achep.acdisplay.utils.AccessUtils;
import com.achep.acdisplay.utils.ToastUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Dialog fragment that shows some info about this application.
 *
 * @author Artem Chepurnoy
 */
public class SetupPermissionsDialog extends DialogFragment {

    private static final String TAG = "AccessDialog";

    private ListView mListView;

    private static class Item {
        public int icon;
        public String title;
        public String summary;
        public Runnable runnable;

        public Item(@DrawableRes int icon,
                    String title, String summary,
                    @NonNull Runnable runnable) {
            this.icon = icon;
            this.title = title;
            this.summary = summary;
            this.runnable = runnable;
        }
    }

    private Item[] buildItems() {
        Context context = getActivity();
        ArrayList<Item> items = new ArrayList<>();

        if (!AccessUtils.isDeviceAdminAccessGranted(context)) {
            items.add(new Item(R.drawable.stat_lock,
                    getString(R.string.access_device_admin),
                    getString(R.string.access_device_admin_description), new Runnable() {
                @Override
                public void run() {
                    Context context = getActivity();
                    ComponentName admin = new ComponentName(context, AdminReceiver.class);
                    Intent intent = new Intent()
                            .setAction(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);

                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        ToastUtils.showLong(context, R.string.access_device_admin_grant_manually);
                        Log.e(TAG, "Device admins activity not found.");
                    }
                }
            }));
        }

        if (!AccessUtils.isNotificationAccessGranted(context)) {
            items.add(new Item(R.drawable.stat_notify,
                    getString(R.string.access_notifications),
                    getString(R.string.access_notifications_description), new Runnable() {
                @Override
                public void run() {
                    if (Device.hasJellyBeanMR2Api()) {
                        launchNotificationSettings();
                    } else {
                        launchAccessibilitySettings();
                    }
                }

                private void launchNotificationSettings() {
                    Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        ToastUtils.showLong(getActivity(), R.string.access_notifications_grant_manually);
                        Log.e(TAG, "Notification listeners activity not found.");
                    }
                }

                private void launchAccessibilitySettings() {
                    Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Log.wtf(TAG, "Accessibility settings not found!");
                    }
                }
            }));
        }

        return items.toArray(new Item[items.size()]);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        assert context != null;

        View view = new DialogHelper.Builder(context)
                .setTitle(R.string.access_title)
                .setView(R.layout.fragment_access)
                .createSkeletonView();

        // Make title more red
        TextView title = (TextView) view.findViewById(R.id.title);
        title.setTextColor(title.getCurrentTextColor() & 0xFFFF2020 | 0xFF << 16);

        mListView = (ListView) view.findViewById(R.id.list);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Adapter adapter = (Adapter) parent.getAdapter();
                Item item = adapter.getItem(position);
                item.runnable.run();
            }
        });

        return new AlertDialog.Builder(context)
                .setView(view)
                .setNeutralButton(R.string.later, null)
                .create();
    }

    @Override
    public void onResume() {
        super.onResume();

        Item[] items = buildItems();
        if (items.length == 0) {
            Config.getInstance().setEnabled(getActivity(), true, null);
            dismiss();
        } else {
            Adapter adapter = new Adapter(getActivity(), items);
            mListView.setAdapter(adapter);
        }
    }

    public static class Adapter extends ArrayAdapter<Item> {

        private final Context mContext;
        private final LayoutInflater mInflater;
        private final HashMap<Integer, WeakReference<Drawable>> mCache;

        public Adapter(Context context, Item[] items) {
            super(context, 0, items);
            mContext = context;
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mCache = new HashMap<>();
        }

        private static class Holder {
            ImageView icon;
            TextView title;
            TextView summary;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Item item = getItem(position);
            final Holder holder;
            final View view;

            if (convertView == null) {
                holder = new Holder();
                view = mInflater.inflate(R.layout.preference_header_item, parent, false);
                assert view != null;

                int padding = mContext.getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
                view.setPadding(padding, view.getPaddingTop(), padding, view.getPaddingBottom());

                holder.icon = (ImageView) view.findViewById(R.id.icon);
                holder.title = (TextView) view.findViewById(android.R.id.title);
                holder.summary = (TextView) view.findViewById(android.R.id.summary);

                view.setTag(holder);
            } else {
                view = convertView;
                holder = (Holder) view.getTag();
            }

            holder.title.setText(item.title);
            holder.summary.setText(item.summary);

            // Cache drawables for smoother scrolling.
            Drawable drawable;
            WeakReference<Drawable> drawableLink = mCache.get(item.icon);
            if (drawableLink == null || (drawable = drawableLink.get()) == null) {
                holder.icon.setImageResource(item.icon);
                mCache.put(item.icon, new WeakReference<>(holder.icon.getDrawable()));
            } else {
                holder.icon.setImageDrawable(drawable);
            }

            return view;
        }
    }
}
