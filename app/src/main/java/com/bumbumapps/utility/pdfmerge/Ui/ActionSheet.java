package com.bumbumapps.utility.pdfmerge.Ui;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumbumapps.utility.pdfmerge.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

/** A Material bottom sheet with a title and a list of icon + text actions. */
public final class ActionSheet {
    private final BottomSheetDialog dialog;
    private final ViewGroup rows;
    private final Activity activity;

    public ActionSheet(Activity a, CharSequence title, CharSequence subtitle) {
        activity = a;
        dialog = new BottomSheetDialog(a);
        View v = LayoutInflater.from(a).inflate(R.layout.sheet_actions, null);
        ((TextView) v.findViewById(R.id.sheetTitle)).setText(title);
        TextView sub = v.findViewById(R.id.sheetSubtitle);
        if (subtitle == null) sub.setVisibility(View.GONE);
        else sub.setText(subtitle);
        rows = v.findViewById(R.id.sheetRows);
        dialog.setContentView(v);
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.getBehavior().setSkipCollapsed(true);
    }

    public ActionSheet add(int icon, int title, int subtitle, Runnable action) {
        View row = LayoutInflater.from(activity).inflate(R.layout.item_sheet_action, rows, false);
        ((ImageView) row.findViewById(R.id.rowIcon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.rowTitle)).setText(title);
        TextView sub = row.findViewById(R.id.rowSubtitle);
        if (subtitle != 0) {
            sub.setText(subtitle);
            sub.setVisibility(View.VISIBLE);
        }
        row.setOnClickListener(x -> {
            dialog.dismiss();
            action.run();
        });
        rows.addView(row);
        return this;
    }

    public ActionSheet add(int icon, int title, Runnable action) {
        return add(icon, title, 0, action);
    }

    public void show() {
        dialog.show();
    }
}
