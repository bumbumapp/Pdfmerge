package com.bumbumapps.utility.pdfmerge;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.print.PdfPrint;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;

public class WebViewActivity extends AppCompatActivity {

    private static final String DESKTOP_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2049.0 Safari/537.36";
    WebView webView;
    PdfPrint pdfPrint;
    String fileName;
    public BottomSheetDialog bottomSheetDialog;
    private ProgressBar progressBar;
    private File convertedFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.webViewFloatingButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createWebPrintJob(webView);
            }
        });
        fab.hide();

        final EditText urltext = (EditText) findViewById(R.id.urlText);
        ImageButton goUrl = (ImageButton) findViewById(R.id.goButton);

        webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setUserAgentString(DESKTOP_USER_AGENT);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {

                super.onPageStarted(view, url, favicon);
                findViewById(R.id.webViewProgressBar).setVisibility(View.VISIBLE);
                fab.hide();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            public void onPageFinished(WebView view, String url) {
                fab.show();
                findViewById(R.id.webViewProgressBar).setVisibility(View.INVISIBLE);
            }
        });


        goUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String urlLink = urltext.getText().toString();
                WebView webView = (WebView) findViewById(R.id.webView);
                webView.loadUrl(urlLink);
            }
        });

        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet, null);
        TextView textView = bottomSheetView.findViewById(R.id.progressText);
        textView.setText("Converting to PDF");
        bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.setCancelable(false);
        progressBar = bottomSheetView.findViewById(R.id.progressBar);
        progressBar.setIndeterminate(true);

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }


    private void createWebPrintJob(WebView webView) {

        String jobName = getString(R.string.app_name) + " Document";
        PrintAttributes attributes = new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(new PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS).build();

        File root = getCacheDir();
        File myDir = new File(root + "/PDFMerger");
        if (!myDir.exists()) {
            myDir.mkdirs();
        }
        PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(jobName);
        fileName = "HTML_" + System.currentTimeMillis() + ".pdf";
        convertedFile = new File(myDir, fileName);
        pdfPrint = new PdfPrint(attributes, printAdapter, myDir, fileName, this);
        bottomSheetDialog.show();
        pdfPrint.print();
       /* new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                bottomSheetDialog.show();
            }

            @Override
            protected Void doInBackground(Void... voids) {

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
            }

        }.execute();*/

    }

    public void makeResult() {
        Intent i = new Intent();
        String fileName = convertedFile.getAbsolutePath();
        i.putExtra("WebFileName", fileName);
        setResult(RESULT_OK, i);
        finish();
    }

}
