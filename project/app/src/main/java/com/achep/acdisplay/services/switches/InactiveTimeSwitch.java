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
package com.achep.acdisplay.services.switches;

import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.InactiveTimeHelper;
import com.achep.acdisplay.services.Switch;
import com.achep.base.content.ConfigBase;

import java.util.Timer;
import java.util.TimerTask;

import static com.achep.base.Build.DEBUG;

/**
 * Prevents {@link com.achep.acdisplay.services.activemode.ActiveModeService} from listening to
 * sensors on inactive time (if corresponding option is enabled.)
 *
 * @author Artem Chepurnoy
 * @see com.achep.acdisplay.ui.fragments.settings.MoreSettings
 */
// TODO: Implement event based inactive time handling.
public final class InactiveTimeSwitch extends Switch implements
        ConfigBase.OnConfigChangedListener {

    private static final int INACTIVE_HOURS_CHECK_PERIOD = 1000 * 60 * 5; // 5 min.

    private final Config mConfig;
    private final ConfigBase.Option mOption;
    private Timer mTimer;

    public InactiveTimeSwitch(@NonNull Context context, @NonNull Callback callback,
                              @NonNull ConfigBase.Option option) {
        super(context, callback);
        mConfig = Config.getInstance();
        mOption = option;
    }

    @Override
    public void onCreate() {
        mConfig.registerListener(this);
        updateState();
    }

    @Override
    public void onDestroy() {
        mConfig.unregisterListener(this);

        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    @Override
    public boolean isActive() {
        return !isEnabled() || !InactiveTimeHelper.isInactiveTime(mConfig);
    }

    private void updateState() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (isEnabled()) {

            // Start a timer to monitor when inactive time
            // will end or start. This is awful.
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {

                private static final String TAG = "InactiveTimeTicker";

                private boolean firstTick = true;
                private boolean inactivePrev = false;

                @Override
                public void run() {
                    boolean inactive = InactiveTimeHelper.isInactiveTime(mConfig);
                    boolean changed = inactive != inactivePrev || firstTick;

                    firstTick = false;

                    if (DEBUG)
                        Log.d(TAG, "On timer tick: elapsed_real_time="
                                + SystemClock.elapsedRealtime());

                    if (changed) {
                        inactivePrev = inactive;

                        if (DEBUG)
                            Log.d(TAG, "is_inactive_time=" + inactive);

                        if (inactive) {
                            requestInactive();
                        } else {
                            requestActive();
                        }
                    }
                }
            }, 0, INACTIVE_HOURS_CHECK_PERIOD);
        } else {
            requestActive();
        }
    }

    @Override
    public void onConfigChanged(@NonNull ConfigBase configBase,
                                @NonNull String key,
                                @NonNull Object value) {
        if (mOption.getKey(mConfig).equals(key)) {
            updateState();
            return;
        }

        switch (key) {
            case Config.KEY_INACTIVE_TIME_FROM:
            case Config.KEY_INACTIVE_TIME_TO:
                if (!isEnabled()) {
                    break;
                }

                // Immediately update sensors' blocker.
            case Config.KEY_INACTIVE_TIME_ENABLED:
                updateState();
                break;
        }
    }

    /**
     * @return {@code true} if the inactive time is enabled, {@code false} otherwise.
     */
    private boolean isEnabled() {
        return mConfig.isInactiveTimeEnabled() && (boolean) mOption.read(mConfig);
    }

}
