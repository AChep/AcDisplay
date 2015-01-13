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
package com.achep.acdisplay.ui.fragments.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
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
import com.achep.acdisplay.R;
import com.achep.acdisplay.receiver.AdminReceiver;
import com.achep.acdisplay.utils.AccessUtils;
import com.achep.base.Device;
import com.achep.base.ui.DialogBuilder;
import com.achep.base.utils.ToastUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Dialog fragment that shows some info about this application.
 *
 * @author Artem Chepurnoy
 */
public class SetupPermissionsDialog extends DialogFragment {

    private static final String TAG = "AccessDialog";

    private ListView mListView;
    private Adapter mAdapter;
    private Item[] mItems;

    private abstract static class Item {

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

        public abstract boolean isAllowed();

    }

    private static class DeviceAdminItem extends Item {

        private Context mContext;

        public DeviceAdminItem(@DrawableRes int icon,
                               String title, String summary,
                               @NonNull Runnable runnable,
                               Context context) {
            super(icon, title, summary, runnable);
            mContext = context;
        }

        @Override
        public boolean isAllowed() {
            return AccessUtils.isDeviceAdminAccessGranted(mContext);
        }

    }

    private static class NotificationListenerItem extends Item {

        private Context mContext;

        public NotificationListenerItem(@DrawableRes int icon,
                                        String title, String summary,
                                        @NonNull Runnable runnable,
                                        Context context) {
            super(icon, title, summary, runnable);
            mContext = context;
        }

        @Override
        public boolean isAllowed() {
            return AccessUtils.isNotificationAccessGranted(mContext);
        }

    }

    private Item[] buildItems() {
        Context context = getActivity();
        ArrayList<Item> items = new ArrayList<>();
        items.add(new DeviceAdminItem(R.drawable.stat_lock,
                getString(R.string.permissions_device_admin),
                getString(R.string.permissions_device_admin_description),
                new Runnable() {

                    @Override
                    public void run() {
                        Context context = getActivity();
                        ComponentName admin = new ComponentName(context, AdminReceiver.class);
                        Intent intent = new Intent()
                                .setAction(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);

                        try {
                            startActivity(intent);

                            // TODO: Fix 'Device admin request intent acts strange on Lollipop'
                            if (Device.hasLollipopApi()) {
                                // Yes, they broke the intent somehow.
                                String message = getString(
                                        R.string.permissions_device_admin_troubleshooting,
                                        getString(R.string.permissions_device_admin_grant_manually));
                                ToastUtils.showLong(context, message);
                            }
                        } catch (ActivityNotFoundException e) {
                            ToastUtils.showLong(context, R.string.permissions_device_admin_grant_manually);
                            Log.e(TAG, "Device admins activity not found.");
                        }
                    }

                }, context));
        items.add(new NotificationListenerItem(R.drawable.stat_notify,
                getString(R.string.permissions_notifications),
                getString(R.string.permissions_notifications_description),
                new Runnable() {

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
                            ToastUtils.showLong(getActivity(), R.string.permissions_notifications_grant_manually);
                            Log.e(TAG, "Notification listeners activity not found.");
                        }
                    }

                    private void launchAccessibilitySettings() {
                        Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                        try {
                            startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            String message = "Accessibility settings not found!";
                            ToastUtils.showLong(getActivity(), message);
                            Log.wtf(TAG, message);
                        }
                    }

                }, context));

        return items.toArray(new Item[items.size()]);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        assert context != null;

        View view = new DialogBuilder(context)
                .setTitle(R.string.permissions_dialog_title)
                .setView(R.layout.fragment_access)
                .createSkeletonView();

        // Make title more red
        TextView title = (TextView) view.findViewById(R.id.title);
        title.setTextColor(title.getCurrentTextColor() & 0xFFFF2020 | 0xFF << 16);

        mListView = (ListView) view.findViewById(R.id.list);
        mAdapter = new Adapter(context, new ArrayList<Item>());
        mListView.setAdapter(mAdapter);
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mItems = buildItems();
    }

    @Override
    public void onResume() {
        super.onResume();

        List<Item> data = mAdapter.mDataset;
        data.clear();
        for (Item item : mItems) {
            if (!item.isAllowed()) {
                data.add(item);
            }
        }
        if (data.size() == 0) {
            Config.getInstance().setEnabled(getActivity(), true, null);
            dismiss();
        }
        mAdapter.notifyDataSetChanged();
    }

    public static class Adapter extends ArrayAdapter<Item> {

        private final Context mContext;
        private final List<Item> mDataset;
        private final LayoutInflater mInflater;
        private final HashMap<Integer, WeakReference<Drawable>> mDrawableCache;

        static class ViewHolder {

            ImageView icon;
            TextView title;
            TextView summary;

            public ViewHolder(View itemView) {
                this.icon = (ImageView) itemView.findViewById(R.id.icon);
                this.title = (TextView) itemView.findViewById(R.id.title);
                this.summary = (TextView) itemView.findViewById(R.id.summary);
            }

        }

        public Adapter(Context context, List<Item> items) {
            super(context, 0);
            mContext = context;
            mDataset = items;
            mInflater = LayoutInflater.from(getContext());
            mDrawableCache = new HashMap<>(Math.min(getCount(), 10));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Item item = getItem(position);
            final ViewHolder holder;
            final View view;

            if (convertView == null) {
                view = mInflater.inflate(R.layout.item_blah, parent, false);
                assert view != null;
                holder = new ViewHolder(view);

                int padding = mContext.getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
                view.setPadding(padding, view.getPaddingTop(), padding, view.getPaddingBottom());

                view.setTag(holder);
            } else {
                view = convertView;
                holder = (ViewHolder) view.getTag();
            }

            holder.title.setText(item.title);
            holder.summary.setText(item.summary);

            // Cache drawables for smoother scrolling.
            Drawable drawable;
            WeakReference<Drawable> drawableLink = mDrawableCache.get(item.icon);
            if (drawableLink == null || (drawable = drawableLink.get()) == null) {
                holder.icon.setImageResource(item.icon);
                mDrawableCache.put(item.icon, new WeakReference<>(holder.icon.getDrawable()));
            } else {
                holder.icon.setImageDrawable(drawable);
            }

            return view;
        }

        @Override
        public int getCount() {
            return mDataset.size();
        }

        @Override
        public Item getItem(int position) {
            return mDataset.get(position);
        }

    }

}
