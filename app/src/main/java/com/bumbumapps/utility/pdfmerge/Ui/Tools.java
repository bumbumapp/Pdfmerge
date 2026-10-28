package com.bumbumapps.utility.pdfmerge.Ui;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.util.Consumer;

import com.bumbumapps.utility.pdfmerge.PDFEditor.PDFEditorActivity;
import com.bumbumapps.utility.pdfmerge.R;
import com.bumbumapps.utility.pdfmerge.Ads.AdsManager;
import com.bumbumapps.utility.pdfmerge.ResultActivity;
import com.bumbumapps.utility.pdfmerge.SplitActivity;
import com.bumbumapps.utility.pdfmerge.Utility.PdfNames;
import com.bumbumapps.utility.pdfmerge.Utility.PdfTools;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Entry points of the single-file tools (split, organize, protect, unlock), incl. password handling. */
public final class Tools {
    private static final String TAG = "Tools";
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface Work<T> {
        T run() throws Exception;
    }

    public interface WithPassword {
        void run(File file, String password);
    }

    private Tools() {
    }

    /** Runs {@code work} in the background behind a busy dialog; errors become a toast. */
    public static <T> void run(Activity a, Work<T> work, Consumer<T> done) {
        Busy busy = Busy.show(a, R.string.working);
        EXEC.execute(() -> {
            T result = null;
            Throwable error = null;
            try {
                result = work.run();
            } catch (Throwable t) {
                Log.w(TAG, "tool failed", t);
                error = t;
            }
            final T r = result;
            final Throwable e = error;
            MAIN.post(() -> {
                busy.dismiss();
                if (a.isFinishing() || a.isDestroyed()) return;
                if (e != null) Toast.makeText(a, R.string.operation_failed, Toast.LENGTH_LONG).show();
                else done.accept(r);
            });
        });
    }

    /** Calls {@code next} with the file and its password (null if it has none), asking the user if needed. */
    public static void withPassword(Activity a, File src, String name, WithPassword next) {
        run(a, () -> PdfTools.needsPassword(src), needs -> {
            if (!needs) {
                next.run(src, null);
            } else {
                PasswordPrompt.unlock(a, a.getString(R.string.file_protected_title, name),
                        a.getString(R.string.password_to_continue), a.getString(R.string.ok), src,
                        pw -> next.run(src, pw), null);
            }
        });
    }

    public static void split(Activity a, File src, String name) {
        withPassword(a, src, name, (f, pw) -> a.startActivity(SplitActivity.intent(a, f, pw, name)));
    }

    public static void organize(Activity a, File src, String name, boolean inPlace) {
        withPassword(a, src, name, (f, pw) -> a.startActivity(PDFEditorActivity.intent(a, f, pw, name, inPlace)));
    }

    public static void protect(Activity a, File src, String name) {
        run(a, () -> PdfTools.needsPassword(src), needs -> {
            if (needs) {
                Toast.makeText(a, R.string.already_protected, Toast.LENGTH_LONG).show();
                return;
            }
            PasswordPrompt.choose(a, pw -> run(a, () -> {
                File out = PdfNames.uniqueFile(FileActions.myDir(a), PdfNames.baseName(name) + "_protected.pdf");
                PdfTools.encrypt(src, null, out, pw);
                return out;
            }, out -> AdsManager.get(a).onBreakPoint(a, () -> ResultActivity.start(a, ResultActivity.MODE_PROTECT, out))));
        });
    }

    public static void unlock(Activity a, File src, String name) {
        run(a, () -> new boolean[]{PdfTools.needsPassword(src), PdfTools.isEncrypted(src)}, st -> {
            if (!st[0] && !st[1]) {
                Toast.makeText(a, R.string.not_protected, Toast.LENGTH_LONG).show();
            } else if (!st[0]) {
                // Only an owner password (editing restrictions): remove it directly.
                decryptTo(a, src, null, name);
            } else {
                PasswordPrompt.unlock(a, a.getString(R.string.unlock_title), a.getString(R.string.unlock_body),
                        a.getString(R.string.unlock_action), src, pw -> decryptTo(a, src, pw, name), null);
            }
        });
    }

    private static void decryptTo(Activity a, File src, String pw, String name) {
        run(a, () -> {
            File out = PdfNames.uniqueFile(FileActions.myDir(a), PdfNames.baseName(name) + "_unlocked.pdf");
            PdfTools.decrypt(src, pw, out);
            return out;
        }, out -> AdsManager.get(a).onBreakPoint(a, () -> ResultActivity.start(a, ResultActivity.MODE_UNLOCK, out)));
    }
}
