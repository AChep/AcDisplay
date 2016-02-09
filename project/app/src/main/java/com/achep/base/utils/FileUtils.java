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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
     * @return {@code true} if all files were deleted successfully
     * or did not exits, {@code false} otherwise or if given file is null.
     */
    public static boolean deleteRecursive(@Nullable File file) {
        if (file != null) {
            if (!file.exists()) return true;

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

    public static void writeToFileFrom(
            @NonNull File file,
            @NonNull InputStream in)
            throws IOException {
        IOUtils.readFullyWriteToOutputStream(in, new FileOutputStream(file));
    }

    public static boolean writeToFileAppend(@NonNull File file, @NonNull CharSequence text) {
        try {
            //noinspection ResultOfMethodCallIgnored
            file.createNewFile();
        } catch (IOException e) {
            Log.w(TAG, "Failed to create the file: file=" + file.getName());
            return false;
        }

        OutputStream os = null;
        InputStream is = null;
        try {
            os = new FileOutputStream(file, true);
            is = new ByteArrayInputStream(text.toString().getBytes("UTF-8"));

            int read;
            final byte[] buffer = new byte[1024];
            do {
                read = is.read(buffer, 0, buffer.length);
                if (read > 0) os.write(buffer, 0, read);
            } while (read > 0);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Failed to append to a file: file=" + file.getName());
            e.printStackTrace();
        } finally {
            // Try to close the stream.
            if (os != null) try {
                os.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close the stream!");
                e.printStackTrace();
            }

            // Try to close the stream.
            if (is != null) try {
                is.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close the stream!");
                e.printStackTrace();
            }
        }
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
            return IOUtils.readTextFromBufferedReader(bufferedReader);
        } catch (IOException e) {
            if (DEBUG) Log.e(TAG, "Failed to read file=" + file);
            return null;
        }
    }

}
