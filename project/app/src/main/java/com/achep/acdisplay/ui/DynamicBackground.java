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
package com.achep.acdisplay.ui;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.transitions.everywhere.TransitionManager;
import android.transitions.everywhere.hidden.Crossfade;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.acdisplay.ui.fragments.AcDisplayFragment;
import com.achep.base.utils.Operator;

import static com.achep.base.Build.DEBUG;

/**
 * Created by Artem Chepurnoy on 22.11.2014.
 */
public abstract class DynamicBackground {

    protected static final String TAG = "DynamicBackground";

    private static final boolean TRANSITION_IS_NOT_BUGGY = false;

    public static DynamicBackground newInstance(@NonNull AcDisplayFragment fragment,
                                                @Nullable ImageView imageView) {
        return TRANSITION_IS_NOT_BUGGY
                ? new DynamicBackgroundTransition(fragment, imageView)
                : new DynamicBackgroundCompat(fragment, imageView);
    }

    protected final AcDisplayFragment mFragment;
    protected final ImageView mImageView;

    protected DynamicBackground(@NonNull AcDisplayFragment fragment,
                                @Nullable ImageView imageView) {
        mFragment = fragment;
        mImageView = imageView;

        if (mImageView == null) {
            return;
        }

        initBackground();
    }

    protected abstract void initBackground();

    protected abstract void dispatchSetBackgroundInternal(@Nullable Bitmap bitmap);

    /**
     * Clears background.
     *
     * @see #dispatchSetBackground(android.graphics.Bitmap, int)
     */
    public void dispatchClearBackground() {
        dispatchSetBackground(null);
    }

    /**
     * Smoothly sets the background. This feature is known as "Dynamic background".
     *
     * @param bitmap the bitmap to display, or {@code null} to hide previous background.
     * @param mask   one of the following:
     *               {@link Config#DYNAMIC_BG_ARTWORK_MASK},
     *               {@link Config#DYNAMIC_BG_NOTIFICATION_MASK} or
     *               {@code 0} to bypass mask checking.
     * @see #dispatchClearBackground()
     */
    public void dispatchSetBackground(@Nullable Bitmap bitmap, int mask) {
        Config config = mFragment.getConfig();
        if (mask == 0 || Operator.bitAnd(config.getDynamicBackgroundMode(), mask)) {
            dispatchSetBackground(bitmap);
        }
    }

    /**
     * Smoothly sets the background.
     */
    private void dispatchSetBackground(@Nullable Bitmap bitmap) {
        if (mImageView == null) return;
        if (false) {
            if (DEBUG) Log.d(TAG, "Skipped background change: using default one.");
            return;
        }

        if (mFragment.isPowerSaveMode()) { // No animations and no background.
            mImageView.setImageBitmap(null);
        } else {
            dispatchSetBackgroundInternal(bitmap);
        }
    }

    /**
     * {@inheritDoc}
     */
    private static class DynamicBackgroundTransition extends DynamicBackground {

        private Crossfade mTransition;

        /**
         * {@inheritDoc}
         */
        DynamicBackgroundTransition(
                @NonNull AcDisplayFragment fragment,
                @Nullable ImageView imageView) {
            super(fragment, imageView);
        }

        @Override
        protected void initBackground() {
            assert mImageView != null;

            mTransition = new Crossfade();
            mTransition.addTarget(mImageView);
        }

        @Override
        protected void dispatchSetBackgroundInternal(@Nullable Bitmap bitmap) {
            assert mImageView != null;

            if (mImageView.getWidth() > 0 && mImageView.getHeight() > 0) {
                // Avoid of bug in Crossfade.java that causes 0-sized Bitmap to be
                // created.
                // https://github.com/andkulikov/transitions-everywhere/blob/master/library/src/main/java/android/transitions/everywhere/hidden/Crossfade.java
                TransitionManager.beginDelayedTransition((ViewGroup) mFragment.getView(), mTransition);
            }

            mImageView.setImageBitmap(bitmap);
        }

    }

    /**
     * {@inheritDoc}
     */
    private static class DynamicBackgroundCompat extends DynamicBackground {

        private Animator mEnterAnimation;
        private Animator mExitAnimation;

        /**
         * {@inheritDoc}
         */
        DynamicBackgroundCompat(
                @NonNull AcDisplayFragment fragment,
                @Nullable ImageView imageView) {
            super(fragment, imageView);
        }

        @Override
        protected void initBackground() {
            assert mImageView != null;
            Context context = mFragment.getActivity();

            // Setup enter animation.
            mEnterAnimation = AnimatorInflater.loadAnimator(context, R.animator.background_enter);
            mEnterAnimation.setTarget(mImageView);
            mEnterAnimation.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationStart(Animator animation) {
                    mImageView.setVisibility(View.VISIBLE);
                }

            });

            // Setup exit animation.
            mExitAnimation = AnimatorInflater.loadAnimator(context, R.animator.background_exit);
            mExitAnimation.setTarget(mImageView);
            mExitAnimation.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationEnd(Animator animation) {
                    mImageView.setVisibility(View.GONE);
                    mImageView.setImageDrawable(null);
                }

            });
        }

        @Override
        protected void dispatchSetBackgroundInternal(@Nullable Bitmap bitmap) {
            assert mImageView != null;

            if (bitmap == null) { // Clear background.
                if (mImageView.getDrawable() != null) {
                    mEnterAnimation.cancel();
                    mExitAnimation.start();
                }
            } else { // Set background.
                BitmapDrawable drawable = new BitmapDrawable(getResources(), bitmap);
                Drawable drawablePrev = mImageView.getDrawable();

                if (mExitAnimation.isRunning()) {
                    mExitAnimation.cancel();
                    mEnterAnimation.setupEndValues();
                }

                if (drawablePrev == null) {
                    mImageView.setImageDrawable(drawable);
                    mEnterAnimation.start();
                } else {
                    if (drawablePrev instanceof TransitionDrawable) {
                        TransitionDrawable d = (TransitionDrawable) drawablePrev;
                        drawablePrev = d.getDrawable(1);
                        d.resetTransition();
                    }

                    Drawable[] arrayDrawable = new Drawable[]{drawablePrev, drawable};
                    TransitionDrawable transitionDrawable = new TransitionDrawable(arrayDrawable);
                    transitionDrawable.setCrossFadeEnabled(true);
                    mImageView.setImageDrawable(transitionDrawable);

                    // Start cross-fade animation.
                    int duration = getResources().getInteger(android.R.integer.config_shortAnimTime);
                    transitionDrawable.startTransition(duration);
                }
            }
        }

        private Resources getResources() {
            return mFragment.getResources();
        }

    }

}
