package com.bumbumapps.utility.pdfmerge.Ui;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Consumer;

import com.bumbumapps.utility.pdfmerge.R;
import com.bumbumapps.utility.pdfmerge.Utility.PdfTools;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Password dialogs: unlock an existing PDF (verified) or choose a new one (with confirmation). */
public final class PasswordPrompt {
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();

    private PasswordPrompt() {
    }

    /** Verifies a password off the main thread. */
    public interface Check {
        boolean ok(String password);
    }

    /** Asks for the password of {@code file}; calls {@code onOk} only with a password that opens it. */
    public static void unlock(Activity a, CharSequence title, CharSequence message, CharSequence action, File file,
                              Consumer<String> onOk, Runnable onCancel) {
        unlock(a, title, message, action, a.getString(R.string.cancel), pw -> PdfTools.checkPassword(file, pw), onOk, onCancel);
    }

    public static void unlock(Activity a, CharSequence title, CharSequence message, CharSequence action,
                              CharSequence cancel, Check check, Consumer<String> onOk, Runnable onCancel) {
        View v = LayoutInflater.from(a).inflate(R.layout.dialog_password, null);
        TextInputLayout l1 = v.findViewById(R.id.passwordLayout);
        TextInputEditText e1 = v.findViewById(R.id.passwordInput);
        v.findViewById(R.id.confirmLayout).setVisibility(View.GONE);
        AlertDialog d = new MaterialAlertDialogBuilder(a)
                .setTitle(title)
                .setMessage(message)
                .setView(v)
                .setCancelable(false)
                .setNegativeButton(cancel, (di, w) -> {
                    if (onCancel != null) onCancel.run();
                })
                .setPositiveButton(action, null)
                .create();
        d.setOnShowListener(x -> {
            View ok = d.getButton(AlertDialog.BUTTON_POSITIVE);
            final boolean[] busy = {false};
            Runnable submit = () -> {
                // Hardware/IME "done" can fire twice; never verify or accept twice.
                if (busy[0]) return;
                String pw = e1.getText() == null ? "" : e1.getText().toString();
                if (pw.isEmpty()) {
                    l1.setError(a.getString(R.string.password_empty));
                    return;
                }
                ok.setEnabled(false);
                busy[0] = true;
                EXEC.execute(() -> {
                    boolean good = check.ok(pw);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (a.isFinishing() || a.isDestroyed()) return;
                        ok.setEnabled(true);
                        if (good) {
                            if (!d.isShowing()) return;
                            d.dismiss();
                            onOk.accept(pw);
                        } else {
                            busy[0] = false;
                            l1.setError(a.getString(R.string.wrong_password));
                        }
                    });
                });
            };
            ok.setOnClickListener(b -> submit.run());
            e1.setOnEditorActionListener((tv, id, ev) -> {
                boolean enter = ev != null && ev.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER
                        && ev.getAction() == android.view.KeyEvent.ACTION_DOWN;
                if (id == EditorInfo.IME_ACTION_DONE || enter) {
                    submit.run();
                    return true;
                }
                return false;
            });
            e1.requestFocus();
        });
        d.show();
    }

    /** Asks for a new password twice. */
    public static void choose(Activity a, Consumer<String> onOk) {
        View v = LayoutInflater.from(a).inflate(R.layout.dialog_password, null);
        TextInputLayout l1 = v.findViewById(R.id.passwordLayout);
        TextInputEditText e1 = v.findViewById(R.id.passwordInput);
        TextInputLayout l2 = v.findViewById(R.id.confirmLayout);
        TextInputEditText e2 = v.findViewById(R.id.confirmInput);
        e1.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        e1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        AlertDialog d = new MaterialAlertDialogBuilder(a)
                .setTitle(R.string.protect_title)
                .setMessage(R.string.protect_body)
                .setView(v)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.protect_action, null)
                .create();
        d.setOnShowListener(x -> {
            d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
                String p1 = e1.getText() == null ? "" : e1.getText().toString();
                String p2 = e2.getText() == null ? "" : e2.getText().toString();
                l1.setError(null);
                l2.setError(null);
                if (p1.isEmpty()) {
                    l1.setError(a.getString(R.string.password_empty));
                } else if (!p1.equals(p2)) {
                    l2.setError(a.getString(R.string.passwords_differ));
                } else {
                    d.dismiss();
                    onOk.accept(p1);
                }
            });
            e1.requestFocus();
        });
        d.show();
    }
}
