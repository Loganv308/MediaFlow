package com.loganv308;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.loganv308.Logger;

public class FileScanner {
    private static final String MEDIAMOUNT = "/mnt/NASMedia";
    
    private static List<File> mediaFiles = new ArrayList<>();

    private static Logger logger = new Logger();

    public static void walkDirectory(File file) {
        if(file.isFile()) {
            System.out.println("Media File Found: " + file.getAbsolutePath());
        }
    }
}
