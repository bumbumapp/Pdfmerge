package com.bumbumapps.utility.pdfmerge.Utility;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Pure helpers for file names and selections (unit tested). */
public final class PdfNames {

    private PdfNames() {
    }

    /**
     * Turns user input into a safe PDF file name: strips path separators and characters that are
     * illegal on common file systems, trims, and guarantees exactly one ".pdf" extension.
     * Returns null when nothing usable remains.
     */
    public static String toPdfFileName(String input) {
        if (input == null) return null;
        String name = input.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
        while (name.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            name = name.substring(0, name.length() - 4).trim();
        }
        while (name.startsWith(".")) {
            name = name.substring(1);
        }
        if (name.isEmpty()) return null;
        if (name.length() > 120) name = name.substring(0, 120);
        return name + ".pdf";
    }

    /** Name without the ".pdf" extension, for prefilling edit fields. */
    public static String baseName(String fileName) {
        if (fileName == null) return "";
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return fileName.substring(0, fileName.length() - 4);
        }
        return fileName;
    }

    /**
     * Returns a file in {@code dir} named {@code fileName}; if it already exists, appends " (1)",
     * " (2)" … so an existing document is never silently overwritten.
     */
    public static File uniqueFile(File dir, String fileName) {
        File f = new File(dir, fileName);
        if (!f.exists()) return f;
        String base = baseName(fileName);
        for (int i = 1; i < 10000; i++) {
            f = new File(dir, base + " (" + i + ").pdf");
            if (!f.exists()) return f;
        }
        return new File(dir, base + " " + System.currentTimeMillis() + ".pdf");
    }

    /** Items at the selected adapter positions (ignores out-of-range positions). */
    public static <T> List<T> pick(List<T> items, List<Integer> positions) {
        List<T> out = new ArrayList<>();
        if (items == null || positions == null) return out;
        List<Integer> sorted = new ArrayList<>(positions);
        Collections.sort(sorted);
        for (Integer p : sorted) {
            if (p != null && p >= 0 && p < items.size()) out.add(items.get(p));
        }
        return out;
    }

    /** Human readable size, e.g. "1.2 MB". */
    public static String readableSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.0f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb);
        return String.format(Locale.US, "%.2f GB", mb / 1024.0);
    }

    /** True if the first bytes of a stream are a PDF header ("%PDF-"), allowing leading junk/BOM up to 1 KB. */
    public static boolean looksLikePdf(byte[] head, int length) {
        if (head == null) return false;
        int n = Math.min(length, head.length);
        for (int i = 0; i + 4 < n; i++) {
            if (head[i] == '%' && head[i + 1] == 'P' && head[i + 2] == 'D' && head[i + 3] == 'F' && head[i + 4] == '-') {
                return true;
            }
        }
        return false;
    }

    /** True for JPEG magic bytes. */
    public static boolean looksLikeJpeg(byte[] head, int length) {
        return head != null && length >= 2 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8;
    }

    /** File name without its last extension ("Photo.jpg" → "Photo"). */
    public static String stripExtension(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    /** Case-insensitive "natural" order: "Page 2" < "Page 10". */
    public static int naturalCompare(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        int i = 0, j = 0;
        while (i < a.length() && j < b.length()) {
            char ca = a.charAt(i), cb = b.charAt(j);
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                int si = i, sj = j;
                while (i < a.length() && Character.isDigit(a.charAt(i))) i++;
                while (j < b.length() && Character.isDigit(b.charAt(j))) j++;
                String na = a.substring(si, i).replaceFirst("^0+(?=.)", "");
                String nb = b.substring(sj, j).replaceFirst("^0+(?=.)", "");
                if (na.length() != nb.length()) return na.length() - nb.length();
                int c = na.compareTo(nb);
                if (c != 0) return c;
            } else {
                int c = Character.compare(Character.toLowerCase(ca), Character.toLowerCase(cb));
                if (c != 0) return c;
                i++;
                j++;
            }
        }
        return (a.length() - i) - (b.length() - j);
    }

    /**
     * Turns what the user typed in the address field into a URL: adds https:// when no scheme is
     * given and falls back to a web search for plain words. Returns null for empty input.
     */
    public static String normalizeUrl(String input) {
        if (input == null) return null;
        String s = input.trim();
        if (s.isEmpty()) return null;
        String lower = s.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) return s;
        if (!s.contains(" ") && s.contains(".") && !s.startsWith(".") && !s.endsWith(".")) return "https://" + s;
        try {
            return "https://www.google.com/search?q=" + java.net.URLEncoder.encode(s, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return null;
        }
    }
}
