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
package com.achep.acdisplay.plugins.xposed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.achep.acdisplay.App;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public class OverrideHomeButton implements IXposedHookZygoteInit {

    private static boolean active = false;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {

        /**
         * Register BroadcastReceiver in PhoneWindowManager.init(…) so that we
         * can enable and disable the home button on demand.
         */
        findAndHookMethod("com.android.internal.policy.impl.PhoneWindowManager", null, "init",
                Context.class, "android.view.IWindowManager", "android.view.WindowManagerPolicy.WindowManagerFuncs",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        BroadcastReceiver receiver = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                switch (intent.getAction()) {
                                    case App.ACTION_EAT_HOME_PRESS_START:
                                        active = true;
                                        break;
                                    case App.ACTION_EAT_HOME_PRESS_STOP:
                                        active = false;
                                        break;
                                }
                            }
                        };
                        IntentFilter filter = new IntentFilter();
                        filter.addAction(App.ACTION_EAT_HOME_PRESS_START);
                        filter.addAction(App.ACTION_EAT_HOME_PRESS_STOP);
                        Context context = (Context) getObjectField(param.thisObject, "mContext");
                        context.registerReceiver(receiver, filter);
                    }
                }
        );

        /**
         * If active, have PhoneWindowManager.launchHomeFromHotKey() do nothing.
         */
        findAndHookMethod("com.android.internal.policy.impl.PhoneWindowManager", null, "launchHomeFromHotKey",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (active)
                            param.setResult(null);
                    }
                }
        );
    }
}
