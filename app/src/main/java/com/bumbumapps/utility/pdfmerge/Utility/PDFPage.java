package com.bumbumapps.utility.pdfmerge.Utility;

import android.graphics.Bitmap;

import com.itextpdf.text.Rectangle;


public class PDFPage {

    PDFDocument document;
    int index;
    Bitmap bitmap;
    int rotation = 0;

    public PDFPage(int index, PDFDocument document) {
        this.document = document;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public Boolean isPortrait() {
        Rectangle rectangle = getPageSizeWithRotation(index + 1);

        if (rectangle.getHeight() >= rectangle.getWidth())
            return true;
        else
            return false;
    }

    public Rectangle getPageSizeWithRotation(int index) {
        Rectangle rect = this.document.getPdfReader().getPageSize(index);
        int rotation = this.document.getPdfReader().getPageRotation(index);

        rotation += this.rotation;
        if (rotation < 0) {
            rotation += 360;
        } else if (rotation > 360) {
            rotation -= 360;
        }
        while (rotation > 0) {
            rect = rect.rotate();
            rotation -= 90;
        }
        return rect;
    }

    public Bitmap getBitmap() {
        if (bitmap == null) {
            bitmap = this.document.showPage(index);
        }

        return bitmap;
    }

    public void setRotation(int angle) {
        rotation += angle;
        if (rotation < 0) {
            rotation += 360;
        } else if (angle > 360) {
            rotation -= 360;
        }
    }

    public int getRotation() {
        return rotation;
    }
}
