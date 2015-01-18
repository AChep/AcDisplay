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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.acdisplay.receiver.AdminReceiver;
import com.achep.acdisplay.utils.AccessUtils;
import com.achep.base.Device;
import com.achep.base.ui.DialogBuilder;
import com.achep.base.ui.adapters.BetterArrayAdapter;
import com.achep.base.utils.ToastUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Dialog fragment that helps user to give all needed permissions
 * to the app.
 *
 * @author Artem Chepurnoy
 */
public class SetupPermissionsDialog extends DialogFragment {

    private static final String TAG = "AccessDialog";

    private Adapter mAdapter;
    private Item[] mItems;

    private abstract static class Item implements Runnable {

        protected static void startActivityWithErrorMessage(
                @NonNull Context context, @NonNull Intent intent,
                @NonNull String errorMessageLog,
                @Nullable String errorMessageToast) {
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, errorMessageLog);
                if (errorMessageToast != null) ToastUtils.showLong(context, errorMessageToast);
            }
        }

        public final int icon;
        public final String title;
        public final String summary;

        protected final Context mContext;

        public Item(@NonNull Context context, @DrawableRes int icon,
                    @StringRes int titleRes, @StringRes int summaryRes) {
            mContext = context;
            this.icon = icon;
            this.title = context.getString(titleRes);
            this.summary = context.getString(summaryRes);
        }

        public abstract boolean isDone();

    }

    /**
     * Device admin.
     *
     * @author Artem Chepurnoy
     * @see com.achep.acdisplay.utils.AccessUtils#hasDeviceAdminAccess(android.content.Context)
     */
    private static class DeviceAdminItem extends Item {

        public DeviceAdminItem(@NonNull Context context, @DrawableRes int icon,
                               @StringRes int titleRes, @StringRes int summaryRes) {
            super(context, icon, titleRes, summaryRes);
        }

        @Override
        public void run() {
            ComponentName admin = new ComponentName(mContext, AdminReceiver.class);
            Intent intent = new Intent()
                    .setAction(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
            String message = "Device admins activity not found.";
            String messageToast = mContext.getString(R.string.permissions_device_admin_grant_manually);
            startActivityWithErrorMessage(mContext, intent, message, messageToast);
        }

        @Override
        public boolean isDone() {
            return AccessUtils.hasDeviceAdminAccess(mContext);
        }

    }

    /**
     * Notification listener service if {@link com.achep.base.Device#hasJellyBeanMR2Api() Android 4.3}
     * and accessibility service otherwise.
     *
     * @author Artem Chepurnoy
     * @see com.achep.acdisplay.utils.AccessUtils#hasNotificationAccess(android.content.Context)
     */
    private static class NotificationListenerItem extends Item {

        public NotificationListenerItem(@NonNull Context context, @DrawableRes int icon,
                                        @StringRes int titleRes, @StringRes int summaryRes) {
            super(context, icon, titleRes, summaryRes);
        }

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
            String message = "Notification listeners activity not found.";
            String messageToast = mContext.getString(R.string.permissions_notifications_grant_manually);
            startActivityWithErrorMessage(mContext, intent, message, messageToast);
        }

        private void launchAccessibilitySettings() {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            String message = "Accessibility settings not found!";
            startActivityWithErrorMessage(mContext, intent, message, null);
        }

        @Override
        public boolean isDone() {
            return AccessUtils.hasNotificationAccess(mContext);
        }

    }

    /**
     * Usage stats
     *
     * @author Artem Chepurnoy
     * @see com.achep.acdisplay.utils.AccessUtils#hasUsageStatsAccess(android.content.Context)
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static class UsageStatsItem extends Item {

        public UsageStatsItem(@NonNull Context context, @DrawableRes int icon,
                              @StringRes int titleRes, @StringRes int summaryRes) {
            super(context, icon, titleRes, summaryRes);
        }

        @Override
        public void run() {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivityWithErrorMessage(mContext, intent, "Usage access settings not found.", null);
        }

        @Override
        public boolean isDone() {
            return AccessUtils.hasUsageStatsAccess(mContext);
        }

    }

    @NonNull
    private Item[] buildItems() {
        final Context context = getActivity();
        final ArrayList<Item> items = new ArrayList<>();
        items.add(new DeviceAdminItem(context, R.drawable.stat_lock,
                R.string.permissions_device_admin, R.string.permissions_device_admin_description));
        items.add(new NotificationListenerItem(context, R.drawable.stat_notify,
                R.string.permissions_notifications, R.string.permissions_notifications_description));
        items.add(new UsageStatsItem(context, R.drawable.ic_settings_apps_white,
                R.string.permissions_usage_stats, R.string.permissions_usage_stats_description));
        return items.toArray(new Item[items.size()]);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        assert context != null;

        View view = new DialogBuilder(context)
                .setTitle(R.string.permissions_dialog_title)
                .setView(R.layout.dialog_permissions)
                .createSkeletonView();

        // Make title more red
        TextView title = (TextView) view.findViewById(R.id.title);
        title.setTextColor(title.getCurrentTextColor() & 0xFFFF3333 | 0xFF << 16);

        ListView listView = (ListView) view.findViewById(R.id.list);
        mAdapter = new Adapter(context, new ArrayList<Item>());
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Adapter adapter = (Adapter) parent.getAdapter();
                Item item = adapter.getItem(position);
                item.run();
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
            if (!item.isDone()) {
                data.add(item);
            }
        }
        if (data.size() == 0) {
            Config.getInstance().setEnabled(getActivity(), true, null);
            dismiss();
        }
        mAdapter.notifyDataSetChanged();
    }

    public static class Adapter extends BetterArrayAdapter<Item> {

        private final List<Item> mDataset;
        private final HashMap<Integer, WeakReference<Drawable>> mDrawableCache;

        private static class ViewHolder extends BetterArrayAdapter.ViewHolder {

            final ImageView icon;
            final TextView title;
            final TextView summary;

            public ViewHolder(@NonNull View view) {
                super(view);
                this.icon = (ImageView) view.findViewById(R.id.icon);
                this.title = (TextView) view.findViewById(R.id.title);
                this.summary = (TextView) view.findViewById(R.id.summary);
            }

        }

        public Adapter(Context context, List<Item> items) {
            super(context, R.layout.item_blah);
            mDataset = items;
            mDrawableCache = new HashMap<>(Math.min(getCount(), 10));
        }

        @NonNull
        @Override
        protected BetterArrayAdapter.ViewHolder onCreateViewHolder(@NonNull View view) {
            int padding = mContext.getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
            view.setPadding(padding, view.getPaddingTop(), padding, view.getPaddingBottom());
            return new ViewHolder(view);
        }

        @Override
        protected void onBindViewHolder(@NonNull BetterArrayAdapter.ViewHolder viewHolder, int i) {
            final Item item = getItem(i);
            final ViewHolder holder = (ViewHolder) viewHolder;

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
