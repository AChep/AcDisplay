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

import android.content.Context;
import android.content.SharedPreferences;

import com.achep.base.content.SharedList;
import com.achep.base.utils.FileUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import static org.junit.Assert.assertTrue;

@Config(manifest = "./src/main/AndroidManifest.xml")
@RunWith(RobolectricTestRunner.class)
public class AcDisplayRobolectricTest {

    @Test
    public void testSomething() {
        // Placeholder
    }

    @Test
    public void testFileUtils() {
        Context context = Robolectric.application;
        File file = new File(context.getFilesDir(), "test_file_utils.txt");

        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            sb.append(random.nextInt(10));
        }
        String text = sb.toString();

        // Test writing to file.
        assertTrue(FileUtils.writeToFile(file, text));

        // Test reading from file.
        assertTrue(text.equals(FileUtils.readTextFile(file)));

        // Test removing file.
        assertTrue(FileUtils.deleteRecursive(file));
        assertTrue(!file.exists());
    }

    @Test
    public void testSharedList() {
        // Create strings data
        final int initialSize = 200;
        ArrayList<String> list = new ArrayList<>(initialSize);
        for (int i = 0; i < initialSize; i++) list.add(Double.toString(Math.random() + i));

        // Initializate the list
        Context context = Robolectric.application;
        SharedListString origin = new SharedListString(context);
        for (String str : list) origin.put(context, str);
        for (int i = 0, j = 0; i * 4 < initialSize; i++) {
            j = (int) (Math.random() * list.size());
            origin.remove(context, list.get(j));
            list.remove(j);
        }

        // Testing placeholders
        list.add("Cute kitten :3");
        origin.put(context, list.get(list.size() - 1));

        // Testing overwriting
        list.add(list.get(0) + "");
        origin.put(context, list.get(list.size() - 1));

        // Check equality
        SharedListString restored = new SharedListString(context);
        for (String str : list) assertTrue(restored.contains(str));
    }

    /**
     * Saver for {@link String string}.
     */
    private static class StringSaver extends SharedList.Saver<String> {

        private static final String KEY_STR = "str_";

        /**
         * {@inheritDoc}
         */
        @Override
        public SharedPreferences.Editor put(String string, SharedPreferences.Editor editor, int position) {
            editor.putString(KEY_STR + position, string);
            return editor;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String get(SharedPreferences prefs, int position) {
            return prefs.getString(KEY_STR + position, null);
        }

    }

    /**
     * The most simple implementation of {@link com.achep.base.content.SharedList shared list}.
     */
    private static class SharedListString extends SharedList<String, StringSaver> {

        public static final String PREF_NAME = "test_shared_list";

        protected SharedListString(Context context) {
            super(context);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected String getPreferencesFileName() {
            return PREF_NAME;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected StringSaver onCreateSaver() {
            return new StringSaver();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean isOverwriteAllowed(String str) {
            return true;
        }
    }

}