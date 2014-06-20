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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.achep.acdisplay.AsyncTask;
import com.achep.acdisplay.Build;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.DialogHelper;
import com.achep.acdisplay.R;
import com.achep.acdisplay.utils.FileUtils;
import com.achep.acdisplay.utils.NetworkUtils;
import com.achep.acdisplay.utils.RawReader;

import java.io.File;
import java.util.Locale;

/**
 * Dialog fragment that shows FAQ.
 *
 * @author Artem Chepurnoy
 */
public class HelpDialog extends DialogFragment {

    private static final String FILE_NAME = "faq.html";
    private static final String FILE_URL = Build.Links.REPOSITORY_RAW + "src/main/res/raw-%1$s/faq.html";

    private ProgressBar mProgressBar;
    private TextView mTextView;

    private CharSequence mFaqMessage;

    private AsyncTask.DownloadText mDownloaderTask;
    private AsyncTask.DownloadText.Callback mDownloaderCallback =
            new AsyncTask.DownloadText.Callback() {
                @Override
                public void onDownloaded(String text) {
                    updateFaq(text);
                }
            };

    private long mResumedAtTime;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (NetworkUtils.isOnline(activity)) {
            // Download latest FAQ from the GitHub
            // and store to file if available.
            // TODO: Fix url to English version of FAQ
            String url = String.format(FILE_URL, Locale.getDefault().getLanguage());
            mDownloaderTask = new AsyncTask.DownloadText(mDownloaderCallback);
            mDownloaderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
        } else {
            updateFaq(null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mResumedAtTime = SystemClock.elapsedRealtime();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Make sure that user really read the Help dialog.
        long elapsedTime = SystemClock.elapsedRealtime() - mResumedAtTime;
        if (elapsedTime > getResources().getInteger(R.integer.config_maxHelpUserReadFuckyou)) {
            Config.Triggers triggers = Config.getInstance().getTriggers();
            triggers.setHelpRead(getActivity(), true, null);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        AsyncTask.stop(mDownloaderTask);
        mDownloaderTask = null;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = new DialogHelper.Builder(getActivity())
                .setIcon(R.drawable.ic_dialog_help)
                .setTitle(R.string.help)
                .setView(R.layout.fragment_help)
                .createCommonView();
        mTextView = (TextView) view.findViewById(R.id.message);
        mTextView.setVisibility(View.GONE);
        mTextView.setMovementMethod(new LinkMovementMethod());
        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.VISIBLE);

        if (mFaqMessage != null) {
            populateFaq();
        }

        return new AlertDialog.Builder(getActivity())
                .setNegativeButton(R.string.close, null)
                .setView(view)
                .create();
    }

    /**
     * @param text latest FAQ to update from, or {@code null} to keep old.
     */
    private void updateFaq(String text) {
        if (text != null) {
            // Save text to file on internal storage
            // to keep latest FAQ offline.
            FileUtils.writeToFile(getFile(), text);
        } else {
            text = readFromFile();
            if (text == null) text = readFromRaw();
        }

        mFaqMessage = Html.fromHtml(text);
        populateFaq();
    }

    /**
     * Applies current {@link #mFaqMessage FAQ message} to views, or does nothing
     * if {@link #mTextView views} are not created yet.
     */
    private void populateFaq() {
        if (mTextView != null) {
            mTextView.setText(mFaqMessage);
            mTextView.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
        }
    }

    /**
     * @return Built-in FAQ, that may be not up-to-date.
     * @see #readFromFile()
     */
    private String readFromRaw() {
        return RawReader.readTextFileFromRawResource(getActivity(), R.raw.faq);
    }

    /**
     * @return Previously downloaded FAQ, that may be not up-to-date.
     * @see #readFromRaw()
     */
    private String readFromFile() {
        return FileUtils.readTextFile(getFile());
    }

    private File getFile() {
        return new File(getActivity().getFilesDir(), FILE_NAME);
    }

}
