package com.bumbumapps.utility.pdfmerge.Utility;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.exceptions.BadPasswordException;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** iText based PDF operations that do not depend on Android (unit tested on the JVM). */
public final class PdfTools {

    private PdfTools() {
    }

    /** Thrown when a document needs a (different) password. */
    public static class PasswordRequiredException extends IOException {
        public PasswordRequiredException(Throwable cause) {
            super("Password required", cause);
        }
    }

    public static PdfReader open(File src, String password) throws IOException {
        try {
            byte[] pw = password == null || password.isEmpty() ? null : password.getBytes(StandardCharsets.UTF_8);
            // The user proved knowledge of the document (or it has no user password); allow
            // re-saving even when only an owner password restricts editing.
            PdfReader.unethicalreading = true;
            return new PdfReader(src.getAbsolutePath(), pw);
        } catch (BadPasswordException e) {
            throw new PasswordRequiredException(e);
        }
    }

    public static boolean isEncrypted(File src) {
        try {
            PdfReader r = new PdfReader(src.getAbsolutePath());
            boolean enc = r.isEncrypted();
            r.close();
            return enc;
        } catch (BadPasswordException e) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static int pageCount(File src, String password) throws IOException {
        PdfReader r = open(src, password);
        try {
            return r.getNumberOfPages();
        } finally {
            r.close();
        }
    }

    /** Writes an unencrypted copy of {@code src} (all pages) to {@code dst}. */
    public static void decrypt(File src, String password, File dst) throws IOException {
        int n = pageCount(src, password);
        List<Integer> all = new ArrayList<>();
        for (int i = 1; i <= n; i++) all.add(i);
        extractPages(src, password, all, dst);
    }

    /**
     * Copies the given 1-based pages (in the given order) of {@code src} into a new PDF {@code dst}.
     */
    public static void extractPages(File src, String password, List<Integer> pages, File dst) throws IOException {
        if (pages == null || pages.isEmpty()) throw new IllegalArgumentException("No pages selected");
        PdfReader reader = open(src, password);
        Document document = new Document();
        try (OutputStream out = new FileOutputStream(dst)) {
            PdfCopy copy = new PdfCopy(document, out);
            document.open();
            int n = reader.getNumberOfPages();
            for (Integer p : pages) {
                if (p == null || p < 1 || p > n) throw new IllegalArgumentException("Page out of range: " + p);
                copy.addPage(copy.getImportedPage(reader, p));
            }
            document.close();
        } catch (DocumentException e) {
            throw new IOException(e);
        } finally {
            reader.close();
        }
    }

    /**
     * Groups pages 1..pageCount into consecutive chunks of {@code every} pages.
     * e.g. (5, 2) → [[1,2],[3,4],[5]].
     */
    public static List<List<Integer>> chunks(int pageCount, int every) {
        if (every < 1) throw new IllegalArgumentException("every must be >= 1");
        List<List<Integer>> out = new ArrayList<>();
        for (int start = 1; start <= pageCount; start += every) {
            List<Integer> group = new ArrayList<>();
            for (int p = start; p < start + every && p <= pageCount; p++) group.add(p);
            out.add(group);
        }
        return out;
    }

    /**
     * Parses a page-range expression such as "1-3, 5, 8-6" into 1-based page numbers, keeping
     * order and skipping duplicates. Throws IllegalArgumentException on invalid input or pages
     * outside 1..pageCount.
     */
    public static List<Integer> parseRanges(String expr, int pageCount) {
        if (expr == null || expr.trim().isEmpty()) throw new IllegalArgumentException("Empty range");
        List<Integer> out = new ArrayList<>();
        for (String part : expr.split("[,;]")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            int a, b;
            String[] ab = p.split("\\s*-\\s*");
            try {
                if (ab.length == 1) {
                    a = b = Integer.parseInt(ab[0].trim());
                } else if (ab.length == 2) {
                    a = Integer.parseInt(ab[0].trim());
                    b = Integer.parseInt(ab[1].trim());
                } else {
                    throw new IllegalArgumentException("Bad range: " + p);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Bad range: " + p);
            }
            if (a < 1 || b < 1 || a > pageCount || b > pageCount) {
                throw new IllegalArgumentException("Page out of range: " + p);
            }
            int step = a <= b ? 1 : -1;
            for (int i = a; ; i += step) {
                if (!out.contains(i)) out.add(i);
                if (i == b) break;
            }
        }
        if (out.isEmpty()) throw new IllegalArgumentException("Empty range");
        return out;
    }
}
