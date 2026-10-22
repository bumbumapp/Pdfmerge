package com.bumbumapps.utility.pdfmerge;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.print.PdfPrint;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.bumbumapps.utility.pdfmerge.Utility.PdfNames;
import com.bumbumapps.utility.pdfmerge.Utility.UiInsets;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;

/** Load a web page and print it to PDF. */
public class WebViewActivity extends AppCompatActivity {

    private WebView webView;
    private BottomSheetDialog bottomSheetDialog;
    private File convertedFile;
    private ExtendedFloatingActionButton fab;
    private LinearProgressIndicator progress;
    private TextInputEditText urlText;
    private boolean printing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        UiInsets.pad(findViewById(R.id.webRoot), false, true);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        fab = findViewById(R.id.webViewFloatingButton);
        fab.setOnClickListener(v -> createWebPrintJob());
        progress = findViewById(R.id.webViewProgressBar);
        urlText = findViewById(R.id.urlText);
        TextInputLayout urlLayout = findViewById(R.id.urlLayout);
        urlLayout.setEndIconOnClickListener(v -> go());
        urlText.setOnEditorActionListener((TextView v, int id, KeyEvent e) -> {
            if (id == EditorInfo.IME_ACTION_GO || (e != null && e.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                go();
                return true;
            }
            return false;
        });

        webView = findViewById(R.id.webView);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgressCompat(newProgress, true);
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progress.setVisibility(View.VISIBLE);
                urlText.setText(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String scheme = request.getUrl().getScheme();
                // Keep http(s) navigation inside; ignore intent:, mailto:, market: … links.
                return scheme == null || !(scheme.equals("http") || scheme.equals("https"));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progress.setVisibility(View.INVISIBLE);
                if (!"about:blank".equals(url)) fab.show();
            }
        });

        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet, null);
        ((TextView) bottomSheetView.findViewById(R.id.progressText)).setText(R.string.web_converting);
        LinearProgressIndicator sheetProgress = bottomSheetView.findViewById(R.id.progressBar);
        sheetProgress.setIndeterminate(true);
        bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.setCancelable(false);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (printing) return;
                if (webView.canGoBack()) webView.goBack();
                else finish();
            }
        });

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            urlText.requestFocus();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    private void go() {
        String url = PdfNames.normalizeUrl(urlText.getText() == null ? "" : urlText.getText().toString());
        if (url == null) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(urlText.getWindowToken(), 0);
        urlText.clearFocus();
        fab.hide();
        webView.loadUrl(url);
    }

    private void createWebPrintJob() {
        String jobName = getString(R.string.app_name) + " Document";
        PrintAttributes attributes = new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(new PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS).build();
        File myDir = new File(getCacheDir(), "PDFMerger");
        if (!myDir.exists()) myDir.mkdirs();
        PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(jobName);
        String title = webView.getTitle();
        String name = PdfNames.toPdfFileName(title == null || title.trim().isEmpty() ? "Web page" : title);
        if (name == null) name = "Web page.pdf";
        convertedFile = PdfNames.uniqueFile(myDir, name);
        printing = true;
        bottomSheetDialog.show();
        new PdfPrint(attributes, printAdapter, myDir, convertedFile.getName(), this).print();
    }

    /** Called by {@link PdfPrint} when the page was written. */
    public void onPrinted(boolean ok) {
        printing = false;
        if (isFinishing() || isDestroyed()) return;
        if (bottomSheetDialog.isShowing()) bottomSheetDialog.dismiss();
        if (!ok || convertedFile == null || convertedFile.length() == 0) {
            Toast.makeText(this, R.string.operation_failed, Toast.LENGTH_LONG).show();
            return;
        }
        Intent i = new Intent();
        i.putExtra("WebFileName", convertedFile.getAbsolutePath());
        setResult(RESULT_OK, i);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (bottomSheetDialog != null && bottomSheetDialog.isShowing()) bottomSheetDialog.dismiss();
        webView.destroy();
        super.onDestroy();
    }
}
