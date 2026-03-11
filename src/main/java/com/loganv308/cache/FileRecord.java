package com.loganv308.cache;

import java.io.Serializable;
import java.nio.file.Path;

public class FileRecord implements Serializable{
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
