package com.bumbumapps.utility.pdfmerge;

import static org.junit.Assert.assertEquals;

import com.bumbumapps.utility.pdfmerge.Utility.PDFPage;

import org.junit.Test;

public class RotationTest {
    @Test public void normalizesFullTurns() {
        // Bug: rotating a page right 4 times stored /Rotate 360 (and 450 after 5 turns), which some viewers reject.
        assertEquals(0, PDFPage.normalize(360));
        assertEquals(90, PDFPage.normalize(450));
        assertEquals(270, PDFPage.normalize(-90));
        assertEquals(0, PDFPage.normalize(-360));
    }
}
