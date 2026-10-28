package com.bumbumapps.utility.pdfmerge.PDFEditor;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.LruCache;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumbumapps.utility.pdfmerge.DataAdapter.AdapterGridBasic;
import com.bumbumapps.utility.pdfmerge.R;
import com.bumbumapps.utility.pdfmerge.Ads.AdsManager;
import com.bumbumapps.utility.pdfmerge.ResultActivity;
import com.bumbumapps.utility.pdfmerge.Ui.FileActions;
import com.bumbumapps.utility.pdfmerge.Ui.Thumbs;
import com.bumbumapps.utility.pdfmerge.Ui.Tools;
import com.bumbumapps.utility.pdfmerge.Utility.ItemTouchHelperClass;
import com.bumbumapps.utility.pdfmerge.Utility.PDFPage;
import com.bumbumapps.utility.pdfmerge.Utility.PdfNames;
import com.bumbumapps.utility.pdfmerge.Utility.PdfTools;
import com.bumbumapps.utility.pdfmerge.Utility.UiInsets;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Page organizer: reorder (drag), rotate and delete pages with live thumbnails. */
public class PDFEditorActivity extends AppCompatActivity {
    private static final String EXTRA_PATH = "path";
    private static final String EXTRA_PASSWORD = "password";
    private static final String EXTRA_NAME = "name";
    private static final String EXTRA_IN_PLACE = "inPlace";

    public static Intent intent(Context c, File file, String password, String name, boolean inPlace) {
        Intent i = new Intent(c, PDFEditorActivity.class);
        i.putExtra(EXTRA_PATH, file.getAbsolutePath());
        i.putExtra(EXTRA_PASSWORD, password);
        i.putExtra(EXTRA_NAME, name);
        i.putExtra(EXTRA_IN_PLACE, inPlace);
        return i;
    }

    private final ExecutorService renderThread = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ArrayList<PDFPage> pages = new ArrayList<>();
    private LruCache<Integer, Bitmap> cache;
    private PdfRenderer renderer;
    private ParcelFileDescriptor fd;
    private File source;
    private File renderCopy;
    private String password;
    private String name;
    private boolean inPlace;
    private boolean dirty;
    private boolean destroyed;
    private int thumbWidth;

    private RecyclerView recyclerView;
    private AdapterGridBasic mAdapter;
    private ItemTouchHelper itemTouchHelper;
    private View progress;
    private MaterialButton saveButton;
    private MaterialToolbar toolbar;
    private ActionMode actionMode;
    private final ActionModeCallback actionModeCallback = new ActionModeCallback();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdfeditor);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        UiInsets.pad(findViewById(R.id.editorRoot), false, true);
        progress = findViewById(R.id.editProgress);
        saveButton = findViewById(R.id.editorSave);
        saveButton.setOnClickListener(v -> save());

        String path = getIntent().getStringExtra(EXTRA_PATH);
        if (path == null || !new File(path).exists()) {
            finish();
            return;
        }
        source = new File(path);
        password = getIntent().getStringExtra(EXTRA_PASSWORD);
        name = getIntent().getStringExtra(EXTRA_NAME);
        if (name == null) name = source.getName();
        inPlace = getIntent().getBooleanExtra(EXTRA_IN_PLACE, false) && FileActions.isInMyDir(this, source);
        setTitle(R.string.tool_organize);
        toolbar.setSubtitle(name);

        int maxKb = (int) (Runtime.getRuntime().maxMemory() / 1024 / 6);
        cache = new LruCache<Integer, Bitmap>(maxKb) {
            @Override
            protected int sizeOf(Integer key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };
        int span = getResources().getConfiguration().screenWidthDp >= 600 ? 4 : 2;
        thumbWidth = Math.min(700, getResources().getDisplayMetrics().widthPixels / span);

        recyclerView = findViewById(R.id.recycleViewEditor);
        recyclerView.setLayoutManager(new GridLayoutManager(this, span));
        recyclerView.addItemDecoration(new com.bumbumapps.utility.pdfmerge.DataAdapter.SpacingItemDecoration(span,
                (int) (8 * getResources().getDisplayMetrics().density), true));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!dirty) {
                    finish();
                    return;
                }
                new MaterialAlertDialogBuilder(PDFEditorActivity.this)
                        .setTitle(R.string.organize_unsaved_title)
                        .setNegativeButton(R.string.cancel, null)
                        .setNeutralButton(R.string.discard, (d, w) -> finish())
                        .setPositiveButton(R.string.organize_save, (d, w) -> save())
                        .show();
            }
        });

        open();
    }

    private void open() {
        renderThread.execute(() -> {
            try {
                File toRender = source;
                if (password != null) {
                    File dir = new File(getCacheDir(), "editor");
                    dir.mkdirs();
                    renderCopy = new File(dir, "render_" + System.currentTimeMillis() + ".pdf");
                    PdfTools.decrypt(source, password, renderCopy);
                    toRender = renderCopy;
                }
                List<PDFPage> list = new ArrayList<>();
                PdfReader reader = PdfTools.open(toRender, null);
                try {
                    for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                        Rectangle r = reader.getPageSizeWithRotation(i);
                        list.add(new PDFPage(i - 1, r.getHeight() >= r.getWidth()));
                    }
                } finally {
                    reader.close();
                }
                fd = ParcelFileDescriptor.open(toRender, ParcelFileDescriptor.MODE_READ_ONLY);
                renderer = new PdfRenderer(fd);
                main.post(() -> showPages(list));
            } catch (Throwable e) {
                main.post(() -> {
                    if (destroyed) return;
                    Toast.makeText(this, R.string.pick_pdf_failed, Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    private void showPages(List<PDFPage> list) {
        if (destroyed) return;
        progress.setVisibility(View.GONE);
        pages.addAll(list);
        mAdapter = new AdapterGridBasic(pages, this::render);
        mAdapter.setListener(new AdapterGridBasic.Listener() {
            @Override
            public void onRotate(PDFPage page, int position, int degrees) {
                page.setRotation(degrees);
                markDirty();
            }

            @Override
            public void onDelete(PDFPage page, int position) {
                deletePages(java.util.Collections.singletonList(page));
            }

            @Override
            public void onClick(PDFPage page, int position) {
                if (actionMode != null) toggleSelection(position);
            }

            @Override
            public void onLongClick(PDFPage page, int position) {
                if (actionMode == null) actionMode = startSupportActionMode(actionModeCallback);
                toggleSelection(position);
            }

            @Override
            public void onChanged() {
                markDirty();
            }
        });
        mAdapter.setDragListener(vh -> {
            if (actionMode == null) itemTouchHelper.startDrag(vh);
        });
        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelperClass(mAdapter, true));
        itemTouchHelper.attachToRecyclerView(recyclerView);
        recyclerView.setAdapter(mAdapter);
        recyclerView.scheduleLayoutAnimation();
        updateSubtitle();
    }

    private void render(int index, AdapterGridBasic.Renderer.Callback cb) {
        Bitmap cached = cache.get(index);
        if (cached != null) {
            cb.done(cached);
            return;
        }
        renderThread.execute(() -> {
            if (renderer == null || destroyed) return;
            try {
                PdfRenderer.Page p = renderer.openPage(index);
                int w = thumbWidth;
                int h = Math.max(1, (int) ((long) w * p.getHeight() / Math.max(1, p.getWidth())));
                Bitmap bmp = Bitmap.createBitmap(w, Math.min(h, w * 3), Bitmap.Config.ARGB_8888);
                bmp.eraseColor(Color.WHITE);
                p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                p.close();
                cache.put(index, bmp);
                main.post(() -> {
                    if (!destroyed) cb.done(bmp);
                });
            } catch (Throwable ignored) {
            }
        });
    }

    private void markDirty() {
        dirty = true;
        saveButton.setEnabled(true);
    }

    private void updateSubtitle() {
        toolbar.setSubtitle(name + " · " + getResources().getQuantityString(R.plurals.page_count, pages.size(), pages.size()));
    }

    private void deletePages(List<PDFPage> toDelete) {
        if (toDelete.size() >= pages.size()) {
            Toast.makeText(this, R.string.organize_last_page, Toast.LENGTH_LONG).show();
            return;
        }
        for (PDFPage p : toDelete) {
            int i = pages.indexOf(p);
            if (i < 0) continue;
            pages.remove(i);
            mAdapter.forget(p);
            mAdapter.notifyItemRemoved(i);
        }
        mAdapter.notifyItemRangeChanged(0, pages.size(), "order");
        updateSubtitle();
        markDirty();
    }

    private void save() {
        if (pages.isEmpty()) return;
        List<Integer> order = new ArrayList<>();
        List<Integer> rotations = new ArrayList<>();
        for (PDFPage p : pages) {
            order.add(p.getIndex() + 1);
            rotations.add(p.getRotation());
        }
        Tools.run(this, () -> {
            File dir = FileActions.myDir(this);
            File tmp = new File(dir, ".organize_" + System.currentTimeMillis() + ".tmp");
            try {
                PdfTools.organize(source, password, order, rotations, tmp);
                File out;
                if (inPlace) {
                    out = source;
                    PdfTools.replace(tmp, out);
                    Thumbs.invalidate(out);
                } else {
                    out = PdfNames.uniqueFile(dir, PdfNames.baseName(name) + "_organized.pdf");
                    if (!tmp.renameTo(out)) throw new java.io.IOException("rename failed");
                }
                return out;
            } finally {
                tmp.delete();
            }
        }, out -> {
            dirty = false;
            AdsManager.get(this).onBreakPoint(this, () -> {
                ResultActivity.start(this, ResultActivity.MODE_SAVED, out);
                finish();
            });
        });
    }

    // ------------------------------------------------------------ menu & selection

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.editor_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mAdapter == null) return super.onOptionsItemSelected(item);
        int id = item.getItemId();
        java.util.function.IntPredicate test;
        if (id == R.id.editor_selectAll) test = i -> true;
        else if (id == R.id.editor_selectOddPages) test = i -> i % 2 == 0;
        else if (id == R.id.editor_selectEvenPages) test = i -> i % 2 == 1;
        else if (id == R.id.editor_selectPortraitPages) test = i -> pages.get(i).isPortrait();
        else if (id == R.id.editor_selectLandscapePages) test = i -> !pages.get(i).isPortrait();
        else return super.onOptionsItemSelected(item);
        if (actionMode == null) actionMode = startSupportActionMode(actionModeCallback);
        mAdapter.selectWhere(test);
        updateActionTitle();
        return true;
    }

    private void toggleSelection(int position) {
        mAdapter.toggleSelection(position);
        updateActionTitle();
    }

    private void updateActionTitle() {
        if (actionMode == null) return;
        int count = mAdapter.getSelectedItemCount();
        if (count == 0) actionMode.finish();
        else actionMode.setTitle(getString(R.string.selected_count, count));
    }

    private class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_editactionmode, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int id = item.getItemId();
            List<PDFPage> sel = mAdapter.getSelectedPages();
            if (id == R.id.edit_delete) {
                if (sel.size() >= pages.size()) {
                    Toast.makeText(PDFEditorActivity.this, R.string.organize_last_page, Toast.LENGTH_LONG).show();
                    return true;
                }
                mode.finish();
                deletePages(sel);
                return true;
            }
            if (id == R.id.edit_rotateRight || id == R.id.edit_rotateleft) {
                int deg = id == R.id.edit_rotateRight ? 90 : -90;
                for (PDFPage p : sel) {
                    p.setRotation(deg);
                    mAdapter.notifyItemChanged(pages.indexOf(p), "rot");
                }
                markDirty();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mAdapter.clearSelections();
            actionMode = null;
        }
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        renderThread.execute(() -> {
            try {
                if (renderer != null) renderer.close();
            } catch (Throwable ignored) {
            }
            try {
                if (fd != null) fd.close();
            } catch (Throwable ignored) {
            }
            if (renderCopy != null) renderCopy.delete();
        });
        renderThread.shutdown();
        super.onDestroy();
    }
}
