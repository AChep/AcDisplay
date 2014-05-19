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

import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;

/**
 * Created by Artem on 29.03.2014.
 */
final class Utils {

    /**
     * Removes all kinds of multiple spaces from given string.
     */
    static String removeSpaces(String string) {
        if (string == null) return null;
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
        int length = messages.length;

        boolean highlight = length > 1;
        int[] trackStart = new int[length];
        int[] trackEnd = new int[length];

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            CharSequence line = Utils.removeSpaces(messages[i].toString());

            if (highlight) {
                int offset = sb.length();
                int end = Utils.indexOf(line, ' ');

                if (end != -1) {
                    trackStart[i] = offset;
                    trackEnd[i] = offset + end;
                }
            }

            sb.append(line);
            sb.append('\n');
        }

        CharSequence text = Utils.removeSpaces(sb.toString());
        if (highlight) {
            Spannable textSpannable = new SpannableString(text);
            for (int i = 0; i < length; i++) {
                if (trackEnd[i] == 0) continue;

                textSpannable.setSpan(new ForegroundColorSpan(0xaaFFFFFF),
                        trackStart[i], trackStart[i] + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                textSpannable.setSpan(new UnderlineSpan(),
                        trackStart[i], trackStart[i] + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        /*textSpannable.setSpan(new AbsoluteSizeSpan(19, true),
                                trackStart[i], trackStart[i] + 1,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);*/
            }
            text = textSpannable;
        }

        return text;
    }
}
