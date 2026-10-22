package com.bumbumapps.utility.pdfmerge;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.content.IntentCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumbumapps.utility.pdfmerge.Ads.AdsManager;
import com.bumbumapps.utility.pdfmerge.Controller.PDFMerger;
import com.bumbumapps.utility.pdfmerge.TextToPDF.TextToPDF;
import com.bumbumapps.utility.pdfmerge.Ui.ActionSheet;
import com.bumbumapps.utility.pdfmerge.Ui.PasswordPrompt;
import com.bumbumapps.utility.pdfmerge.Ui.Thumbs;
import com.bumbumapps.utility.pdfmerge.Utility.ItemTouchHelperClass;
import com.bumbumapps.utility.pdfmerge.Utility.PDFDocument;
import com.bumbumapps.utility.pdfmerge.Utility.PdfNames;
import com.bumbumapps.utility.pdfmerge.Utility.PdfTools;
import com.bumbumapps.utility.pdfmerge.Utility.RecyclerViewEmptySupport;
import com.bumbumapps.utility.pdfmerge.Utility.UiInsets;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Collect PDFs/images, reorder them and merge into one PDF. */
public class MergeActivity extends AppCompatActivity {
    private static final String STATE_ITEMS = "items";
    private static final String STATE_PASSWORDS = "passwords";
    private static final String STATE_CAMERA = "camera";
    private static final int MAX_PICK = 100;

    private final ArrayList<PDFDocument> dataset = new ArrayList<>();
    private final List<PDFDocument> removedTemps = new ArrayList<>();
    private final ExecutorService checker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private CustomRecyclerViewAdapter adapter;
    private ItemTouchHelper itemTouchHelper;
    private RecyclerViewEmptySupport mRecyclerView;
    private View mCoordLayout;
    private MaterialToolbar toolbar;
    private MaterialButton mergeButton;

    private BottomSheetDialog progressSheet;
    private LinearProgressIndicator progressBar;
    private TextView progressBarPercentage;
    private TextView progressBarCount;

    private String mCurrentCameraFile;
    /** True while the picker opened on launch is showing: cancelling it with no files closes the screen. */
    private boolean initialPick;
    private boolean merging;

    private final ActivityResultLauncher<String[]> openDocs = registerForActivityResult(
            new ActivityResultContracts.OpenMultipleDocuments(), this::addUris);
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia = registerForActivityResult(
            new ActivityResultContracts.PickMultipleVisualMedia(MAX_PICK), this::addUris);
    private final ActivityResultLauncher<Uri> takePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), this::onPictureTaken);
    private final ActivityResultLauncher<Intent> converter = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), this::onConverted);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merge);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        UiInsets.pad(findViewById(R.id.mergeRoot), false, true);
        mCoordLayout = findViewById(R.id.myCoordinatorLayout);

        initProgressSheet();
        initList();
        mergeButton = findViewById(R.id.mergePDFItemFAB);
        mergeButton.setOnClickListener(v -> showMergeOptions());
        findViewById(R.id.addMoreButton).setOnClickListener(v -> showSources());
        findViewById(R.id.emptyAddButton).setOnClickListener(v -> showSources());

        AdsManager.get(this).loadBanner(this, findViewById(R.id.adContainer));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (merging) return;
                if (dataset.size() > 1) {
                    new MaterialAlertDialogBuilder(MergeActivity.this)
                            .setTitle(R.string.discard_list_title)
                            .setMessage(R.string.discard_list_body)
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.leave, (d, w) -> finish())
                            .show();
                } else {
                    finish();
                }
            }
        });

        String action = getIntent().getStringExtra("ActivityAction");
        setTitleFor(action);
        if (savedInstanceState != null) {
            restore(savedInstanceState);
        } else if (action != null) {
            runInitialAction(action);
        }
        updateUi();
    }

    private void setTitleFor(String action) {
        int title = R.string.merge_title;
        if ("PhotoPicker".equals(action)) title = R.string.tool_images;
        else if ("CameraActivity".equals(action)) title = R.string.tool_scan;
        toolbar.setTitle(title);
    }

    private void runInitialAction(String action) {
        switch (action) {
            case "FileSearch":
            case "CloudFileSearch":
                initialPick = true;
                performFileSearch();
                break;
            case "PhotoPicker":
                initialPick = true;
                pickPhotos();
                break;
            case "CameraActivity":
                initialPick = true;
                startCamera();
                break;
            case "HtmlActivity":
                initialPick = true;
                converter.launch(new Intent(this, WebViewActivity.class));
                break;
            case "TextToPDFActivity":
                initialPick = true;
                converter.launch(new Intent(this, TextToPDF.class));
                break;
            case "ImageSend":
                ArrayList<Uri> uris = IntentCompat.getParcelableArrayListExtra(getIntent(), "image", Uri.class);
                addUris(uris);
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------ state

    @Override
    protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        ArrayList<String> items = new ArrayList<>();
        ArrayList<String> passwords = new ArrayList<>();
        for (PDFDocument d : dataset) {
            Object src = d.source();
            items.add(src instanceof Uri ? "u" + src : (d.isTemp() ? "t" : "f") + ((File) src).getAbsolutePath());
            passwords.add(d.getPassword() == null ? "" : d.getPassword());
        }
        out.putStringArrayList(STATE_ITEMS, items);
        out.putStringArrayList(STATE_PASSWORDS, passwords);
        out.putString(STATE_CAMERA, mCurrentCameraFile);
    }

    private void restore(Bundle in) {
        mCurrentCameraFile = in.getString(STATE_CAMERA);
        ArrayList<String> items = in.getStringArrayList(STATE_ITEMS);
        ArrayList<String> passwords = in.getStringArrayList(STATE_PASSWORDS);
        if (items == null) return;
        for (int i = 0; i < items.size(); i++) {
            String s = items.get(i);
            PDFDocument d;
            try {
                if (s.startsWith("u")) d = new PDFDocument(this, Uri.parse(s.substring(1)));
                else d = new PDFDocument(s.substring(1), s.startsWith("t"));
            } catch (Exception e) {
                continue;
            }
            if (passwords != null && i < passwords.size() && !passwords.get(i).isEmpty()) d.setPassword(passwords.get(i));
            dataset.add(d);
        }
        adapter.notifyItemRangeInserted(0, dataset.size());
    }

    // ------------------------------------------------------------ list

    private void initList() {
        mRecyclerView = findViewById(R.id.recycleView);
        mRecyclerView.setEmptyView(findViewById(R.id.toDoEmptyView));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CustomRecyclerViewAdapter(dataset);
        adapter.setOnClickListener(new CustomRecyclerViewAdapter.OnClickListener() {
            @Override
            public void onItemClick(View view, PDFDocument obj, int pos) {
                preview(obj);
            }

            @Override
            public void onRemove(PDFDocument obj, int pos) {
                removeAt(pos, true);
            }
        });
        adapter.setDragListener(vh -> itemTouchHelper.startDrag(vh));
        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelperClass(adapter, false));
        itemTouchHelper.attachToRecyclerView(mRecyclerView);
        mRecyclerView.setAdapter(adapter);
    }

    private void removeAt(int pos, boolean undoable) {
        if (pos < 0 || pos >= dataset.size()) return;
        PDFDocument doc = dataset.remove(pos);
        adapter.notifyItemRemoved(pos);
        adapter.notifyItemRangeChanged(pos, dataset.size() - pos, "order");
        updateUi();
        if (!undoable) {
            if (doc.isTemp()) removedTemps.add(doc);
            return;
        }
        Snackbar.make(mCoordLayout, getString(R.string.removed_file, doc.getFileName()), Snackbar.LENGTH_LONG)
                
                .setAction(R.string.undo, v -> {
                    int at = Math.min(pos, dataset.size());
                    dataset.add(at, doc);
                    adapter.notifyItemInserted(at);
                    adapter.notifyItemRangeChanged(at, dataset.size() - at, "order");
                    updateUi();
                })
                .addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar s, int event) {
                        if (event != DISMISS_EVENT_ACTION && doc.isTemp()) removedTemps.add(doc);
                    }
                })
                .show();
    }

    private void updateUi() {
        int n = dataset.size();
        mergeButton.setEnabled(n > 0 && !merging);
        mergeButton.setText(n > 1 ? getString(R.string.merge_button, n) : getString(R.string.merge_button_one));
        toolbar.setSubtitle(n == 0 ? null : getResources().getQuantityString(R.plurals.file_count, n, n)
                + (n > 1 ? " · " + getString(R.string.merge_hint) : ""));
        findViewById(R.id.mergeBar).setVisibility(n == 0 ? View.GONE : View.VISIBLE);
        invalidateOptionsMenu();
    }

    private void addUris(List<Uri> uris) {
        boolean wasInitial = initialPick;
        initialPick = false;
        if (uris == null || uris.isEmpty()) {
            if (wasInitial && dataset.isEmpty()) finish();
            return;
        }
        List<PDFDocument> added = new ArrayList<>();
        List<String> unreadable = new ArrayList<>();
        for (Uri uri : uris) {
            if (uri == null) continue;
            if (!canRead(uri)) {
                String n = uri.getLastPathSegment();
                unreadable.add(n == null ? uri.toString() : n);
                continue;
            }
            try {
                added.add(new PDFDocument(this, uri));
            } catch (Exception e) {
                unreadable.add(String.valueOf(uri.getLastPathSegment()));
            }
        }
        addDocuments(added);
        if (!unreadable.isEmpty()) {
            Snackbar.make(mCoordLayout, getString(R.string.merge_skipped, android.text.TextUtils.join(", ", unreadable)),
                    Snackbar.LENGTH_LONG).show();
        }
        if (wasInitial && dataset.isEmpty()) finish();
    }

    /** Files shared by other apps may come without read permission or be gone already. */
    private boolean canRead(Uri uri) {
        try (java.io.InputStream in = getContentResolver().openInputStream(uri)) {
            return in != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void addDocuments(List<PDFDocument> docs) {
        if (docs.isEmpty()) return;
        int start = dataset.size();
        dataset.addAll(docs);
        adapter.notifyItemRangeInserted(start, docs.size());
        mRecyclerView.smoothScrollToPosition(dataset.size() - 1);
        updateUi();
        checkProtected(docs);
    }

    /** Asks for the password of each newly added protected PDF (or removes it). */
    private void checkProtected(List<PDFDocument> docs) {
        checker.execute(() -> {
            List<PDFDocument> locked = new ArrayList<>();
            for (PDFDocument d : docs) {
                if (d.isPdf() && d.getPassword() == null && Thumbs.isLocked(getApplicationContext(), d.source())) {
                    locked.add(d);
                }
            }
            if (!locked.isEmpty()) main.post(() -> promptNext(locked, 0));
        });
    }

    private void promptNext(List<PDFDocument> locked, int i) {
        if (i >= locked.size() || isFinishing() || isDestroyed()) return;
        PDFDocument d = locked.get(i);
        if (!dataset.contains(d)) {
            promptNext(locked, i + 1);
            return;
        }
        PasswordPrompt.unlock(this, getString(R.string.file_protected_title, d.getFileName()),
                getString(R.string.file_protected_body), getString(R.string.ok), getString(R.string.skip_file),
                pw -> checkDocPassword(d, pw),
                pw -> {
                    d.setPassword(pw);
                    promptNext(locked, i + 1);
                },
                () -> {
                    removeAt(dataset.indexOf(d), false);
                    promptNext(locked, i + 1);
                });
    }

    private boolean checkDocPassword(PDFDocument d, String pw) {
        try {
            Object src = d.source();
            java.io.InputStream in = src instanceof Uri
                    ? getContentResolver().openInputStream((Uri) src)
                    : new java.io.FileInputStream((File) src);
            return in != null && PdfTools.checkPassword(in, pw);
        } catch (Exception e) {
            return false;
        }
    }

    private void preview(PDFDocument doc) {
        try {
            Object src = doc.source();
            Uri uri = src instanceof Uri ? (Uri) src
                    : FileProvider.getUriForFile(this, getPackageName() + ".provider", (File) src);
            if (doc.isPdf()) {
                Intent i = src instanceof File ? PdfViewerActivity.forFile(this, (File) src)
                        : PdfViewerActivity.forUri(this, uri, doc.getFileName());
                startActivity(i);
            } else {
                Intent target = new Intent(Intent.ACTION_VIEW);
                target.setDataAndType(uri, "image/*");
                target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(target, doc.getFileName()));
            }
        } catch (ActivityNotFoundException e) {
            Snackbar.make(mCoordLayout, R.string.no_app_found, Snackbar.LENGTH_LONG).show();
        } catch (Exception e) {
            Snackbar.make(mCoordLayout, R.string.file_opening_error, Snackbar.LENGTH_LONG).show();
        }
    }

    // ------------------------------------------------------------ sources

    private void showSources() {
        new ActionSheet(this, getString(R.string.add_files), null)
                .add(R.drawable.ic_m_folder, R.string.src_files, R.string.src_files_sub, this::performFileSearch)
                .add(R.drawable.ic_m_image, R.string.src_photos, R.string.src_photos_sub, this::pickPhotos)
                .add(R.drawable.ic_m_camera, R.string.src_camera, R.string.src_camera_sub, this::startCamera)
                .add(R.drawable.ic_m_text, R.string.src_text, R.string.src_text_sub,
                        () -> converter.launch(new Intent(this, TextToPDF.class)))
                .add(R.drawable.ic_m_web, R.string.src_web, R.string.src_web_sub,
                        () -> converter.launch(new Intent(this, WebViewActivity.class)))
                .show();
    }

    public void performFileSearch() {
        try {
            openDocs.launch(new String[]{"application/pdf", "image/jpeg", "image/png"});
        } catch (ActivityNotFoundException e) {
            initialPick = false;
            Snackbar.make(mCoordLayout, R.string.no_app_found, Snackbar.LENGTH_LONG).show();
        }
    }

    private void pickPhotos() {
        pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void startCamera() {
        File dir = new File(getCacheDir(), "PDFMerger");
        dir.mkdirs();
        File photo = new File(dir, "Scan_" + DateFormat.format("yyyyMMdd_HHmmss", System.currentTimeMillis()) + ".jpg");
        mCurrentCameraFile = photo.getAbsolutePath();
        try {
            takePicture.launch(FileProvider.getUriForFile(this, getPackageName() + ".provider", photo));
        } catch (ActivityNotFoundException e) {
            initialPick = false;
            Snackbar.make(mCoordLayout, R.string.no_camera, Snackbar.LENGTH_LONG).show();
        }
    }

    private void onPictureTaken(Boolean ok) {
        boolean wasInitial = initialPick;
        initialPick = false;
        File f = mCurrentCameraFile == null ? null : new File(mCurrentCameraFile);
        if (Boolean.TRUE.equals(ok) && f != null && f.length() > 0) {
            PDFDocument d = new PDFDocument(f.getAbsolutePath(), true);
            addDocuments(Collections.singletonList(d));
        } else if (wasInitial && dataset.isEmpty()) {
            finish();
        }
    }

    private void onConverted(ActivityResult r) {
        boolean wasInitial = initialPick;
        initialPick = false;
        Intent data = r.getData();
        String path = data == null ? null : data.getStringExtra("WebFileName");
        if (r.getResultCode() == RESULT_OK && path != null && new File(path).exists()) {
            addDocuments(Collections.singletonList(new PDFDocument(path, true)));
        } else if (wasInitial && dataset.isEmpty()) {
            finish();
        }
    }

    // ------------------------------------------------------------ menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_merge, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean many = dataset.size() > 1;
        for (int i = 0; i < menu.size(); i++) menu.getItem(i).setVisible(many || menu.getItem(i).getItemId() == R.id.action_clear && !dataset.isEmpty());
        return super.onPrepareOptionsMenu(menu);
    }

    @android.annotation.SuppressLint("NotifyDataSetChanged")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_sort_az || id == R.id.action_sort_za) {
            Collections.sort(dataset, (a, b) -> naturalCompare(a.getFileName(), b.getFileName()));
            if (id == R.id.action_sort_za) Collections.reverse(dataset);
        } else if (id == R.id.action_reverse) {
            Collections.reverse(dataset);
        } else if (id == R.id.action_clear) {
            for (PDFDocument d : dataset) if (d.isTemp()) removedTemps.add(d);
            dataset.clear();
        } else {
            return super.onOptionsItemSelected(item);
        }
        adapter.notifyDataSetChanged();
        updateUi();
        return true;
    }

    /** Case-insensitive compare that orders "Page 2" before "Page 10". */
    static int naturalCompare(String a, String b) {
        return PdfNames.naturalCompare(a, b);
    }

    // ------------------------------------------------------------ merge

    private void showMergeOptions() {
        if (dataset.isEmpty()) {
            Snackbar.make(mCoordLayout, R.string.merge_need_files, Snackbar.LENGTH_LONG).show();
            return;
        }
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View v = LayoutInflater.from(this).inflate(R.layout.sheet_merge_options, null);
        sheet.setContentView(v);
        sheet.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        sheet.getBehavior().setSkipCollapsed(true);

        TextInputLayout nameLayout = v.findViewById(R.id.fileNameLayout);
        TextInputEditText name = v.findViewById(R.id.editText2);
        ChipGroup quality = v.findViewById(R.id.compression);
        MaterialSwitch secure = v.findViewById(R.id.securePDF);
        TextInputLayout pwLayout = v.findViewById(R.id.passwordLayout);
        TextInputEditText pw = v.findViewById(R.id.password);
        MaterialButton go = v.findViewById(R.id.bt_save);

        name.setText(defaultName());
        boolean hasImages = false;
        for (PDFDocument d : dataset) if (!d.isPdf()) hasImages = true;
        quality.setVisibility(hasImages ? View.VISIBLE : View.GONE);
        v.findViewById(R.id.qualityLabel).setVisibility(hasImages ? View.VISIBLE : View.GONE);
        secure.setOnCheckedChangeListener((b, checked) -> {
            pwLayout.setVisibility(checked ? View.VISIBLE : View.GONE);
            if (checked) pw.requestFocus();
        });
        go.setText(mergeButton.getText());
        go.setOnClickListener(b -> {
            String fileName = PdfNames.toPdfFileName(name.getText() == null ? "" : name.getText().toString());
            if (fileName == null) {
                nameLayout.setError(getString(R.string.name_empty));
                return;
            }
            String password = pw.getText() == null ? "" : pw.getText().toString();
            if (secure.isChecked() && password.isEmpty()) {
                pwLayout.setError(getString(R.string.password_empty));
                return;
            }
            int q = quality.getCheckedChipId();
            String compression = q == R.id.qualitySmall ? "High" : q == R.id.qualityBalanced ? "Medium" : "Low";
            sheet.dismiss();
            startMerge(fileName, compression, secure.isChecked() ? password : null);
        });
        sheet.show();
    }

    private String defaultName() {
        if (dataset.size() == 1) return PdfNames.baseName(PdfNames.stripExtension(dataset.get(0).getFileName()));
        return "Merged " + DateFormat.format("yyyy-MM-dd HH.mm", System.currentTimeMillis());
    }

    private void startMerge(String fileName, String compression, String password) {
        merging = true;
        updateUi();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        PDFMerger merger = new PDFMerger(this, fileName);
        merger.setDataSet(new ArrayList<>(dataset));
        merger.setCompression(compression);
        if (password != null) merger.setPassword(password);
        merger.execute();
    }

    private void initProgressSheet() {
        View v = LayoutInflater.from(this).inflate(R.layout.bottom_sheet, null);
        progressSheet = new BottomSheetDialog(this);
        progressSheet.setContentView(v);
        progressSheet.setCancelable(false);
        progressSheet.getBehavior().setDraggable(false);
        progressBar = v.findViewById(R.id.progressBar);
        progressBarPercentage = v.findViewById(R.id.progressPercentage);
        progressBarCount = v.findViewById(R.id.progressCount);
    }

    public void showBottomSheet(int size) {
        if (isFinishing() || isDestroyed()) return;
        progressBar.setMax(Math.max(1, size));
        progressBar.setProgress(0);
        progressSheet.show();
    }

    public void setProgress(int progress, int total) {
        progressBar.setProgressCompat(progress, true);
        progressBarCount.setText(String.format(Locale.getDefault(), "%d / %d", progress, total));
        int percentage = total > 0 ? (progress * 100) / total : 100;
        progressBarPercentage.setText(String.format(Locale.getDefault(), "%d%%", percentage));
    }

    public void runPostExecution(boolean success, final String filePath, String skipped) {
        merging = false;
        if (isFinishing() || isDestroyed()) return;
        dismissProgressDialog();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        updateUi();
        if (!success) {
            Snackbar.make(mCoordLayout, R.string.merge_failed, Snackbar.LENGTH_LONG).show();
            return;
        }
        if (skipped != null && !skipped.isEmpty()) {
            Toast.makeText(this, getString(R.string.merge_skipped, skipped), Toast.LENGTH_LONG).show();
        }
        AdsManager.get(this).onBreakPoint(this, () -> {
            ResultActivity.start(this, ResultActivity.MODE_MERGE, new File(filePath));
            finish();
        });
    }

    private void dismissProgressDialog() {
        if (progressSheet != null && progressSheet.isShowing()) progressSheet.dismiss();
    }

    @Override
    public void onDestroy() {
        dismissProgressDialog();
        checker.shutdownNow();
        super.onDestroy();
        if (isFinishing()) {
            // Temporary PDFs (camera, text, web) are only needed until the merge is done.
            for (PDFDocument d : dataset) d.deleteFile();
            for (PDFDocument d : removedTemps) d.deleteFile();
        }
    }
}
