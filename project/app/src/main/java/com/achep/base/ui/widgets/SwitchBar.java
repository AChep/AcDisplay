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
package com.achep.base.ui.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.achep.acdisplay.R;
import com.achep.base.interfaces.ICheckable;
import com.achep.base.utils.RippleUtils;

/**
 * Material-designed switch bar.
 *
 * @author Artem Chepurnoy
 */
public class SwitchBar extends LinearLayout implements ICheckable {

    private TextView mTextView;
    private ImageView mIconView;
    private SwitchCompat mSwitch;

    private CompoundButton.OnCheckedChangeListener mPublicListener;
    private final CompoundButton.OnCheckedChangeListener mListener =
            new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mSwitch.post(new Runnable() {
                        @Override
                        public void run() {
                            updateText(mSwitch.isChecked());
                        }
                    });

                    if (mPublicListener != null) {
                        mPublicListener.onCheckedChanged(buttonView, isChecked);
                    }
                }

            };

    private void updateText(boolean isChecked) {
        mTextView.setText(isChecked
                ? R.string.on
                : R.string.off);
    }

    public SwitchBar(Context context) {
        super(context);
    }

    public SwitchBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SwitchBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressLint("WrongViewCast")
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (!isInEditMode()) {
            RippleUtils.makeFor(this, false);
        }

        mTextView = (TextView) findViewById(R.id.title);
        mIconView = (ImageView) findViewById(R.id.icon);
        mSwitch = (SwitchCompat) findViewById(R.id.switch_);
        mSwitch.setOnCheckedChangeListener(mListener);
        updateText(mSwitch.isChecked());

        // Toggle switch on click on the panel.
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle();
            }
        });
    }

    /*
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        post(new Runnable() {
            @Override
            public void run() {
                // TODO: Somehow update padding left to fit with the title of ActionBar.
            }
        });
    }
    */

    public void setIconResource(@DrawableRes int resource) {
        mIconView.setImageResource(resource);
    }

    public void setIconDrawable(@Nullable Drawable drawable) {
        mIconView.setImageDrawable(drawable);
    }

    @NonNull
    public ImageView getIconView() {
        return mIconView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
        mPublicListener = listener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setChecked(boolean checked) {
        mSwitch.setChecked(checked);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isChecked() {
        return mSwitch.isChecked();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void toggle() {
        mSwitch.toggle();
    }

}
