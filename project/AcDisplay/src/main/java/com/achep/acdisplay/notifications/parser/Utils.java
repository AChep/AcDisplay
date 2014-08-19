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

package com.achep.acdisplay.notifications.parser;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;

import com.achep.acdisplay.Build;

/**
 * Created by Artem on 29.03.2014.
 */
final class Utils {

    private static final String TAG = "NotificationParser.Utils";

    /**
     * Removes all kinds of multiple spaces from given string.
     */
    static String removeSpaces(CharSequence cs) {
        if (cs == null) return null;
        String string = cs instanceof String
                ? (String) cs : cs.toString();
        return string
                .replaceAll("(\\s+$|^\\s+)", "")
                .replaceAll("\n+", "\n");
    }

    static int indexOf(CharSequence charSequence, char c) {
        int length = charSequence.length();
        for (int i = 0; i < length; i++) {
            char letter = charSequence.charAt(i);
            if (letter == c) {
                return i;
            }
        }
        return -1;
    }

    static CharSequence mergeLargeMessage(CharSequence[] messages) {
        if (messages == null) return null;
        int length = messages.length;

        boolean highlight = length > 1; // highlight first letters of messages or no?

        SpannableStringBuilder sb = new SpannableStringBuilder();
        for (CharSequence message : messages) {
            if (TextUtils.isEmpty(message)) {
                if (Build.DEBUG) Log.w(TAG, "One of text lines was null!");
                continue;
            }

            int start = sb.length();

            CharSequence line = Utils.removeSpaces(message);
            sb.append(line);
            sb.append('\n');

            if (highlight) {
                sb.setSpan(new ForegroundColorSpan(0xaaFFFFFF),
                        start, start + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new UnderlineSpan(),
                        start, start + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        return Utils.removeSpaces(sb);
    }
}
