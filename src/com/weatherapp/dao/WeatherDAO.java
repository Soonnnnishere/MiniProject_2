package com.weatherapp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.weatherapp.db.DBConnection;
import com.weatherapp.model.WeatherRecord;
import com.weatherapp.model.TemperatureReport;

/**
 * WeatherDAO — Data Access Object for the weather_data table.
 *
 * WHAT IS A DAO? (viva)
 *   A DAO is the ONLY class that knows SQL. It hides all database details
 *   behind plain Java methods (batchInsert, search, update, softDelete...).
 *   The servlets just call those methods; they never write SQL themselves.
 *   Benefit = separation of concerns: if we changed databases, only the DAO
 *   would change, not the servlets.
 *
 * KEY TECHNIQUES USED HERE:
 *   - PreparedStatement (parameterised SQL) -> prevents SQL injection, faster.
 *   - try-with-resources -> auto-closes Connection/Statement/ResultSet so we
 *     never leak DB connections, even if an exception is thrown.
 *   - JDBC batching (addBatch/executeBatch) -> bulk-inserts 96k rows fast.
 *   - Soft delete -> an UPDATE, never a DELETE.
 */
public class WeatherDAO {

    // How many rows we accumulate before flushing a batch to the DB.
    private static final int BATCH_SIZE = 1000;

    // ---- SQL kept as constants (readable + reused) ----

    // '?' are PLACEHOLDERS. We never glue user values into the SQL string; we
    // bind them with ps.setXxx(). That is what blocks SQL injection.
    private static final String INSERT_SQL =
            "INSERT INTO weather_data " +
            "(formatted_date, summary, precip_type, temperature_c, apparent_temperature_c, " +
            " humidity, wind_speed_kmh, wind_bearing_deg, visibility_km, loud_cover, " +
            " pressure_mb, daily_summary) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    // SOFT-DELETE RULE: every read filters is_active = 1 so soft-deleted rows
    // (is_active = 0) are invisible to the whole application.
    private static final String SEARCH_FILTER =
            " WHERE is_active = 1 AND (summary LIKE ? OR formatted_date LIKE ? OR daily_summary LIKE ?) ";

    // LIMIT ? OFFSET ? = the SQL that powers pagination (return only one page).
    private static final String SEARCH_SQL =
            "SELECT * FROM weather_data" + SEARCH_FILTER + " ORDER BY id LIMIT ? OFFSET ?";

    private static final String COUNT_SQL =
            "SELECT COUNT(*) FROM weather_data" + SEARCH_FILTER;

    private static final String FIND_BY_ID_SQL =
            "SELECT * FROM weather_data WHERE id = ?";

    private static final String UPDATE_SQL =
            "UPDATE weather_data SET summary = ?, precip_type = ?, temperature_c = ?, " +
            "apparent_temperature_c = ?, humidity = ?, wind_speed_kmh = ?, wind_bearing_deg = ?, " +
            "visibility_km = ?, pressure_mb = ?, daily_summary = ? WHERE id = ?";

    // The heart of the soft-delete feature: flip the flag, keep the row.
    private static final String SOFT_DELETE_SQL =
            "UPDATE weather_data SET is_active = 0 WHERE id = ?";

    // Hard reset of the WHOLE dataset (administrative "Clear Dataset" action).
    // TRUNCATE empties the table AND resets AUTO_INCREMENT back to 1.
    private static final String CLEAR_SQL = "TRUNCATE TABLE weather_data";

    // Batch summary statistics for the export report (avg/max + count as context).
    private static final String STATS_SQL =
            "SELECT AVG(temperature_c) AS avg_t, MAX(temperature_c) AS max_t, " +
            "COUNT(*) AS cnt FROM weather_data WHERE is_active = 1";

    // First-100 sample (same as the live stream) for running-average milestones.
    private static final String MILESTONE_SAMPLE_SQL =
            "SELECT temperature_c FROM weather_data WHERE is_active = 1 ORDER BY id ASC LIMIT 100";

    /**
     * Bulk-inserts records using JDBC BATCHING.
     *
     * HOW BATCHING WORKS (viva favourite):
     *   For each record we call ps.setXxx(...) then ps.addBatch() — this queues
     *   the row. Every 1000 rows we call ps.executeBatch(), which sends them to
     *   MySQL together. Combined with rewriteBatchedStatements=true in the URL,
     *   the driver packs them into a single multi-row INSERT, so 96k rows insert
     *   in seconds instead of 96k separate network round-trips.
     *
     * @return number of rows inserted
     */
    public int batchInsert(List<WeatherRecord> records) throws SQLException {
        if (records == null || records.isEmpty()) {
            return 0;
        }
        int inserted = 0;
        int pending = 0;
        // try-with-resources: conn and ps are auto-closed when the block ends.
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {

            for (WeatherRecord r : records) {
                // Bind each '?' (1-based index) to a value of the right type.
                ps.setString(1, r.getFormattedDate());
                ps.setString(2, r.getSummary());
                // precip_type can be NULL in the data -> use setNull, not "".
                if (r.getPrecipType() == null) {
                    ps.setNull(3, Types.VARCHAR);
                } else {
                    ps.setString(3, r.getPrecipType());
                }
                ps.setDouble(4, r.getTemperatureC());
                ps.setDouble(5, r.getApparentTemperatureC());
                ps.setDouble(6, r.getHumidity());
                ps.setDouble(7, r.getWindSpeedKmh());
                ps.setInt(8, r.getWindBearingDeg());
                ps.setDouble(9, r.getVisibilityKm());
                ps.setDouble(10, r.getLoudCover());
                ps.setDouble(11, r.getPressureMb());
                ps.setString(12, r.getDailySummary());

                ps.addBatch();   // queue this row
                pending++;

                // Flush every BATCH_SIZE rows to keep memory bounded.
                if (pending % BATCH_SIZE == 0) {
                    int[] results = ps.executeBatch();
                    inserted += countAffected(results, BATCH_SIZE);
                    pending = 0;
                }
            }

            // Flush whatever is left in the final partial batch.
            if (pending > 0) {
                int[] results = ps.executeBatch();
                inserted += countAffected(results, pending);
            }
        }
        return inserted;
    }

    /**
     * Sums the per-row results from executeBatch(). Some drivers return
     * SUCCESS_NO_INFO (-2) meaning "it worked but I won't tell you the count",
     * so we count that as one successful row.
     */
    private static int countAffected(int[] results, int fallback) {
        int total = 0;
        boolean hadNoInfo = false;
        for (int r : results) {
            if (r >= 0) {
                total += r;
            } else if (r == java.sql.Statement.SUCCESS_NO_INFO) {
                total += 1;
                hadNoInfo = true;
            }
            // EXECUTE_FAILED (-3) contributes 0
        }
        if (hadNoInfo && total == 0) {
            return fallback;
        }
        return total;
    }

    /**
     * Returns ONE page of active records matching the search text.
     *
     * @param q        search text (blank = match everything)
     * @param page     1-based page number
     * @param pageSize rows per page
     */
    public List<WeatherRecord> search(String q, int page, int pageSize) throws SQLException {
        String like = buildLike(q);
        // OFFSET = rows to skip = (page-1) * pageSize. Page 1 skips 0, page 2
        // skips pageSize, etc. LIMIT then returns at most pageSize rows.
        int offset = (page - 1) * pageSize;
        List<WeatherRecord> out = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SEARCH_SQL)) {
            // Bind the three LIKE placeholders + LIMIT + OFFSET.
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setInt(4, pageSize);
            ps.setInt(5, offset);

            // executeQuery() returns a ResultSet (a cursor over the rows).
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {             // advance row by row
                    out.add(mapRow(rs));        // convert each row to an object
                }
            }
        }
        return out;
    }

    /**
     * Counts ALL active rows matching the same filter. Needed to work out how
     * many pages exist (totalPages = ceil(count / pageSize)).
     */
    public int count(String q) throws SQLException {
        String like = buildLike(q);
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(COUNT_SQL)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);   // the COUNT(*) value, column 1
                }
            }
        }
        return 0;
    }

    /**
     * Loads a single record by id (used to pre-fill the Edit form).
     * No is_active filter here, so you could even view a soft-deleted row.
     * @return the record, or null if no such id
     */
    public WeatherRecord findById(long id) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_ID_SQL)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Updates the editable fields of a record (the Edit feature).
     * @return true if a row was changed
     */
    public boolean update(WeatherRecord r) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {
            ps.setString(1, r.getSummary());
            if (r.getPrecipType() == null) {
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setString(2, r.getPrecipType());
            }
            ps.setDouble(3, r.getTemperatureC());
            ps.setDouble(4, r.getApparentTemperatureC());
            ps.setDouble(5, r.getHumidity());
            ps.setDouble(6, r.getWindSpeedKmh());
            ps.setInt(7, r.getWindBearingDeg());
            ps.setDouble(8, r.getVisibilityKm());
            ps.setDouble(9, r.getPressureMb());
            ps.setString(10, r.getDailySummary());
            ps.setLong(11, r.getId());
            // executeUpdate() returns the number of rows changed.
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * SOFT DELETE — the mandatory feature.
     *   Runs UPDATE ... SET is_active = 0. The row STAYS in the table; it is
     *   just flagged inactive, so every read (which filters is_active = 1) hides
     *   it. We deliberately NEVER use SQL DELETE, so data is recoverable and the
     *   analysis modules can ignore it cleanly.
     * @return true if a row was flagged
     */
    public boolean softDelete(long id) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SOFT_DELETE_SQL)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * CLEAR / RESET the entire dataset (hard reset).
     *   Runs TRUNCATE TABLE, which removes every row and resets the
     *   auto-increment id back to 1. This is an administrative "start fresh"
     *   action used before re-importing the CSV, and is deliberately DISTINCT
     *   from softDelete(): soft-delete hides ONE record but keeps it; this
     *   wipes the whole table so a re-upload does not create duplicates.
     */
    public void clearAll() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(CLEAR_SQL)) {
            ps.executeUpdate();
        }
    }

    /**
     * Assembles the full temperature analysis report for the Export module:
     *   - batch summary (average, maximum + count as context) in one aggregate query;
     *   - real-time running-average milestones (running avg after 25/50/75/100
     *     of the first 100 records — the same sample the live SSE stream uses).
     * Member 3's ExportServlet turns this object into CSV or JSON.
     */
    public TemperatureReport getTemperatureReport() throws SQLException {
        TemperatureReport report = new TemperatureReport();
        try (Connection conn = DBConnection.getConnection()) {

            // 1) Batch summary stats.
            try (PreparedStatement ps = conn.prepareStatement(STATS_SQL);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    report.setAverage(rs.getDouble("avg_t"));
                    report.setMaximum(rs.getDouble("max_t"));
                    report.setCount(rs.getLong("cnt"));
                }
            }

            // 2) Running-average milestones over the first 100 active records.
            int[] targets = {25, 50, 75, 100};
            int[] mc = new int[targets.length];
            double[] ma = new double[targets.length];
            int found = 0;
            try (PreparedStatement ps = conn.prepareStatement(MILESTONE_SAMPLE_SQL);
                 ResultSet rs = ps.executeQuery()) {
                double sum = 0;
                int n = 0;
                while (rs.next()) {
                    sum += rs.getDouble("temperature_c");
                    n++;
                    for (int t : targets) {
                        if (n == t) {           // hit a milestone -> snapshot the running avg
                            mc[found] = n;
                            ma[found] = sum / n;
                            found++;
                        }
                    }
                }
            }
            // Trim to milestones actually reached (dataset may have < 100 rows).
            report.setMilestoneCounts(java.util.Arrays.copyOf(mc, found));
            report.setMilestoneAverages(java.util.Arrays.copyOf(ma, found));
        }
        return report;
    }

    /** Builds the LIKE pattern. Blank query -> "%" which matches every row. */
    private static String buildLike(String q) {
        if (q == null || q.trim().isEmpty()) {
            return "%";
        }
        // %text% = "contains text" search.
        return "%" + q.trim() + "%";
    }

    /**
     * mapRow — converts the CURRENT row of a ResultSet into a WeatherRecord.
     * Kept as one private helper so search()/findById() don't duplicate it (DRY).
     * rs.getString/getDouble/getInt read a column by its name.
     */
    private static WeatherRecord mapRow(ResultSet rs) throws SQLException {
        WeatherRecord r = new WeatherRecord();
        r.setId(rs.getLong("id"));
        r.setFormattedDate(rs.getString("formatted_date"));
        r.setSummary(rs.getString("summary"));
        r.setPrecipType(rs.getString("precip_type")); // returns null if column was NULL
        r.setTemperatureC(rs.getDouble("temperature_c"));
        r.setApparentTemperatureC(rs.getDouble("apparent_temperature_c"));
        r.setHumidity(rs.getDouble("humidity"));
        r.setWindSpeedKmh(rs.getDouble("wind_speed_kmh"));
        r.setWindBearingDeg(rs.getInt("wind_bearing_deg"));
        r.setVisibilityKm(rs.getDouble("visibility_km"));
        r.setLoudCover(rs.getDouble("loud_cover"));
        r.setPressureMb(rs.getDouble("pressure_mb"));
        r.setDailySummary(rs.getString("daily_summary"));
        r.setIsActive(rs.getInt("is_active"));
        return r;
    }
}
