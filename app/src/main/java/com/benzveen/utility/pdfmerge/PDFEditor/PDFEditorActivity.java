package com.benzveen.utility.pdfmerge.PDFEditor;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.benzveen.utility.pdfmerge.DataAdapter.AdapterGridBasic;
import com.benzveen.utility.pdfmerge.DataAdapter.SpacingItemDecoration;
import com.benzveen.utility.pdfmerge.R;
import com.benzveen.utility.pdfmerge.Utility.FileUtils;
import com.benzveen.utility.pdfmerge.Utility.ItemTouchHelperClass;
import com.benzveen.utility.pdfmerge.Utility.PDFDocument;
import com.benzveen.utility.pdfmerge.Utility.PDFPage;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfNumber;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PDFEditorActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdapterGridBasic mAdapter;
    public static File currentFile;
    private PDFDocument document;
    private ArrayList<PDFPage> pages;
    public ItemTouchHelper itemTouchHelper;
    private ProgressBar progressBar;
    private ActionMode actionMode;
    private ActionModeCallback actionModeCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdfeditor);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        if (currentFile != null)
            setTitle(currentFile.getName());
        progressBar = findViewById(R.id.editProgress);
        OpenDocument("");

        actionModeCallback = new ActionModeCallback();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.editor_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.editor_save) {
            progressBar.setVisibility(View.VISIBLE);
            RearrangePage();
        }
        if (id == R.id.editor_selectAll) {
            if (actionMode == null) {
                actionMode = startSupportActionMode(actionModeCallback);
            }
            selectAll();
        }
        if (id == R.id.editor_selectEvenPages) {
            if (actionMode == null) {
                actionMode = startSupportActionMode(actionModeCallback);
            }
            selectOddPages(false);

        }
        if (id == R.id.editor_selectOddPages) {
            if (actionMode == null) {
                actionMode = startSupportActionMode(actionModeCallback);
            }
            selectOddPages(true);

        }
        if (id == R.id.editor_selectPortraitPages) {
            if (actionMode == null) {
                actionMode = startSupportActionMode(actionModeCallback);
            }
            selectPotrait(true);
        }
        if (id == R.id.editor_selectLandscapePages) {
            if (actionMode == null) {
                actionMode = startSupportActionMode(actionModeCallback);
            }
            selectPotrait(false);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void initComponent() {
        recyclerView = findViewById(R.id.recycleViewEditor);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setItemAnimator(null);
        recyclerView.addItemDecoration(new SpacingItemDecoration(2, FileUtils.dpToPx(this, 8), true));
        ArrayList<PDFPage> pageCollection = getPDFPAges();
        mAdapter = new AdapterGridBasic(this, pageCollection);
        recyclerView.setAdapter(mAdapter);

        mAdapter.setDragListener(new AdapterGridBasic.OnStartDragListener() {
            @Override
            public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
                if (actionMode == null)
                    itemTouchHelper.startDrag(viewHolder);
            }
        });

        mAdapter.setOnItemClickListener(new AdapterGridBasic.OnItemClickListener() {
            @Override
            public void onItemClick(View view, PDFPage obj, int position) {
                if (mAdapter.getSelectedItemCount() > 0) {
                    enableActionMode(position);
                } else {
                    // currenSelected = position;
                    // CropImage.activity(mAdapter.getItem(position).getImageDocument())
                    //.start(mainActivity);
                    DoClickAction(view, pages.get(position), position);
                }
            }

            @Override
            public void onItemLongClick(View view, PDFPage obj, int pos) {
                enableActionMode(pos);
            }
        });
        ItemTouchHelper.Callback callback = new ItemTouchHelperClass(mAdapter, true);
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);


    }

    public ArrayList<PDFPage> getPDFPAges() {
        pages = new ArrayList<>();
        if (document != null) {
            for (int i = 0; i < document.getmNumPages(); i++) {
                PDFPage page = new PDFPage(i, document);
                pages.add(page);
            }
        }
        return pages;
    }

    public void DoClickAction(View view, PDFPage obj, int position) {
        switch (view.getId()) {
            case R.id.rotatLeft:
                obj.setRotation(-90);
                mAdapter.notifyItemChanged(position);
                break;
            case R.id.rotateRight:
                obj.setRotation(90);
                mAdapter.notifyItemChanged(position);
                break;
            case R.id.delete:
                if (pages.size() > 1) {
                    pages.remove(obj);
                    mAdapter.notifyDataSetChanged();
                } else
                    Toast.makeText(PDFEditorActivity.this, "Cannot delete page, At least one page must present in the PDF document", Toast.LENGTH_LONG).show();
                break;
        }

    }

    void OpenDocument(String password) {
        try {
            document = new PDFDocument(currentFile.getAbsolutePath());
            document.openRenderer();
            initComponent();
        } catch (Exception e) {
            Toast toast = Toast.makeText(this, "PDF is password protected. Please make sure" +
                    " you run this on a unprotected PDF file.", Toast.LENGTH_LONG);
            toast.show();
            finish();
        }
    }


    public void RearrangePage() {
        new AsyncTask<Object, Object, Boolean>(

        ) {
            @Override
            protected Boolean doInBackground(Object[] objects) {
                try {
                    PdfReader reader = document.getPdfReader();
                    List<Integer> selectedPages = new ArrayList<>();
                    for (PDFPage page : pages) {
                        selectedPages.add(page.getIndex() + 1);
                        if (page.getRotation() > 0) {
                            int rotation = page.getRotation();
                            PdfDictionary pageDictionary = reader.getPageN(page.getIndex() + 1);
                            int rotation1 = reader.getPageRotation(page.getIndex() + 1);
                            rotation += rotation1;

                            if (rotation < 0) {
                                rotation += 360;
                            } else if (rotation > 360) {
                                rotation -= 360;
                            }
                            pageDictionary.put(PdfName.ROTATE, new PdfNumber(rotation));
                        }
                    }
                    reader.selectPages(selectedPages);
                    PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(currentFile));
                    stamper.close();
                    reader.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return false;
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                progressBar.setVisibility(View.GONE);
                if (result)
                    Toast.makeText(PDFEditorActivity.this, "PDF Document saved successfully: " + currentFile.getName(), Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(PDFEditorActivity.this, "Unexpected error occurs while saving the document", Toast.LENGTH_LONG).show();
                try {
                    document.closeRenderer();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finish();
            }
        }.execute();


    }


    private void enableActionMode(int position) {
        if (actionMode == null) {
            actionMode = startSupportActionMode(actionModeCallback);
        }
        toggleSelection(position);
    }

    private void toggleSelection(int position) {
        mAdapter.toggleSelection(position);
        int count = mAdapter.getSelectedItemCount();

        if (count == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(String.valueOf(count));
            actionMode.invalidate();
        }
    }

    private class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_editactionmode, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.edit_delete) {
                showCustomDeleteAllDialog(mode);
                return true;
            }
            if (id == R.id.edit_rotateRight) {
                RotateAll(90);
                return true;
            }
            if (id == R.id.edit_rotateleft) {
                RotateAll(-90);
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mAdapter.clearSelections();
            actionMode = null;

        }

    }

    private void RotateAll(int rotation) {
        List<Integer> selectedItemPositions = mAdapter.getSelectedItems();
        for (Integer i : selectedItemPositions) {
            pages.get(i).setRotation(rotation);
            mAdapter.notifyItemChanged(i);
        }

    }

    private void selectAll() {
        mAdapter.selectAll();
        int count = mAdapter.getSelectedItemCount();

        if (count == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(String.valueOf(count));
            actionMode.invalidate();
        }
    }

    private void DeleteAll() {
        List<Integer> selectedItemPositions = mAdapter.getSelectedItems();
        for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
            mAdapter.removeData(selectedItemPositions.get(i));
        }
        mAdapter.notifyDataSetChanged();
    }

    private void selectOddPages(Boolean value) {
        if (value)
            mAdapter.selectOdd();
        else
            mAdapter.selectEven();
        int count = mAdapter.getSelectedItemCount();

        if (count == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(String.valueOf(count));
            actionMode.invalidate();
        }
    }

    private void selectPotrait(Boolean value) {
        mAdapter.selectPotrait(value);
        int count = mAdapter.getSelectedItemCount();
        if (count == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(String.valueOf(count));
            actionMode.invalidate();
        }
    }


    public void showCustomDeleteAllDialog(final ActionMode mode) {
        boolean isSelectedAll = false;
        if (mAdapter.getSelectedItemCount() == pages.size()) {
            isSelectedAll = true;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (!isSelectedAll)
            builder.setMessage("Are you sure you want to delete the pages from the document?");
        else
            builder.setMessage("You cannot delete all pages. At least one page must remain.");

        final boolean finalIsSelectedAll = isSelectedAll;
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (!finalIsSelectedAll) {
                    DeleteAll();
                    mode.finish();
                }
            }
        });
        if (!isSelectedAll) {
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                }
            });
        }

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
