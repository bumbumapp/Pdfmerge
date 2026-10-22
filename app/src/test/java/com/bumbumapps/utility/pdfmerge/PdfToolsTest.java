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

    @Test public void encryptRoundTrip() throws Exception {
        File src = makePdf(3, null);
        assertFalse(PdfTools.needsPassword(src));
        File locked = tmp.newFile();
        PdfTools.encrypt(src, null, locked, "pässwörd");
        assertTrue(PdfTools.needsPassword(locked));
        assertFalse(PdfTools.checkPassword(locked, "nope"));
        assertTrue(PdfTools.checkPassword(locked, "pässwörd"));
        File plain = tmp.newFile();
        PdfTools.decrypt(locked, "pässwörd", plain);
        assertFalse(PdfTools.needsPassword(plain));
        assertEquals("Page 3", text(plain, 3));
    }

    @Test public void organizeReordersRotatesAndDrops() throws Exception {
        File src = makePdf(4, null);
        File out = tmp.newFile();
        PdfTools.organize(src, null, Arrays.asList(3, 1, 4), Arrays.asList(90, 0, 270), out);
        PdfReader r = new PdfReader(out.getAbsolutePath());
        try {
            assertEquals(3, r.getNumberOfPages());
            assertEquals(90, r.getPageRotation(1));
            assertEquals(0, r.getPageRotation(2));
            assertEquals(270, r.getPageRotation(3));
        } finally {
            r.close();
        }
        assertEquals("Page 3", text(out, 1));
        assertEquals("Page 4", text(out, 3));
    }

    @Test public void organizeWorksOnProtectedSource() throws Exception {
        File src = makePdf(2, "pw");
        File out = tmp.newFile();
        PdfTools.organize(src, "pw", Arrays.asList(2, 1), Arrays.asList(0, 0), out);
        // The edited copy stays protected with the same password.
        assertTrue(PdfTools.needsPassword(out));
        File plain = tmp.newFile();
        PdfTools.decrypt(out, "pw", plain);
        assertEquals("Page 2", text(plain, 1));
    }

    @Test public void replaceSwapsFile() throws Exception {
        File target = makePdf(1, null);
        File tmpFile = new File(target.getParentFile(), "x.tmp");
        java.nio.file.Files.copy(makePdf(2, null).toPath(), tmpFile.toPath());
        PdfTools.replace(tmpFile, target);
        assertFalse(tmpFile.exists());
        assertEquals(2, PdfTools.pageCount(target, null));
    }

    private File makeOwnerOnlyPdf() throws Exception {
        File f = tmp.newFile();
        Document d = new Document();
        PdfWriter w = PdfWriter.getInstance(d, new FileOutputStream(f));
        // No user password, only an owner password restricting copying/editing.
        w.setEncryption(null, "owner".getBytes(), PdfWriter.ALLOW_PRINTING, PdfWriter.STANDARD_ENCRYPTION_128);
        d.open();
        d.add(new Paragraph("Restricted"));
        d.close();
        return f;
    }

    @Test public void appendMergesPlainOwnerOnlyAndProtectedInputs() throws Exception {
        // Bug: PDFs with only editing restrictions (owner password) were skipped by merge,
        // and protected PDFs could not be merged at all.
        File out = tmp.newFile();
        Document doc = new Document();
        com.itextpdf.text.pdf.PdfCopy copy = new com.itextpdf.text.pdf.PdfSmartCopy(doc, new FileOutputStream(out));
        doc.open();
        PdfTools.appendPdf(copy, new java.io.FileInputStream(makePdf(2, null)), null);
        PdfTools.appendPdf(copy, new java.io.FileInputStream(makeOwnerOnlyPdf()), null);
        try {
            PdfTools.appendPdf(copy, new java.io.FileInputStream(makePdf(1, "pw")), null);
            fail("expected password error");
        } catch (PdfTools.PasswordRequiredException expected) {
        }
        PdfTools.appendPdf(copy, new java.io.FileInputStream(makePdf(1, "pw")), "pw");
        doc.close();
        assertEquals(4, PdfTools.pageCount(out, null));
        assertEquals("Restricted", text(out, 3));
        assertEquals("Page 1", text(out, 4));
    }
}
