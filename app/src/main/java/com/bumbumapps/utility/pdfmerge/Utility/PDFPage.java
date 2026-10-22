package com.bumbumapps.utility.pdfmerge.Utility;

/** A page in the page organizer: its original index, extra rotation and orientation. */
public class PDFPage {

    private final int index;
    private final boolean portrait;
    private int rotation = 0;

    public PDFPage(int index, boolean portrait) {
        this.index = index;
        this.portrait = portrait;
    }

    /** 0-based index of the page in the source document. */
    public int getIndex() {
        return index;
    }

    /** Orientation as displayed, including the user's extra rotation. */
    public boolean isPortrait() {
        return (rotation % 180 == 0) == portrait;
    }

    public void setRotation(int angle) {
        rotation = normalize(rotation + angle);
    }

    /** Maps any angle to 0, 90, 180 or 270. */
    public static int normalize(int angle) {
        return ((angle % 360) + 360) % 360;
    }

    public int getRotation() {
        return rotation;
    }
}
