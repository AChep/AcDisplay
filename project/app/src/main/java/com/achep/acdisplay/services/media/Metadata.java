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
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Created by achep on 08.06.14.
 */
public class Metadata {

    @Nullable
    public CharSequence title;
    @Nullable
    public CharSequence artist;
    @Nullable
    public CharSequence album;
    @Nullable
    public CharSequence subtitle;
    @Nullable
    public Bitmap bitmap;
    @Nullable
    public String id;
    public long duration = -1;

    /**
     * @see #isEmpty()
     */
    public void clear() {
        title = null;
        artist = null;
        subtitle = null;
        bitmap = null;
        id = null;
        duration = -1;
    }

    void generateSubtitle() {
        // General subtitle's format is
        // ARTIST - ALBUM
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(artist)) {
            sb.append(artist);

            if (!TextUtils.isEmpty(album)) {
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
                + " artist=" + artist
                + " subtitle=" + subtitle
                + " duration=" + duration
                + " id=" + id
                + " bitmap=" + bitmap + "]";
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 2041)
                .append(title)
                .append(artist)
                .append(subtitle)
                .append(duration)
                .append(id)
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
                .append(id, metadata.id)
                .append(title, metadata.title)
                .append(artist, metadata.artist)
                .append(subtitle, metadata.subtitle)
                .append(bitmap, metadata.bitmap)
                .isEquals();
    }

    /**
     * @return {@code true} if metadata is empty
     * @see #clear()
     */
    public boolean isEmpty() {
        return title == null
                && artist == null
                && subtitle == null
                && bitmap == null
                && id == null
                && duration == -1;
    }

}
