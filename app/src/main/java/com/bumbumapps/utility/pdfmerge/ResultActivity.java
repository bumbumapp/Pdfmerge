package com.bumbumapps.utility.pdfmerge;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumbumapps.utility.pdfmerge.DataAdapter.MainRecycleViewAdapter;
import com.bumbumapps.utility.pdfmerge.Ui.FileActions;
import com.bumbumapps.utility.pdfmerge.Ui.Thumbs;
import com.bumbumapps.utility.pdfmerge.Ui.Tools;
import com.bumbumapps.utility.pdfmerge.Utility.UiInsets;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Success screen shown after a merge, split, protect, unlock or page edit. */
public class ResultActivity extends AppCompatActivity {
    public static final String MODE_MERGE = "merge";
    public static final String MODE_SPLIT = "split";
    public static final String MODE_PROTECT = "protect";
    public static final String MODE_UNLOCK = "unlock";
    public static final String MODE_SAVED = "saved";
    private static final String EXTRA_MODE = "mode";
    private static final String EXTRA_PATHS = "paths";

    public static void start(Context c, String mode, File file) {
        start(c, mode, new ArrayList<>(Collections.singletonList(file)));
    }

    public static void start(Context c, String mode, ArrayList<File> files) {
        ArrayList<String> paths = new ArrayList<>();
        for (File f : files) paths.add(f.getAbsolutePath());
        Intent i = new Intent(c, ResultActivity.class);
        i.putExtra(EXTRA_MODE, mode);
        i.putStringArrayListExtra(EXTRA_PATHS, paths);
        if (!(c instanceof Activity)) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
    }

    private final List<File> files = new ArrayList<>();
    private final ActivityResultLauncher<String> createDoc =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/pdf"), this::onSaveTarget);
    private final ActivityResultLauncher<Uri> pickFolder =
            registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), this::onFolderPicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        UiInsets.pad(findViewById(R.id.resultRoot), true, true);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ArrayList<String> paths = getIntent().getStringArrayListExtra(EXTRA_PATHS);
        if (paths != null) for (String p : paths) {
            File f = new File(p);
            if (f.exists()) files.add(f);
        }
        if (files.isEmpty()) {
            finish();
            return;
        }
        String mode = getIntent().getStringExtra(EXTRA_MODE);
        TextView title = findViewById(R.id.resultTitle);
        title.setText(titleFor(mode));
        ((TextView) findViewById(R.id.resultSubtitle)).setText(getString(R.string.result_files_created,
                getResources().getQuantityString(R.plurals.file_count, files.size(), files.size())));

        MaterialButton primary = findViewById(R.id.btnPrimary);
        MaterialButton share = findViewById(R.id.btnShare);
        MaterialButton save = findViewById(R.id.btnSave);
        if (files.size() == 1) {
            File f = files.get(0);
            bindSingle(f);
            primary.setText(R.string.action_open);
            primary.setIconResource(R.drawable.ic_m_pdf);
            primary.setOnClickListener(v -> FileActions.view(this, f));
            save.setOnClickListener(v -> {
                try {
                    createDoc.launch(f.getName());
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, R.string.no_app_found, Toast.LENGTH_LONG).show();
                }
            });
            share.setOnClickListener(v -> FileActions.share(this, files));
        } else {
            findViewById(R.id.singleCard).setVisibility(View.GONE);
            RecyclerView list = findViewById(R.id.resultList);
            list.setVisibility(View.VISIBLE);
            list.setLayoutManager(new LinearLayoutManager(this));
            MainRecycleViewAdapter adapter = new MainRecycleViewAdapter(this, files);
            adapter.setOnItemClickListener(new MainRecycleViewAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View view, File value, int position) {
                    if (view.getId() == R.id.extramenu) FileActions.share(ResultActivity.this, Collections.singletonList(value));
                    else FileActions.view(ResultActivity.this, value);
                }

                @Override
                public void onItemLongClick(View view, File obj, int pos) {
                }
            });
            list.setAdapter(adapter);
            primary.setText(R.string.share_all);
            primary.setIconResource(R.drawable.ic_m_share);
            primary.setOnClickListener(v -> FileActions.share(this, files));
            share.setVisibility(View.GONE);
            android.view.ViewGroup.MarginLayoutParams lp = (android.view.ViewGroup.MarginLayoutParams) save.getLayoutParams();
            lp.setMarginStart(0);
            save.setLayoutParams(lp);
            save.setText(R.string.action_save_all_folder);
            save.setOnClickListener(v -> {
                try {
                    pickFolder.launch(null);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, R.string.no_app_found, Toast.LENGTH_LONG).show();
                }
            });
        }
        findViewById(R.id.btnHome).setOnClickListener(v -> goHome());
        if (savedInstanceState == null) celebrate();
    }

    private int titleFor(String mode) {
        if (MODE_SPLIT.equals(mode)) return R.string.result_title_split;
        if (MODE_PROTECT.equals(mode)) return R.string.result_title_protect;
        if (MODE_UNLOCK.equals(mode)) return R.string.result_title_unlock;
        if (MODE_SAVED.equals(mode)) return R.string.result_title_saved;
        return R.string.result_title_merge;
    }

    private void bindSingle(File f) {
        ImageView thumb = findViewById(R.id.singleThumb);
        TextView meta = findViewById(R.id.singleMeta);
        ((TextView) findViewById(R.id.singleName)).setText(f.getName());
        meta.setText(FileActions.meta(this, f, 0, false));
        findViewById(R.id.singleCard).setOnClickListener(v -> FileActions.view(this, f));
        int w = (int) (168 * getResources().getDisplayMetrics().density);
        Thumbs.load(this, thumb, f, Thumbs.key(f), w, info -> {
            if (info.bitmap != null) {
                thumb.setImageTintList(null);
                thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
                thumb.setImageBitmap(info.bitmap);
                thumb.setAlpha(0f);
                thumb.animate().alpha(1f).setDuration(250).start();
            } else if (info.locked) {
                thumb.setImageResource(R.drawable.ic_m_lock);
            }
            meta.setText(FileActions.meta(this, f, info.pages, info.locked));
        });
    }

    private void celebrate() {
        View ring = findViewById(R.id.successRing);
        View icon = findViewById(R.id.successIcon);
        ring.setScaleX(0.4f);
        ring.setScaleY(0.4f);
        ring.setAlpha(0f);
        icon.setScaleX(0f);
        icon.setScaleY(0f);
        icon.setRotation(-45f);
        ring.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(380)
                .setInterpolator(new OvershootInterpolator(1.6f)).start();
        icon.animate().scaleX(1f).scaleY(1f).rotation(0f).setStartDelay(160).setDuration(420)
                .setInterpolator(new OvershootInterpolator(2.2f)).start();
        View card = findViewById(files.size() == 1 ? R.id.singleCard : R.id.resultList);
        card.setTranslationY(60f);
        card.setAlpha(0f);
        card.animate().translationY(0f).alpha(1f).setStartDelay(260).setDuration(360).start();
    }

    private void goHome() {
        Intent i = new Intent(this, Main2Activity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    private void onSaveTarget(Uri uri) {
        if (uri == null) return;
        File f = files.get(0);
        Tools.run(this, () -> {
            FileActions.copyTo(this, f, uri);
            return FileActions.displayName(this, uri);
        }, name -> Toast.makeText(this, getString(R.string.saved_to, name), Toast.LENGTH_LONG).show());
    }

    private void onFolderPicked(Uri tree) {
        if (tree == null) return;
        Tools.run(this, () -> {
            DocumentFile dir = DocumentFile.fromTreeUri(this, tree);
            if (dir == null) throw new java.io.IOException("No folder");
            for (File f : files) {
                DocumentFile out = dir.createFile("application/pdf", f.getName());
                if (out == null) throw new java.io.IOException("Cannot create " + f.getName());
                FileActions.copyTo(this, f, out.getUri());
            }
            return dir.getName();
        }, name -> Toast.makeText(this, getString(R.string.saved_to, name), Toast.LENGTH_LONG).show());
    }
}
