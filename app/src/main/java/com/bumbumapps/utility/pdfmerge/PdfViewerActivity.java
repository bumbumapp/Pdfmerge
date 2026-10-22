package com.bumbumapps.utility.pdfmerge;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.text.InputType;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumbumapps.utility.pdfmerge.Utility.PdfTools;
import com.bumbumapps.utility.pdfmerge.Utility.UiInsets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * In-app PDF viewer built on the platform {@link PdfRenderer} (no native libraries, so it is
 * 16 KB page-size safe). Password protected files are decrypted to the cache with iText first.
 */
public class PdfViewerActivity extends AppCompatActivity {

    public static final String EXTRA_PATH = "path";
    public static final String EXTRA_TITLE = "title";

    public static Intent forFile(Context c, File file) {
        Intent i = new Intent(c, PdfViewerActivity.class);
        i.putExtra(EXTRA_PATH, file.getAbsolutePath());
        i.putExtra(EXTRA_TITLE, file.getName());
        return i;
    }

    public static Intent forUri(Context c, Uri uri, String title) {
        Intent i = new Intent(c, PdfViewerActivity.class);
        i.setData(uri);
        i.putExtra(EXTRA_TITLE, title);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return i;
    }

    private final ExecutorService renderThread = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private PdfRenderer renderer;
    private ParcelFileDescriptor fd;
    private File source;
    private File decrypted;
    private int pageCount;
    private int[] pageW, pageH;
    private LruCache<Integer, Bitmap> cache;
    private RecyclerView list;
    private TextView pageIndicator;
    private View progress;
    private String title;
    private boolean destroyed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        UiInsets.pad(findViewById(R.id.viewerRoot), true, false);
        list = findViewById(R.id.pageList);
        pageIndicator = findViewById(R.id.pageIndicator);
        progress = findViewById(R.id.viewerProgress);
        UiInsets.pad(list, false, true);
        UiInsets.marginBottom(pageIndicator);
        list.setLayoutManager(new LinearLayoutManager(this));

        int maxKb = (int) (Runtime.getRuntime().maxMemory() / 1024 / 6);
        cache = new LruCache<Integer, Bitmap>(maxKb) {
            @Override
            protected int sizeOf(Integer key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };

        title = getIntent().getStringExtra(EXTRA_TITLE);
        setTitle(title != null ? title : getString(R.string.app_name));

        renderThread.execute(() -> {
            try {
                source = resolveSource();
                main.post(() -> openDocument(null));
            } catch (Exception e) {
                main.post(this::failAndFinish);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private File resolveSource() throws IOException {
        String path = getIntent().getStringExtra(EXTRA_PATH);
        if (path != null) return new File(path);
        Uri uri = getIntent().getData();
        if (uri == null) throw new IOException("No document");
        if ("file".equals(uri.getScheme()) && uri.getPath() != null) return new File(uri.getPath());
        File dir = new File(getCacheDir(), "viewer");
        dir.mkdirs();
        File out = new File(dir, "view_" + System.currentTimeMillis() + ".pdf");
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream os = new FileOutputStream(out)) {
            if (in == null) throw new IOException("Cannot open");
            byte[] buf = new byte[64 * 1024];
            int r;
            while ((r = in.read(buf)) != -1) os.write(buf, 0, r);
        }
        decrypted = out; // temp copy, deleted on destroy
        return out;
    }

    private void openDocument(String password) {
        progress.setVisibility(View.VISIBLE);
        renderThread.execute(() -> {
            try {
                File toRender = source;
                if (password != null) {
                    File dir = new File(getCacheDir(), "viewer");
                    dir.mkdirs();
                    File out = new File(dir, "decrypted_" + System.currentTimeMillis() + ".pdf");
                    PdfTools.decrypt(source, password, out);
                    toRender = out;
                }
                closeRenderer();
                fd = ParcelFileDescriptor.open(toRender, ParcelFileDescriptor.MODE_READ_ONLY);
                renderer = new PdfRenderer(fd);
                pageCount = renderer.getPageCount();
                pageW = new int[pageCount];
                pageH = new int[pageCount];
                for (int i = 0; i < pageCount; i++) {
                    PdfRenderer.Page p = renderer.openPage(i);
                    pageW[i] = p.getWidth();
                    pageH[i] = p.getHeight();
                    p.close();
                }
                if (toRender != source) {
                    final File tmp = toRender;
                    main.post(() -> registerTemp(tmp));
                }
                main.post(this::showPages);
            } catch (PdfTools.PasswordRequiredException e) {
                android.util.Log.w("PdfViewer", "password required", e);
                main.post(() -> askPassword(true));
            } catch (SecurityException e) {
                // PdfRenderer cannot open encrypted documents.
                android.util.Log.w("PdfViewer", "renderer security", e);
                main.post(() -> askPassword(password != null));
            } catch (Throwable e) {
                android.util.Log.e("PdfViewer", "open failed", e);
                main.post(this::failAndFinish);
            }
        });
    }

    private File extraTemp;

    private void registerTemp(File f) {
        extraTemp = f;
    }

    private void failAndFinish() {
        if (destroyed) return;
        Toast.makeText(this, R.string.file_opening_error, Toast.LENGTH_LONG).show();
        finish();
    }

    private void askPassword(boolean wrong) {
        if (destroyed) return;
        progress.setVisibility(View.GONE);
        if (wrong) Toast.makeText(this, R.string.wrong_password, Toast.LENGTH_SHORT).show();
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint(R.string.enter_password);
        FrameLayout box = new FrameLayout(this);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(input);
        new AlertDialog.Builder(this)
                .setTitle(R.string.protected_pdf)
                .setView(box)
                .setCancelable(false)
                .setNegativeButton(android.R.string.cancel, (d, w) -> finish())
                .setPositiveButton(R.string.ok, (d, w) -> openDocument(input.getText().toString()))
                .show();
    }

    private void showPages() {
        if (destroyed) return;
        progress.setVisibility(View.GONE);
        list.setAdapter(new PageAdapter());
        updateIndicator(0);
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm != null) updateIndicator(Math.max(0, lm.findFirstVisibleItemPosition()));
            }
        });
    }

    private void updateIndicator(int page) {
        pageIndicator.setText(getString(R.string.page_x_of_y, page + 1, pageCount));
        pageIndicator.setVisibility(pageCount > 0 ? View.VISIBLE : View.GONE);
    }

    private void closeRenderer() {
        try {
            if (renderer != null) renderer.close();
        } catch (Exception ignored) {
        }
        renderer = null;
        try {
            if (fd != null) fd.close();
        } catch (Exception ignored) {
        }
        fd = null;
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        renderThread.execute(() -> {
            closeRenderer();
            if (decrypted != null) decrypted.delete();
            if (extraTemp != null) extraTemp.delete();
        });
        renderThread.shutdown();
        super.onDestroy();
    }

    private class PageAdapter extends RecyclerView.Adapter<PageHolder> {
        @NonNull
        @Override
        public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pdf_page, parent, false);
            return new PageHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PageHolder h, int pos) {
            final int position = pos;
            final int width = list.getWidth() - list.getPaddingLeft() - list.getPaddingRight()
                    - h.itemView.getPaddingLeft() - h.itemView.getPaddingRight();
            final int w = Math.max(width, 200);
            final int hgt = Math.max(1, (int) ((long) w * pageH[position] / Math.max(1, pageW[position])));
            ViewGroup.LayoutParams lp = h.image.getLayoutParams();
            lp.height = hgt;
            h.image.setLayoutParams(lp);
            h.image.setContentDescription(getString(R.string.page_x_of_y, position + 1, pageCount));
            Bitmap cached = cache.get(position);
            h.boundPage = position;
            if (cached != null) {
                h.image.setImageBitmap(cached);
                return;
            }
            h.image.setImageDrawable(null);
            final int page = position;
            renderThread.execute(() -> {
                if (renderer == null || destroyed) return;
                try {
                    // Render at most ~2x screen width for sharp text without huge bitmaps.
                    int bw = Math.min(w, 1600);
                    int bh = Math.max(1, (int) ((long) bw * pageH[page] / Math.max(1, pageW[page])));
                    Bitmap bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
                    bmp.eraseColor(Color.WHITE);
                    PdfRenderer.Page p = renderer.openPage(page);
                    p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    p.close();
                    cache.put(page, bmp);
                    main.post(() -> {
                        if (h.boundPage == page) h.image.setImageBitmap(bmp);
                    });
                } catch (Throwable ignored) {
                }
            });
        }

        @Override
        public int getItemCount() {
            return pageCount;
        }
    }

    static class PageHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        int boundPage = -1;

        PageHolder(View v) {
            super(v);
            image = v.findViewById(R.id.pageImage);
        }
    }
}
