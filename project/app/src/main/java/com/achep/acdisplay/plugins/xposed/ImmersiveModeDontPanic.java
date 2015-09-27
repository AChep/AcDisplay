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

import android.util.Log;

import com.achep.base.Device;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * Android shows you an help message when you first start an app in Immersive Mode.
 * When you press the 'OK' button, Android sets a flag to remember you saw
 * this message, and stops showing it in the future for this app.
 * <p>
 * However, Android resets this flag when a panicking user is detected.
 * This is a safeguard measure to help people who don't know what's happening
 * (if they dismissed the message without reading it.) Panicking is detected when
 * the user turns the screen on and off more than once within 5 seconds.
 * <p>
 * This module makes the method responsible for this check do nothing, thus removing this annoyance.
 */
public class ImmersiveModeDontPanic implements IXposedHookZygoteInit {

    private static final String TAG = "xposed:ImmersivePanic";

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        if (Device.hasLollipopApi()) {
            // Unfortunately this removes all immersive panic reports,
            // not only about AcDisplay.
            XC_MethodHook hook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    param.setResult(null);
                }
            };

            findAndHookMethod(
                    "com.android.internal.policy.impl.ImmersiveModeConfirmation", null,
                    "handlePanic", hook);
        } else {
            XC_MethodHook hook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    String pkg = (String) param.args[0];
                    if (pkg != null && pkg.startsWith("com.achep.acdisplay")) {
                        param.setResult(null);

                        Log.i(TAG, "An unconfirmation of AcDisplay\'s immersive mode passed to hell.");
                    }
                }
            };

            findAndHookMethod(
                    "com.android.internal.policy.impl.ImmersiveModeConfirmation", null,
                    "unconfirmPackage", String.class, hook);
        }
    }

}
