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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;

import com.achep.acdisplay.Build;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.Device;
import com.achep.acdisplay.DialogHelper;
import com.achep.acdisplay.R;
import com.achep.acdisplay.providers.LogAttachmentProvider;
import com.achep.acdisplay.utils.FileUtils;
import com.achep.acdisplay.utils.IntentUtils;
import com.achep.acdisplay.utils.PackageUtils;
import com.achep.acdisplay.utils.ToastUtils;
import com.achep.acdisplay.utils.ViewUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Feedback dialog fragment.
 * <p/>
 * Provides an UI for sending bugs & suggestions on my email.
 */
public class FeedbackDialog extends DialogFragment implements Config.OnConfigChangedListener {

    private static final String TAG = "FeedbackDialog";

    private View mFaqContainer;

    private Spinner mSpinner;
    private EditText mEditText;
    private CheckBox mAttachLogCheckBox;

    private AdapterView.OnItemSelectedListener mListener =
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    // Show "Attach log" checkbox only if the type
                    // of this message is "Bug".
                    ViewUtils.setVisible(mAttachLogCheckBox, position == 0);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    ViewUtils.setVisible(mAttachLogCheckBox, false);
                }
            };

    @Override
    public void onResume() {
        super.onResume();

        Config config = Config.getInstance();
        Config.Triggers triggers = config.getTriggers();

        config.registerListener(this);
        updateFaqPanel(triggers.isHelpRead());
    }

    @Override
    public void onPause() {
        super.onPause();

        Config config = Config.getInstance();
        config.unregisterListener(this);
    }

    @Override
    public void onConfigChanged(Config config, String key, Object value) {
        switch (key) {
            case Config.KEY_TRIG_HELP_READ:
                updateFaqPanel((boolean) value);
                break;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        assert activity != null;

        View view = new DialogHelper.Builder(activity)
                .setIcon(getResources().getDrawable(R.drawable.ic_dialog_mail))
                .setTitle(getString(R.string.feedback))
                .setView(R.layout.fragment_dialog_feedback)
                .createSkeletonView();
        final AlertDialog alertDialog = new AlertDialog.Builder(activity)
                .setView(view)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.feedback_send, null)
                .create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {
                Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                assert button != null;
                button.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        send();
                    }
                });
            }
        });

        mSpinner = (Spinner) view.findViewById(R.id.type);
        mSpinner.setOnItemSelectedListener(mListener);
        mEditText = (EditText) view.findViewById(R.id.message);
        mAttachLogCheckBox = (CheckBox) view.findViewById(R.id.checkbox);
        mAttachLogCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    return;
                }

                CharSequence messageText = getString(R.string.feedback_attach_log_description);
                new DialogHelper.Builder(getActivity())
                        .setMessage(messageText)
                        .wrap()
                        .setPositiveButton(android.R.string.ok, null)
                        .create()
                        .show();
            }
        });

        // Frequently asked questions panel
        Config.Triggers triggers = Config.getInstance().getTriggers();
        if (!triggers.isHelpRead()) initFaqPanel((ViewGroup) view);

        return alertDialog;
    }

    /**
     * Initialize Frequently asked questions panel. This panel is here to reduce
     * the number of already answered questions.
     */
    private void initFaqPanel(ViewGroup root) {
        mFaqContainer = ((ViewStub) root.findViewById(R.id.faq)).inflate();
        mFaqContainer.findViewById(R.id.faq).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogHelper.showHelpDialog(getActivity());
            }
        });
    }

    /**
     * Removes Frequently asked questions panel from the view
     * and sets {@link #mFaqContainer} to null.
     * After calling this method you no longer able to get panel back.
     */
    private void recycleFaqPanel() {
        ViewGroup viewGroup = (ViewGroup) mFaqContainer.getParent();
        viewGroup.removeView(mFaqContainer);
        mFaqContainer = null;
    }

    /**
     * {@link #recycleFaqPanel() Recycles} Frequently asked questions panel when it's not needed
     * anymore.
     *
     * @param isHelpRead {@code true} to recycle panel, {@code false} to do nothing.
     */
    private void updateFaqPanel(boolean isHelpRead) {
        if (mFaqContainer != null && isHelpRead) {
            recycleFaqPanel();
        }
    }

    private void send() {
        Activity context = getActivity();
        CharSequence message = mEditText.getText();

        // Check for message's length
        int msgMinLength = getResources().getInteger(R.integer.config_feedback_minMessageLength);
        if (message == null || (message.length() < msgMinLength && !Build.DEBUG)) {
            String toastText = getString(R.string.feedback_error_msg_too_short, msgMinLength);
            ToastUtils.showShort(context, toastText);
            return;
        }

        CharSequence title = createTitleMessage(context, mSpinner.getSelectedItemPosition());
        CharSequence body = createBodyMessage(context, message);
        Intent intent = new Intent()
                .putExtra(Intent.EXTRA_EMAIL, new String[]{Build.SUPPORT_EMAIL})
                .putExtra(Intent.EXTRA_SUBJECT, title)
                .putExtra(Intent.EXTRA_TEXT, body);

        if (mAttachLogCheckBox.getVisibility() == View.VISIBLE && mAttachLogCheckBox.isChecked()) {
            attachLog(intent);
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("message/rfc822");
        } else {
            intent.setAction(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:")); // only email apps should handle it
        }

        if (IntentUtils.hasActivityForThat(context, intent)) {
            startActivity(intent);
            dismiss();
        } else {
            String toastText = getString(R.string.feedback_error_no_app);
            ToastUtils.showLong(context, toastText);
        }
    }

    private CharSequence createTitleMessage(Context context, int type) {
        CharSequence osVersion = Device.hasKitKatApi() ? "KK" : Device.hasJellyBeanMR2Api() ? "JB" : "WTF";
        CharSequence[] typeNames = new CharSequence[]{"bug", "suggestion", "other"};
        return AboutDialog.getVersionName(context) + ": " + osVersion + ", " + typeNames[type];
    }

    private CharSequence createBodyMessage(Context context, CharSequence msg) {
        PackageInfo pi;
        try {
            pi = context
                    .getPackageManager()
                    .getPackageInfo(PackageUtils.getName(context), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.wtf(TAG, "Failed to find my own PackageInfo.");
            return msg;
        }

        return "" + msg +
                '\n' +
                '\n' +
                "- - - - - - - - - - device details - - - - - - - - -" + '\n' +
                "app_version:" + pi.versionName + '(' + pi.versionCode + ")\n" +
                "android_version:" + android.os.Build.VERSION.RELEASE + '(' + android.os.Build.VERSION.SDK_INT + ")\n" +
                "build_display:" + android.os.Build.DISPLAY + '\n' +
                "build_brand:" + android.os.Build.BRAND + '\n' +
                "build_model:" + android.os.Build.MODEL + '\n' +
                "language:" + Locale.getDefault().getLanguage();
    }

    // TODO: If root is available, get normal (system events included) logcat.
    private void attachLog(Intent intent) {
        Context context = getActivity();
        StringBuilder log = new StringBuilder();

        try {
            // Read logs from runtime
            String[] logcatCmd = new String[]{"logcat", "-v", "threadtime", "-d"};
            Process process = Runtime.getRuntime().exec(logcatCmd);
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                log.append(line);
                log.append('\n');
            }

            // Write everything to a file
            File cacheDir = context.getCacheDir();
            if (cacheDir == null) {
                throw new IOException("Cache directory inaccessible");
            }
            File logsDir = new File(cacheDir, LogAttachmentProvider.DIRECTORY);
            FileUtils.deleteRecursive(logsDir);
            logsDir.mkdirs();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String fileName = "AcDisplay_log_" + sdf.format(new Date()) + ".txt";
            File logFile = new File(logsDir, fileName);

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(logFile)));
            writer.write(log.toString());
            writer.close();

            // Put extra stream to intent
            Uri uri = Uri.parse("content://" + LogAttachmentProvider.AUTHORITY + "/" + fileName);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
        } catch (IOException e) {
            ToastUtils.showShort(context, getString(R.string.feedback_error_accessing_log));
        }
    }
}
