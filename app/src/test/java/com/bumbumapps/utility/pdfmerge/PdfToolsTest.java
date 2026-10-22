package com.bumbumapps.utility.pdfmerge;

import static org.junit.Assert.*;

import com.bumbumapps.utility.pdfmerge.Utility.PdfTools;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;

public class PdfToolsTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private File makePdf(int pages, String password) throws Exception {
        File f = tmp.newFile();
        Document d = new Document();
        PdfWriter w = PdfWriter.getInstance(d, new FileOutputStream(f));
        if (password != null) w.setEncryption(password.getBytes(), null, PdfWriter.ALLOW_PRINTING, PdfWriter.STANDARD_ENCRYPTION_128);
        d.open();
        for (int i = 1; i <= pages; i++) {
            d.add(new Paragraph("Page " + i));
            if (i < pages) d.newPage();
        }
        d.close();
        return f;
    }

    private String text(File f, int page) throws Exception {
        PdfReader r = new PdfReader(f.getAbsolutePath());
        try {
            return PdfTextExtractor.getTextFromPage(r, page).trim();
        } finally {
            r.close();
        }
    }

    @Test public void extractKeepsOrder() throws Exception {
        File src = makePdf(5, null);
        File out = tmp.newFile();
        PdfTools.extractPages(src, null, Arrays.asList(4, 2), out);
        assertEquals(2, PdfTools.pageCount(out, null));
        assertEquals("Page 4", text(out, 1));
        assertEquals("Page 2", text(out, 2));
    }

    @Test public void decryptWithPassword() throws Exception {
        File src = makePdf(2, "secret");
        assertTrue(PdfTools.isEncrypted(src));
        try {
            PdfTools.pageCount(src, "wrong");
            fail("expected password error");
        } catch (PdfTools.PasswordRequiredException expected) {
        }
        File out = tmp.newFile();
        PdfTools.decrypt(src, "secret", out);
        assertFalse(PdfTools.isEncrypted(out));
        assertEquals("Page 2", text(out, 2));
    }

    @Test public void chunks() {
        assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5)), PdfTools.chunks(5, 2));
        assertEquals(1, PdfTools.chunks(3, 10).size());
    }

    @Test public void parseRanges() {
        List<Integer> r = PdfTools.parseRanges("1-3, 5; 8-6, 2", 10);
        assertEquals(Arrays.asList(1, 2, 3, 5, 8, 7, 6), r);
        for (String bad : new String[]{"", "0", "11", "a-b", "1-2-3", "4-12"}) {
            try {
                PdfTools.parseRanges(bad, 10);
                fail("accepted " + bad);
            } catch (IllegalArgumentException expected) {
            }
        }
    }
}
