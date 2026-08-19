package com.loganv308.cache;

import java.io.Serializable;
import java.nio.file.Path;

import com.loganv308.enums.JobState;

// Serializable filerecord used to store cached file paths from the NAS, as they never change. New ones will be automatically detected and added to cache once re-encoded.
public class FileRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fileName;
    private String fullPath;
    private String nasRelativePath;
    private long lastModified;
    private long fileSizeBytes;
    private JobState state;
    private int retryCount;
    private String lastError;

    public FileRecord(Path p, String nasRelativePath, long lastModified) {
        this.fileName = p.getFileName().toString();
        this.fullPath = p.toString();
        this.nasRelativePath = nasRelativePath;
        this.lastModified = lastModified;
        this.state = JobState.DISCOVERED;
        this.retryCount = 0;
    }

    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }

    // Secondary "did this file actually change" signal alongside lastModified —
    // SMB/CIFS mounts can report slightly different mtimes across separate mount
    // sessions (e.g. after a container restart), but file size is far more stable.
    // A file is only treated as changed if BOTH mtime and size disagree.
    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getFullPath() { return fullPath; }
    public String getFileName() { return fileName; }
    public String getNasRelativePath() { return nasRelativePath; }

    public JobState getState() { return state; }
    public void setState(JobState state) { this.state = state; }

    public int getRetryCount() { return retryCount; }
    public void incrementRetryCount() { this.retryCount++; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}
