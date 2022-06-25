package com.bumbumapps.utility.pdfmerge.TextToPDF;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.print.PdfPrint;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.MediaStore;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumbumapps.utility.pdfmerge.R;
import com.github.irshulx.Editor;
import com.github.irshulx.EditorListener;
import com.github.irshulx.models.EditorTextStyle;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import top.defaults.colorpicker.ColorPickerPopup;


public class TextToPDF extends AppCompatActivity {
    Editor editor;
    PdfPrint pdfPrint;
    private File convertedFile;
    public ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_to_pdf);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        editor = findViewById(R.id.editor);
        editor.render();
        progressBar = findViewById(R.id.textToPDFProgress);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = "<style> img { display: block; margin-left: auto; margin-right: auto; } blockquote{ background-color: #e7f3fe;border-left: 6px solid #2196F3;  margin-bottom: 15px;   padding: 4px 12px;} .editor-image-subtitle{text-align: center;}</style>";
                text += editor.getContentAsHTML();
                WebView webView = new WebView(TextToPDF.this);
                webView.setWebViewClient(new WebViewHelper(TextToPDF.this));
                File root = getCacheDir();
                File myDir = new File(root + "/HtmlData");
                String baseUrl = "file://" + myDir+"/";
                webView.loadDataWithBaseURL(baseUrl, text, "text/html; charset=utf-8", "UTF-8", null);
            }
        });

        setUpEditor();

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void setUpEditor() {
        findViewById(R.id.action_h1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.updateTextStyle(EditorTextStyle.H1);
            }
        });

        findViewById(R.id.action_h2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.updateTextStyle(EditorTextStyle.H2);
            }
        });

        findViewById(R.id.action_h3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.updateTextStyle(EditorTextStyle.H3);
            }
        });

        findViewById(R.id.action_bold).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.updateTextStyle(EditorTextStyle.BOLD);
            }
        });

        findViewById(R.id.action_Italic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.updateTextStyle(EditorTextStyle.ITALIC);
            }
        });

        findViewById(R.id.action_indent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.updateTextStyle(EditorTextStyle.INDENT);
            }
        });

        findViewById(R.id.action_blockquote).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.updateTextStyle(EditorTextStyle.BLOCKQUOTE);
            }
        });

        findViewById(R.id.action_outdent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.updateTextStyle(EditorTextStyle.OUTDENT);
            }
        });

        findViewById(R.id.action_bulleted).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.insertList(false);
            }
        });

        findViewById(R.id.action_unordered_numbered).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.insertList(true);
            }
        });

        findViewById(R.id.action_hr).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.insertDivider();
            }
        });

        findViewById(R.id.action_color).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ColorPickerPopup.Builder(TextToPDF.this)
                        .initialColor(Color.RED) // Set initial color
                        .enableAlpha(true) // Enable alpha slider or not
                        .okTitle("Choose")
                        .cancelTitle("Cancel")
                        .showIndicator(true)
                        .showValue(true)
                        .build()
                        .show(findViewById(android.R.id.content), new ColorPickerPopup.ColorPickerObserver() {
                            @Override
                            public void onColorPicked(int color) {
                                Toast.makeText(TextToPDF.this, "picked" + colorHex(color), Toast.LENGTH_LONG).show();
                                editor.updateTextColor(colorHex(color));
                            }
                        });
            }
        });

        findViewById(R.id.action_insert_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.openImagePicker();
            }
        });

        findViewById(R.id.action_insert_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.insertLink();
            }
        });

        findViewById(R.id.action_erase).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.clearAllContents();
            }
        });

        editor.setEditorListener(new EditorListener() {
            @Override
            public void onTextChanged(EditText editText, Editable text) {
                // Toast.makeText(EditorTestActivity.this, text, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUpload(Bitmap image, String uuid) {
                Toast.makeText(TextToPDF.this, uuid, Toast.LENGTH_LONG).show();
                createTempData(uuid, image);
                editor.onImageUploadComplete(uuid + ".png", uuid);
                // editor.onImageUploadFailed(uuid);
            }

            @Override
            public View onRenderMacro(String name, Map<String, Object> props, int index) {
                return null;
            }

        });
    }

    private String colorHex(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        return String.format(Locale.getDefault(), "#%02X%02X%02X", r, g, b);
    }

    private void createTempData(String filename, Bitmap bmp) {
        File root = getCacheDir();
        File myDir = new File(root + "/HtmlData");
        if (!myDir.exists()) {
            myDir.mkdirs();
        }
        File convertedFile = new File(myDir, filename + ".png");
        String path = convertedFile.getAbsolutePath();
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(convertedFile);
            bmp = resize(bmp, 595, 589);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static Bitmap resize(Bitmap image, int maxWidth, int maxHeight) {
        if (maxHeight > 0 && maxWidth > 0) {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > ratioBitmap) {
                finalWidth = (int) ((float) maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float) maxWidth / ratioBitmap);
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
            return image;
        } else {
            return image;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == editor.PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            CropImage.activity(uri)
                    .start(this);
        } else if (resultCode == Activity.RESULT_CANCELED) {
            //Write your code if there's no result
            Toast.makeText(getApplicationContext(), "Cancelled", Toast.LENGTH_SHORT).show();
            // editor.RestoreState();
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult cropped = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri result = cropped.getUri();
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), result);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                editor.insertImage(bitmap);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = cropped.getError();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        File root = getCacheDir();
        File myDir = new File(root + "/HtmlData");
        if (myDir.exists()) {
            myDir.delete();
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Editor?")
                .setMessage("Are you sure you want to exit the editor?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        finish();
                    }

                })
                .setNegativeButton("No", null)
                .show();
    }

    public void PrintDocument(Context context, WebView webView) {
        PrintManager printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);
        String str = context.getString(R.string.app_name) + " Print Test";
        PrintDocumentAdapter createPrintDocumentAdapter = Build.VERSION.SDK_INT >= 21 ? webView.createPrintDocumentAdapter(str) : webView.createPrintDocumentAdapter();
        if (printManager != null) {
            printManager.print(str, createPrintDocumentAdapter, new PrintAttributes.Builder().build());
        }
    }

    class WebViewHelper extends WebViewClient {

        TextToPDF textToPDFHelper;

        WebViewHelper(TextToPDF textToPDF) {
            this.textToPDFHelper = textToPDF;
        }

        public void onPageFinished(WebView webView, String str) {
            //textToPDFHelper.PrintDocument(textToPDFHelper, webView);
            createWebPrintJob(webView);
        }

        public boolean shouldOverrideUrlLoading(WebView webView, String str) {
            return false;
        }
    }

    private void createWebPrintJob(WebView webView) {

        String jobName = getString(R.string.app_name) + " Document";
        PrintAttributes attributes = new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(new PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS).build();

        File root = getCacheDir();
        File myDir = new File(root + "/PDFMerger");
        if (!myDir.exists()) {
            myDir.mkdirs();
        }
        PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(jobName);
        String fileName = "TextToPDF" + System.currentTimeMillis() + ".pdf";
        convertedFile = new File(myDir, fileName);
        pdfPrint = new PdfPrint(attributes, printAdapter, myDir, fileName, this);
        progressBar.setVisibility(View.VISIBLE);
        pdfPrint.print();
    }

    public void makeResult() {
        Intent i = new Intent();
        String fileName = convertedFile.getAbsolutePath();
        i.putExtra("WebFileName", fileName);
        setResult(RESULT_OK, i);
        finish();
    }

}
