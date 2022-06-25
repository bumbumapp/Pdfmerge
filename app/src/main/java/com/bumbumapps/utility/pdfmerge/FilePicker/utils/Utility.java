package com.bumbumapps.utility.pdfmerge.FilePicker.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import com.bumbumapps.utility.pdfmerge.FilePicker.model.FileListItem;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 * @author akshay sunil masram
 */
public class Utility {

    public static boolean checkStorageAccessPermissions(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            String permission = "android.permission.READ_EXTERNAL_STORAGE";
            int res = context.checkCallingOrSelfPermission(permission);
            return (res == PackageManager.PERMISSION_GRANTED);
        }
        else {
            return true;
        }
    }

    public static ArrayList<FileListItem> prepareFileListEntries(ArrayList<FileListItem> internalList, File inter, ExtensionFilter filter) {
        try {

            for (File name : inter.listFiles(filter)) {
                if (name.canRead()) {
                    FileListItem item = new FileListItem();
                    item.setFilename(name.getName());
                    item.setDirectory(name.isDirectory());
                    item.setLocation(name.getAbsolutePath());
                    item.setTime(name.lastModified());
                    internalList.add(item);
                }
            }
            Collections.sort(internalList);
        }
        catch (NullPointerException e) {
            e.printStackTrace();
            internalList=new ArrayList<>();
        }
        return internalList;
    }

    private boolean hasSupportLibraryInClasspath() {
        try {
            Class.forName("com.android.support:appcompat-v7");
            return true;
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        return false;
    }
}
