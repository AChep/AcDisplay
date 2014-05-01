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
package com.achep.activedisplay.utils;

import java.io.File;

/**
 * Helper class with utils related to file system and files.
 *
 * @author Artem Chepurnoy
 */
public class FileUtils {

    /**
     * Deletes all files from given directory recursively.
     *
     * @return True if all files were deleted successfully, False otherwise or if given file is null.
     */
    public static boolean deleteRecursive(File file) {
        if (file != null) {
            File[] children;
            if (file.isDirectory() && (children = file.listFiles()) != null && children.length > 0) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            } else {
                int i = 0;
                for (; i < 5; i++) {
                    if (file.delete()) {
                        i = 0;
                        break;
                    }
                }
                return i == 0;
            }
        }
        return false;
    }

}
