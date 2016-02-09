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
package com.achep.acdisplay;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.achep.base.content.SharedList;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

/**
 * JUnit4 unit tests for the shared list.
 *
 * @author Artem Chepurnoy
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SharedListInstTest extends InstrumentationTestCase {

    @Test
    public void test() {
        // Create strings data
        final int initialSize = 200;
        ArrayList<String> list = new ArrayList<>(initialSize);
        for (int i = 0; i < initialSize; i++) list.add(Double.toString(Math.random() + i));

        // Initialize the list
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        SharedListString origin = new SharedListString(context);
        for (String str : list) origin.put(context, str);
        for (int i = 0, j; i * 4 < initialSize; i++) {
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
        @NonNull
        @Override
        public SharedPreferences.Editor put(@NonNull String string,
                                            @NonNull SharedPreferences.Editor editor,
                                            int position) {
            editor.putString(KEY_STR + position, string);
            return editor;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String get(@NonNull SharedPreferences prefs, int position) {
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
        @NonNull
        @Override
        protected String getPreferencesFileName() {
            return PREF_NAME;
        }

        /**
         * {@inheritDoc}
         */
        @NonNull
        @Override
        protected StringSaver onCreateSaver() {
            return new StringSaver();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean isOverwriteAllowed(@NonNull String str) {
            return true;
        }
    }

}
