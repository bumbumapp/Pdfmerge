package com.benzveen.utility.pdfmerge.Utility;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;

import com.itextpdf.text.pdf.PdfReader;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class PDFDocument {
    private Uri fileUri;
    private String fileName = "";
    private long fileSize;
    long lastModified = 0;
    private File file;
    private boolean isHtmlConversion;
    Context context;

    private PdfRenderer mPdfRenderer;
    private PdfRenderer.Page mCurrentPage;
    private ParcelFileDescriptor mFileDescriptor;
    private PdfReader mReader;
    private int mNumPages;

    public PDFDocument(Context context, Uri fileData) {
        fileUri = fileData;
        this.context = context;
        String scheme = fileUri.getScheme();
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            fileName = fileUri.getLastPathSegment();
            file = new File(fileUri.getPath());
            fileSize = file.length();
            lastModified = file.lastModified();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            try {
                Cursor returnCursor =
                        context.getContentResolver().query(fileUri, null, null, null, null);
                if (returnCursor != null) {
                    int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
                    returnCursor.moveToFirst();
                    fileName = returnCursor.getString(nameIndex);
                    if (fileName == null) {
                        fileName = "Input Document.pdf";
                    }
                    fileSize = returnCursor.getLong(sizeIndex);

                    returnCursor.close();
                }
            } catch (Exception ex) {
                if (fileName == null)
                    fileName = "Input File";
            }
            String path = FileComparator.getPath(context, fileData);
            if (path != null) {
                File file = new File(path);
                if (file != null && file.exists())
                    lastModified = file.lastModified();
            }
        }
    }

    public PDFDocument(String fileName, boolean isHtmlConversion) {
        this.file = new File(fileName);
        this.fileName = file.getName();
        lastModified = file.lastModified();
        this.fileSize = file.length();
        this.isHtmlConversion = isHtmlConversion;
    }

    public PDFDocument(String fileName) {
        this.file = new File(fileName);
        lastModified = file.lastModified();
        this.fileName = file.getName();
        this.fileSize = file.length();
    }

    public String getFileName() {
        return fileName;
    }

    public long getSize() {
        return fileSize;
    }

    public long getLastModified() {
        return lastModified;
    }

    public Uri getPDFFile() {
        return fileUri;
    }

    public int getmNumPages() {
        if (this.mNumPages == 0) {
            this.mNumPages = mPdfRenderer.getPageCount();
        }
        return mNumPages;
    }

    public File getFile() {
        if (file == null) {
            file = new File(fileUri.getPath());
        }
        return file;
    }

    public void deleteFile() {
        if (isHtmlConversion) {
            if (getFile().exists()) {
                getFile().delete();
            }
        }
    }

    public PdfReader getPdfReader() {
        if (this.mReader == null) {
            try {
                InputStream stream = new FileInputStream(this.file);
                this.mReader = new PdfReader(stream);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return mReader;
    }

    public void openRenderer() throws IOException {
        mFileDescriptor = ParcelFileDescriptor.open(this.file, ParcelFileDescriptor.MODE_READ_ONLY);
        if (mFileDescriptor != null) {
            mPdfRenderer = new PdfRenderer(mFileDescriptor);
        }
    }


    public Bitmap showPage(int index) {
        if (this.getmNumPages() <= index) {
            return null;
        }

        if (null != mCurrentPage) {
            mCurrentPage.close();
        }

        mCurrentPage = mPdfRenderer.openPage(index);

        int width = 96 * mCurrentPage.getWidth() / 72;
        int height = 96 * mCurrentPage.getHeight() / 72;
        Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        bitmap.setHasAlpha(false);
        bitmap.eraseColor(-1);
        mCurrentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        return bitmap;
    }

    public void closeRenderer() throws Exception {
        if (mCurrentPage != null)
            mCurrentPage.close();
        if (mPdfRenderer != null)
            mPdfRenderer.close();
        if (mFileDescriptor != null)
            mFileDescriptor.close();
    }
}
