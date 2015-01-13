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
package com.achep.base.utils.logcat;

import android.support.annotation.Nullable;

import com.achep.base.utils.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Artem Chepurnoy on 09.12.2014.
 */
// TODO: Pull the code of capturing the logcat from the CatLog app.
// Link: https://github.com/nolanlawson/Catlog
public class Logcat {

    @Nullable
    public static String capture() {
        try {
            String[] command = new String[]{"logcat", "-v", "threadtime", "-d"};
            Process process = Runtime.getRuntime().exec(command);
            return FileUtils.readTextFromBufferedReader(
                    new BufferedReader(
                            new InputStreamReader(
                                    process.getInputStream())));
        } catch (IOException e) {
            return null;
        }
    }

}
