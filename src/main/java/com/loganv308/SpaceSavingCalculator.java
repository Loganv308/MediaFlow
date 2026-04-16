package com.loganv308;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class SpaceSavingCalculator {

    // ── JSON-serialisable inner record ──────────────────────────────────────
    public static class FileEntry {
        @JsonProperty("file")         public String file;
        @JsonProperty("originalGB")   public double originalGB;
        @JsonProperty("encodedGB")    public double encodedGB;
        @JsonProperty("savedGB")      public double savedGB;

        public FileEntry() {}
        public FileEntry(String file, double originalGB, double encodedGB, double savedGB) {
            this.file       = file;
            this.originalGB = originalGB;
            this.encodedGB  = encodedGB;
            this.savedGB    = savedGB;
        }
    }

    // ── Top-level JSON structure ─────────────────────────────────────────────
    public static class SavingsReport {
        @JsonProperty("totalSpaceSavedGB") public double totalSpaceSavedGB = 0.0;
        @JsonProperty("filesProcessed")    public int    filesProcessed    = 0;
        @JsonProperty("skippedLarger")     public int    skippedLarger     = 0;
        @JsonProperty("log")               public List<FileEntry> log      = new ArrayList<>();
    }

    // ── Instance state ───────────────────────────────────────────────────────
    private final SavingsReport report;
    private static final String JSON_PATH = "TotalSaved.json";
    private static final ObjectMapper om   =
        new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public SpaceSavingCalculator(double ignored) {
        // Load existing report from disk so totals survive restarts
        SavingsReport loaded = null;
        File f = new File(JSON_PATH);
        if (f.exists()) {
            try { loaded = om.readValue(f, SavingsReport.class); }
            catch (IOException e) { System.err.println("Could not load TotalSaved.json, starting fresh."); }
        }
        this.report = (loaded != null) ? loaded : new SavingsReport();
    }

    // ── File-size helper (does NOT mutate report) ────────────────────────────
    /**
     * Returns the size of a single file in GB. Pure utility — no side effects.
     */
    public double getFileSizeGB(Path path) {
        try {
            long bytes = Files.size(path);
            double gb   = bytes / (1024.0 * 1024.0 * 1024.0);
            return Math.floor(gb * 100) / 100.0;
        } catch (IOException e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    // ── Record a successful encode (saved space) ─────────────────────────────
    /**
     * Call this only when encodedGB < originalGB.
     * Adds the entry to the log, updates totals, and persists to disk.
     */
    public void recordSaving(String fileName, double originalGB, double encodedGB) {
        double saved = Math.floor((originalGB - encodedGB) * 100) / 100.0;

        report.log.add(new FileEntry(fileName, originalGB, encodedGB, saved));
        report.totalSpaceSavedGB = Math.floor((report.totalSpaceSavedGB + saved) * 100) / 100.0;
        report.filesProcessed++;

        persist();
    }

    // ── Record a skipped file (re-encode was larger) ─────────────────────────
    public void recordSkipped(String fileName, double originalGB, double encodedGB) {
        // Still log it for visibility, but savedGB will be 0 or negative
        report.log.add(new FileEntry(fileName, originalGB, encodedGB, 0.0));
        report.skippedLarger++;

        persist();
    }

    // ── Getters for Runner ───────────────────────────────────────────────────
    public double getTotalSavedGB()  { return report.totalSpaceSavedGB; }
    public int    getFilesProcessed(){ return report.filesProcessed;    }
    public int    getSkippedLarger() { return report.skippedLarger;     }

    // ── Persist to disk ──────────────────────────────────────────────────────
    private void persist() {
        try {
            om.writeValue(new File(JSON_PATH), report);
        } catch (IOException e) {
            System.err.println("Failed to write TotalSaved.json: " + e.getMessage());
        }
    }
}