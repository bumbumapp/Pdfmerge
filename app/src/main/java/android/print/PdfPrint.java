package android.print;

import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;

import androidx.appcompat.app.AppCompatActivity;

import com.benzveen.utility.pdfmerge.TextToPDF.TextToPDF;
import com.benzveen.utility.pdfmerge.WebViewActivity;

import java.io.File;

public class PdfPrint {

    private static final String TAG = PdfPrint.class.getSimpleName();
    private final PrintAttributes printAttributes;
    PrintDocumentAdapter printAdapter;
    File path;
    String fileName;
    AppCompatActivity webViewActivity;

    public PdfPrint(PrintAttributes printAttributes, PrintDocumentAdapter printAdapter, File path, String fileName, AppCompatActivity webViewActivity) {
        this.printAttributes = printAttributes;
        this.path = path;
        this.fileName = fileName;
        this.printAdapter = printAdapter;
        this.webViewActivity = webViewActivity;
    }

    public void print() {
        printAdapter.onLayout(null, printAttributes, null, new PrintDocumentAdapter.LayoutResultCallback() {
            @Override
            public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
                try {
                    printAdapter.onWrite(new PageRange[]{PageRange.ALL_PAGES}, getOutputFile(path, fileName), new CancellationSignal(), new PrintDocumentAdapter.WriteResultCallback() {
                        @Override
                        public void onWriteFinished(PageRange[] pages) {
                            super.onWriteFinished(pages);

                            if (webViewActivity instanceof WebViewActivity) {
                                WebViewActivity activity = (WebViewActivity) webViewActivity;
                                activity.bottomSheetDialog.dismiss();
                                activity.makeResult();
                            } else {
                                TextToPDF textToPDF = (TextToPDF) webViewActivity;
                                textToPDF.makeResult();
                            }
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }, null);
    }

    private ParcelFileDescriptor getOutputFile(File path, String fileName) {
        if (!path.exists()) {
            path.mkdirs();
        }
        File file = new File(path, fileName);
        try {
            file.createNewFile();
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE);
        } catch (Exception e) {
        }
        return null;
    }

}