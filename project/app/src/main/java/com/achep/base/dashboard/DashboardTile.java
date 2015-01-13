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
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.achep.acdisplay.R;

/**
 * Description of a single dashboard tile that the user can select.
 */
public class DashboardTile implements Parcelable {

    /**
     * Default value for {@link com.achep.base.dashboard.DashboardTile#id DashboardTile.id}
     * indicating that no identifier value is set. All other values (including those below -1)
     * are valid.
     */
    public static final int TILE_ID_UNDEFINED = -1;

    /**
     * Identifier for this tile, to correlate with a new list when
     * it is updated.  The default value is
     * {@link #TILE_ID_UNDEFINED}, meaning no id.
     */
    public long id = TILE_ID_UNDEFINED;

    /**
     * Resource ID of title of the tile that is shown to the user.
     */
    public int titleRes;

    /**
     * Title of the tile that is shown to the user.
     */
    public CharSequence title;

    /**
     * Resource ID of optional summary describing what this tile controls.
     */
    public int summaryRes;

    /**
     * Optional summary describing what this tile controls.
     */
    public CharSequence summary;

    /**
     * Optional icon resource to show for this tile.
     */
    public final int iconRes;

    /**
     * Full class name of the fragment to display when this tile is
     * selected.
     */
    public final String fragment;

    /**
     * Optional arguments to supply to the fragment when it is
     * instantiated.
     */
    public Bundle fragmentArguments;

    /**
     * Intent to launch when the preference is selected.
     */
    public Intent intent;

    /**
     * Optional additional data for use by subclasses of the activity
     */
    public Bundle extras;

    public DashboardTile(Context context, AttributeSet attrs) {
        TypedArray sa = context.obtainStyledAttributes(attrs, R.styleable.DashboardTile);
        TypedValue tv;

        id = sa.getResourceId(R.styleable.DashboardTile_dashboard_id, TILE_ID_UNDEFINED);
        iconRes = sa.getResourceId(R.styleable.DashboardTile_dashboard_icon, 0);
        fragment = sa.getString(R.styleable.DashboardTile_dashboard_fragment);

        tv = sa.peekValue(R.styleable.DashboardTile_dashboard_title);
        if (tv != null && tv.type == TypedValue.TYPE_STRING) {
            if (tv.resourceId != 0) {
                titleRes = tv.resourceId;
            } else {
                title = tv.string;
            }
        }

        tv = sa.peekValue(R.styleable.DashboardTile_dashboard_summary);
        if (tv != null && tv.type == TypedValue.TYPE_STRING) {
            if (tv.resourceId != 0) {
                summaryRes = tv.resourceId;
            } else {
                summary = tv.string;
            }
        }

        sa.recycle();
    }

    /**
     * Return the currently set title.  If {@link #titleRes} is set,
     * this resource is loaded from <var>res</var> and returned.  Otherwise
     * {@link #title} is returned.
     */
    public CharSequence getTitle(Resources res) {
        return titleRes != 0 ? res.getText(titleRes) : title;
    }

    /**
     * Return the currently set summary.  If {@link #summaryRes} is set,
     * this resource is loaded from <var>res</var> and returned.  Otherwise
     * {@link #summary} is returned.
     */
    public CharSequence getSummary(Resources res) {
        return summaryRes != 0 ? res.getText(summaryRes) : summary;
    }

    //-- PARCELABLE IMPLEMENTATION --------------------------------------------

    public static final Creator<DashboardTile> CREATOR =
            new Creator<DashboardTile>() {

                public DashboardTile createFromParcel(Parcel source) {
                    return new DashboardTile(source);
                }

                public DashboardTile[] newArray(int size) {
                    return new DashboardTile[size];
                }

            };

    private DashboardTile(Parcel in) {
        id = in.readLong();
        titleRes = in.readInt();
        title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        summaryRes = in.readInt();
        summary = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        iconRes = in.readInt();
        fragment = in.readString();
        fragmentArguments = in.readBundle();
        if (in.readInt() != 0) {
            intent = Intent.CREATOR.createFromParcel(in);
        }
        extras = in.readBundle();
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
        dest.writeLong(id);
        dest.writeInt(titleRes);
        TextUtils.writeToParcel(title, dest, flags);
        dest.writeInt(summaryRes);
        TextUtils.writeToParcel(summary, dest, flags);
        dest.writeInt(iconRes);
        dest.writeString(fragment);
        dest.writeBundle(fragmentArguments);
        if (intent != null) {
            dest.writeInt(1);
            intent.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
        dest.writeBundle(extras);
    }

}
