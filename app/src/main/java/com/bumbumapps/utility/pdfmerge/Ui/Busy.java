package com.bumbumapps.utility.pdfmerge.Ui;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.bumbumapps.utility.pdfmerge.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Small modal "working…" dialog for short background operations. */
public final class Busy {
    private final AlertDialog dialog;

    private Busy(AlertDialog dialog) {
        this.dialog = dialog;
    }

    public static Busy show(Activity a, int textRes) {
        View v = LayoutInflater.from(a).inflate(R.layout.dialog_busy, null);
        ((TextView) v.findViewById(R.id.busyText)).setText(textRes);
        AlertDialog d = new MaterialAlertDialogBuilder(a).setView(v).setCancelable(false).create();
        d.show();
        return new Busy(d);
    }

    public void dismiss() {
        try {
            if (dialog.isShowing()) dialog.dismiss();
        } catch (IllegalArgumentException ignored) {
            // Activity already gone.
        }
    }
}
