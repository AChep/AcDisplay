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
package com.achep.acdisplay.ui.components;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.ui.fragments.AcDisplayFragment;
import com.achep.base.tests.Check;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Base of AcDisplay's widgets.
 *
 * @author Artem Chepurnoy
 */
public abstract class Widget {

    @NonNull
    private final AcDisplayFragment mFragment;

    @NonNull
    protected final Callback mCallback;

    private ViewGroup mView;
    private View mIconView;

    private boolean mAttached;
    private boolean mStarted;

    /**
     * Interface definition for a callback to the host fragment.
     */
    public interface Callback {

        /**
         * Requests the fragment to update widget's dynamic background.
         *
         * @see #getBackground()
         * @see #getBackgroundMask()
         */
        void requestBackgroundUpdate(@NonNull Widget widget);

        /**
         * Requests the fragment to restart the timeout.
         */
        void requestTimeoutRestart(@NonNull Widget widget);

        /**
         * Requests the fragment to stick this widget and turn on the special timeout mode.
         */
        void requestWidgetStick(@NonNull Widget widget);

    }

    public Widget(@NonNull Callback callback, @NonNull AcDisplayFragment fragment) {
        mCallback = callback;
        mFragment = fragment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(23, 651)
                .append(mFragment)
                .append(mView)
                .append(mIconView)
                .append(mAttached)
                .append(mStarted)
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
                .append(mAttached, widget.mAttached)
                .append(mStarted, widget.mStarted)
                .append(mFragment, widget.mFragment)
                .append(mView, widget.mView)
                .append(mIconView, widget.mIconView)
                .isEquals();
    }

    /**
     * @return the host fragment
     */
    @NonNull
    public AcDisplayFragment getFragment() {
        return mFragment;
    }

    @NonNull
    public Config getConfig() {
        return mFragment.getConfig();
    }

    //-- HOME WIDGET ----------------------------------------------------------

    public boolean isHomeWidget() {
        return false;
    }

    public boolean hasClock() {
        return false;
    }

    //-- DISMISSING WIDGET ----------------------------------------------------

    /**
     * @return {@code true} if the widget can be dismissed, {@code false} otherwise
     * @see #onDismiss()
     */
    public boolean isDismissible() {
        return !isHomeWidget();
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
    @Nullable
    public CharSequence getReadAloudText() {
        return null;
    }

    //-- DYNAMIC BACKGROUND ---------------------------------------------------

    /**
     * @return The bitmap to be used as background, {@code null} if no background.
     * @see #getBackgroundMask()
     */
    @Nullable
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

    public final void start() {
        if (mStarted) return;
        mStarted = true;
        onStart();
    }

    public final void stop() {
        if (!mStarted) return;
        mStarted = false;
        onStop();
    }

    public final boolean isStarted() {
        return mStarted;
    }

    public void onStart() { /* empty */ }

    /**
     * This is called when the {@link #getView() view} is attached to host fragment.
     * Here you need to update view's content.
     *
     * @see #onViewDetached()
     * @see #isViewAttached()
     */
    public void onViewAttached() {
        Check.getInstance().isFalse(mAttached);
        mAttached = true;
    }

    /**
     * This is called when the {@link #getView() view} is detached from host fragment.
     *
     * @see #onViewAttached()
     * @see #isViewAttached()
     */
    public void onViewDetached() {
        Check.getInstance().isTrue(mAttached);
        mAttached = false;
    }

    public boolean isViewAttached() {
        return mAttached;
    }

    public void onStop() { /* empty */ }

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

    protected View onCreateIconView(
            @NonNull LayoutInflater inflater,
            @NonNull ViewGroup container) {
        return null;
    }

    protected ViewGroup onCreateView(
            @NonNull LayoutInflater inflater,
            @NonNull ViewGroup container,
            @Nullable ViewGroup view) {
        return null;
    }

}
