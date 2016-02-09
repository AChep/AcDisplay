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

import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils {

    public static void readFullyWriteToOutputStream(
            @NonNull InputStream in,
            @NonNull OutputStream out)
            throws IOException {
        try {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        } finally {
            out.close();
        }
    }

    /**
     * Reads text from given {@link BufferedReader} line-by-line.
     *
     * @return text from given {@link BufferedReader}.
     * @throws IOException
     */
    @NonNull
    public static String readTextFromBufferedReader(@NonNull BufferedReader bufferedReader) throws IOException {

        // Store all lines to string builder to
        // reduce memory using.
        final StringBuilder body = new StringBuilder();
        String nextLine;
        try {
            while ((nextLine = bufferedReader.readLine()) != null) {
                body.append(nextLine);
                body.append('\n');
            }
            int pos = body.length() - 1;
            if (pos >= 0) {
                body.deleteCharAt(pos);
            }
        } finally {
            bufferedReader.close();
        }

        return body.toString();
    }

}