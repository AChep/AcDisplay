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
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;

import com.achep.acdisplay.Build;
import com.achep.acdisplay.Device;
import com.achep.acdisplay.DialogHelper;
import com.achep.acdisplay.utils.FileUtils;
import com.achep.acdisplay.utils.IntentUtils;
import com.achep.acdisplay.utils.PackageUtils;
import com.achep.acdisplay.utils.ToastUtils;
import com.achep.acdisplay.utils.ViewUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Feedback dialog fragment.
 * <p/>
 * Provides an UI for sending bugs & suggestions on my email.
 */
public class FeedbackDialog extends DialogFragment {

    private static final String TAG = "FeedbackDialog";

    private Spinner mSpinner;
    private EditText mEditText;
    private CheckBox mAttachLogCheckBox;

    private boolean mTriedToSendShortMessage;

    private boolean mBroadcasting;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        assert activity != null;

        LayoutInflater inflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View root = inflater.inflate(R.layout.fragment_dialog_feedback, null);
        assert root != null;

        mSpinner = (Spinner) root.findViewById(R.id.type);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ViewUtils.setVisible(mAttachLogCheckBox, position == 0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                ViewUtils.setVisible(mAttachLogCheckBox, false);
            }
        });
        mEditText = (EditText) root.findViewById(R.id.message);
        mAttachLogCheckBox = (CheckBox) root.findViewById(R.id.checkbox);
        mAttachLogCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mBroadcasting || !isChecked) {
                    return;
                }

                CharSequence messageText = getString(R.string.feedback_attach_log_description);
                new DialogHelper.Builder(getActivity())
                        .setMessage(messageText)
                        .wrap()
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mBroadcasting = true;
                                mAttachLogCheckBox.setChecked(false);
                                mBroadcasting = false;
                            }
                        })
                        .setPositiveButton(android.R.string.ok, null)
                        .create()
                        .show();
            }
        });

        final AlertDialog alertDialog = new DialogHelper.Builder(getActivity())
                .setIcon(getResources().getDrawable(R.drawable.ic_dialog_mail))
                .setTitle(getString(R.string.feedback))
                .setView(root)
                .wrap()
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

        return alertDialog;
    }

    private void send() {
        Activity activity = getActivity();
        CharSequence message = mEditText.getText();

        // Check for message's length
        int messageMinLength = getResources().getInteger(
                R.integer.config_feedback_minMessageLength);
        if (message == null || (message.length() < messageMinLength && !Build.DEBUG)) {
            String toastText = getString(R.string.feedback_error_msg_too_short, messageMinLength);
            ToastUtils.showShort(activity, toastText);
            mTriedToSendShortMessage = true;
            return;
        }

        PackageInfo pi;
        try {
            pi = activity
                    .getPackageManager()
                    .getPackageInfo(PackageUtils.getName(activity), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.wtf(TAG, "Failed to find my PackageInfo.");
            return;
        }

        CharSequence title = createTitleMessage(activity, mSpinner.getSelectedItemPosition());
        CharSequence body = createBodyMessage(pi, message);
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

        if (IntentUtils.hasActivityForThat(activity, intent)) {
            startActivity(intent);
            dismiss();
        } else {
            String toastText = getString(R.string.feedback_error_no_app);
            ToastUtils.showLong(activity, toastText);
        }
    }

    private CharSequence createTitleMessage(Context context, int type) {
        CharSequence osVersion = Device.hasKitKatApi() ? "KK" : Device.hasJellyBeanMR2Api() ? "JB" : "XX";
        CharSequence[] typeNames = new CharSequence[]{"bug", "suggestion", "other"};
        return AboutDialog.getVersionName(context) + ": " + osVersion + ", " + typeNames[type];
    }

    private CharSequence createBodyMessage(PackageInfo pi, CharSequence msg) {
        return "" + msg +
                '\n' +
                '\n' +
                "- - - - - - - - - - device details - - - - - - - - -" + '\n' +
                "app_version:" + pi.versionName + '(' + pi.versionCode + ")\n" +
                "android_version:" + android.os.Build.VERSION.RELEASE + '(' + android.os.Build.VERSION.SDK_INT + ")\n" +
                "build_display:" + android.os.Build.DISPLAY + '\n' +
                "build_brand:" + android.os.Build.BRAND + '\n' +
                "build_model:" + android.os.Build.MODEL + '\n' +
                "had_short_message:" + mTriedToSendShortMessage + '\n' +
                "language:" + Locale.getDefault().getLanguage();
    }

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

    public static class LogAttachmentProvider extends ContentProvider {

        private static final String TAG = "LogAttachmentProvider";

        static final String AUTHORITY = "com.achep.acdisplay.logs";
        static final String DIRECTORY = "logs";

        private static final String COLUMN_DATA = "_data";

        @Override
        public boolean onCreate() {
            return true;
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection,
                            String[] selectionArgs, String orderBy) {
            List<String> pathSegments = uri.getPathSegments();
            String fileName = pathSegments.get(0);
            File logFile = getContext().getCacheDir();
            if (logFile == null) {
                Log.e(TAG, "No cache dir.");
                return null;
            }

            logFile = new File(new File(logFile, DIRECTORY), fileName);
            if (!logFile.exists()) {
                Log.e(TAG, "Requested log file doesn't exist.");
                return null;
            }

            if (projection == null) {
                projection = new String[]{
                        COLUMN_DATA,
                        OpenableColumns.DISPLAY_NAME,
                        OpenableColumns.SIZE,
                };
            }

            MatrixCursor matrixCursor = new MatrixCursor(projection, 1);
            Object[] row = new Object[projection.length];
            for (int col = 0; col < projection.length; col++) {
                switch (projection[col]) {
                    case COLUMN_DATA:
                        row[col] = logFile.getAbsolutePath();
                        break;
                    case OpenableColumns.DISPLAY_NAME:
                        row[col] = fileName;
                        break;
                    case OpenableColumns.SIZE:
                        row[col] = logFile.length();
                        break;
                }
            }
            matrixCursor.addRow(row);
            return matrixCursor;
        }

        @Override
        public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
            return openFileHelper(uri, "r");
        }

        @Override
        public String getType(Uri uri) {
            return "text/plain";
        }

        @Override
        public Uri insert(Uri uri, ContentValues values) {
            throw new UnsupportedOperationException("insert not supported");
        }

        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) {
            throw new UnsupportedOperationException("delete not supported");
        }

        @Override
        public int update(Uri uri, ContentValues contentValues, String selection,
                          String[] selectionArgs) {
            throw new UnsupportedOperationException("update not supported");
        }
    }
}
