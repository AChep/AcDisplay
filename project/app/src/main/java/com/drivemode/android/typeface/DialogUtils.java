package com.drivemode.android.typeface;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.widget.Button;
import android.widget.TextView;

/**
 * @author KeishinYokomaku
 */
final class DialogUtils {
    public static <D extends Dialog> void setTypeface(TypefaceHelper helper, D dialog, String typefaceName, int style) {
        if (dialog instanceof ProgressDialog) {
            setTypeface(helper, (ProgressDialog) dialog, typefaceName, style);
        } else if (dialog instanceof AlertDialog) {
            setTypeface(helper, (AlertDialog) dialog, typefaceName, style);
        }
    }

    private static void setTypeface(TypefaceHelper helper, AlertDialog alertDialog, String typefaceName, int style) {
        Button positive = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button negative = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        Button neutral = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        TextView message = (TextView) alertDialog.findViewById(android.R.id.message);
        if (positive != null) {
            helper.setTypeface(positive, typefaceName, style);
        }
        if (negative != null) {
            helper.setTypeface(negative, typefaceName, style);
        }
        if (neutral != null) {
            helper.setTypeface(neutral, typefaceName, style);
        }
        if (message != null) {
            helper.setTypeface(message, typefaceName, style);
        }
    }

    private static void setTypeface(TypefaceHelper helper, ProgressDialog progressDialog, String typefaceName, int style) {
        TextView message = (TextView) progressDialog.findViewById(android.R.id.message);
        if (message != null) {
            helper.setTypeface(message, typefaceName, style);
        }
    }
}
