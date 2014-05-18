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

package com.achep.acdisplay.acdisplay.components;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.achep.acdisplay.acdisplay.AcDisplayFragment;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Created by achep on 14.05.14 for AcDisplay.
 *
 * @author Artem Chepurnoy
 */
public abstract class Widget {

    private AcDisplayFragment mAcDisplayFragment;

    private ViewGroup mExpandedViewGroup;
    private View mCollapsedView;

    private boolean mShown;

    public Widget(AcDisplayFragment fragment) {
        mAcDisplayFragment = fragment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(23, 651)
                .append(mAcDisplayFragment)
                .append(mExpandedViewGroup)
                .append(mCollapsedView)
                .append(mShown)
                .toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Widget))
            return false;

        Widget widget = (Widget) o;
        return new EqualsBuilder()
                .append(mShown, widget.mShown)
                .append(mAcDisplayFragment, widget.mAcDisplayFragment)
                .append(mExpandedViewGroup, widget.mExpandedViewGroup)
                .append(mCollapsedView, widget.mCollapsedView)
                .isEquals();
    }

    public AcDisplayFragment getHostFragment() {
        return mAcDisplayFragment;
    }

    /**
     * @return {@code true} if the widget can be dismissed, {@code false} otherwise
     * @see #onDismissed(boolean)
     */
    public boolean isDismissible() {
        return false;
    }

    /**
     * Called when widget (may be not widget, but its content) has been dismissed.
     *
     * @param right {@code true} if dismissed by swipe to right, {@code false} otherwise.
     * @see #isDismissible()
     */
    public void onDismissed(boolean right) { /* reserved for children */ }

    /**
     * @return The bitmap to be used as background, {@code null} if no background.
     */
    public Bitmap getBackground() {
        return null;
    }

    public boolean isExpandedViewAttached() {
        return mShown;
    }

    public void onExpandedViewAttached() {
        mShown = true;
    }

    public void onExpandedViewDetached() {
        mShown = false;
    }

    /**
     * @return {@code true} if the widget contains large view, {@code false} otherwise
     */
    public boolean hasExpandedView() {
        return getExpandedView() != null;
    }

    public View getCollapsedView() {
        return mCollapsedView;
    }

    public ViewGroup getExpandedView() {
        return mExpandedViewGroup;
    }

    public View createCollapsedView(LayoutInflater inflater, ViewGroup container) {
        return (mCollapsedView = onCreateCollapsedView(inflater, container));
    }

    public ViewGroup createExpandedView(LayoutInflater inflater, ViewGroup container,
                                        ViewGroup sceneView) {
        return (mExpandedViewGroup = onCreateExpandedView(inflater, container, sceneView));
    }

    protected abstract View onCreateCollapsedView(LayoutInflater inflater, ViewGroup container);

    protected abstract ViewGroup onCreateExpandedView(LayoutInflater inflater, ViewGroup container,
                                                      ViewGroup view);

}
