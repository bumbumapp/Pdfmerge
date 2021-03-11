package com.benzveen.utility.pdfmerge.Utility;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;

import java.util.Comparator;

public class FileComparator {

    public static Comparator<PDFDocument> getLastModifiedFileComparator() {
        return new Comparator<PDFDocument>() {
            @Override
            public int compare(PDFDocument file1, PDFDocument file2) {
                long result = 0;
                if (!isDescending)
                    result = file2.getLastModified() - file1.getLastModified();
                else
                    result = file1.getLastModified() - file2.getLastModified();
                if (result < 0) {
                    return -1;
                } else if (result > 0) {
                    return 1;
                } else {
                    return 0;
                }
            }
        };
    }

    public static Comparator<PDFDocument> getSizeFileComparator() {
        return new Comparator<PDFDocument>() {
            @Override
            public int compare(final PDFDocument file1, final PDFDocument file2) {
                long size1 = 0;
                size1 = file1.getSize();
                long size2 = 0;
                size2 = file2.getSize();
                long result = 0;
                if (!isDescending)
                    result = size1 - size2;
                else
                    result = size2 - size1;
                if (result < 0) {
                    return -1;
                } else if (result > 0) {
                    return 1;
                } else {
                    return 0;
                }
            }
        };

    }


    public static boolean isDescending = false;

    public static Comparator<PDFDocument> getNameFileComparator() {
        return new Comparator<PDFDocument>() {
            @Override
            public int compare(PDFDocument f1, PDFDocument f2) {
                if (!isDescending)
                    return f1.getFileName().compareTo(f2.getFileName());
                else
                    return f2.getFileName().compareTo(f1.getFileName());
            }
        };
    }

    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        try {
            // DocumentProvider
            if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }
                }
            }
        } catch (Exception ex) {
            return null;
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }
}
