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

import android.content.Context;
import android.graphics.Bitmap;
import android.media.RemoteControlClient;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.achep.acdisplay.AsyncTask;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.acdisplay.acdisplay.AcDisplayFragment2;
import com.achep.acdisplay.acdisplay.BackgroundFactoryThread;
import com.achep.acdisplay.services.media.MediaController;
import com.achep.acdisplay.services.media.Metadata;
import com.achep.acdisplay.utils.ViewUtils;

/**
 * Created by Artem on 02.04.2014.
 */
public class MediaWidget extends Widget implements
        MediaController.MediaListener,
        View.OnClickListener,
        View.OnLongClickListener {

    private final MediaController mMediaController;

    private ImageView mArtwork;
    private TextView mTrack;
    private TextView mArtist;
    private ImageButton mButtonPrevious;
    private ImageButton mButtonPlayPause;
    private ImageButton mButtonNext;

    private Bitmap mArtworkOrigin;
    private Bitmap mArtworkBlurred;

    private BackgroundFactoryThread mBlurThread;
    private final BackgroundFactoryThread.Callback mBlurThreadCallback = new BackgroundFactoryThread.Callback() {
        @Override
        public void onBackgroundCreated(Bitmap bitmap) {
            mArtworkBlurred = bitmap;
            populateBackground();
        }
    };

    public MediaWidget(@NonNull Callback callback, @NonNull AcDisplayFragment2 fragment) {
        super(callback, fragment);
        mMediaController = fragment.getMediaController();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mMediaController.registerListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMediaController.unregisterListener(this);
    }

    @Override
    public void onMediaChanged(@NonNull MediaController controller, int event) {
        switch (event) {
            case MediaController.EVENT_METADATA_CHANGED:
                updateMetadata(controller.getMetadata());
                break;
            case MediaController.EVENT_PLAYSTATE_CHANGED:
                updatePlayState(controller.getPlayState());
                break;
        }
    }

    private void updateMetadata(@NonNull Metadata metadata) {
        if (mArtworkOrigin == null || !mArtworkOrigin.sameAs(metadata.bitmap)) {
            mArtworkOrigin = metadata.bitmap;

            AsyncTask.stop(mBlurThread);
            if (metadata.bitmap != null) {
                Context context = getHostFragment().getActivity();
                mBlurThread = new BackgroundFactoryThread(context, metadata.bitmap, mBlurThreadCallback);
                mBlurThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                mArtworkBlurred = null;
                populateBackground();
            }
        }

        populateMetadata();
    }

    private void updatePlayState(int currentPlayState) {
        final int imageResId;
        final int imageDescId;
        switch (currentPlayState) {
            case RemoteControlClient.PLAYSTATE_ERROR:
                imageResId = android.R.drawable.stat_sys_warning;
                imageDescId = R.string.media_play_description;
                break;
            case RemoteControlClient.PLAYSTATE_PLAYING:
                imageResId = R.drawable.ic_media_pause;
                imageDescId = R.string.media_pause_description;
                break;
            case RemoteControlClient.PLAYSTATE_BUFFERING:
                imageResId = R.drawable.ic_media_stop;
                imageDescId = R.string.media_stop_description;
                break;
            case RemoteControlClient.PLAYSTATE_PAUSED:
            default:
                imageResId = R.drawable.ic_media_play;
                imageDescId = R.string.media_play_description;
                break;
        }

        mButtonPlayPause.setImageResource(imageResId);
        mButtonPlayPause.setContentDescription(getHostFragment().getString(imageDescId));
    }

    /**
     * Updates the content of the view to latest metadata
     * provided by {@link com.achep.acdisplay.services.media.MediaController#getMetadata()}.
     */
    private void populateMetadata() {
        Metadata metadata = mMediaController.getMetadata();
        ViewUtils.safelySetText(mTrack, metadata.trackTitle);
        ViewUtils.safelySetText(mArtist, metadata.artist);

        if (mArtwork != null) {
            mArtwork.setImageBitmap(metadata.bitmap);
        }
    }

    /**
     * Requests host to update dynamic background.
     *
     * @see #getBackground()
     * @see #getBackgroundMask()
     */
    private void populateBackground() {
        mCallback.requestBackgroundUpdate(this);
    }

    @Nullable
    @Override
    public Bitmap getBackground() {
        return mArtworkBlurred;
    }

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

        mArtwork = (ImageView) sceneView.findViewById(R.id.artwork);
        mTrack = (TextView) sceneView.findViewById(R.id.track);
        mArtist = (TextView) sceneView.findViewById(R.id.artist_album);
        mButtonPrevious = (ImageButton) sceneView.findViewById(R.id.previous);
        mButtonPlayPause = (ImageButton) sceneView.findViewById(R.id.play);
        mButtonNext = (ImageButton) sceneView.findViewById(R.id.next);

        if (!initialize) {
            return sceneView;
        }

        mButtonPrevious.setOnClickListener(this);
        mButtonPlayPause.setOnClickListener(this);
        mButtonPlayPause.setOnLongClickListener(this);
        mButtonNext.setOnClickListener(this);

        return sceneView;
    }

    @Override
    public void onClick(@NonNull View v) {
        int keyCode;
        if (v == mButtonPrevious) {
            keyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS;
        } else if (v == mButtonPlayPause) {
            keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
        } else if (v == mButtonNext) {
            keyCode = KeyEvent.KEYCODE_MEDIA_NEXT;
        } else {
            return;
        }

        mMediaController.sendMediaButtonClick(keyCode);
        mCallback.requestTimeoutRestart(this);
    }

    @Override
    public boolean onLongClick(@NonNull View v) {
        if (v == mButtonPlayPause) {
            mMediaController.sendMediaButtonClick(KeyEvent.KEYCODE_MEDIA_STOP);
        } else {
            return false;
        }
        mCallback.requestTimeoutRestart(this);
        return true;
    }
}
