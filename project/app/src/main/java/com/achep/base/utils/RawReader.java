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
package com.achep.base.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import timber.log.Timber;

/**
 * Raw resources utils.
 *
 * @author Artem Chepurnoy
 */
public final class RawReader {

    private static final String TAG = "RawReader";

    /**
     * Reads text from raw resource.
     *
     * @param resource id of raw file to read, or {@code 0}.
     * @return text from raw resource, or {@code null}
     * if reading has failed or resource is {@code 0}.
     */
    @Nullable
    public static String readText(@NonNull Context context, @RawRes int resource) {
        if (resource == 0) return null;

        final InputStream inputStream = context.getResources().openRawResource(resource);
        final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        try {
            return IOUtils.readTextFromBufferedReader(bufferedReader);
        } catch (IOException e) {
            Timber.tag(TAG).e("Failed to read raw resource: " + resource);
            return null;
        }
    }

}
