package com.bumbumapps.utility.pdfmerge.FilePicker.model;

import java.util.LinkedHashMap;
import java.util.Set;

/**
 * @author akshay sunil masram
 */
public class MarkedItemList {
    private static LinkedHashMap<String,FileListItem> ourInstance = new LinkedHashMap<>();

    public static void addSelectedItem(FileListItem item) {
        ourInstance.put(item.getLocation(),item);
    }

    public static void removeSelectedItem(String key) {
        ourInstance.remove(key);
    }

    public static boolean hasItem(String key) {
        return ourInstance.containsKey(key);
    }

    public static void clearSelectionList() {
        ourInstance = new LinkedHashMap<>();
    }

    public static void addSingleFile(FileListItem item) {
        ourInstance = new LinkedHashMap<>();
        ourInstance.put(item.getLocation(),item);
    }

    public static String[] getSelectedPaths() {
        Set<String> paths = ourInstance.keySet();
        String[] fpaths = new String[paths.size()];
        int i=0;
        for(String path:paths)
        {   fpaths[i++]=path;
        }
        return fpaths;
    }

    public static int getFileCount() {
        return ourInstance.size();
    }
}
