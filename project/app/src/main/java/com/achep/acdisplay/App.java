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
package com.achep.acdisplay;

import android.app.Application;
import android.support.annotation.NonNull;

import com.achep.acdisplay.blacklist.Blacklist;
import com.achep.acdisplay.notifications.NotificationHelper;
import com.achep.acdisplay.notifications.NotificationPresenter;
import com.achep.acdisplay.permissions.AccessManager;
import com.achep.acdisplay.services.KeyguardService;
import com.achep.acdisplay.services.SensorsDumpService;
import com.achep.acdisplay.services.activemode.ActiveModeService;
import com.achep.base.AppHeap;
import com.achep.base.content.ConfigBase;
import com.achep.base.permissions.Permission;
import com.achep.base.permissions.PermissionGroup;
import com.achep.base.utils.smiley.SmileyParser;

/**
 * Created by Artem on 22.02.14.
 */
public class App extends Application {

    private static final String TAG = "App";

    public static final int ACCENT_COLOR = 0xFF607D8B;

    public static final int ID_NOTIFY_INIT = 30;
    public static final int ID_NOTIFY_TEST = 40;
    public static final int ID_NOTIFY_BATH = 50;
    public static final int ID_NOTIFY_APP_AUTO_DISABLED = 60;

    public static final String ACTION_BIND_MEDIA_CONTROL_SERVICE = "com.achep.acdisplay.BIND_MEDIA_CONTROL_SERVICE";

    public static final String ACTION_ENABLE = "com.achep.acdisplay.ENABLE";
    public static final String ACTION_DISABLE = "com.achep.acdisplay.DISABLE";
    public static final String ACTION_TOGGLE = "com.achep.acdisplay.TOGGLE";

    public static final String ACTION_STATE_START = "com.achep.acdisplay.STATE_START";
    public static final String ACTION_STATE_RESUME = "com.achep.acdisplay.STATE_RESUME";
    public static final String ACTION_STATE_PAUSE = "com.achep.acdisplay.STATE_PAUSE";
    public static final String ACTION_STATE_STOP = "com.achep.acdisplay.STATE_STOP";

    public static final String ACTION_EAT_HOME_PRESS_START = "com.achep.acdisplay.EAT_HOME_PRESS_START";
    public static final String ACTION_EAT_HOME_PRESS_STOP = "com.achep.acdisplay.EAT_HOME_PRESS_STOP";

    public static final String ACTION_INTERNAL_TIMEOUT = "TIMEOUT";
    public static final String ACTION_INTERNAL_PING_SENSORS = "PING_SENSORS";

    private AccessManager mAccessManager;

    @NonNull
    private static App instance;

    public App() {
        instance = this;
    }

    @Override
    public void onCreate() {
        mAccessManager = new AccessManager(this);

        AppHeap.getInstance().init(this);
        Config.getInstance().init(this);
        Blacklist.getInstance().init(this);
        SmileyParser.init(this);

        // Init the main notification listener.
        NotificationPresenter.getInstance().setOnNotificationPostedListener(
                Config.getInstance().isEnabled()
                        ? Presenter.getInstance()
                        : null);

        super.onCreate();

        // Check the main switch.
        String divider = getString(R.string.settings_multi_list_divider);
        Config config = Config.getInstance();
        if (config.isEnabled()) {
            StringBuilder sb = new StringBuilder();
            boolean foundAny = false;

            PermissionGroup pg = getAccessManager().getMasterPermissions();
            for (Permission permission : pg.permissions) {
                if (!permission.isActive()) {
                    if (foundAny) {
                        sb.append(divider);
                    } else foundAny = true;
                    sb.append(getString(permission.getTitleResource()));
                }
            }

            if (foundAny) {
                String list = sb.toString();
                list = list.charAt(0) + list.substring(1).toLowerCase();

                ConfigBase.Option option = config.getOption(Config.KEY_ENABLED);
                option.write(config, this, false, null);

                NotificationHelper.sendNotification(this, App.ID_NOTIFY_APP_AUTO_DISABLED, list);
            }
        }

        // Launch keyguard and (or) active mode on
        // app launch.
        KeyguardService.handleState(this);
        ActiveModeService.handleState(this);
        SensorsDumpService.handleState(this);
    }

    @Override
    public void onLowMemory() {
        Config.getInstance().onLowMemory();
        Blacklist.getInstance().onLowMemory();
        NotificationPresenter.getInstance().onLowMemory();
        mAccessManager.onLowMemory();
        super.onLowMemory();
    }

    @NonNull
    public static AccessManager getAccessManager() {
        assert instance.mAccessManager != null;
        return instance.mAccessManager;
    }

}
