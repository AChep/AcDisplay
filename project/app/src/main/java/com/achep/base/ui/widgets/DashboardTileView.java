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
package com.achep.base.ui.widgets;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.achep.acdisplay.R;
import com.achep.base.dashboard.DashboardTile;
import com.achep.base.ui.activities.SettingsActivity;
import com.achep.base.utils.RippleUtils;

public class DashboardTileView extends LinearLayout implements View.OnClickListener {

    private static final int DEFAULT_COL_SPAN = 1;

    private ImageView mImageView;
    private TextView mTitleTextView;
    private TextView mStatusTextView;
    private View mDivider;

    private int mColSpan = DEFAULT_COL_SPAN;

    private DashboardTile mTile;

    public DashboardTileView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mImageView = (ImageView) findViewById(R.id.icon);
        mTitleTextView = (TextView) findViewById(R.id.title);
        mStatusTextView = (TextView) findViewById(R.id.status);
        mDivider = findViewById(R.id.tile_divider);

        setOnClickListener(this);
        setFocusable(true);

        RippleUtils.makeFor(this, true);
    }

    public void setDashboardTile(@NonNull DashboardTile tile) {
        mTile = tile;
        Resources res = getResources();

        if (tile.iconRes > 0) {
            mImageView.setImageResource(tile.iconRes);
        } else {
            mImageView.setImageDrawable(null);
            mImageView.setBackground(null);
        }

        mTitleTextView.setText(tile.getTitle(res));

        CharSequence summary = tile.getSummary(res);
        if (!TextUtils.isEmpty(summary)) {
            mStatusTextView.setVisibility(View.VISIBLE);
            mStatusTextView.setText(summary);
        } else {
            mStatusTextView.setVisibility(View.GONE);
        }
    }

    public void setDividerVisibility(boolean visible) {
        mDivider.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    void setColumnSpan(int span) {
        mColSpan = span;
    }

    int getColumnSpan() {
        return mColSpan;
    }

    @Override
    public void onClick(View v) {
        if (mTile.fragment != null) {
            SettingsActivity.Utils.startWithFragment(getContext(),
                    mTile.fragment, mTile.fragmentArguments, null, 0,
                    mTile.titleRes, mTile.getTitle(getResources()));
        } else if (mTile.intent != null) {
            getContext().startActivity(mTile.intent);
        }
    }

}
