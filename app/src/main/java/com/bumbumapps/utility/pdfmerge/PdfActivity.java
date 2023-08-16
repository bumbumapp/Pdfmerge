package com.bumbumapps.utility.pdfmerge;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.bumbumapps.utility.pdfmerge.R;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfPasswordException;

import java.util.List;

public class PdfActivity extends AppCompatActivity implements OnPageChangeListener, OnLoadCompleteListener {
    PDFView pdfView;
    Integer pageNumber = 0;
    String pdfFileName;
    String TAG="PdfActivity";
    private String PASSWORD = null;

    int position=-1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);
        init();
    }
    private void init(){
        pdfView= (PDFView)findViewById(R.id.pdfView);
        position = getIntent().getIntExtra("position",-1);
        displayFromSdcard();
    }

    private void displayFromSdcard() {
        if (!Main2Activity.items.isEmpty()) {
            pdfFileName = Main2Activity.items.get(position).getName();
            pdfView.fromFile(Main2Activity.items.get(position))
                    .defaultPage(pageNumber)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .onPageChange(this)
                    .onError(new OnErrorListener() {
                                 @Override
                                 public void onError(Throwable t) {
                                     if (t instanceof PdfPasswordException) {
                                         if (PASSWORD !=null){
                                             Toast.makeText(PdfActivity.this,R.string.wrong_password,Toast.LENGTH_LONG).show();
                                         }
                                         askForPdfPassword();
                                     } else {
                                         Toast.makeText(PdfActivity.this, R.string.file_opening_error, Toast.LENGTH_LONG).show();
                                         Log.e(TAG, getString(R.string.file_opening_error), t);
                                     }
                                 }

                             }
                    )
                    .enableAnnotationRendering(true)
                    .onLoad(this)
                    .password(PASSWORD)
                    .scrollHandle(new DefaultScrollHandle(this))
                    .load();
        }
    }
    @Override
    public void onPageChanged(int page, int pageCount) {
        pageNumber = page;
        setTitle(String.format("%s %s / %s", pdfFileName, page + 1, pageCount));
    }

    private void askForPdfPassword() {
        final EditText[] passwordInput = new EditText[1]; // Declare as final array to access inside the listener
        final AlertDialog alert = new AlertDialog.Builder(PdfActivity.this, R.style.DialogThemeLight)
                .setTitle(R.string.protected_pdf)
                .setView(R.layout.check_password)
                .setIcon(R.drawable.lock)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onBackPressed();
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PASSWORD = passwordInput[0].getText().toString();
                        displayFromSdcard();
                    }
                })
                .create();

        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                passwordInput[0] = alert.findViewById(R.id.passwordInput); // Initialize the array element

                // Initialize the array element
            }
        });

        alert.setCancelable(false);
        alert.show();

    }
    @Override
    public void loadComplete(int nbPages) {
        PdfDocument.Meta meta = pdfView.getDocumentMeta();
        printBookmarksTree(pdfView.getTableOfContents(), "-");

    }

    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
        for (PdfDocument.Bookmark b : tree) {

            Log.e(TAG, String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("onPause","'Onpause");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(this,Main2Activity.class));
    }
}