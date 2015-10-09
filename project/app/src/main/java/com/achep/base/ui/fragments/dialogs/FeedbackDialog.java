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
package com.achep.base.ui.fragments.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.acdisplay.providers.LogAttachmentProvider;
import com.achep.acdisplay.ui.DialogHelper;
import com.achep.base.Build;
import com.achep.base.Device;
import com.achep.base.content.ConfigBase;
import com.achep.base.providers.LogsProviderBase;
import com.achep.base.utils.FileUtils;
import com.achep.base.utils.IntentUtils;
import com.achep.base.utils.PackageUtils;
import com.achep.base.utils.ResUtils;
import com.achep.base.utils.ToastUtils;
import com.achep.base.utils.ViewUtils;
import com.achep.base.utils.logcat.Logcat;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static com.achep.base.Build.DEBUG;

/**
 * Feedback dialog fragment.
 * <p/>
 * Provides an UI for sending bugs & suggestions on my email.
 */
public class FeedbackDialog extends DialogFragment implements ConfigBase.OnConfigChangedListener {

    private View mFaqContainer;

    private Spinner mSpinner;
    private EditText mEditText;
    private CheckBox mAttachLogCheckBox;

    private final AdapterView.OnItemSelectedListener mListener =
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    // Show "Attach log" checkbox only if the type
                    // of this message is "Issue".
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
        config.registerListener(this);

        updateFaqPanel(config.getTriggers().isHelpRead());
    }

    @Override
    public void onPause() {
        Config config = Config.getInstance();
        config.unregisterListener(this);

        super.onPause();
    }

    @Override
    public void onConfigChanged(@NonNull ConfigBase config,
                                @NonNull String key,
                                @NonNull Object value) {
        switch (key) {
            case Config.KEY_TRIG_HELP_READ:
                boolean read = (boolean) value;
                updateFaqPanel(read);
                break;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        assert activity != null;

        MaterialDialog md = new MaterialDialog.Builder(activity)
                .iconRes(R.drawable.ic_email_white_24dp)
                .title(R.string.feedback_dialog_title)
                .customView(R.layout.feedback_dialog, true)
                .negativeText(android.R.string.cancel)
                .positiveText(R.string.send)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog,
                                        @NonNull DialogAction dialogAction) {
                        Context context = getActivity();
                        CharSequence message = mEditText.getText();

                        if (isMessageLongEnough(message)) {
                            boolean attachLog = mAttachLogCheckBox.isChecked()
                                    && mAttachLogCheckBox.getVisibility() == View.VISIBLE;

                            int type = mSpinner.getSelectedItemPosition();
                            CharSequence title = createTitle(context, type);
                            CharSequence body = createBody(context, message);
                            send(title, body, attachLog);
                        } else {
                            String toastText = getString(
                                    R.string.feedback_error_msg_too_short,
                                    getMinMessageLength());
                            ToastUtils.showShort(context, toastText);
                        }
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog,
                                        @NonNull DialogAction dialogAction) {
                        dismiss();
                    }
                })
                .autoDismiss(false)
                .build();

        View view = md.getCustomView();
        assert view != null;
        mSpinner = (Spinner) view.findViewById(R.id.type);
        mSpinner.setOnItemSelectedListener(mListener);
        mEditText = (EditText) view.findViewById(R.id.message);
        mAttachLogCheckBox = (CheckBox) view.findViewById(R.id.checkbox);

        // Frequently asked questions panel
        Config.Triggers triggers = Config.getInstance().getTriggers();
        if (!triggers.isHelpRead()) initFaqPanel((ViewGroup) view);

        return md;
    }

    /**
     * Initialize Frequently asked questions panel. This panel is here to reduce
     * the number of already answered questions.
     */
    private void initFaqPanel(@NonNull ViewGroup root) {
        mFaqContainer = ((ViewStub) root.findViewById(R.id.faq)).inflate();
        mFaqContainer.findViewById(R.id.faq).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppCompatActivity activity = (AppCompatActivity) getActivity();
                DialogHelper.showHelpDialog(activity);
            }
        });
    }

    /**
     * Removes Frequently asked questions panel from the view
     * and sets {@link #mFaqContainer} to null.
     * After calling this method you no longer able to get panel back.
     */
    private void recycleFaqPanel() {
        ViewUtils.removeViewParent(mFaqContainer);
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

    private void send(@NonNull CharSequence title,
                      @NonNull CharSequence body, boolean attachLog) {
        Activity context = getActivity();
        String[] recipients = {Build.SUPPORT_EMAIL};
        Intent intent = new Intent()
                .putExtra(Intent.EXTRA_EMAIL, recipients)
                .putExtra(Intent.EXTRA_SUBJECT, title)
                .putExtra(Intent.EXTRA_TEXT, body);

        if (attachLog) {
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
            ToastUtils.showLong(context, R.string.feedback_error_no_app);
        }
    }

    /**
     * Creates the title of the email.
     *
     * @param type one of the following types:
     *             0 - issue
     *             1 - suggestion
     *             2 - other
     * @return the title of the email.
     */
    @NonNull
    private CharSequence createTitle(@NonNull Context context, int type) {
        CharSequence osVersion = Device.API_VERSION_NAME_SHORT;
        CharSequence[] types = new CharSequence[]{"issue", "suggestion", "other"};
        return AboutDialog.getVersionName(context) + ": " + osVersion + ", " + types[type];
    }

    /**
     * Creates the body of the email. It automatically adds some
     * info about the device.
     *
     * @param msg the message that been typed by user.
     * @return the body of the email
     */
    @NonNull
    private CharSequence createBody(@NonNull Context context, @NonNull CharSequence msg) {
        final String extra;

        do {
            PackageInfo pi;
            try {
                pi = context
                        .getPackageManager()
                        .getPackageInfo(PackageUtils.getName(context), 0);
            } catch (PackageManager.NameNotFoundException e) {
                extra = "There was an exception while getting my own package info.";
                break;
            }

            JSONObject obj = new JSONObject();
            try {
                Config config = Config.getInstance();

                // App related stuff
                obj.put("app_version_code", pi.versionCode);
                obj.put("app_version_name", pi.versionName);
                obj.put("app_timestamp", Build.TIME_STAMP);
                obj.put("app_is_debug", DEBUG);
                obj.put("app_is_help_read", config.getTriggers().isHelpRead());
                obj.put("app_launch_count", config.getTriggers().getLaunchCount());

                // Device related stuff
                obj.put("language", Locale.getDefault().getLanguage());
                obj.put("android_version_release", android.os.Build.VERSION.RELEASE);
                obj.put("android_version_sdk_int", android.os.Build.VERSION.SDK_INT);
                obj.put("android_build_display", android.os.Build.DISPLAY);
                obj.put("android_build_brand", android.os.Build.BRAND);
                obj.put("android_build_model", android.os.Build.MODEL);
            } catch (JSONException ignored) {
                extra = "There was an exception while building JSON.";
                break;
            }

            extra = obj.toString().replaceAll(",\"", ", \"");
        } while (false);

        return msg + "\n\nExtras (added automatically & do not change):\n" + extra;
    }

    private boolean isMessageLongEnough(@Nullable CharSequence message) {
        return message != null && message.length() >= getMinMessageLength();
    }

    private int getMinMessageLength() {
        return getResources().getInteger(R.integer.config_feedback_minMessageLength);
    }

    private void attachLog(@NonNull Intent intent) {
        Context context = getActivity();

        try {
            String log = Logcat.capture();
            if (log == null)
                throw new Exception("Failed to capture the logcat.");

            // Prepare cache directory.
            File cacheDir = context.getCacheDir();
            if (cacheDir == null)
                throw new Exception("Cache directory is inaccessible");
            File directory = new File(cacheDir, LogsProviderBase.DIRECTORY);
            FileUtils.deleteRecursive(directory); // Clean-up cache folder
            if (!directory.mkdirs())
                throw new Exception("Failed to create cache directory.");

            // Create log file.
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String fileName = "AcDisplay_log_" + sdf.format(new Date()) + ".txt";
            File file = new File(directory, fileName);

            // Write to the file.
            if (!FileUtils.writeToFile(file, log))
                throw new Exception("Failed to write log to the file.");

            // Put extra stream to the intent.
            Uri uri = Uri.parse("content://" + LogAttachmentProvider.AUTHORITY + "/" + fileName);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
        } catch (Exception e) {
            String message = ResUtils.getString(getResources(), R.string.feedback_error_accessing_log, e.getMessage());
            ToastUtils.showLong(context, message);
        }
    }

}
