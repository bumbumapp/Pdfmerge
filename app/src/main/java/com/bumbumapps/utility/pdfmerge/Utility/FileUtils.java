package com.bumbumapps.utility.pdfmerge.Utility;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

public class FileUtils {

    public static void copy(File selectedFile, DocumentFile newFile, Context context) throws IOException {
        try {
            OutputStream out = context.getContentResolver().openOutputStream(newFile.getUri());
            FileInputStream in = new FileInputStream(selectedFile.getPath());
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int dpToPx(Context c, int dp) {
        Resources r = c.getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }
}
