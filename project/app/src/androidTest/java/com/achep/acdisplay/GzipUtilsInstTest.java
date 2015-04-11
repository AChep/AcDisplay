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

import android.support.test.runner.AndroidJUnit4;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.achep.base.utils.GzipUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

/**
 * JUnit4 unit tests for the Gzip utils.
 *
 * @author Artem Chepurnoy
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class GzipUtilsInstTest extends InstrumentationTestCase {

    private String mText;

    @Before
    public void setUp() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5000; i++) sb.append(random.nextInt(10));
        mText = sb.toString();
    }

    @Test
    public void test() {
        String compressed = GzipUtils.compress(mText);
        String decompressed = GzipUtils.decompress(compressed);
        assertEquals(mText, decompressed);
    }

}
