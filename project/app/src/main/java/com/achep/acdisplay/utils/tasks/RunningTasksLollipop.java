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
package com.achep.acdisplay.utils.tasks;

import android.annotation.TargetApi;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Artem Chepurnoy
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class RunningTasksLollipop extends RunningTasks {

    private static final String TAG = "RunningTasksLollipop";

    private static final String USAGE_STATS_MANAGER = "usagestats";

    /**
     * {@inheritDoc}
     */
    @Nullable
    public String getRunningTasksTop(@NonNull Context context) {
        try {
            UsageStats usageStats = getUsageStatsTop(context);
            return usageStats != null ? usageStats.getPackageName() : null;
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to get usage stats! Permissions denied!");
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("ResourceType")
    @Nullable
    private SortedMap<Long, UsageStats> getUsageStats(@NonNull Context context)
            throws SecurityException {
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(USAGE_STATS_MANAGER);

        // We get usage stats for the last 30 seconds
        final long timeEnd = System.currentTimeMillis();
        final long timeBegin = timeEnd - 30 * 1000; // +30 sec.
        List<UsageStats> statsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
                timeBegin,
                timeEnd);

        if (statsList != null) {
            // Sort the stats by the last time used
            SortedMap<Long, UsageStats> statsSortedMap = new TreeMap<>();
            for (final UsageStats usageStats : statsList) {
                // Filter system decor apps
                if ("com.android.systemui".equals(usageStats.getPackageName())) {
                    continue;
                }

                statsSortedMap.put(usageStats.getLastTimeUsed(), usageStats);
            }
            return statsSortedMap;
        }
        return null;
    }

    @Nullable
    private UsageStats getUsageStatsTop(@NonNull Context context) throws SecurityException {
        SortedMap<Long, UsageStats> statsSortedMap = getUsageStats(context);
        return statsSortedMap != null && !statsSortedMap.isEmpty()
                ? statsSortedMap.get(statsSortedMap.lastKey())
                : null;
    }

}
