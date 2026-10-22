package com.bumbumapps.utility.pdfmerge.TextToPDF;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumbumapps.utility.pdfmerge.R;
import com.bumbumapps.utility.pdfmerge.Utility.UiInsets;
import com.google.android.material.chip.ChipGroup;


import java.io.File;
import java.util.concurrent.Executors;

/** Write text and turn it into a PDF page set that is added to the merge list. */
public class TextToPDF extends AppCompatActivity {
    private EditText titleInput;
    private EditText bodyInput;
    private ProgressBar progressBar;
    private ChipGroup sizeGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_to_pdf);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        UiInsets.pad(findViewById(R.id.textRoot), true, true);

        titleInput = findViewById(R.id.textTitle);
        bodyInput = findViewById(R.id.textBody);
        progressBar = findViewById(R.id.textToPDFProgress);
        sizeGroup = findViewById(R.id.fontSizeGroup);
        String shared = getIntent().getStringExtra("text");
        if (shared != null) bodyInput.setText(shared);

        com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> createPdf());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmExit();
            }
        });
    }

    private void confirmExit() {
        if (bodyInput.getText().length() == 0 && titleInput.getText().length() == 0) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.discard_text_title)
                .setMessage(R.string.discard_text_message)
                .setPositiveButton(R.string.discard, (d, w) -> finish())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        confirmExit();
        return true;
    }

    private float fontSize() {
        int id = sizeGroup.getCheckedChipId();
        if (id == R.id.sizeSmall) return 10f;
        if (id == R.id.sizeLarge) return 16f;
        return 12f;
    }

    private void createPdf() {
        final String title = titleInput.getText().toString();
        final String body = bodyInput.getText().toString();
        if (body.trim().isEmpty() && title.trim().isEmpty()) {
            bodyInput.setError(getString(R.string.text_empty));
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        final float size = fontSize();
        final File dir = new File(getCacheDir(), "PDFMerger");
        dir.mkdirs();
        final File out = new File(dir, "Text_" + System.currentTimeMillis() + ".pdf");
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean ok;
            try {
                TextPdfWriter.write(title, body, size, out);
                ok = true;
            } catch (Exception e) {
                ok = false;
            }
            final boolean success = ok;
            new Handler(Looper.getMainLooper()).post(() -> {
                progressBar.setVisibility(View.GONE);
                if (success) {
                    Intent i = new Intent();
                    i.putExtra("WebFileName", out.getAbsolutePath());
                    setResult(RESULT_OK, i);
                    finish();
                } else {
                    Toast.makeText(this, R.string.text_failed, Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}
