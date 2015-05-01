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

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.graphics.Palette;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.achep.acdisplay.Atomic;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.acdisplay.graphics.BackgroundFactory;
import com.achep.acdisplay.services.media.MediaController2;
import com.achep.acdisplay.services.media.Metadata;
import com.achep.acdisplay.ui.fragments.AcDisplayFragment;
import com.achep.base.Device;
import com.achep.base.tests.Check;
import com.achep.base.ui.drawables.PlayPauseDrawable;
import com.achep.base.ui.drawables.RippleDrawable2;
import com.achep.base.utils.Operator;
import com.achep.base.utils.ResUtils;
import com.achep.base.utils.RippleUtils;
import com.achep.base.utils.ViewUtils;

import static com.achep.base.Build.DEBUG;

/**
 * Media widget for {@link com.achep.acdisplay.ui.fragments.AcDisplayFragment} that provides
 * basic media controls and has a nice skin.
 *
 * @author Artem Chepurnoy
 */
public class MediaWidget extends Widget implements
        MediaController2.MediaListener,
        View.OnClickListener,
        View.OnLongClickListener, SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "MediaWidget";

    private final MediaController2 mMediaController;
    private final PlayPauseDrawable mPlayPauseDrawable;
    private final Drawable mWarningDrawable;

    private ImageView mArtworkView;
    private TextView mTitleView;
    private TextView mSubtitleView;
    private ImageButton mButtonPrevious;
    private ImageButton mButtonPlayPause;
    private ImageButton mButtonNext;
    // Seek
    private ViewGroup mSeekLayout;
    private TextView mPositionText;
    private TextView mDurationText;
    private SeekBar mSeekBar;

    private boolean mIdle;

    private int mArtworkColor = Color.WHITE;
    private Bitmap mArtwork;
    private Bitmap mArtworkBackground;
    private AsyncTask<Bitmap, Void, Palette> mPaletteWorker;
    private AsyncTask<Void, Void, Bitmap> mBackgroundWorker;

    private final Palette.PaletteAsyncListener mPaletteCallback =
            new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(@NonNull Palette palette) {
                    mArtworkColor = palette.getVibrantColor(Color.WHITE);
                    updatePlayPauseButtonColor(mArtworkColor);
                    updateSeekBarColor(mArtworkColor);
                }
            };

    private final BackgroundFactory.BackgroundAsyncListener mBackgroundCallback =
            new BackgroundFactory.BackgroundAsyncListener() {
                @Override
                public void onGenerated(@NonNull Bitmap bitmap) {
                    mArtworkBackground = bitmap;
                    populateBackground();
                }
            };

    private final Atomic.Callback mSeekAtomicCallback = new Atomic.Callback() {

        private static final int REFRESH_RATE = 1000; // ms.

        @NonNull
        private final Handler mHandler = new Handler();
        @NonNull
        private final Runnable mRunnable = new Runnable() {
            @Override
            public void run() {
                if (mSeekBarTracking) {
                    // FIXME: Get rid of this workaround by implementing the states of
                    // AcDisplay fragment.
                    mCallback.requestTimeoutRestart(MediaWidget.this);
                } else {
                    // Update the seek bar.
                    long position = mMediaController.getPlaybackPosition();
                    long duration = mMediaController.getMetadata().duration;
                    Check.getInstance().isTrue(duration > 0);
                    float ratio = (float) ((double) position / duration);
                    float progress = mSeekBar.getMax() * ratio;
                    mSeekBar.setProgress(Math.round(progress));
                    // Update the playback position text.
                    mPositionText.setText(formatTime(position));
                }
                // Refresh schedule.
                if (mSeekUiAtomic.isRunning()) {
                    if (DEBUG) Log.d(TAG, "Seek bar refresh tick.");
                    mHandler.postDelayed(this, REFRESH_RATE);
                }
            }
        };

        @Override
        public void onStart(Object... objects) {
            mHandler.post(mRunnable);
            mSeekLayout.setVisibility(View.VISIBLE);
        }

        @Override
        public void onStop(Object... objects) {
            mHandler.removeCallbacks(mRunnable);
            mSeekLayout.setVisibility(View.GONE);
            // Workaround for a bug with the transition manager,
            // which causes seek layout to be semi-transparent,
            // but not gone.
            mSeekLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mSeekLayout.setVisibility(View.VISIBLE);
                    mSeekLayout.setVisibility(View.GONE);
                    mSeekLayout.postInvalidate();
                }
            }, 80);
        }
    };

    private final Atomic mSeekUiAtomic = new Atomic(mSeekAtomicCallback, "MediaWidget:SeekBar");
    private boolean mSeekBarTracking;

    public MediaWidget(@NonNull Callback callback, @NonNull AcDisplayFragment fragment) {
        super(callback, fragment);
        mMediaController = fragment.getMediaController2();

        Resources res = fragment.getResources();
        mPlayPauseDrawable = new PlayPauseDrawable();
        mPlayPauseDrawable.setSize(res.getDimensionPixelSize(R.dimen.media_btn_actual_size));
        mWarningDrawable = ResUtils.getDrawable(fragment.getActivity(), R.drawable.ic_action_warning_white);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isHomeWidget() {
        return true;
    }

    @Override
    public void onViewAttached() {
        super.onViewAttached();
        mIdle = false;
        mMediaController.registerListener(this);
        onMetadataChanged(mMediaController.getMetadata());
        onPlaybackStateChanged(mMediaController.getPlaybackState());
        mIdle = true;
    }

    @Override
    public void onViewDetached() {
        mMediaController.unregisterListener(this);
        mSeekUiAtomic.stop();
        super.onViewDetached();
    }

    @Override
    public void onStop() {
        mSeekUiAtomic.stop();
        super.onStop();
    }

    @Override
    public void onMetadataChanged(@NonNull Metadata metadata) {
        populateMetadata();
        final Bitmap bitmap = metadata.bitmap;

        // Check if artwork are equals. If so, then we don't need to
        // generate everything from the beginning.
        if (mArtwork == bitmap || mArtwork != null && mArtwork.sameAs(bitmap)) {
            return;
        }

        mArtwork = bitmap;
        mArtworkBackground = null;

        com.achep.base.async.AsyncTask.stop(mPaletteWorker);
        com.achep.base.async.AsyncTask.stop(mBackgroundWorker);
        updatePlayPauseButtonColor(Color.WHITE); // Reset color
        updateSeekBarColor(Color.WHITE); // Reset color

        if (bitmap != null) {
            // TODO: Load the vibrant color only.
            mArtworkColor = Color.WHITE;
            mPaletteWorker = new Palette.Builder(bitmap)
                    .maximumColorCount(16)
                    .generate(mPaletteCallback);

            int dynamicBgMode = getConfig().getDynamicBackgroundMode();
            if (Operator.bitAnd(dynamicBgMode, getBackgroundMask())) {
                mBackgroundWorker = BackgroundFactory.generateAsync(bitmap, mBackgroundCallback);
                return; // Do not reset the background.
            }
        } else {
            mPaletteWorker = null;
        }

        mBackgroundWorker = null;
        populateBackground();
    }

    @Override
    public void onPlaybackStateChanged(int state) {

        // Making transformation rule for the warning icon is too
        // much overkill for me.
        if (state == PlaybackStateCompat.STATE_ERROR) {
            mButtonPlayPause.setImageDrawable(mWarningDrawable);
        } else {
            mButtonPlayPause.setImageDrawable(mPlayPauseDrawable);
        }

        if (DEBUG) Log.d(TAG, "Playback state is " + state);

        final int imageDescId;
        switch (state) {
            case PlaybackStateCompat.STATE_ERROR:
                imageDescId = R.string.media_play_description;
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                mPlayPauseDrawable.transformToPause();
                imageDescId = R.string.media_pause_description;
                break;
            case PlaybackStateCompat.STATE_BUFFERING:
            case PlaybackStateCompat.STATE_STOPPED:
                mPlayPauseDrawable.transformToStop();
                imageDescId = R.string.media_stop_description;
                break;
            case PlaybackStateCompat.STATE_PAUSED:
            default:
                mPlayPauseDrawable.transformToPlay();
                imageDescId = R.string.media_play_description;
                break;
        }

        mButtonPlayPause.setContentDescription(getFragment().getString(imageDescId));
    }

    /**
     * Updates the content of the view to latest metadata
     * provided by {@link com.achep.acdisplay.services.media.MediaController2#getMetadata()}.
     */
    private void populateMetadata() {
        if (mIdle) {
            ViewGroup vg = getView();
            if (Device.hasKitKatApi() && vg.isLaidOut() && getFragment().isAnimatable()) {
                TransitionManager.beginDelayedTransition(vg);
            }
        }

        Metadata metadata = mMediaController.getMetadata();
        ViewUtils.safelySetText(mTitleView, metadata.title);
        ViewUtils.safelySetText(mSubtitleView, metadata.subtitle);
        mDurationText.setText(formatTime(metadata.duration));
        mSeekUiAtomic.stop();
        mSeekBar.setMax(Math.min(100, (int) (metadata.duration / 1000L)));

        if (mArtworkView != null) {
            mArtworkView.setImageBitmap(metadata.bitmap);
        }
    }

    /**
     * Requests host to update dynamic background.
     *
     * @see #getBackground()
     * @see #getBackgroundMask()
     */
    private void populateBackground() {
        if (isViewAttached()) {
            mCallback.requestBackgroundUpdate(this);
        }
    }

    @SuppressLint("NewApi")
    private void updatePlayPauseButtonColor(int color) {
        if (Device.hasLollipopApi()) {
            RippleDrawable2 rippleDrawable = (RippleDrawable2) mButtonPlayPause.getBackground();
            rippleDrawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        } else {
            RippleUtils.makeFor(ColorStateList.valueOf(color), false, mButtonPlayPause);
        }
    }

    private void updateSeekBarColor(int color) {
        mSeekBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public Bitmap getBackground() {
        return mArtworkBackground == null
                ? mArtwork
                : mArtworkBackground;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBackgroundMask() {
        return Config.DYNAMIC_BG_ARTWORK_MASK;
    }

    @Override
    protected ViewGroup onCreateView(
            @NonNull LayoutInflater inflater,
            @NonNull ViewGroup container,
            @Nullable ViewGroup sceneView) {
        boolean initialize = sceneView == null;
        if (initialize) {
            sceneView = (ViewGroup) inflater.inflate(R.layout.acdisplay_scene_music, container, false);
            assert sceneView != null;
        }

        mArtworkView = (ImageView) sceneView.findViewById(R.id.artwork);
        ViewGroup infoLayout = (ViewGroup) sceneView.findViewById(R.id.metadata);
        mTitleView = (TextView) infoLayout.findViewById(R.id.media_title);
        mSubtitleView = (TextView) infoLayout.findViewById(R.id.media_subtitle);
        mButtonPrevious = (ImageButton) sceneView.findViewById(R.id.previous);
        mButtonPlayPause = (ImageButton) sceneView.findViewById(R.id.play);
        mButtonNext = (ImageButton) sceneView.findViewById(R.id.next);
        mSeekLayout = (ViewGroup) sceneView.findViewById(R.id.seek_layout);
        mSeekBar = (SeekBar) mSeekLayout.findViewById(R.id.seek_bar);
        mPositionText = (TextView) mSeekLayout.findViewById(R.id.playback_position);
        mDurationText = (TextView) mSeekLayout.findViewById(R.id.duration);

        if (!initialize) {
            return sceneView;
        }

        mSeekBar.setOnSeekBarChangeListener(this);
        mButtonPrevious.setOnClickListener(this);
        mButtonPlayPause.setImageDrawable(mPlayPauseDrawable);
        mButtonPlayPause.setOnClickListener(this);
        mButtonPlayPause.setOnLongClickListener(this);
        mButtonNext.setOnClickListener(this);

        // Show the seek-panel on long click.
        infoLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Don't allow seeking on a weird song.
                if (mMediaController.getMetadata().duration <= 0
                        || mMediaController.getPlaybackPosition() < 0) {
                    if (mSeekUiAtomic.isRunning()) {
                        toggleSeekUiVisibility();
                        return true;
                    }
                    return false;
                }

                toggleSeekUiVisibility();
                return true;
            }

            private void toggleSeekUiVisibility() {
                ViewGroup vg = getView();
                if (Device.hasKitKatApi() && vg.isLaidOut() && getFragment().isAnimatable()) {
                    TransitionManager.beginDelayedTransition(vg);
                }
                mSeekUiAtomic.react(!mSeekUiAtomic.isRunning());
                mCallback.requestTimeoutRestart(MediaWidget.this);
            }
        });

        if (Device.hasLollipopApi()) {
            // FIXME: Ripple doesn't work if the background is set (masked ripple works fine, but ugly).
            // Apply our own ripple drawable with slightly extended abilities, such
            // as setting color filter.
            ColorStateList csl = container.getResources().getColorStateList(R.color.ripple_dark);
            mButtonPlayPause.setBackground(new RippleDrawable2(csl, null, null));
        } else {
            RippleUtils.makeFor(false, true,
                    mButtonNext,
                    mButtonPlayPause,
                    mButtonPrevious);
        }

        updatePlayPauseButtonColor(mArtworkColor);
        updateSeekBarColor(mArtworkColor);

        return sceneView;
    }

    @Override
    public void onClick(@NonNull View v) {
        int action;
        if (v == mButtonPrevious) {
            action = MediaController2.ACTION_SKIP_TO_PREVIOUS;
        } else if (v == mButtonPlayPause) {
            action = MediaController2.ACTION_PLAY_PAUSE;
        } else if (v == mButtonNext) {
            action = MediaController2.ACTION_SKIP_TO_NEXT;
        } else {
            Log.wtf(TAG, "Received click event from an unknown view.");
            return;
        }

        mMediaController.sendMediaAction(action);
        mCallback.requestTimeoutRestart(this);
    }

    @Override
    public boolean onLongClick(@NonNull View v) {
        if (v == mButtonPlayPause) {
            mMediaController.sendMediaAction(MediaController2.ACTION_STOP);
        } else {
            Log.wtf(TAG, "Received long-click event from an unknown view.");
            return false;
        }

        mCallback.requestTimeoutRestart(this);
        return true;
    }

    //-- SEEKING SONGS --------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            final long position = getPlaybackSeekPosition();
            mPositionText.setText(formatTime(position));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mSeekBarTracking = true;
        mCallback.requestTimeoutRestart(MediaWidget.this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mSeekBarTracking) {
            final long position = getPlaybackSeekPosition();
            if (DEBUG) Log.d(TAG, "Seeking to " + position + " of "
                    + mMediaController.getMetadata().duration);
            mMediaController.seekTo(position);
        }
        mSeekBarTracking = false;
    }

    private long getPlaybackSeekPosition() {
        double pos = mSeekBar.getProgress();
        double max = mSeekBar.getMax();
        double ratio = pos / max;
        long duration = mMediaController.getMetadata().duration;
        return (long) Math.ceil(duration * ratio);
    }

    @NonNull
    private String formatTime(long time) {
        time /= 1000L; // get rid of millis.
        int s = (int) (time % 60L);
        int m = (int) (time / 60L);
        return formatNumber(m) + ":" + formatNumber(s);
    }

    @NonNull
    private String formatNumber(int a) {
        String str = Integer.toString(a);
        return a > 9 ? str : "0" + str;
    }

}
