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

import com.achep.activedisplay.Config;
import com.achep.activedisplay.activemode.ActiveModeHandler;
import com.achep.activedisplay.notifications.NotificationPresenter;
import com.achep.activedisplay.notifications.OpenStatusBarNotification;

/**
 * Prevents {@link com.achep.activedisplay.activemode.ActiveModeService} from listening to
 * sensors while notification list is empty (if corresponding option is enabled.)
 *
 * @author Artem Chepurnoy
 * @see com.achep.activedisplay.settings.ActiveFragment
 */
public final class WithoutNotifiesHandler extends ActiveModeHandler implements
        NotificationPresenter.OnNotificationListChangedListener,
        Config.OnConfigChangedListener {

    private Config mConfig;
    private NotificationPresenter mNotificationPresenter;

    public WithoutNotifiesHandler(Context context, Callback callback) {
        super(context, callback);
    }

    @Override
    public void onCreate() {
        mConfig = Config.getInstance();
        mConfig.addOnConfigChangedListener(this);

        mNotificationPresenter = NotificationPresenter.getInstance(getContext());
        mNotificationPresenter.addOnNotificationListChangedListener(this);
        updateState();
    }

    @Override
    public void onDestroy() {
        mConfig.removeOnConfigChangedListener(this);
        mNotificationPresenter.removeOnNotificationListChangedListener(this);
    }

    @Override
    public boolean isActive() {
        boolean enabled = mConfig.isActiveModeWithoutNotifiesEnabled();
        return enabled || mNotificationPresenter.getList().size() > 0;
    }

    private void updateState() {
        if (isActive()) {
            requestActive();
        } else {
            requestInactive();
        }
    }

    @Override
    public void onConfigChanged(Config config, String key, Object value) {
        switch (key) {
            case Config.KEY_ACTIVE_MODE_WITHOUT_NOTIFICATIONS:
                if ((boolean) value) {
                    requestActive();
                } else {
                    // If you've disabled the active mode, check the
                    // amount of notifications and probably stop
                    // listening.
                    updateState();
                }
                break;
        }
    }

    @Override
    public void onNotificationListChanged(NotificationPresenter np,
                                          OpenStatusBarNotification osbn,
                                          int event) {
        switch (event) {
            case NotificationPresenter.EVENT_BATH:
            case NotificationPresenter.EVENT_POSTED:
            case NotificationPresenter.EVENT_REMOVED:
                updateState();
                break;
        }
    }
}
