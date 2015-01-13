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
package com.achep.acdisplay.services.activemode.handlers;

import android.content.Context;
import android.support.annotation.NonNull;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.notifications.NotificationPresenter;
import com.achep.acdisplay.notifications.OpenNotification;
import com.achep.acdisplay.services.activemode.ActiveModeHandler;
import com.achep.base.content.ConfigBase;

/**
 * Prevents {@link com.achep.acdisplay.services.activemode.ActiveModeService} from listening to
 * sensors while notification list is empty (if corresponding option is enabled.)
 *
 * @author Artem Chepurnoy
 * @see com.achep.acdisplay.ui.fragments.settings.ActiveModeSettings
 */
public final class WithoutNotifiesHandler extends ActiveModeHandler implements
        NotificationPresenter.OnNotificationListChangedListener,
        ConfigBase.OnConfigChangedListener {

    private Config mConfig;
    private NotificationPresenter mNotificationPresenter;

    public WithoutNotifiesHandler(@NonNull Context context, @NonNull Callback callback) {
        super(context, callback);
    }

    @Override
    public void onCreate() {
        mConfig = Config.getInstance();
        mConfig.registerListener(this);

        mNotificationPresenter = NotificationPresenter.getInstance();
        mNotificationPresenter.registerListener(this);
    }

    @Override
    public void onDestroy() {
        mConfig.unregisterListener(this);
        mNotificationPresenter.unregisterListener(this);
    }

    @Override
    public boolean isActive() {
        boolean enabled = mConfig.isActiveModeWithoutNotifiesEnabled();
        return enabled || mNotificationPresenter.size() > 0;
    }

    @Override
    public void onConfigChanged(@NonNull ConfigBase configBase,
                                @NonNull String key,
                                @NonNull Object value) {
        switch (key) {
            case Config.KEY_ACTIVE_MODE_WITHOUT_NOTIFICATIONS:
                if ((boolean) value) {
                    requestActive();
                } else {
                    // If you've disabled the active mode, check the
                    // amount of notifications and probably stop
                    // listening.
                    requestActive();
                }
                break;
        }
    }

    @Override
    public void onNotificationListChanged(@NonNull NotificationPresenter np,
                                          OpenNotification osbn,
                                          int event, boolean f) {
        switch (event) {
            case NotificationPresenter.EVENT_BATH:
            case NotificationPresenter.EVENT_POSTED:
            case NotificationPresenter.EVENT_REMOVED:
                requestActive();
                break;
        }
    }
}
