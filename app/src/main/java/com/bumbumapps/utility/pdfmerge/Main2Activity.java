package com.bumbumapps.utility.pdfmerge;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.IntentCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumbumapps.utility.pdfmerge.Ads.AdsManager;
import com.bumbumapps.utility.pdfmerge.DataAdapter.MainRecycleViewAdapter;
import com.bumbumapps.utility.pdfmerge.DataAdapter.ToolAdapter;
import com.bumbumapps.utility.pdfmerge.Ui.ActionSheet;
import com.bumbumapps.utility.pdfmerge.Ui.FileActions;
import com.bumbumapps.utility.pdfmerge.Ui.Tools;
import com.bumbumapps.utility.pdfmerge.Utility.PdfNames;
import com.bumbumapps.utility.pdfmerge.Utility.RecyclerViewEmptySupport;
import com.bumbumapps.utility.pdfmerge.Utility.UiInsets;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Home: primary "merge" action, tool grid and the user's PDFs. */
public class Main2Activity extends AppCompatActivity {

    private static final int TOOL_IMAGES = 1, TOOL_SCAN = 2, TOOL_SPLIT = 3, TOOL_ORGANIZE = 4,
            TOOL_PROTECT = 5, TOOL_UNLOCK = 6, TOOL_TEXT = 7, TOOL_WEB = 8;
    private static final String STATE_PENDING_TOOL = "pendingTool";

    private final List<File> items = new ArrayList<>();
    private RecyclerViewEmptySupport recyclerView;
    private MainRecycleViewAdapter mAdapter;
    private ActionMode actionMode;
    private final ActionModeCallback actionModeCallback = new ActionModeCallback();
    private android.widget.TextView filesCount;

    private int pendingTool;
    private File pendingSave;
    private List<File> pendingSaveAll = Collections.emptyList();

    private final ActivityResultLauncher<String[]> pickPdf =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onPdfPicked);
    private final ActivityResultLauncher<String> createDoc =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/pdf"), this::onSaveTarget);
    private final ActivityResultLauncher<Uri> pickFolder =
            registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), this::onFolderPicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (savedInstanceState != null) pendingTool = savedInstanceState.getInt(STATE_PENDING_TOOL);

        UiInsets.pad(findViewById(R.id.homeRoot), false, true);
        UiInsets.pad(findViewById(R.id.homeContent), false, false);

        AdsManager ads = AdsManager.get(this);
        ads.gatherConsent(this);
        ads.loadBanner(this, findViewById(R.id.adContainer));

        findViewById(R.id.btnMergeStart).setOnClickListener(v -> startMerge("FileSearch"));

        RecyclerView tools = findViewById(R.id.toolsGrid);
        int span = getResources().getConfiguration().screenWidthDp >= 600 ? 8 : 4;
        tools.setLayoutManager(new GridLayoutManager(this, span));
        tools.setAdapter(new ToolAdapter(new ToolAdapter.Tool[]{
                new ToolAdapter.Tool(TOOL_IMAGES, R.drawable.ic_m_image, R.string.tool_images),
                new ToolAdapter.Tool(TOOL_SCAN, R.drawable.ic_m_camera, R.string.tool_scan),
                new ToolAdapter.Tool(TOOL_SPLIT, R.drawable.ic_m_split, R.string.tool_split),
                new ToolAdapter.Tool(TOOL_ORGANIZE, R.drawable.ic_m_organize, R.string.tool_organize),
                new ToolAdapter.Tool(TOOL_PROTECT, R.drawable.ic_m_lock, R.string.tool_protect),
                new ToolAdapter.Tool(TOOL_UNLOCK, R.drawable.ic_m_lock_open, R.string.tool_unlock),
                new ToolAdapter.Tool(TOOL_TEXT, R.drawable.ic_m_text, R.string.tool_text),
                new ToolAdapter.Tool(TOOL_WEB, R.drawable.ic_m_web, R.string.tool_web),
        }, this::onTool));

        filesCount = findViewById(R.id.filesCount);
        recyclerView = findViewById(R.id.mainRecycleView);
        recyclerView.setEmptyView(findViewById(R.id.toDoEmptyView));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new MainRecycleViewAdapter(this, items);
        mAdapter.setOnItemClickListener(new MainRecycleViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, File value, int position) {
                if (actionMode != null) {
                    toggleSelection(position);
                } else if (view.getId() == R.id.extramenu) {
                    showFileSheet(value);
                } else {
                    FileActions.view(Main2Activity.this, value);
                }
            }

            @Override
            public void onItemLongClick(View view, File obj, int pos) {
                if (actionMode == null) actionMode = startSupportActionMode(actionModeCallback);
                toggleSelection(pos);
            }
        });
        recyclerView.setAdapter(mAdapter);

        if (savedInstanceState == null) handleExternalData(getIntent());
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        handleExternalData(intent);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_PENDING_TOOL, pendingTool);
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadFiles();
    }

    // ---------------------------------------------------------------- tools

    private void onTool(ToolAdapter.Tool t) {
        switch (t.id) {
            case TOOL_IMAGES:
                startMerge("PhotoPicker");
                break;
            case TOOL_SCAN:
                startMerge("CameraActivity");
                break;
            case TOOL_TEXT:
                startMerge("TextToPDFActivity");
                break;
            case TOOL_WEB:
                startMerge("HtmlActivity");
                break;
            default:
                pendingTool = t.id;
                try {
                    pickPdf.launch(new String[]{"application/pdf"});
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, R.string.no_app_found, Toast.LENGTH_LONG).show();
                }
        }
    }

    private void onPdfPicked(Uri uri) {
        int tool = pendingTool;
        pendingTool = 0;
        if (uri == null || tool == 0) return;
        Tools.run(this, () -> FileActions.importToCache(this, uri), file -> runTool(tool, file, file.getName(), false));
    }

    private void runTool(int tool, File file, String name, boolean inMyFiles) {
        switch (tool) {
            case TOOL_SPLIT:
                Tools.split(this, file, name);
                break;
            case TOOL_ORGANIZE:
                Tools.organize(this, file, name, inMyFiles);
                break;
            case TOOL_PROTECT:
                Tools.protect(this, file, name);
                break;
            case TOOL_UNLOCK:
                Tools.unlock(this, file, name);
                break;
        }
    }

    private void startMerge(String action) {
        Intent intent = new Intent(this, MergeActivity.class);
        intent.putExtra("ActivityAction", action);
        startActivity(intent);
    }

    private void startMerge(ArrayList<Uri> uris) {
        Intent intent = new Intent(this, MergeActivity.class);
        intent.putExtra("ActivityAction", "ImageSend");
        intent.putParcelableArrayListExtra("image", uris);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    /** Files shared to the app from other apps ("Share → PDF Merge"). */
    private void handleExternalData(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        String type = intent.getType();
        if (type == null) return;
        if (!(type.startsWith("image/") || "application/pdf".equals(type))) return;
        if (Intent.ACTION_SEND.equals(action)) {
            Uri uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri.class);
            if (uri != null) startMerge(new ArrayList<>(Collections.singletonList(uri)));
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            ArrayList<Uri> uris = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri.class);
            if (uris != null && !uris.isEmpty()) startMerge(uris);
        }
    }

    // ---------------------------------------------------------------- files

    @android.annotation.SuppressLint("NotifyDataSetChanged")
    private void reloadFiles() {
        if (actionMode != null) actionMode.finish();
        File[] files = FileActions.myDir(this).listFiles(f -> f.isFile() && f.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".pdf"));
        if (files == null) files = new File[0];
        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        items.clear();
        items.addAll(Arrays.asList(files));
        mAdapter.notifyDataSetChanged();
        filesCount.setText(items.isEmpty() ? "" : getResources().getQuantityString(R.plurals.file_count, items.size(), items.size()));
    }

    private void showFileSheet(File f) {
        new ActionSheet(this, f.getName(), FileActions.meta(this, f, 0, false))
                .add(R.drawable.ic_m_pdf, R.string.action_open, () -> FileActions.view(this, f))
                .add(R.drawable.ic_m_share, R.string.action_share, () -> FileActions.share(this, Collections.singletonList(f)))
                .add(R.drawable.ic_m_download, R.string.action_save_device, () -> saveToDevice(f))
                .add(R.drawable.ic_m_organize, R.string.tool_organize, () -> runTool(TOOL_ORGANIZE, f, f.getName(), true))
                .add(R.drawable.ic_m_split, R.string.tool_split, () -> runTool(TOOL_SPLIT, f, f.getName(), true))
                .add(R.drawable.ic_m_lock, R.string.tool_protect, () -> runTool(TOOL_PROTECT, f, f.getName(), true))
                .add(R.drawable.ic_m_lock_open, R.string.tool_unlock, () -> runTool(TOOL_UNLOCK, f, f.getName(), true))
                .add(R.drawable.ic_m_open, R.string.action_open_with, () -> FileActions.openWith(this, f))
                .add(R.drawable.ic_m_edit, R.string.action_rename, () -> showRenameDialog(f))
                .add(R.drawable.ic_m_delete, R.string.action_delete, () -> confirmDelete(Collections.singletonList(f)))
                .show();
    }

    private void saveToDevice(File f) {
        pendingSave = f;
        try {
            createDoc.launch(f.getName());
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_app_found, Toast.LENGTH_LONG).show();
        }
    }

    private void onSaveTarget(Uri uri) {
        File f = pendingSave;
        pendingSave = null;
        if (uri == null || f == null) return;
        Tools.run(this, () -> {
            FileActions.copyTo(this, f, uri);
            return FileActions.displayName(this, uri);
        }, name -> Toast.makeText(this, getString(R.string.saved_to, name), Toast.LENGTH_LONG).show());
    }

    private void onFolderPicked(Uri tree) {
        List<File> files = pendingSaveAll;
        pendingSaveAll = Collections.emptyList();
        if (tree == null || files.isEmpty()) return;
        Tools.run(this, () -> {
            DocumentFile dir = DocumentFile.fromTreeUri(this, tree);
            if (dir == null) throw new java.io.IOException("No folder");
            for (File f : files) {
                DocumentFile out = dir.createFile("application/pdf", f.getName());
                if (out == null) throw new java.io.IOException("Cannot create " + f.getName());
                FileActions.copyTo(this, f, out.getUri());
            }
            return dir.getName();
        }, name -> {
            if (actionMode != null) actionMode.finish();
            Toast.makeText(this, getString(R.string.saved_to, name), Toast.LENGTH_LONG).show();
        });
    }

    private void showRenameDialog(final File f) {
        View view = LayoutInflater.from(this).inflate(R.layout.rename_layout, null);
        TextInputLayout layout = view.findViewById(R.id.renameLayout);
        TextInputEditText edit = view.findViewById(R.id.renameEditText2);
        edit.setText(PdfNames.baseName(f.getName()));
        edit.selectAll();
        androidx.appcompat.app.AlertDialog d = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.rename_title)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.action_rename, null)
                .create();
        d.setOnShowListener(x -> {
            edit.requestFocus();
            d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
                String newName = PdfNames.toPdfFileName(edit.getText() == null ? "" : edit.getText().toString());
                if (newName == null) {
                    layout.setError(getString(R.string.name_empty));
                    return;
                }
                if (!newName.equals(f.getName())) {
                    File target = PdfNames.uniqueFile(f.getParentFile(), newName);
                    if (!f.renameTo(target)) {
                        Toast.makeText(this, R.string.rename_failed, Toast.LENGTH_LONG).show();
                    }
                }
                d.dismiss();
                reloadFiles();
            });
        });
        d.show();
    }

    private void confirmDelete(List<File> files) {
        String title = files.size() == 1 ? getString(R.string.delete_one_title) : getString(R.string.delete_many_title, files.size());
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(R.string.delete_body)
                .setIcon(R.drawable.ic_m_delete)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.action_delete, (d, w) -> {
                    for (File f : files) f.delete();
                    if (actionMode != null) actionMode.finish();
                    reloadFiles();
                })
                .show();
    }

    // ---------------------------------------------------------------- menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main2, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem privacy = menu.findItem(R.id.action_privacy);
        if (privacy != null) privacy.setVisible(AdsManager.get(this).isPrivacyOptionsRequired());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_privacy) {
            AdsManager.get(this).showPrivacyOptions(this);
            return true;
        } else if (id == R.id.action_rateapp) {
            openStore();
            return true;
        } else if (id == R.id.action_tell) {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text, getPackageName()));
            send.setType("text/plain");
            startActivity(Intent.createChooser(send, getString(R.string.action_tell)));
            return true;
        } else if (id == R.id.action_about) {
            showAbout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openStore() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
        } catch (ActivityNotFoundException e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
            } catch (ActivityNotFoundException ignored) {
                Toast.makeText(this, R.string.no_app_found, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showAbout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME)
                .setMessage(R.string.about_body)
                .setIcon(R.mipmap.app_logo)
                .setPositiveButton(R.string.ok, null)
                .setNeutralButton(R.string.about_source, (d, w) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bumbumapp/Pdfmerge")));
                    } catch (ActivityNotFoundException ignored) {
                    }
                })
                .show();
    }

    // ---------------------------------------------------------------- selection

    private void toggleSelection(int position) {
        mAdapter.toggleSelection(position);
        int count = mAdapter.getSelectedItemCount();
        if (count == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(getString(R.string.selected_count, count));
        }
    }

    private List<File> selectedFiles() {
        return PdfNames.pick(items, mAdapter.getSelectedItems());
    }

    private class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_mainactionmode, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.action_delete) {
                confirmDelete(selectedFiles());
                return true;
            } else if (id == R.id.select_all) {
                mAdapter.selectAll();
                mode.setTitle(getString(R.string.selected_count, mAdapter.getSelectedItemCount()));
                return true;
            } else if (id == R.id.action_share) {
                FileActions.share(Main2Activity.this, selectedFiles());
                mode.finish();
                return true;
            } else if (id == R.id.action_copyTo) {
                pendingSaveAll = selectedFiles();
                try {
                    pickFolder.launch(null);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(Main2Activity.this, R.string.no_app_found, Toast.LENGTH_LONG).show();
                }
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
}
