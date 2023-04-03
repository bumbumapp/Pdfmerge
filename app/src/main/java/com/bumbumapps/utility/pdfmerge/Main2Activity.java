package com.bumbumapps.utility.pdfmerge;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.InsetDrawable;
import android.net.Uri;
import android.os.Bundle;

import com.bumbumapps.utility.pdfmerge.PDFEditor.PDFEditorActivity;
import com.bumbumapps.utility.pdfmerge.Utility.FileUtils;
import com.bumbumapps.utility.pdfmerge.Utility.MyBounceInterpolator;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.GridLayoutManager;

import android.view.LayoutInflater;
import android.view.View;

import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumbumapps.utility.pdfmerge.DataAdapter.MainRecycleViewAdapter;
import com.bumbumapps.utility.pdfmerge.DataAdapter.SpacingItemDecoration;
import com.bumbumapps.utility.pdfmerge.Utility.RecyclerViewEmptySupport;
import com.bumbumapps.utility.pdfmerge.Utility.ViewAnimation;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Main2Activity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private FloatingActionButton mAddPDFFAB;
    private boolean rotate = false;
    public static List<File> items = null;
    ImageView about;

    private static final int Merge_Request_CODE = 42;
    private static final String Interstitial = "ca-app-pub-8618312760828115/6802346412";
    private RecyclerViewEmptySupport recyclerView;
    private MainRecycleViewAdapter mAdapter;
    private BottomSheetDialog mBottomSheetDialog;
    private Main2Activity currentActivity;
    private ActionModeCallback actionModeCallback;
    private ActionMode actionMode;
    private static final int RQS_OPEN_DOCUMENT_TREE = 24;
    private static final int RQS_OPEN_DOCUMENT_TREE_ALL = 43;
    private File selectedFile;
    Menu nav_Menu;
    private int PDFEDITOR_Request_CODE = 46;
    private View lyt_addFiles;
    private View lyt_textToPDF;
    private View lyt_cameraToPDF;
    private View lyt_htmlToPDF;
    private View back_drop;
    private View lyt_addCloudFiles;
    private String mCurrentClickedActivity;
    private AppCompatImageView mGiftBox;
    private LinearLayout adView;
    private AdView mAdView;
    private int totalSessionCount = 0;
    public static ArrayList<File> fileList = new ArrayList<File>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentActivity = this;
        setContentView(R.layout.activity_main2);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final Animation myAnim = AnimationUtils.loadAnimation(Main2Activity.this, R.anim.bounce);
        // Use bounce interpolator with amplitude 0.2 and frequency 20
        MyBounceInterpolator interpolator = new MyBounceInterpolator(0.2, 20);
        myAnim.setInterpolator(interpolator);
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
//        NavigationView navigationView = findViewById(R.id.nav_view);
//        navigationView.setNavigationItemSelectedListener(this);
//        nav_Menu = navigationView.getMenu();

        CheckStoragePermission();
//        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
//        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
//                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
//        drawer.addDrawerListener(toggle);
//        toggle.syncState();


        final FloatingActionButton maddHtmlFAB = findViewById(R.id.mainaddHtmlFAB);
        final FloatingActionButton maddCloudFAB = findViewById(R.id.mainCloudFilesFAB);
        final FloatingActionButton maddCameraFAB = findViewById(R.id.mainaddCameraFAB);
        final FloatingActionButton maddFilesFAB = findViewById(R.id.mainaddFilesFAB);
        final FloatingActionButton maddTextFAB = findViewById(R.id.mainTextFAB);

        lyt_addFiles = findViewById(R.id.lyt_addFiles);
        lyt_textToPDF = findViewById(R.id.lyt_textToPDF);
        about=findViewById(R.id.about);
        lyt_cameraToPDF = findViewById(R.id.lyt_cameraToPDF);
        lyt_addCloudFiles = findViewById(R.id.lyt_addCloudFiles);
        lyt_htmlToPDF = findViewById(R.id.lyt_htmlToPDF);
        back_drop = findViewById(R.id.back_drop);
        ViewAnimation.initShowOut(lyt_addFiles);
        ViewAnimation.initShowOut(lyt_textToPDF);
        ViewAnimation.initShowOut(lyt_cameraToPDF);
        ViewAnimation.initShowOut(lyt_htmlToPDF);
        ViewAnimation.initShowOut(lyt_addCloudFiles);
        back_drop.setVisibility(View.GONE);

        back_drop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFabMode(mAddPDFFAB);
            }
        });
        mAddPDFFAB = findViewById(R.id.mainaddToDoItemFAB);

        mAddPDFFAB.startAnimation(myAnim);

        mAddPDFFAB.setOnClickListener(new View.OnClickListener() {

            @SuppressWarnings("deprecation")
            @Override
            public void onClick(View v) {
                toggleFabMode(v);
            }
        });
        about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialogAbout();
            }
        });
         openWithAnotherApplication();
        maddHtmlFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentClickedActivity = "HtmlActivity";
                StartMergeActivity("HtmlActivity");
            }

        });

        maddCloudFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentClickedActivity = "CloudFileSearch";
                StartMergeActivity("CloudFileSearch");
            }

        });
        maddFilesFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentClickedActivity = "FileSearch";
                StartMergeActivity("FileSearch");
            }

        });

        maddCameraFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentClickedActivity = "CameraActivity";
                StartMergeActivity("CameraActivity");
            }

        });

        maddTextFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentClickedActivity = "TextToPDFActivity";
                StartMergeActivity("TextToPDFActivity");
            }

        });


        recyclerView = findViewById(R.id.mainRecycleView);
        recyclerView.setEmptyView(findViewById(R.id.toDoEmptyView));
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.addItemDecoration(new SpacingItemDecoration(2, FileUtils.dpToPx(this, 8), true));
        recyclerView.setHasFixedSize(true);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    if (mAddPDFFAB.getVisibility() == View.VISIBLE)
                        mAddPDFFAB.hide();
                    if (rotate) {
                        if (maddHtmlFAB.getVisibility() == View.VISIBLE)
                            maddHtmlFAB.hide();
                        if (maddCameraFAB.getVisibility() == View.VISIBLE)
                            maddCameraFAB.hide();
                        if (maddFilesFAB.getVisibility() == View.VISIBLE)
                            maddFilesFAB.hide();
                        if (maddTextFAB.getVisibility() == View.VISIBLE)
                            maddTextFAB.hide();
                    }
                } else if (dy < 0) {
                    if (mAddPDFFAB.getVisibility() != View.VISIBLE)
                        mAddPDFFAB.show();
                    if (rotate) {
                        if (maddHtmlFAB.getVisibility() != View.VISIBLE)
                            maddHtmlFAB.show();
                        if (maddCameraFAB.getVisibility() != View.VISIBLE)
                            maddCameraFAB.show();
                        if (maddFilesFAB.getVisibility() != View.VISIBLE)
                            maddFilesFAB.show();
                        if (maddTextFAB.getVisibility() != View.VISIBLE)
                            maddTextFAB.show();
                    }

                }
            }
        });
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            CheckStoragePermission();
        }

        CreateDataSource();

        actionModeCallback = new ActionModeCallback();


        HandleExternalData();
    }

    private void openWithAnotherApplication() {

    }

    private void toggleFabMode(View v) {
        rotate = ViewAnimation.rotateFab(v, !rotate);
        if (rotate) {
            ViewAnimation.showIn(lyt_addFiles);
            ViewAnimation.showIn(lyt_textToPDF);
            ViewAnimation.showIn(lyt_cameraToPDF);
            ViewAnimation.showIn(lyt_htmlToPDF);
            ViewAnimation.showIn(lyt_addCloudFiles);
            back_drop.setVisibility(View.VISIBLE);
        } else {
            ViewAnimation.showOut(lyt_addFiles);
            ViewAnimation.showOut(lyt_textToPDF);
            ViewAnimation.showOut(lyt_cameraToPDF);
            ViewAnimation.showOut(lyt_htmlToPDF);
            ViewAnimation.showOut(lyt_addCloudFiles);
            back_drop.setVisibility(View.GONE);
        }
    }

    private void HandleExternalData() {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                handleSendImage(intent); // Handle single image being sent
            } else if ("application/pdf".equals(type)) {
                handleSendImage(intent);
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                handleSendMultipleImages(intent); // Handle multiple images being sent
            } else if ("application/pdf".equals(type)) {
                handleSendMultipleImages(intent);
            }
        }
    }

    void handleSendImage(Intent intent) {
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            ArrayList<Uri> list = new ArrayList<>();
            list.add(imageUri);
            StartMergeActivity("ImageSend", list);
        }
    }

    void handleSendMultipleImages(Intent intent) {
        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (imageUris != null) {
            StartMergeActivity("ImageSend", imageUris);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        CreateDataSource();
    }

//    @Override
//    public void onBackPressed() {
////        DrawerLayout drawer = findViewById(R.id.drawer_layout);
////        if (drawer.isDrawerOpen(GravityCompat.START)) {
////            drawer.closeDrawer(GravityCompat.START);
////        }
//    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main2, menu);
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_rateapp) {
            Intent goToMarket = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()));
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            try {
                startActivity(goToMarket);
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
            }
            return true;
        } else if (id == R.id.action_more) {
            Intent goToMarket = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pub:Benzveen"));
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            try {
                startActivity(goToMarket);
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/developer?id=Benzveen+Inc.")));
            }
            return true;
        } else if (id == R.id.action_tell) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, "I am using PDF Merge tool to merge PDF,JPEG,PNG and HTML into single PDF. Download Now https://play.google.com/store/apps/details?id=" + getPackageName());
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.home) {
            // Handle the camera action
        } else if (id == R.id.nav_share) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, "I am using PDF Merge tool to merge PDF,JPEG,PNG and HTML into single PDF. Download Now https://play.google.com/store/apps/details?id=" + getPackageName());
            sendIntent.setType("text/plain");
            startActivity(sendIntent);

        } else if (id == R.id.nav_send) {
            Intent Email = new Intent(Intent.ACTION_SEND);
            Email.setType("text/email");
            Email.putExtra(Intent.EXTRA_EMAIL, new String[]{"benzveen@gmail.com"});
            Email.putExtra(Intent.EXTRA_SUBJECT, "Feedback");
            startActivity(Intent.createChooser(Email, "Send Feedback"));
        } else if (id == R.id.nav_rate_app) {
            Intent goToMarket = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()));
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            try {
                startActivity(goToMarket);
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
            }
        } else if (id == R.id.nav_about) {
            showDialogAbout();
        }
//        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
//        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        try {
            if (requestCode == Merge_Request_CODE && resultCode == Activity.RESULT_OK) {
                if (result != null) {
                    CreateDataSource();
                    mAdapter.notifyItemInserted(items.size() - 1);
                }
            }
            if (resultCode == RESULT_OK && requestCode == RQS_OPEN_DOCUMENT_TREE) {
                if (result != null) {
                    Uri uriTree = result.getData();
                    DocumentFile documentFile = DocumentFile.fromTreeUri(this, uriTree);
                    if (documentFile != null) {
                        if (selectedFile != null) {
                            DocumentFile newFile = documentFile.createFile("application/pdf", selectedFile.getName());
                            try {
                                FileUtils.copy(selectedFile, newFile, Main2Activity.this);

                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast toast = Toast.makeText(this, "Unknown error occurs while copying files to: " + documentFile.getName(), Toast.LENGTH_LONG);
                                toast.show();
                            }
                            selectedFile = null;
                            if (mBottomSheetDialog != null)
                                mBottomSheetDialog.dismiss();
                            Toast toast = Toast.makeText(this, "Files Copied to: " + documentFile.getName(), Toast.LENGTH_LONG);
                            toast.show();
                        } else {
                            Toast toast = Toast.makeText(this, "Unknown error occurs while copying files to: " + documentFile.getName(), Toast.LENGTH_LONG);
                            toast.show();
                        }
                    }
                }
            }
            if (resultCode == RESULT_OK && requestCode == RQS_OPEN_DOCUMENT_TREE_ALL) {
                if (result != null) {
                    List<Integer> selectedItemPositions = mAdapter.getSelectedItems();
                    ArrayList<Uri> files = new ArrayList<Uri>();
                    Uri uriTree = result.getData();
                    DocumentFile documentFile = DocumentFile.fromTreeUri(this, uriTree);

                    if (documentFile != null) {
                        for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
                            File file = items.get(i);
                            DocumentFile newFile = documentFile.createFile("application/pdf", file.getName());
                            try {
                                FileUtils.copy(file, newFile, Main2Activity.this);

                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast toast = Toast.makeText(this, "Unknown error occurs while copying files to: " + documentFile.getName(), Toast.LENGTH_LONG);
                                toast.show();
                            }
                        }
                        if (actionMode != null)
                            actionMode.finish();
                        Toast toast = Toast.makeText(this, "Files Copied to: " + documentFile.getName(), Toast.LENGTH_LONG);
                        toast.show();
                    } else {
                        Toast toast = Toast.makeText(this, "Unknown error occurs while copying files to: " + documentFile.getName(), Toast.LENGTH_LONG);
                        toast.show();
                    }

                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    private void CreateDataSource() {
        if (actionMode != null) {
            actionMode.finish();
        }
        items = new ArrayList<File>();

        File root = getFilesDir();
        File myDir = new File(root + "/PDFMerger");
        if (!myDir.exists()) {
            myDir.mkdirs();
        }
        File[] files = myDir.listFiles();

        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                long result = file2.lastModified() - file1.lastModified();
                if (result < 0) {
                    return -1;
                } else if (result > 0) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        for (int i = 0; i < files.length; i++) {
            items.add(files[i]);
        }

        //set data and list adapter
        mAdapter = new MainRecycleViewAdapter(this, items);
        mAdapter.setOnItemClickListener(new MainRecycleViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, File value, int position) {
                if (mAdapter.getSelectedItemCount() > 0) {
                    enableActionMode(position);
                } else {
                    if (view instanceof LinearLayout) {
                        if (value != null) {
                            Intent intent = new Intent(getApplicationContext(), PdfActivity.class);
                            intent.putExtra("position", position);
                            startActivity(intent);

//                            Intent target = new Intent(Intent.ACTION_VIEW);
//                            Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", value);
//                            target.setDataAndType(contentUri, "application/pdf");
//                            target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
//                            target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//
//                            Intent intent = Intent.createChooser(target, "Open File");
                            try {
                                startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                //Snackbar.make(mCoordLayout, "Install PDF reader application.", Snackbar.LENGTH_LONG).show();
                            }
                        }
                    } else {
                        showBottomSheetDialog(value);
                    }
                }
            }

            @Override
            public void onItemLongClick(View view, File obj, int pos) {
                enableActionMode(pos);
            }

        });

        recyclerView.setAdapter(mAdapter);
    }

    public void StartMergeActivity(String message) {
        Intent intent = new Intent(getApplicationContext(), MergeActivity.class);
        intent.putExtra("ActivityAction", message);
        startActivityForResult(intent, Merge_Request_CODE);
    }

    public void StartMergeActivity(String message, ArrayList<Uri> imageUris) {
        Intent intent = new Intent(getApplicationContext(), MergeActivity.class);
        intent.putExtra("ActivityAction", message);
        intent.putExtra("image", imageUris);
        startActivityForResult(intent, Merge_Request_CODE);
    }


    private void CheckStoragePermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle("Storage Permission");
                alertDialog.setMessage("Storage permission is required in order to " +
                        "provide PDF merge feature, please enable permission in app settings");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Settings",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                                startActivity(i);
                                if (dialog != null) {
                                    dialog.dismiss();
                                }
                            }
                        });
                alertDialog.show();
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        2);
            }
        }
    }

    private void showBottomSheetDialog(final File currentFile) {
        final View view = getLayoutInflater().inflate(R.layout.sheet_list, null);
        mBottomSheetDialog = new BottomSheetDialog(this);
        mBottomSheetDialog.setContentView(view);

        (view.findViewById(R.id.lyt_openwith)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBottomSheetDialog != null)
                    mBottomSheetDialog.dismiss();
                Intent target = new Intent(Intent.ACTION_VIEW);
                            Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", currentFile);
                            target.setDataAndType(contentUri, "application/pdf");
                            target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            Intent intent = Intent.createChooser(target, "Open File");
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    //Snackbar.make(mCoordLayout, "Install PDF reader application.", Snackbar.LENGTH_LONG).show();
                }

            }
        });
        (view.findViewById(R.id.lyt_email)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBottomSheetDialog != null)
                    mBottomSheetDialog.dismiss();
                Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", currentFile);
                Intent target = new Intent(Intent.ACTION_SEND);
                target.setType("text/plain");
                target.putExtra(Intent.EXTRA_STREAM, contentUri);
                target.putExtra(Intent.EXTRA_SUBJECT, "Subject");
                startActivity(Intent.createChooser(target, "Send via Email..."));
            }
        });

        (view.findViewById(R.id.lyt_share)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBottomSheetDialog != null)
                    mBottomSheetDialog.dismiss();
                Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", currentFile);
                Intent target = ShareCompat.IntentBuilder.from(currentActivity).setStream(contentUri).getIntent();
                target.setData(contentUri);
                target.setType("application/pdf");
                target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (target.resolveActivity(getPackageManager()) != null) {
                    startActivity(target);
                }
            }
        });

        (view.findViewById(R.id.lyt_rename)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBottomSheetDialog != null)
                    mBottomSheetDialog.dismiss();
                showCustomRenameDialog(currentFile);

            }
        });

        (view.findViewById(R.id.lyt_delete)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBottomSheetDialog != null)
                    mBottomSheetDialog.dismiss();
                showCustomDeleteDialog(currentFile);

            }
        });

        (view.findViewById(R.id.lyt_copyTo)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(intent, RQS_OPEN_DOCUMENT_TREE);
                selectedFile = currentFile;
            }
        });

        (view.findViewById(R.id.lyt_rearrange)).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (mBottomSheetDialog != null)
                    mBottomSheetDialog.dismiss();
                Intent intent = new Intent(getApplicationContext(), PDFEditorActivity.class);
                PDFEditorActivity.currentFile = currentFile;
                startActivityForResult(intent, PDFEDITOR_Request_CODE);
            }
        });

        mBottomSheetDialog.show();
        mBottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mBottomSheetDialog = null;
            }
        });
    }

    public void showCustomDeleteDialog(final File currentFile) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure want to delete this file?");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                currentFile.delete();
                CreateDataSource();
                mAdapter.notifyItemInserted(items.size() - 1);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    public void showCustomRenameDialog(final File currentFile) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.rename_layout, null);
        builder.setView(view);
        final EditText editText = (EditText) view.findViewById(R.id.renameEditText2);
        editText.setText(currentFile.getName());
        builder.setTitle("Rename");
        builder.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                File root = getFilesDir();
                File file = new File(root + "/PDFMerger", editText.getText().toString());
                currentFile.renameTo(file);
                if (dialog != null)
                    dialog.dismiss();
                CreateDataSource();
                mAdapter.notifyItemInserted(items.size() - 1);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void showCustomDeleteAllDialog(final ActionMode mode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure want to delete the selected files?");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                deleteItems();
                mode.finish();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteItems() {
        List<Integer> selectedItemPositions = mAdapter.getSelectedItems();
        for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
            File file = items.get(selectedItemPositions.get(i));
            file.delete();
            mAdapter.removeData(selectedItemPositions.get(i));
        }
        CreateDataSource();
        mAdapter.notifyDataSetChanged();
    }

    //<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
    //Action mode implementation
    //<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
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

    private void copyToAll() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(intent, RQS_OPEN_DOCUMENT_TREE_ALL);
    }

    private void shareAll() {
        Intent target = ShareCompat.IntentBuilder.from(currentActivity).getIntent();
        target.setAction(Intent.ACTION_SEND_MULTIPLE);
        List<Integer> selectedItemPositions = mAdapter.getSelectedItems();
        ArrayList<Uri> files = new ArrayList<Uri>();
        for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
            File file = items.get(i);
            Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", file);
            files.add(contentUri);
        }
        target.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
        target.setType("application/pdf");
        target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (target.resolveActivity(getPackageManager()) != null) {
            startActivity(target);
        }
        actionMode.finish();
    }

    private class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_mainactionmode, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.action_delete) {
                showCustomDeleteAllDialog(mode);
                return true;
            }
            if (id == R.id.select_all) {
                selectAll();
                return true;
            }
            if (id == R.id.action_share) {
                shareAll();
                return true;
            }
            if (id == R.id.action_copyTo) {
                copyToAll();
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
    //>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

    private void showDialogAbout() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
        dialog.setContentView(R.layout.dialog_about);
        dialog.setCancelable(true);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());

        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        ColorDrawable back = new ColorDrawable(Color.TRANSPARENT);
        int margin = 20;
        InsetDrawable inset = new InsetDrawable(back, margin);
        dialog.getWindow().setBackgroundDrawable(inset);
        ((ImageButton) dialog.findViewById(R.id.bt_close)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog != null)
                    dialog.dismiss();
            }
        });

        ((Button) dialog.findViewById(R.id.bt_licence)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url="https://github.com/Benzveen/pdf_merge/blob/main/LICENSE";
                Intent urlIntent=new Intent(Intent.ACTION_VIEW,Uri.parse(url));
                startActivity(urlIntent);
            }
        });
        ((Button) dialog.findViewById(R.id.app_source_code)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url="https://github.com/bumbumapp/Pdfmerge";
                Intent urlIntent=new Intent(Intent.ACTION_VIEW,Uri.parse(url));
                startActivity(urlIntent);
            }
        });
        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }

    private void showDialogPrivacy() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
        dialog.setContentView(R.layout.privacy_layout);
        dialog.setCancelable(true);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;

        ((ImageButton) dialog.findViewById(R.id.bt_close)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog != null)
                    dialog.dismiss();
            }
        });


        dialog.getWindow().setAttributes(lp);
        dialog.show();
    }

    AlertDialog ratingAlertDialog = null;

    private void ShowRatingDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.rate_dialog, null);
        dialogBuilder.setView(dialogView);
        final AppCompatCheckBox donotDis = dialogView.findViewById(R.id.donotdis);
        Button rateNow = dialogView.findViewById(R.id.bt_rateNow);
        rateNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()));
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                try {
                    startActivity(goToMarket);
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
                }
            }
        });
        Button exit = dialogView.findViewById(R.id.bt_exit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ratingAlertDialog != null) {
                    ratingAlertDialog.dismiss();
                }
                finish();
            }
        });

        ratingAlertDialog = dialogBuilder.create();
        ratingAlertDialog.show();

    }
}
