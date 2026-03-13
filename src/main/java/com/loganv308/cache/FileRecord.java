package com.loganv308.cache;

import java.io.Serializable;
import java.nio.file.Path;

// Serializable filerecord used to store cached file paths from the NAS, as they never change. New ones will be automatically detected and added to cache once re-encoded. 
public class FileRecord implements Serializable {
    private String fileName;
    private String fullPath;
    private long lastModified;

    public FileRecord(Path p, long lastModified) {
        this.fileName = p.getFileName().toString();
        this.fullPath = p.toString();
        this.lastModified = lastModified;
    }

    public long getLastModified() { return lastModified; }
    public String getFullPath() { return fullPath; }
    public String getFileName() { return fileName; }
}
