/*
 * Copyright (C) 2015 AChep@xda <artemchep@gmail.com>
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

import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.achep.base.Build.DEBUG;

/**
 * Created by Artem Chepurnoy on 11.04.2015.
 */
public class GzipUtils {

    private static final String TAG = "GzipUtils";

    private static final int BUFFER_SIZE = 32;

    @Nullable
    public static String compress(String input) {
        byte[] bytes = null;
        ByteArrayOutputStream os = null;
        GZIPOutputStream gos = null;
        final int size = input.length();
        try {
            os = new ByteArrayOutputStream(size);
            gos = new GZIPOutputStream(os);
            gos.write(input.getBytes());
            gos.close();
        } catch (IOException e) {
            return null;
        } finally {
            try {
                if (gos != null) gos.close();
                if (os != null) {
                    bytes = os.toByteArray();
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Convert bytes to a string.
        assert bytes != null;
        String output = Base64.encodeToString(bytes, Base64.DEFAULT);

        // Print debug info.
        if (DEBUG) {
            final float rate = (float) output.length() / size;
            Log.d(TAG, "Gzip compress rate is " + Float.toString(rate));
        }

        return output;
    }

    @Nullable
    public static String decompress(String input) {
        StringBuilder sb = new StringBuilder();
        ByteArrayInputStream is = null;
        GZIPInputStream gis = null;
        try {
            final byte[] bytes = Base64.decode(input, Base64.DEFAULT);
            is = new ByteArrayInputStream(bytes);
            gis = new GZIPInputStream(is, BUFFER_SIZE);

            int cache;
            final byte[] data = new byte[BUFFER_SIZE];
            while ((cache = gis.read(data)) != -1) {
                sb.append(new String(data, 0, cache));
            }
        } catch (IOException e) {
            return null;
        } finally {
            try {
                if (gis != null) gis.close();
                if (is != null) is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

}
