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
package com.achep.acdisplay.utils;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.support.annotation.NonNull;

import com.achep.acdisplay.receiver.AdminReceiver;
import com.achep.acdisplay.services.AccessibilityService;
import com.achep.acdisplay.services.MediaService;
import com.achep.acdisplay.utils.tasks.RunningTasks;
import com.achep.base.Device;

/**
 * Created by Artem on 23.01.14.
 */
public class AccessUtils {

    public static boolean hasAllRights(Context context) {
        return isDeviceAdminAccessGranted(context)
                && isNotificationAccessGranted(context)
                && hasUsageStatsAccess(context);
    }

    public static boolean isDeviceAdminAccessGranted(@NonNull Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm.isAdminActive(new ComponentName(context, AdminReceiver.class));
    }

    public static boolean isNotificationAccessGranted(Context context) {
        return Device.hasJellyBeanMR2Api()
                ? MediaService.sService != null
                : AccessibilityService.isRunning;//.isServiceRunning(context);
    }

    public static boolean hasUsageStatsAccess(@NonNull Context context) {
        return Device.hasLollipopApi() && RunningTasks.newInstance().getRunningTasksTop(context) != null;
    }

}
