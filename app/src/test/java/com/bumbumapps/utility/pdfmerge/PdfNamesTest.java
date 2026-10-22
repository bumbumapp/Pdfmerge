package com.bumbumapps.utility.pdfmerge;

import static org.junit.Assert.*;

import com.bumbumapps.utility.pdfmerge.Utility.PdfNames;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class PdfNamesTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    @Test public void renameKeepsPdfExtension() {
        // Bug: renaming "report.pdf" to "report2" produced a file without extension.
        assertEquals("report2.pdf", PdfNames.toPdfFileName("report2"));
        assertEquals("report2.pdf", PdfNames.toPdfFileName("report2.PDF"));
        assertEquals("a.pdf", PdfNames.toPdfFileName("a.pdf.pdf"));
    }

    @Test public void slashesCannotEscapeFolder() {
        // Bug: a "/" in the merge file name made the merge fail (missing directory).
        assertEquals("a_b.pdf", PdfNames.toPdfFileName("a/b"));
        assertEquals("_.._x.pdf", PdfNames.toPdfFileName("/../x"));
    }

    @Test public void emptyNamesRejected() {
        assertNull(PdfNames.toPdfFileName("   "));
        assertNull(PdfNames.toPdfFileName(".pdf"));
        assertNull(PdfNames.toPdfFileName(null));
    }

    @Test public void uniqueFileNeverOverwrites() throws Exception {
        // Bug: merging with an existing name silently deleted the previous document.
        File dir = tmp.newFolder();
        assertTrue(new File(dir, "x.pdf").createNewFile());
        assertEquals("x (1).pdf", PdfNames.uniqueFile(dir, "x.pdf").getName());
        assertTrue(new File(dir, "x (1).pdf").createNewFile());
        assertEquals("x (2).pdf", PdfNames.uniqueFile(dir, "x.pdf").getName());
        assertEquals("y.pdf", PdfNames.uniqueFile(dir, "y.pdf").getName());
    }

    @Test public void pickUsesSelectedPositions() {
        // Bug: "share/copy selected" used loop index instead of the selected positions.
        List<String> items = Arrays.asList("a", "b", "c", "d");
        assertEquals(Arrays.asList("b", "d"), PdfNames.pick(items, Arrays.asList(3, 1)));
        assertEquals(Arrays.asList("a"), PdfNames.pick(items, Arrays.asList(0, 9, -1)));
    }

    @Test public void headerDetection() {
        byte[] pdf = "ï»¿%PDF-1.7".getBytes();
        assertTrue(PdfNames.looksLikePdf(pdf, pdf.length));
        byte[] png = {(byte) 0x89, 'P', 'N', 'G'};
        assertFalse(PdfNames.looksLikePdf(png, png.length));
        assertTrue(PdfNames.looksLikeJpeg(new byte[]{(byte) 0xFF, (byte) 0xD8, 0}, 3));
    }

    @Test public void readableSize() {
        assertEquals("512 B", PdfNames.readableSize(512));
        assertEquals("2 KB", PdfNames.readableSize(2048));
        assertEquals("1.5 MB", PdfNames.readableSize(1572864));
    }
}
