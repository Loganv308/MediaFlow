package com.loganv308;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import org.apache.commons.dbcp2.BasicDataSource;

// Persists encode history (space savings, skips) to Postgres, replacing the
// old TotalSaved.json file. Aggregates (total saved, files processed, skipped)
// are computed with SQL rather than tracked in memory, since the row log is
// now the source of truth.
public class Database {
    private static final Logger log = LoggerFactory.initLogger(Database.class);

    private static final String CREATE_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS encode_history (
            id BIGSERIAL PRIMARY KEY,
            file_name TEXT NOT NULL,
            original_gb DOUBLE PRECISION NOT NULL,
            encoded_gb DOUBLE PRECISION NOT NULL,
            saved_gb DOUBLE PRECISION NOT NULL,
            skipped BOOLEAN NOT NULL,
            processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
        )
        """;

    private static final String CREATE_INDEX_SQL =
        "CREATE INDEX IF NOT EXISTS idx_encode_history_file_name ON encode_history (file_name, processed_at DESC)";

    private final BasicDataSource dataSource;

    public Database(DbConfig config) {
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl("jdbc:postgresql://" + config.host + ":" + config.port + "/" + config.name);
        dataSource.setUsername(config.user);
        dataSource.setPassword(config.password);
        dataSource.setMinIdle(1);
        dataSource.setMaxTotal(4);
        dataSource.setValidationQuery("SELECT 1");

        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute(CREATE_TABLE_SQL);
            s.execute(CREATE_INDEX_SQL);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    // Authoritative memory of "we already tried re-encoding this file and it wasn't
    // worth it" — consulted so a re-encode is never blindly re-attempted just because
    // the local NAS-side cache lost track of a file (e.g. SMB/CIFS timestamp drift
    // across a mount remount). Looks at only the most recent attempt for this file;
    // a later successful write-back would mean the file is now actually HEVC, which
    // ffprobe detects directly regardless of this check.
    public boolean wasPreviouslySkipped(String fileName) {
        String sql = "SELECT skipped FROM encode_history WHERE file_name = ? "
            + "ORDER BY processed_at DESC LIMIT 1";

        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fileName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean("skipped");
            }
        } catch (SQLException e) {
            log.severe("Failed to check prior history for " + fileName + ": " + e.getMessage());
            return false;
        }
    }

    public void recordSaving(String fileName, double originalGB, double encodedGB, double savedGB) {
        insert(fileName, originalGB, encodedGB, savedGB, false);
    }

    public void recordSkipped(String fileName, double originalGB, double encodedGB) {
        insert(fileName, originalGB, encodedGB, 0.0, true);
    }

    public double getTotalSavedGB() {
        return queryDouble("SELECT COALESCE(SUM(saved_gb), 0) FROM encode_history WHERE NOT skipped");
    }

    public int getFilesProcessed() {
        return queryInt("SELECT COUNT(*) FROM encode_history WHERE NOT skipped");
    }

    public int getSkippedLarger() {
        return queryInt("SELECT COUNT(*) FROM encode_history WHERE skipped");
    }

    public void close() {
        try {
            dataSource.close();
        } catch (SQLException e) {
            log.warning("Failed to close database connection pool: " + e.getMessage());
        }
    }

    private void insert(String fileName, double originalGB, double encodedGB, double savedGB, boolean skipped) {
        String sql = "INSERT INTO encode_history (file_name, original_gb, encoded_gb, saved_gb, skipped) "
            + "VALUES (?, ?, ?, ?, ?)";

        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fileName);
            ps.setDouble(2, originalGB);
            ps.setDouble(3, encodedGB);
            ps.setDouble(4, savedGB);
            ps.setBoolean(5, skipped);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.severe("Failed to record encode history for " + fileName + ": " + e.getMessage());
        }
    }

    private double queryDouble(String sql) {
        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        } catch (SQLException e) {
            log.severe("Query failed: " + e.getMessage());
            return 0.0;
        }
    }

    private int queryInt(String sql) {
        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            log.severe("Query failed: " + e.getMessage());
            return 0;
        }
    }
}
