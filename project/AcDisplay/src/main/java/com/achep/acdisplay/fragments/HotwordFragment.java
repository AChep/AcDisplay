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

package com.achep.acdisplay.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import com.achep.acdisplay.Build;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.activities.KeyguardActivity;
import com.achep.acdisplay.hotword.Hotword;
import com.achep.acdisplay.hotword.HotwordStorage;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Set;

/**
 * Created by achep on 23.06.14.
 */
public class HotwordFragment extends Fragment {

    public static final String TAG = "HotwordFragment";
    private static final boolean DEBUG = true && Build.DEBUG;

    private static final int MUTE_STREAM = AudioManager.STREAM_MUSIC;

    private SpeechRecognizer mSpeechRecognizer;
    private AudioManager mAudioManager;
    private Intent mRecognizerIntent;

    private boolean mAudioMuted;
    private boolean mHotwordMatched;
    private Hotword[] mHotwords;

    private RecognitionListener mSpeechListener = new RecognitionListener() {

        @Override
        public void onRmsChanged(float rmsdB) { /* unused */ }

        @Override
        public void onResults(Bundle results) {
            onPartialResults(results);
            if (!mHotwordMatched) {
                startHotwordRecognition();
            }
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
            // Let the beep beep go away, then un-mute
            unMute();
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            if (DEBUG) Log.d(TAG, "onPartialResults");

            ArrayList<String> data = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (data == null) {
                return;
            }

            // Check if the result we have match something we know
            for (String recognizedWord : data) {
                String trimmedRec = recognizedWord.trim();
                if (DEBUG) Log.d(TAG, "TrimmedRed: " + trimmedRec);

                for (Hotword hotword : mHotwords) {
                    if (hotword.speech.equalsIgnoreCase(trimmedRec)) {
                        if (DEBUG) Log.d(TAG, "Matched hotword: " + trimmedRec);

                        // Start action
                        mHotwordMatched = true;
                        try {
                            Intent intent = Intent.parseUri(hotword.action, 0);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            intent = new Intent();
                            intent.setClassName("com.google.android.googlequicksearchbox", "com.google.android.googlequicksearchbox.VoiceSearchActivity");
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startHotwordActivity(intent);
                        } catch (URISyntaxException e) {
                            Log.e(TAG, "Unable to start hotword action: " + hotword.action, e);
                        }
                        break;
                    }
                }
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) { /* unused */ }

        @Override
        public void onError(int error) {
            unMute();
            switch (error) {
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                case SpeechRecognizer.ERROR_NO_MATCH:
                    // Nothing was heard, restart
                    startHotwordRecognition();
                    break;
                default:
                    if (Build.DEBUG) Log.e(TAG, "Speech recognition error: " + error);
                    break;
            }
        }

        @Override
        public void onEndOfSpeech() { /* unused */ }

        @Override
        public void onBufferReceived(byte[] buffer) { /* unused */ }

        @Override
        public void onBeginningOfSpeech() { /* unused */ }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAudioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        loadHotwords();
    }

    @Override
    public void onResume() {
        super.onResume();
        startHotwordRecognition();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopHotwordRecognition();

        // Make sure that we are not muted
        unMute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.destroy();
        }
    }

    /**
     * Setup hotword recognition to start voice search
     */
    public void startHotwordRecognition() {
        if (DEBUG) Log.d(TAG, "Starting hotword recognition...");

        Config config = Config.getInstance();
        if (!isResumed()
                || !config.isHotwordEnabled()    /* hotword is disabled */
                || mAudioManager.isMusicActive() /* music is playing    */) {
            return;
        }

        mHotwordMatched = false;

        if (mSpeechRecognizer == null) {
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(getActivity());
            mSpeechRecognizer.setRecognitionListener(mSpeechListener);

            String model = RecognizerIntent.LANGUAGE_MODEL_FREE_FORM;
            mRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, model);
            mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.achep.acdisplay");
            mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10);
            mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        }

        mute();
        mSpeechRecognizer.startListening(mRecognizerIntent);
    }

    public void stopHotwordRecognition() {
        if (DEBUG) Log.d(TAG, "Stopping hotword recognition...");
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.cancel();
            unMute();
        }
    }

    private void loadHotwords() {
        HotwordStorage storage = HotwordStorage.getInstance(getActivity());
        Set<Hotword> set = storage.valuesSet();
        mHotwords = set.toArray(new Hotword[set.size()]);
    }

    /**
     * Unlocks device if needed and starts given intent.
     */
    private void startHotwordActivity(final Intent intent) {
        Activity activity = getActivity();
        if (activity instanceof KeyguardActivity) {

            // Make sure device is unlocked and run
            // runnable.
            KeyguardActivity keyguard = (KeyguardActivity) activity;
            keyguard.unlock(new Runnable() {
                @Override
                public void run() {
                    startHotwordActivityInternal(intent);
                }
            }, false);
        } else {
            startHotwordActivityInternal(intent);
        }
    }

    private void startHotwordActivityInternal(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Hotword action activity not found: " + intent.getAction());
        }
    }

    private void unMute() {
        if (!mAudioMuted & !(mAudioMuted = false)) return;
        mAudioManager.setStreamMute(MUTE_STREAM, false);
    }

    private void mute() {
        if (mAudioMuted & (mAudioMuted = true)) return;
        mAudioManager.setStreamMute(MUTE_STREAM, true);
    }

}
