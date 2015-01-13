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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

import static com.achep.base.Build.DEBUG;

/**
 * Utils related to file system, files and maybe reading buffers.
 *
 * @author Artem Chepurnoy
 */
public class FileUtils {

    private static final String TAG = "FileUtils";

    private static final int FILE_DELETE_TRIES = 5;

    /**
     * Deletes all files from given directory recursively.
     *
     * @return {@code true} if all files were deleted successfully,
     * {@code false} otherwise or if given file is null.
     */
    public static boolean deleteRecursive(@Nullable File file) {
        if (file != null) {
            File[] children;
            if (file.isDirectory() && (children = file.listFiles()) != null && children.length > 0) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            } else {
                for (int i = 1; i <= FILE_DELETE_TRIES; i++) {
                    if (file.delete()) {
                        if (DEBUG) Log.d(TAG, "Removed file=" + file + " on " + i + " try.");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Writes given text to file (deletes original file).
     *
     * @param file file to write in
     * @return {@code true} is succeed, {@code false} if failed (file state is undefined!).
     */
    public static boolean writeToFile(@NonNull File file, @NonNull CharSequence text) {
        if (file.exists()) {
            if (!deleteRecursive(file)) {
                return false;
            }
        }

        String errorMessage = "";
        FileOutputStream fos = null;
        OutputStreamWriter osw = null;
        try {
            fos = new FileOutputStream(file);
            osw = new OutputStreamWriter(fos);
            osw.append(text);
            return true;
        } catch (IOException e) {
            errorMessage = "[Failed to write to file]";
        } finally {
            try {
                if (osw != null) {
                    osw.close();
                } else if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                errorMessage += "[Failed to close the stream]";
            }
        }
        if (DEBUG) Log.e(TAG, errorMessage + " file=" + file);
        return false;
    }

    /**
     * @return Text read from given file, or {@code null}
     * if file does not exist or reading failed.
     */
    @Nullable
    public static String readTextFile(@NonNull File file) {
        if (!file.exists()) return null;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            return FileUtils.readTextFromBufferedReader(bufferedReader);
        } catch (IOException e) {
            if (DEBUG) Log.e(TAG, "Failed to read file=" + file);
            return null;
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
