/*
 * Copyright (C) 2013-2014 AChep@xda <artemchep@gmail.com>
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
package com.achep.activedisplay.notifications;

import android.content.Context;

import com.achep.activedisplay.Project;
import com.achep.activedisplay.blacklist.SharedList;
import com.achep.activedisplay.utils.LogUtils;

import java.lang.ref.SoftReference;

/**
 * Created by Artem on 09.02.14.
 */
public class Blacklist extends SharedList<String, SharedList.StringSaver> {

    private static SoftReference<Blacklist> sBlacklistSoft;

    public static synchronized Blacklist getInstance(Context context) {
        Blacklist instance;
        if (sBlacklistSoft == null || (instance = sBlacklistSoft.get()) == null) {
            if (Project.DEBUG) LogUtils.track();

            instance = new Blacklist(context);
            sBlacklistSoft = new SoftReference<>(instance);
            return instance;
        }
        return instance;
    }

    private Blacklist(Context context) {
        super(context, StringSaver.class);
    }

    @Override
    protected String getPreferencesFileName() {
        return "blacklist_package";
    }

    public synchronized void put(Context context, OpenStatusBarNotification notification) {
        put(context, getPackageName(notification));
    }

    public synchronized void remove(Context context, OpenStatusBarNotification notification) {
        remove(context, getPackageName(notification));
    }

    public synchronized boolean contains(OpenStatusBarNotification notification) {
        return contains(getPackageName(notification));
    }

    private String getPackageName(OpenStatusBarNotification notification) {
        return notification.getStatusBarNotification().getPackageName();
    }

}
