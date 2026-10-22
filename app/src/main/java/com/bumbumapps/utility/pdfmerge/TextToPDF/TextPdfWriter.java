package com.bumbumapps.utility.pdfmerge.TextToPDF;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Renders plain text (optional title) to an A4 PDF with automatic pagination, fully on-device. */
public final class TextPdfWriter {
    private static final int PAGE_W = 595, PAGE_H = 842, MARGIN = 48;

    private TextPdfWriter() {
    }

    public static int write(String title, String body, float fontSizePt, File out) throws IOException {
        PdfDocument doc = new PdfDocument();
        int contentW = PAGE_W - 2 * MARGIN;
        int contentH = PAGE_H - 2 * MARGIN;

        TextPaint titlePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(fontSizePt * 1.6f);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        TextPaint bodyPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(Color.BLACK);
        bodyPaint.setTextSize(fontSizePt);

        StaticLayout titleLayout = null;
        if (title != null && !title.trim().isEmpty()) {
            titleLayout = StaticLayout.Builder.obtain(title.trim(), 0, title.trim().length(), titlePaint, contentW)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL).build();
        }
        String text = body == null ? "" : body;
        StaticLayout bodyLayout = StaticLayout.Builder.obtain(text, 0, text.length(), bodyPaint, contentW)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0, 1.25f).build();

        int pageNo = 0;
        int line = 0;
        int lineCount = bodyLayout.getLineCount();
        do {
            pageNo++;
            PdfDocument.Page page = doc.startPage(new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create());
            Canvas c = page.getCanvas();
            c.translate(MARGIN, MARGIN);
            int used = 0;
            if (pageNo == 1 && titleLayout != null) {
                titleLayout.draw(c);
                used = titleLayout.getHeight() + (int) (fontSizePt * 1.2f);
                c.translate(0, used);
            }
            int available = contentH - used;
            int startLine = line;
            int startTop = lineCount > 0 ? bodyLayout.getLineTop(startLine) : 0;
            while (line < lineCount && bodyLayout.getLineBottom(line) - startTop <= available) line++;
            if (line == startLine && line < lineCount) line++; // a single giant line: still progress
            if (lineCount > 0) {
                int endBottom = bodyLayout.getLineBottom(line - 1);
                c.save();
                c.clipRect(0, 0, contentW, endBottom - startTop);
                c.translate(0, -startTop);
                bodyLayout.draw(c);
                c.restore();
            }
            doc.finishPage(page);
        } while (line < lineCount);

        try (FileOutputStream os = new FileOutputStream(out)) {
            doc.writeTo(os);
        } finally {
            doc.close();
        }
        return pageNo;
    }
}
