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
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import timber.log.Timber;

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
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String orderBy) {
        File file;

        final List<String> pathSegments = uri.getPathSegments();
        final String fileName = pathSegments.get(0);
        final Context context = getContext();
        if (context == null) {
            Timber.tag(TAG).e("No context.");
            return null;
        }

        file = getContext().getCacheDir();
        if (file == null) {
            Timber.tag(TAG).w("No cache dir.");
            return null;
        }

        file = new File(new File(file, DIRECTORY), fileName);
        if (!file.exists()) {
            Timber.tag(TAG).w("Requested log file doesn't exist.");
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
                    row[col] = file.getAbsolutePath();
                    break;
                case OpenableColumns.DISPLAY_NAME:
                    row[col] = fileName;
                    break;
                case OpenableColumns.SIZE:
                    row[col] = file.length();
                    break;
            }
        }
        matrixCursor.addRow(row);
        return matrixCursor;
    }

    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode)
            throws FileNotFoundException {
        return openFileHelper(uri, "r");
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return "text/plain";
    }

    /**
     * Not supported!
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Insert is not supported!");
    }

    /**
     * Not supported!
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public int delete(@NonNull Uri uri, String selection, String[] args) {
        throw new UnsupportedOperationException("Delete is not supported!");
    }

    /**
     * Not supported!
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] args) {
        throw new UnsupportedOperationException("Update is not supported!");
    }

}
