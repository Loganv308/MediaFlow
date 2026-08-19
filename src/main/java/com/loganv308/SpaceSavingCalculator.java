package com.loganv308;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SpaceSavingCalculator {

    private final Database db;

    public SpaceSavingCalculator(Database db) {
        this.db = db;
    }

    // ── File-size helper (does NOT touch the database) ───────────────────────
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
     */
    public void recordSaving(String fileName, double originalGB, double encodedGB) {
        double saved = Math.floor((originalGB - encodedGB) * 100) / 100.0;
        db.recordSaving(fileName, originalGB, encodedGB, saved);
    }

    // ── Record a skipped file (re-encode was larger) ─────────────────────────
    public void recordSkipped(String fileName, double originalGB, double encodedGB) {
        db.recordSkipped(fileName, originalGB, encodedGB);
    }

    // ── Getters for Runner ───────────────────────────────────────────────────
    public double getTotalSavedGB()  { return db.getTotalSavedGB();  }
    public int    getFilesProcessed(){ return db.getFilesProcessed(); }
    public int    getSkippedLarger() { return db.getSkippedLarger();  }
}
