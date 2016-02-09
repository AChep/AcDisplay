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
package com.achep.base.dashboard;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.achep.acdisplay.R;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class DashboardCategory implements Parcelable, List<DashboardTile> {

    /**
     * Default value for {@link com.achep.base.dashboard.DashboardCategory#id DashboardCategory.id}
     * indicating that no identifier value is set.  All other values (including those below -1)
     * are valid.
     */
    public static final int CAT_ID_UNDEFINED = -1;

    /**
     * Identifier for this tile, to correlate with a new list when
     * it is updated.  The default value is
     * {@link com.achep.base.dashboard.DashboardTile#TILE_ID_UNDEFINED}, meaning no id.
     */
    public long id = CAT_ID_UNDEFINED;

    /**
     * Resource ID of title of the category that is shown to the user.
     */
    public int titleRes;

    /**
     * Title of the category that is shown to the user.
     */
    public CharSequence title;

    /**
     * List of the category's children
     */
    public final List<DashboardTile> tiles = new ArrayList<>();

    public DashboardCategory(Context context, AttributeSet attrs) {
        TypedArray sa = context.obtainStyledAttributes(attrs, R.styleable.DashboardTile);

        id = sa.getResourceId(R.styleable.DashboardTile_dashboard_id, CAT_ID_UNDEFINED);

        TypedValue tv = sa.peekValue(R.styleable.DashboardTile_dashboard_title);
        if (tv != null && tv.type == TypedValue.TYPE_STRING) {
            if (tv.resourceId != 0) {
                titleRes = tv.resourceId;
            } else {
                title = tv.string;
            }
        }

        sa.recycle();
    }

    /**
     * Return the currently set title. If {@link #titleRes} is set,
     * this resource is loaded from <var>res</var> and stored in {@link #title}.
     */
    public CharSequence getTitle(Resources res) {
        if (titleRes != 0) {
            title = res.getText(titleRes);
            titleRes = 0;
        }

        return title;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DashboardCategory))
            return false;

        DashboardCategory dc = (DashboardCategory) o;
        return new EqualsBuilder()
                .append(id, dc.id)
                .append(titleRes, dc.titleRes)
                .append(title, dc.title)
                .append(tiles, dc.tiles)
                .isEquals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(153, 11)
                .append(id)
                .append(titleRes)
                .append(title)
                .append(tiles)
                .toHashCode();
    }

    //-- PARCELABLE IMPLEMENTATION --------------------------------------------

    public static final Creator<DashboardCategory> CREATOR =
            new Creator<DashboardCategory>() {

                public DashboardCategory createFromParcel(Parcel source) {
                    return new DashboardCategory(source);
                }

                public DashboardCategory[] newArray(int size) {
                    return new DashboardCategory[size];
                }

            };

    private DashboardCategory(Parcel in) {
        titleRes = in.readInt();
        title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);

        final int count = in.readInt();

        for (int n = 0; n < count; n++) {
            DashboardTile tile = DashboardTile.CREATOR.createFromParcel(in);
            tiles.add(tile);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(titleRes);
        TextUtils.writeToParcel(title, dest, flags);

        final int count = tiles.size();
        dest.writeInt(count);

        for (int n = 0; n < count; n++) {
            DashboardTile tile = tiles.get(n);
            tile.writeToParcel(dest, flags);
        }
    }

    //-- LIST IMPLEMENTATION --------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(int location, DashboardTile object) {
        tiles.add(location, object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(DashboardTile object) {
        return tiles.add(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addAll(int location, @NonNull Collection<? extends DashboardTile> collection) {
        return tiles.addAll(location, collection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addAll(@NonNull Collection<? extends DashboardTile> collection) {
        return tiles.addAll(collection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        tiles.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(Object object) {
        return tiles.contains(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsAll(@NonNull Collection<?> collection) {
        return tiles.containsAll(collection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DashboardTile get(int location) {
        return tiles.get(location);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int indexOf(Object object) {
        return tiles.indexOf(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return tiles.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Iterator<DashboardTile> iterator() {
        return tiles.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int lastIndexOf(Object object) {
        return tiles.lastIndexOf(object);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public ListIterator<DashboardTile> listIterator() {
        return tiles.listIterator();
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public ListIterator<DashboardTile> listIterator(int location) {
        return tiles.listIterator(location);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DashboardTile remove(int location) {
        return tiles.remove(location);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(Object object) {
        return tiles.remove(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeAll(@NonNull Collection<?> collection) {
        return tiles.removeAll(collection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean retainAll(@NonNull Collection<?> collection) {
        return tiles.retainAll(collection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DashboardTile set(int location, DashboardTile object) {
        return tiles.set(location, object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return tiles.size();
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public List<DashboardTile> subList(int start, int end) {
        return tiles.subList(start, end);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Object[] toArray() {
        return tiles.toArray();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("SuspiciousToArrayCall")
    @NonNull
    @Override
    public <T> T[] toArray(@NonNull T[] array) {
        return tiles.toArray(array);
    }

}
