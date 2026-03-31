package com.loganv308;

import java.nio.file.Path;

class PathConfig {
    public final Path nasRoot;
    public final Path tempDir;
    public final String ffmpegBin;
    public final String ffprobeBin;
    public final boolean isWindows;

    public PathConfig(Path nasRoot, Path tempDir, String ffmpegBin, String ffprobeBin, boolean isWindows) {
        this.nasRoot = nasRoot;
        this.tempDir = tempDir;
        this.ffmpegBin = ffmpegBin;
        this.ffprobeBin = ffprobeBin;
        this.isWindows = isWindows;
    }

   // For String paths
    public String normalizePath(String path) {
        return isWindows ? path.replace("\\", "/") : path;
    }

    // For Path objects
    public Path normalizePath(Path path) {
        return isWindows ? Path.of(path.toString().replace("\\", "/")) : path;
    }
}