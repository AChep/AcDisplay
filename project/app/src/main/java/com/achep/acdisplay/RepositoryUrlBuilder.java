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

package com.achep.acdisplay;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Builder of the links to remote repository.
 *
 * @author Artem Chepurnoy
 */
public class RepositoryUrlBuilder {

    public static final String PROJECT_NAME = "project";
    public static final String MODULE_NAME = "app";

    private static final String USERNAME = "AChep";
    private static final String PROJECT = "AcDisplay";
    private static final String FORMATTER = "https://%1$s/%2$s/%3$s/%4$s/";

    private String mBranch;
    private String mFileName;
    private StringBuilder mPathBuilder;
    private boolean mRawAccess = false;

    @NonNull
    public RepositoryUrlBuilder setBranch(@Nullable String branch) {
        mBranch = branch;
        return this;
    }

    @NonNull
    public RepositoryUrlBuilder setRawAccess(boolean rawAccess) {
        mRawAccess = rawAccess;
        return this;
    }

    @NonNull
    public RepositoryUrlBuilder setFile(String fileName) {
        mFileName = fileName;
        return this;
    }

    @NonNull
    public RepositoryUrlBuilder changeDirectory(@NonNull String dir) {
        if (mPathBuilder == null) {
            mPathBuilder = new StringBuilder();
        }
        mPathBuilder.append(dir).append('/');
        return this;
    }

    @NonNull
    public String build() {
        if (mBranch == null) mBranch = "master";

        String branch;
        String domain;
        String path = mPathBuilder == null ? "" : mPathBuilder.toString();

        do {
            if (mFileName != null) {
                path += mFileName;
                if (mRawAccess) {
                    domain = "raw.githubusercontent.com";
                    branch = "";
                    break;
                } else {
                    branch = "blob/";
                }
            } else {
                branch = "tree/";
            }

            domain = "www.github.com";
        } while (false);

        branch += mBranch;

        return String.format(FORMATTER, domain, USERNAME, PROJECT, branch) + path;
    }

}
