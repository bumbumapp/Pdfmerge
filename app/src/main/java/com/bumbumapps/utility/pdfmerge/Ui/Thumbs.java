package com.bumbumapps.utility.pdfmerge.Ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.LruCache;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Renders first-page thumbnails (and page counts) of PDFs off the main thread with
 * {@link PdfRenderer}. Results are cached and only delivered to views still showing the same item.
 */
public final class Thumbs {

    public static final class Info {
        @Nullable public final Bitmap bitmap;
        public final int pages;
        public final boolean locked;

        Info(@Nullable Bitmap bitmap, int pages, boolean locked) {
            this.bitmap = bitmap;
            this.pages = pages;
            this.locked = locked;
        }
    }

    public interface Callback {
        void onInfo(Info info);
    }

    private static final ExecutorService EXEC = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Info> CACHE = new LruCache<String, Info>(
            (int) Math.min(24 * 1024, Runtime.getRuntime().maxMemory() / 1024 / 10)) {
        @Override
        protected int sizeOf(String key, Info value) {
            return value.bitmap == null ? 1 : Math.max(1, value.bitmap.getByteCount() / 1024);
        }
    };

    private Thumbs() {
    }

    public static String key(File f) {
        return f.getAbsolutePath() + "|" + f.length() + "|" + f.lastModified();
    }

    /** Drops cached thumbnails of a file that was rewritten in place. */
    public static void invalidate(File f) {
        CACHE.remove(key(f));
    }

    /**
     * Loads info for a PDF given as {@link File} or content {@link Uri}. {@code tagView} is used
     * to make sure a recycled view does not receive a stale result.
     */
    public static void load(Context context, ImageView tagView, Object source, String key, int widthPx, Callback cb) {
        tagView.setTag(com.bumbumapps.utility.pdfmerge.R.id.thumb_key, key);
        Info cached = CACHE.get(key);
        if (cached != null) {
            cb.onInfo(cached);
            return;
        }
        final Context app = context.getApplicationContext();
        EXEC.execute(() -> {
            Info info = render(app, source, widthPx);
            if (info.bitmap != null || info.locked) CACHE.put(key, info);
            MAIN.post(() -> {
                if (key.equals(tagView.getTag(com.bumbumapps.utility.pdfmerge.R.id.thumb_key))) cb.onInfo(info);
            });
        });
    }

    /** True if the PDF needs a password to open (blocking, call off the main thread). */
    public static boolean isLocked(Context app, Object source) {
        ParcelFileDescriptor fd = null;
        try {
            fd = source instanceof File
                    ? ParcelFileDescriptor.open((File) source, ParcelFileDescriptor.MODE_READ_ONLY)
                    : app.getContentResolver().openFileDescriptor((Uri) source, "r");
            if (fd == null) return false;
            new PdfRenderer(fd).close();
            return false;
        } catch (SecurityException e) {
            return true;
        } catch (Throwable e) {
            return false;
        } finally {
            try {
                if (fd != null) fd.close();
            } catch (Throwable ignored) {
            }
        }
    }

    static Info render(Context app, Object source, int widthPx) {
        ParcelFileDescriptor fd = null;
        PdfRenderer renderer = null;
        try {
            if (source instanceof File) {
                fd = ParcelFileDescriptor.open((File) source, ParcelFileDescriptor.MODE_READ_ONLY);
            } else {
                fd = app.getContentResolver().openFileDescriptor((Uri) source, "r");
            }
            if (fd == null) return new Info(null, 0, false);
            renderer = new PdfRenderer(fd);
            int pages = renderer.getPageCount();
            if (pages == 0) return new Info(null, 0, false);
            PdfRenderer.Page page = renderer.openPage(0);
            int w = Math.max(48, widthPx);
            int h = Math.max(1, (int) ((long) w * page.getHeight() / Math.max(1, page.getWidth())));
            if (h > w * 3) h = w * 3;
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bmp.eraseColor(Color.WHITE);
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();
            return new Info(bmp, pages, false);
        } catch (SecurityException e) {
            // PdfRenderer refuses password protected documents.
            return new Info(null, 0, true);
        } catch (Throwable e) {
            return new Info(null, 0, false);
        } finally {
            try {
                if (renderer != null) renderer.close();
            } catch (Throwable ignored) {
            }
            try {
                if (fd != null) fd.close();
            } catch (Throwable ignored) {
            }
        }
    }
}
