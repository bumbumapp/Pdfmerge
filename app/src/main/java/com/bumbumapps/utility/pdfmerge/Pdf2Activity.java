package com.bumbumapps.utility.pdfmerge;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfPasswordException;

import java.util.List;

public class Pdf2Activity extends AppCompatActivity implements OnPageChangeListener,OnLoadCompleteListener  {
    PDFView pdfView;
    Integer pageNumber = 0;
    String pdfFileName;
    String TAG = "Pdf2Activity";
    ImageView imageView;
    private String PASSWORD = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf2);

        imageView = findViewById(R.id.imageView);
        pdfFileName = getIntent().getStringExtra("fileName");
        pdfView = (PDFView) findViewById(R.id.pdfView);
        displayFromSdcard();
    }



    private void displayFromSdcard() {
        if (pdfFileName.endsWith(".pdf") && !MergeActivity.files.isEmpty() ){
            pdfView.fromUri(MergeActivity.files.get(0))
                    .defaultPage(pageNumber)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .onPageChange(this)
                    .onError(new OnErrorListener() {
                                 @Override
                                 public void onError(Throwable t) {
                                     if (t instanceof PdfPasswordException) {
                                         if (PASSWORD !=null){
                                             Toast.makeText(Pdf2Activity.this,R.string.wrong_password,Toast.LENGTH_LONG).show();
                                         }
                                         askForPdfPassword();
                                     } else {
                                         Toast.makeText(Pdf2Activity.this, R.string.file_opening_error, Toast.LENGTH_LONG).show();
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

    private void askForPdfPassword() {
        final EditText[] passwordInput = new EditText[1]; // Declare as final array to access inside the listener
        final AlertDialog alert = new AlertDialog.Builder(Pdf2Activity.this, R.style.DialogThemeLight)
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
            }
        });

        alert.setCancelable(false);
        alert.show();


    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        if (pdfFileName.contains(".pdf")) {
            pageNumber = page;
            setTitle(String.format("%s %s / %s", pdfFileName, page + 1, pageCount));
        }
    }

    @Override
    public void loadComplete(int nbPages) {
        if (pdfFileName.contains(".pdf")){
            PdfDocument.Meta meta = pdfView.getDocumentMeta();
            printBookmarksTree(pdfView.getTableOfContents(), "-");}

    }

    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
        if (pdfFileName.contains(".pdf")) {
            for (PdfDocument.Bookmark b : tree) {

                Log.e(TAG, String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

                if (b.hasChildren()) {
                    printBookmarksTree(b.getChildren(), sep + "-");
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }
}