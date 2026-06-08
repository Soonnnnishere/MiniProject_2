package com.weatherapp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.weatherapp.db.DBConnection;

/**
 * ExportDAO — data access for the export_log audit table (Member 3 / Reporting).
 *
 * Separate from WeatherDAO because it owns a different table (export_log).
 *   - logExport()       inserts one audit row each time a file is downloaded.
 *   - getRecentExports() reads back recent rows for the live "Export Log Viewer".
 */
public class ExportDAO {

    private static final String INSERT_LOG_SQL =
            "INSERT INTO export_log (report_type, format) VALUES (?, ?)";

    private static final String RECENT_SQL =
            "SELECT id, report_type, format, exported_at FROM export_log ORDER BY id DESC LIMIT ?";

    /**
     * Records ONE export event (called every time a CSV/JSON download happens).
     * exported_at is filled automatically by the column's DEFAULT CURRENT_TIMESTAMP.
     */
    public void logExport(String reportType, String format) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_LOG_SQL)) {
            ps.setString(1, reportType);
            ps.setString(2, format);
            ps.executeUpdate();
        }
    }

    /**
     * Returns the most recent export-log rows (newest first) as
     * String[]{id, report_type, format, exported_at} — easy to render as JSON.
     */
    public List<String[]> getRecentExports(int limit) throws SQLException {
        List<String[]> out = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(RECENT_SQL)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new String[] {
                            String.valueOf(rs.getLong("id")),
                            rs.getString("report_type"),
                            rs.getString("format"),
                            String.valueOf(rs.getTimestamp("exported_at"))
                    });
                }
            }
        }
        return out;
    }
}
