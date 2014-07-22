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

package com.achep.acdisplay.hotword;

import android.content.SharedPreferences;

import com.achep.acdisplay.blacklist.SharedList;
import com.achep.acdisplay.fragments.HotwordFragment;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
* Created by achep on 25.06.14.
*/
public class Hotword {

    public final String speech;
    public final String action;

    public Hotword(String speech, String action) {
        this.speech = speech.trim();
        this.action = action;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(23, 691)
                .append(speech)
                .append(action)
                .toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Hotword))
            return false;

        Hotword hotword = (Hotword) o;
        return new EqualsBuilder()
                .append(speech, hotword.speech)
                .append(action, hotword.action)
                .isEquals();
    }

    /**
     * Saves / restores hotword.
     */
    public static class Saver extends SharedList.Saver<Hotword> {

        private static final String KEY_SPEECH = "speech_";
        private static final String KEY_ACTION = "action_";

        /**
         * {@inheritDoc}
         */
        @Override
        public SharedPreferences.Editor put(Hotword hotword, SharedPreferences.Editor editor, int position) {
            editor.putString(KEY_SPEECH + position, hotword.speech);
            editor.putString(KEY_ACTION + position, hotword.action);
            return editor;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Hotword get(SharedPreferences prefs, int position) {
            String speech = prefs.getString(KEY_SPEECH + position, "error");
            String action = prefs.getString(KEY_ACTION + position, "error");
            return new Hotword(speech, action);
        }
    }
}
