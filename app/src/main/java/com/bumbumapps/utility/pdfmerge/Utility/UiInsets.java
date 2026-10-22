package com.bumbumapps.utility.pdfmerge.Utility;

import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Edge-to-edge helpers (Android 15+ draws every app edge-to-edge). */
public final class UiInsets {

    private UiInsets() {
    }

    /** Adds system bar + display cutout insets to the view's existing padding on the chosen sides. */
    public static void pad(View view, boolean top, boolean bottom) {
        final int l = view.getPaddingLeft(), t = view.getPaddingTop(), r = view.getPaddingRight(), b = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(l + bars.left, t + (top ? bars.top : 0), r + bars.right,
                    b + (bottom ? Math.max(bars.bottom, ime.bottom) : 0));
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    /** Adds the bottom inset to the view's bottom margin (for FABs / bottom bars). */
    public static void marginBottom(View view) {
        ViewGroup.LayoutParams lp0 = view.getLayoutParams();
        if (!(lp0 instanceof ViewGroup.MarginLayoutParams)) return;
        final int base = ((ViewGroup.MarginLayoutParams) lp0).bottomMargin;
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.bottomMargin = base + bars.bottom;
            v.setLayoutParams(lp);
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
