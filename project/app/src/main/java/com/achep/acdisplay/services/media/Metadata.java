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
package com.achep.acdisplay.services.media;

import android.graphics.Bitmap;
import android.text.TextUtils;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Created by achep on 08.06.14.
 */
public class Metadata {

    public CharSequence title;
    public CharSequence artist;
    public CharSequence album;
    public CharSequence subtitle;
    public Bitmap bitmap;
    public long duration;

    public void clear() {
        title = null;
        subtitle = null;
        bitmap = null;
        duration = -1;
    }

    public void updateSubtitle() {
        // General subtitle's format is
        // ARTIST - ALBUM
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(artist)) {
            sb.append(artist);

            if (TextUtils.isEmpty(album)) {
                sb.append(" - ");
                sb.append(album);
            }
        } else if (!TextUtils.isEmpty(album)) {
            sb.append(album);
        } else {
            sb = null;
        }

        subtitle = sb;
    }

    @Override
    public String toString() {
        return "Metadata[title=" + title
                + " subtitle=" + subtitle
                + " duration=" + duration
                + " bitmap=" + bitmap + "]";
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 2041)
                .append(title)
                .append(subtitle)
                .append(duration)
                .append(bitmap)
                .toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Metadata))
            return false;

        Metadata metadata = (Metadata) o;
        return new EqualsBuilder()
                .append(duration, metadata.duration)
                .append(title, metadata.title)
                .append(subtitle, metadata.title)
                .append(bitmap, metadata.bitmap)
                .isEquals();
    }

}
