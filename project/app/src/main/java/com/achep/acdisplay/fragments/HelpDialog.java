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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.achep.acdisplay.AsyncTask;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.DialogHelper;
import com.achep.acdisplay.R;
import com.achep.acdisplay.RepositoryUrlBuilder;
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
public class HelpDialog extends DialogFragment implements AsyncTask.DownloadText.Callback {

    private static final String FILE_NAME = "faq-%1$s.html";
    private static final String FILE_URL = new RepositoryUrlBuilder()
            .changeDirectory(RepositoryUrlBuilder.ACDISPLAY_PROJECT_NAME)
            .changeDirectory(RepositoryUrlBuilder.ACDISPLAY_MODULE_NAME)
            .changeDirectory("src")
            .changeDirectory("releaseFlavor")
            .changeDirectory("res")
            .changeDirectory("raw-%1$s")
            .setFile("faq.html")
            .setRawAccess(true)
            .build();

    private ProgressBar mProgressBar;
    private TextView mTextView;

    private CharSequence mFaqLocaleSuffix;
    private CharSequence mFaqMessage;
    private AsyncTask.DownloadText mDownloaderTask;

    private long mResumedAtTime;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        Locale locale = Locale.getDefault();
        mFaqLocaleSuffix = locale.getLanguage();

        if (!NetworkUtils.isOnline(activity)) {
            setFaq(null);
            return;
        }

        // Download latest FAQ from the GitHub
        // and store to file if available.

        String localeSuffixRegional = mFaqLocaleSuffix.toString();

        // Try with regional locales if available.
        String localeCountry = locale.getCountry();
        if (!TextUtils.isEmpty(localeCountry)) {
            localeSuffixRegional += "-r" + localeCountry;
        }

        mDownloaderTask = new AsyncTask.DownloadText(this);
        mDownloaderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                String.format(FILE_URL, localeSuffixRegional),
                String.format(FILE_URL, mFaqLocaleSuffix));
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

    @Override
    public void onDownloaded(@NonNull String[] texts) {
        final String faqHtml = texts[0] != null ? texts[0] : texts[1];
        if (faqHtml != null) {

            // Save text to file on internal storage
            // to keep latest FAQ offline.
            writeToFile(faqHtml);
        }

        setFaq(faqHtml);
    }

    /**
     * @param text latest FAQ to update from, or {@code null} to keep old.
     */
    private void setFaq(@Nullable String text) {
        if (text == null) text = readFromFile();
        if (text == null) text = readFromRaw();
        mFaqMessage = Html.fromHtml(text);
        populateFaq(); // update views
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
     * @return Built-in FAQ
     * @see #readFromFile()
     */
    @Nullable
    private String readFromRaw() {
        return RawReader.readTextFileFromRawResource(getActivity(), R.raw.faq);
    }

    /**
     * @return Previously saved FAQ, that may be not up-to-date.
     * @see #readFromRaw()
     * @see #writeToFile(String)
     * @see #mFaqLocaleSuffix
     */
    @Nullable
    private String readFromFile() {
        return FileUtils.readTextFile(getFile(mFaqLocaleSuffix));
    }

    /**
     * Saves FAQ to the {@link #getFile(CharSequence) file} on internal storage.
     *
     * @param faqHtml text to save.
     * @see #readFromFile()
     * @see #mFaqLocaleSuffix
     */
    private void writeToFile(@NonNull String faqHtml) {
        FileUtils.writeToFile(getFile(mFaqLocaleSuffix), faqHtml);
    }

    @NonNull
    private File getFile(@NonNull CharSequence suffix) {
        String filename = String.format(FILE_NAME, suffix);
        return new File(getActivity().getFilesDir(), filename);
    }

}
