package com.bumbumapps.utility.pdfmerge.Ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;

import org.junit.Test;

public class ImportCacheTest {

    @Test
    public void pruneDeletesOnlyImportFoldersOlderThanADay() throws Exception {
        File root = Files.createTempDirectory("import").toFile();
        long now = System.currentTimeMillis();
        File old = new File(root, "1");
        File fresh = new File(root, "2");
        assertTrue(old.mkdirs() && fresh.mkdirs());
        new FileOutputStream(new File(old, "Report.pdf")).close();
        new FileOutputStream(new File(fresh, "Report.pdf")).close();
        assertTrue(old.setLastModified(now - 25L * 60 * 60 * 1000));

        FileActions.pruneImports(root, now);

        assertFalse(old.exists());
        assertTrue(new File(fresh, "Report.pdf").exists());
    }

    @Test
    public void importKeepsTheOriginalNameInItsOwnFolder() throws Exception {
        String src = new String(Files.readAllBytes(
                new File("src/main/java/com/bumbumapps/utility/pdfmerge/Ui/FileActions.java").toPath()));
        String body = src.substring(src.indexOf("public static File importToCache"), src.indexOf("static void pruneImports"));
        // A numbered copy ("Report (3).pdf") would leak into every result name.
        assertFalse(body.contains("uniqueFile"));
        assertTrue(body.contains("new File(dir, name)"));
    }

    @Test
    public void textAndWebFromHomeOpenTheResultScreen() throws Exception {
        String src = new String(Files.readAllBytes(
                new File("src/main/java/com/bumbumapps/utility/pdfmerge/MergeActivity.java").toPath()));
        assertTrue(src.contains("saveConvertedAndShowResult(new File(path))"));
        assertTrue(src.contains("ResultActivity.start(this, ResultActivity.MODE_MERGE, out)"));
    }
}
