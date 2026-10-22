package com.bumbumapps.utility.pdfmerge.Controller;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.bumbumapps.utility.pdfmerge.MergeActivity;
import com.bumbumapps.utility.pdfmerge.Utility.PDFDocument;
import com.bumbumapps.utility.pdfmerge.Utility.PdfNames;
import com.bumbumapps.utility.pdfmerge.Utility.PdfTools;
import com.bumptech.glide.Glide;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSmartCopy;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.RandomAccessFileOrArray;


import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class PDFMerger extends AsyncTask<Void, Integer, Boolean> {
    MergeActivity fragment;
    ArrayList<PDFDocument> dataSet;
    String fileName;
    String password = "";
    String compression;
    File mcurrentFile;
    String filesNotMerged = "";

    public PDFMerger(MergeActivity fragment, String fileName) {
        this.fragment = fragment;
        this.fileName = fileName;
    }

    public void setDataSet(ArrayList<PDFDocument> dataSet) {
        this.dataSet = dataSet;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        fragment.showBottomSheet(dataSet.size());
        fragment.setProgress(0, dataSet.size());
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        File root = fragment.getFilesDir();
        File myDir = new File(root + "/PDFMerger");
        if (!myDir.exists()) {
            myDir.mkdirs();
        }
        // Never overwrite an existing document with the same name.
        mcurrentFile = PdfNames.uniqueFile(myDir, this.fileName);
        fileName = mcurrentFile.getAbsolutePath();
        try {
            Document destination = new Document();
            PdfCopy copy = new PdfSmartCopy(destination, new FileOutputStream(mcurrentFile));
            if (password.length() > 0) {
                byte[] bytes = password.getBytes("UTF-8");
                copy.setEncryption(bytes, null, PdfTools.ALL_PERMISSIONS, PdfWriter.ENCRYPTION_AES_128);
            }
            destination.open();
            for (int i = 0; i < dataSet.size(); i++) {
                PDFDocument document = dataSet.get(i);
                InputStream stream = null;
                if (document.getPDFFile() != null) {
                    stream = fragment.getContentResolver().openInputStream(document.getPDFFile());
                } else if (document.getFile() != null) {
                    stream = new FileInputStream(document.getFile());
                }

                if (stream == null) {
                    filesNotMerged += document.getFileName() + ",";
                    publishProgress(i + 1);
                    continue;
                }
                BufferedInputStream bis = new BufferedInputStream(stream);
                bis.mark(2048);
                byte[] buffer = new byte[1024];
                int len = 0, r;
                while (len < buffer.length && (r = bis.read(buffer, len, buffer.length - len)) != -1) len += r;
                bis.reset();
                if (PdfNames.looksLikePdf(buffer, len)) {
                    MergePDFDocuments(copy, bis, document);
                } else {
                    DrawImage(copy, bis, buffer, document);
                }
                bis.close();
                publishProgress(i + 1);
            }
            try {
                destination.close();
            } catch (Exception ex) {
                ex.printStackTrace();
                if (mcurrentFile.exists()) {
                    mcurrentFile.delete();
                }
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (mcurrentFile.exists()) mcurrentFile.delete();
            return false;
        }

        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        fragment.runPostExecution(Boolean.TRUE.equals(result), fileName, filesNotMerged.replaceAll(",$", "").replace(",", ", "));
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        fragment.setProgress(values[0], dataSet.size());
    }

    public void MergePDFDocuments(PdfCopy copy, InputStream stream, PDFDocument document) {
        try {
            PdfTools.appendPdf(copy, stream, document.getPassword());
        } catch (Exception ex) {
            filesNotMerged += document.getFileName() + ",";
            ex.printStackTrace();
        }
    }


    public void DrawImage(PdfCopy copy, InputStream stream, byte[] buffer, PDFDocument document) {
        try {
            Image image = null;
            int maxDim;
            int quality;
            if ("High".equals(compression)) {
                maxDim = 900;
                quality = 70;
            } else if ("Medium".equals(compression)) {
                maxDim = 1600;
                quality = 82;
            } else {
                maxDim = 2480;
                quality = 92;
            }
            Object source = document.getPDFFile() != null ? document.getPDFFile() : document.getFile();
            // Glide applies EXIF orientation and downsamples, which avoids OOM on huge photos.
            Bitmap loaded = Glide.with(fragment.getApplicationContext()).asBitmap().load(source)
                    .downsample(com.bumptech.glide.load.resource.bitmap.DownsampleStrategy.AT_MOST)
                    .override(maxDim, maxDim).submit().get();
            if (loaded == null) throw new IOException("Unsupported image");
            // Flatten transparency onto white so PNG cut-outs don't turn black in JPEG.
            Bitmap flat = Bitmap.createBitmap(loaded.getWidth(), loaded.getHeight(), Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(flat);
            canvas.drawColor(android.graphics.Color.WHITE);
            canvas.drawBitmap(loaded, 0, 0, null);
            ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
            flat.compress(Bitmap.CompressFormat.JPEG, quality, imageStream);
            flat.recycle();
            image = Image.getInstance(imageStream.toByteArray());
            image.setAlignment(Image.MIDDLE);
            imageStream.close();
            if (image != null) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                Document bigDoc = new Document(PageSize.A4, 40, 40, 40, 40);
                PdfWriter writer = PdfWriter.getInstance(bigDoc, os);
                bigDoc.open();
                image.scaleToFit(new Rectangle(40, 40, PageSize.A4.getWidth() - 40, PageSize.A4.getHeight() - 40));
                bigDoc.add(image);
                bigDoc.close();
                PdfReader reader = new PdfReader(os.toByteArray());
                copy.addDocument(reader);
                reader.close();
                os.close();
            }
        } catch (Exception ex) {
            filesNotMerged += document.getFileName() + ",";
            ex.printStackTrace();
        }
    }
}
