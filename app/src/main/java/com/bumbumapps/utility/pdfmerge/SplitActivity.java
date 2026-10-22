package com.bumbumapps.utility.pdfmerge;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumbumapps.utility.pdfmerge.Ads.AdsManager;
import com.bumbumapps.utility.pdfmerge.Ui.FileActions;
import com.bumbumapps.utility.pdfmerge.Ui.Thumbs;
import com.bumbumapps.utility.pdfmerge.Ui.Tools;
import com.bumbumapps.utility.pdfmerge.Utility.PdfNames;
import com.bumbumapps.utility.pdfmerge.Utility.PdfTools;
import com.bumbumapps.utility.pdfmerge.Utility.UiInsets;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Split a PDF: extract chosen pages into one file, or cut it into files of N pages. */
public class SplitActivity extends AppCompatActivity {
    private static final String EXTRA_PATH = "path";
    private static final String EXTRA_PASSWORD = "password";
    private static final String EXTRA_NAME = "name";

    public static Intent intent(Context c, File file, String password, String name) {
        Intent i = new Intent(c, SplitActivity.class);
        i.putExtra(EXTRA_PATH, file.getAbsolutePath());
        i.putExtra(EXTRA_PASSWORD, password);
        i.putExtra(EXTRA_NAME, name);
        return i;
    }

    private File source;
    private String password;
    private String name;
    private int pageCount;
    private MaterialButtonToggleGroup mode;
    private TextInputLayout rangesLayout, everyLayout;
    private TextInputEditText rangesInput, everyInput;
    private TextView preview;
    private MaterialButton splitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_split);
        UiInsets.pad(findViewById(R.id.splitRoot), true, true);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        String path = getIntent().getStringExtra(EXTRA_PATH);
        password = getIntent().getStringExtra(EXTRA_PASSWORD);
        name = getIntent().getStringExtra(EXTRA_NAME);
        if (path == null || !new File(path).exists()) {
            finish();
            return;
        }
        source = new File(path);
        if (name == null) name = source.getName();
        ((TextView) findViewById(R.id.splitName)).setText(name);

        mode = findViewById(R.id.splitMode);
        rangesLayout = findViewById(R.id.rangesLayout);
        everyLayout = findViewById(R.id.everyLayout);
        rangesInput = findViewById(R.id.rangesInput);
        everyInput = findViewById(R.id.everyInput);
        preview = findViewById(R.id.splitPreview);
        splitButton = findViewById(R.id.splitButton);
        splitButton.setEnabled(false);

        mode.addOnButtonCheckedListener((g, id, checked) -> {
            if (!checked) return;
            rangesLayout.setVisibility(id == R.id.modeRanges ? View.VISIBLE : View.GONE);
            everyLayout.setVisibility(id == R.id.modeEvery ? View.VISIBLE : View.GONE);
            updatePreview();
        });
        TextWatcher w = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                updatePreview();
            }
        };
        rangesInput.addTextChangedListener(w);
        everyInput.addTextChangedListener(w);
        splitButton.setOnClickListener(v -> split());

        ImageView thumb = findViewById(R.id.splitThumb);
        Tools.run(this, () -> PdfTools.pageCount(source, password), n -> {
            pageCount = n;
            ((TextView) findViewById(R.id.splitPages)).setText(
                    getResources().getQuantityString(R.plurals.page_count, n, n));
            if (rangesInput.getText() == null || rangesInput.getText().length() == 0) {
                rangesInput.setText(n > 1 ? "1-" + Math.max(1, n / 2) : "1");
            }
            if (n > 2) everyInput.setText("2");
            updatePreview();
        });
        if (password == null) {
            int px = (int) (144 * getResources().getDisplayMetrics().density);
            Thumbs.load(this, thumb, source, Thumbs.key(source), px, info -> {
                if (info.bitmap != null) {
                    thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    thumb.setImageBitmap(info.bitmap);
                }
            });
        } else {
            thumb.setImageResource(R.drawable.ic_m_lock);
        }
    }

    /** The output groups for the current mode, or null when the input is invalid. */
    private List<List<Integer>> groups() {
        if (pageCount < 1) return null;
        int id = mode.getCheckedButtonId();
        try {
            if (id == R.id.modeRanges) {
                String expr = rangesInput.getText() == null ? "" : rangesInput.getText().toString();
                List<List<Integer>> out = new ArrayList<>();
                out.add(PdfTools.parseRanges(expr, pageCount));
                return out;
            } else if (id == R.id.modeEvery) {
                String s = everyInput.getText() == null ? "" : everyInput.getText().toString().trim();
                int n = Integer.parseInt(s);
                if (n < 1) return null;
                return PdfTools.chunks(pageCount, n);
            } else {
                return PdfTools.chunks(pageCount, 1);
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void updatePreview() {
        List<List<Integer>> g = groups();
        rangesLayout.setError(null);
        everyLayout.setError(null);
        if (g == null) {
            splitButton.setEnabled(false);
            if (pageCount > 0) {
                String msg = getString(R.string.split_invalid, pageCount);
                if (mode.getCheckedButtonId() == R.id.modeRanges) rangesLayout.setError(msg);
                else if (mode.getCheckedButtonId() == R.id.modeEvery) everyLayout.setError(" ");
            }
            preview.setText(null);
            return;
        }
        splitButton.setEnabled(true);
        if (mode.getCheckedButtonId() == R.id.modeRanges) {
            int n = g.get(0).size();
            preview.setText(getString(R.string.split_preview_ranges, getResources().getQuantityString(R.plurals.page_count, n, n)));
        } else {
            int n = g.size();
            preview.setText(getString(R.string.split_preview_files, getResources().getQuantityString(R.plurals.file_count, n, n)));
        }
    }

    private void split() {
        List<List<Integer>> g = groups();
        if (g == null) return;
        String base = PdfNames.baseName(name);
        boolean ranges = mode.getCheckedButtonId() == R.id.modeRanges;
        Tools.run(this, () -> {
            ArrayList<File> out = new ArrayList<>();
            File dir = FileActions.myDir(this);
            try {
                for (List<Integer> pages : g) {
                    String suffix = ranges ? "_pages" : pages.size() == 1 ? "_page " + pages.get(0)
                            : "_pages " + pages.get(0) + "-" + pages.get(pages.size() - 1);
                    File f = PdfNames.uniqueFile(dir, PdfNames.toPdfFileName(base + suffix));
                    PdfTools.extractPages(source, password, pages, f);
                    out.add(f);
                }
            } catch (Exception e) {
                for (File f : out) f.delete();
                throw e;
            }
            return out;
        }, out -> AdsManager.get(this).onBreakPoint(this, () -> {
            ResultActivity.start(this, ResultActivity.MODE_SPLIT, out);
            finish();
        }));
    }
}
