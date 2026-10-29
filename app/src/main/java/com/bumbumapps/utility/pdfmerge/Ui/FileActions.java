package com.bumbumapps.utility.pdfmerge.Ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.format.DateUtils;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.bumbumapps.utility.pdfmerge.PdfViewerActivity;
import com.bumbumapps.utility.pdfmerge.R;
import com.bumbumapps.utility.pdfmerge.Utility.PdfNames;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/** Shared file operations used by several screens. */
public final class FileActions {

    private FileActions() {
    }

    /** Folder that holds every PDF the app creates ("My PDFs"). */
    public static File myDir(Context c) {
        File d = new File(c.getFilesDir(), "PDFMerger");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    public static boolean isInMyDir(Context c, File f) {
        File parent = f.getParentFile();
        return parent != null && parent.getAbsolutePath().equals(myDir(c).getAbsolutePath());
    }

    public static Uri uri(Context c, File f) {
        return FileProvider.getUriForFile(c, c.getPackageName() + ".provider", f);
    }

    public static void view(Activity a, File f) {
        a.startActivity(PdfViewerActivity.forFile(a, f));
    }

    public static void openWith(Activity a, File f) {
        Intent target = new Intent(Intent.ACTION_VIEW);
        target.setDataAndType(uri(a, f), "application/pdf");
        target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            a.startActivity(Intent.createChooser(target, a.getString(R.string.action_open_with)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(a, R.string.no_app_found, Toast.LENGTH_LONG).show();
        }
    }

    public static void share(Activity a, List<File> files) {
        if (files == null || files.isEmpty()) return;
        Intent target;
        if (files.size() == 1) {
            Uri u = uri(a, files.get(0));
            target = new Intent(Intent.ACTION_SEND);
            target.putExtra(Intent.EXTRA_STREAM, u);
            target.setClipData(ClipData.newRawUri("", u));
        } else {
            ArrayList<Uri> uris = new ArrayList<>();
            for (File f : files) uris.add(uri(a, f));
            target = new Intent(Intent.ACTION_SEND_MULTIPLE);
            target.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            ClipData clip = ClipData.newRawUri("", uris.get(0));
            for (int i = 1; i < uris.size(); i++) clip.addItem(new ClipData.Item(uris.get(i)));
            target.setClipData(clip);
        }
        target.setType("application/pdf");
        target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            a.startActivity(Intent.createChooser(target, a.getString(R.string.share_pdf)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(a, R.string.no_app_found, Toast.LENGTH_LONG).show();
        }
    }

    public static String displayName(Context c, Uri uri) {
        String name = null;
        try (Cursor cur = c.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cur != null && cur.moveToFirst()) name = cur.getString(0);
        } catch (Exception ignored) {
        }
        if (name == null) name = uri.getLastPathSegment();
        if (name == null || name.trim().isEmpty()) name = "Document.pdf";
        return name;
    }

    /** Copies a picked document into the cache so file based PDF APIs can use it. */
    public static File importToCache(Context c, Uri uri) throws IOException {
        File root = new File(c.getCacheDir(), "import");
        pruneImports(root, System.currentTimeMillis());
        // Each import gets its own folder so the copy keeps the original file name: results are
        // named after it ("Report_pages.pdf"), never "Report (3)_pages.pdf".
        File dir = new File(root, Long.toString(System.nanoTime()));
        dir.mkdirs();
        String name = PdfNames.toPdfFileName(displayName(c, uri));
        if (name == null) name = "Document.pdf";
        File out = new File(dir, name);
        try (InputStream in = c.getContentResolver().openInputStream(uri);
             OutputStream os = new FileOutputStream(out)) {
            if (in == null) throw new IOException("Cannot open " + uri);
            copy(in, os);
        }
        return out;
    }

    /** Deletes import folders older than a day; a running tool only ever uses the newest one. */
    static void pruneImports(File root, long now) {
        File[] dirs = root.listFiles();
        if (dirs == null) return;
        for (File d : dirs) {
            if (now - d.lastModified() < 24L * 60 * 60 * 1000) continue;
            File[] files = d.listFiles();
            if (files != null) for (File f : files) f.delete();
            d.delete();
        }
    }

    public static void copyTo(Context c, File f, Uri dest) throws IOException {
        try (InputStream in = new FileInputStream(f);
             OutputStream os = c.getContentResolver().openOutputStream(dest, "w")) {
            if (os == null) throw new IOException("Cannot write " + dest);
            copy(in, os);
        }
    }

    public static void copy(InputStream in, OutputStream os) throws IOException {
        byte[] buf = new byte[64 * 1024];
        int r;
        while ((r = in.read(buf)) != -1) os.write(buf, 0, r);
    }

    /** "3 pages · 404 KB · Oct 23" (pages omitted when unknown). */
    public static String meta(Context c, File f, int pages, boolean locked) {
        StringBuilder sb = new StringBuilder();
        if (locked) sb.append(c.getString(R.string.locked)).append(" · ");
        else if (pages > 0) sb.append(c.getResources().getQuantityString(R.plurals.page_count, pages, pages)).append(" · ");
        sb.append(PdfNames.readableSize(f.length()));
        sb.append(" · ").append(DateUtils.formatDateTime(c, f.lastModified(),
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH));
        return sb.toString();
    }
}
