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

package com.achep.activedisplay.activemode.handlers;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.achep.activedisplay.Config;
import com.achep.activedisplay.InactiveHoursHelper;
import com.achep.activedisplay.Project;
import com.achep.activedisplay.activemode.ActiveModeHandler;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Prevents {@link com.achep.activedisplay.activemode.ActiveModeService} from listening to
 * sensors on inactive time (if corresponding option is enabled.)
 *
 * @author Artem Chepurnoy
 * @see com.achep.activedisplay.settings.MoreFragment
 */
// TODO: Implement event based inactive time handling.
public final class InactiveTimeHandler extends ActiveModeHandler implements
        Config.OnConfigChangedListener {

    private static final int INACTIVE_HOURS_CHECK_PERIOD = 1000 * 60 * 5; // ms.

    private Config mConfig;
    private Timer mTimer;

    public InactiveTimeHandler(Context context, Callback callback) {
        super(context, callback);
    }

    @Override
    public void onCreate() {
        mConfig = Config.getInstance();
        mConfig.addOnConfigChangedListener(this);
        updateState();
    }

    @Override
    public void onDestroy() {
        mConfig.removeOnConfigChangedListener(this);
        
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    @Override
    public boolean isActive() {
        boolean enabled = mConfig.isInactiveTimeEnabled();
        return !enabled || !InactiveHoursHelper.isInactiveTime(mConfig);
    }

    private void updateState() {
        if (mTimer != null) mTimer.cancel();
        if (mConfig.isInactiveTimeEnabled()) {

            // Start a timer to monitor when inactive time
            // will end or start. This is awful.
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {

                private static final String TAG = "InactiveTimeTicker";

                private boolean firstTick = true;
                private boolean inactivePrev = false;

                @Override
                public void run() {
                    boolean inactive = InactiveHoursHelper.isInactiveTime(mConfig);
                    boolean changed = inactive != inactivePrev || firstTick;

                    firstTick = false;

                    if (Project.DEBUG)
                        Log.d(TAG, "On timer tick: elapsed_real_time="
                                + SystemClock.elapsedRealtime());

                    if (changed) {
                        inactivePrev = inactive;

                        if (Project.DEBUG)
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
    public void onConfigChanged(Config config, String key, Object value) {
        boolean enabled = config.isInactiveTimeEnabled();
        switch (key) {
            case Config.KEY_INACTIVE_TIME_FROM:
            case Config.KEY_INACTIVE_TIME_TO:
                if (!enabled) {
                    break;
                }

                // Immediately update sensors' blocker.
            case Config.KEY_INACTIVE_TIME_ENABLED:
                updateState();
                break;
        }
    }
}
