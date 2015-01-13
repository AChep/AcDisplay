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
package com.achep.base.providers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

/**
 * Created by achep on 16.06.14.
 */
public abstract class LogsProviderBase extends ContentProvider {

    private static final String TAG = "LogsProvider";
    private static final String COLUMN_DATA = "_data";

    public static final String DIRECTORY = "logs";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String orderBy) {
        final List<String> pathSegments = uri.getPathSegments();
        final String fileName = pathSegments.get(0);
        File logFile;

        logFile = getContext().getCacheDir();
        if (logFile == null) {
            Log.e(TAG, "No cache dir.");
            return null;
        }

        logFile = new File(new File(logFile, DIRECTORY), fileName);
        if (!logFile.exists()) {
            Log.e(TAG, "Requested log file doesn't exist.");
            return null;
        }

        if (projection == null) {
            projection = new String[]{
                    COLUMN_DATA,
                    OpenableColumns.DISPLAY_NAME,
                    OpenableColumns.SIZE,
            };
        }

        MatrixCursor matrixCursor = new MatrixCursor(projection, 1);
        Object[] row = new Object[projection.length];
        for (int col = 0; col < projection.length; col++) {
            switch (projection[col]) {
                case COLUMN_DATA:
                    row[col] = logFile.getAbsolutePath();
                    break;
                case OpenableColumns.DISPLAY_NAME:
                    row[col] = fileName;
                    break;
                case OpenableColumns.SIZE:
                    row[col] = logFile.length();
                    break;
            }
        }
        matrixCursor.addRow(row);
        return matrixCursor;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException {
        return openFileHelper(uri, "r");
    }

    @Override
    public String getType(Uri uri) {
        return "text/plain";
    }

    /**
     * Not supported!
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Insert is not supported!");
    }

    /**
     * Not supported!
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Delete is not supported!");
    }

    /**
     * Not supported!
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public int update(Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {
        throw new UnsupportedOperationException("Update is not supported!");
    }

}
