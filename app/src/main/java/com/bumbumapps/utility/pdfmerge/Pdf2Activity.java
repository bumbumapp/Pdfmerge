package com.bumbumapps.utility.pdfmerge;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.shockwave.pdfium.PdfDocument;

import java.util.List;

public class Pdf2Activity extends AppCompatActivity implements OnPageChangeListener,OnLoadCompleteListener  {
    PDFView pdfView;
    Integer pageNumber = 0;
    String pdfFileName;
    String TAG = "Pdf2Activity";
    ImageView imageView;
    int position = -1;

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

        if (pdfFileName.contains(".pdf")){
            pdfView.fromUri(MergeActivity.files.get(0))
                    .defaultPage(pageNumber)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .onPageChange(this)
                    .enableAnnotationRendering(true)
                    .onLoad(this)
                    .scrollHandle(new DefaultScrollHandle(this))
                    .load();
        }

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