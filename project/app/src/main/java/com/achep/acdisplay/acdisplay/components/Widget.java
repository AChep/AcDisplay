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

import com.achep.acdisplay.Config;
import com.achep.acdisplay.acdisplay.AcDisplayFragment;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Base of AcDisplay's widgets.
 *
 * @author Artem Chepurnoy
 */
public abstract class Widget {

    private final AcDisplayFragment mHostFragment;
    protected final Callback mCallback;

    private ViewGroup mView;
    private View mIconView;

    private boolean mShown;

    /**
     * Interface definition for a callback to host fragment.
     */
    public interface Callback {

        /**
         * Requests fragment to update widget's dynamic background.
         *
         * @see #getBackground()
         * @see #getBackgroundMask()
         */
        public void requestBackgroundUpdate(Widget widget);

        /**
         * Requests fragment to restarts timeout.
         */
        public void requestTimeoutRestart(Widget widget);
    }

    public Widget(Callback callback, AcDisplayFragment fragment) {
        if (callback == null || fragment == null) {
            throw new RuntimeException("Widget can not be initialized without callback or host.");
        }

        mCallback = callback;
        mHostFragment = fragment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(23, 651)
                .append(mHostFragment)
                .append(mView)
                .append(mIconView)
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
                .append(mHostFragment, widget.mHostFragment)
                .append(mView, widget.mView)
                .append(mIconView, widget.mIconView)
                .isEquals();
    }

    public AcDisplayFragment getHostFragment() {
        return mHostFragment;
    }

    //-- DISMISSING WIDGET ----------------------------------------------------

    /**
     * @return {@code true} if the widget can be dismissed, {@code false} otherwise
     * @see #onDismiss()
     */
    public boolean isDismissible() {
        return false;
    }

    /**
     * Called when widget is being dismissed. For example, here you need to dismiss
     * notification from system if it's notification widget.
     *
     * @see #isDismissible()
     */
    public void onDismiss() { /* reserved for children */ }

    //-- READING WIDGET -------------------------------------------------------

    /**
     * @return {@code true} if the widget can be read aloud, {@code false} otherwise
     * @see #getReadAloudText()
     */
    public boolean isReadable() {
        return false;
    }

    /**
     * @return Text to be read aloud.
     * @see #isReadable()
     */
    public String getReadAloudText() {
        return null;
    }

    //-- DYNAMIC BACKGROUND ---------------------------------------------------

    /**
     * @return The bitmap to be used as background, {@code null} if no background.
     * @see #getBackgroundMask()
     */
    public Bitmap getBackground() {
        return null;
    }

    /**
     * @return The mask of widget's background, or {@code 0} to show background
     * not depending on any config.
     * @see Config#DYNAMIC_BG_ARTWORK_MASK
     * @see Config#DYNAMIC_BG_NOTIFICATION_MASK
     * @see #getBackground()
     */
    public int getBackgroundMask() {
        return 0;
    }

    //-- LIFE CYCLE -----------------------------------------------------------

    public void onCreate() { /* empty */ }

    /**
     * This is called when the {@link #getView() view} is attached to host fragment.
     * Here you need to update view's content.
     *
     * @see #onViewDetached()
     * @see #isViewAttached()
     */
    public void onViewAttached() {
        mShown = true;
    }

    /**
     * This is called when the {@link #getView() view} is detached from host fragment.
     *
     * @see #onViewAttached()
     * @see #isViewAttached()
     */
    public void onViewDetached() {
        mShown = false;
    }

    public boolean isViewAttached() {
        return mShown;
    }

    public void onDestroy() { /* empty */ }

    //-- VIEWS ----------------------------------------------------------------

    /**
     * @return The icon of this widget.
     * @see #onCreateIconView(android.view.LayoutInflater, android.view.ViewGroup)
     */
    public View getIconView() {
        return mIconView;
    }

    /**
     * Returns the view of widget. Please note, that this view can be reused by
     * other similar widgets.
     *
     * @return The main view of this widget.
     * @see #onViewAttached()
     * @see #onViewDetached()
     * @see #isViewAttached()
     * @see #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.view.ViewGroup)
     */
    public ViewGroup getView() {
        return mView;
    }

    public View createIconView(LayoutInflater inflater, ViewGroup container) {
        mIconView = onCreateIconView(inflater, container);
        return mIconView;
    }

    public ViewGroup createView(LayoutInflater inflater, ViewGroup container, ViewGroup view) {
        mView = onCreateView(inflater, container, view);
        return mView;
    }

    protected View onCreateIconView(LayoutInflater inflater, ViewGroup container) {
        return null;
    }

    protected ViewGroup onCreateView(LayoutInflater inflater, ViewGroup container, ViewGroup view) {
        return null;
    }

}
