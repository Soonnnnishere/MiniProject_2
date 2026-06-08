package com.weatherapp.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.weatherapp.db.DBConnection;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * TemperatureServlet — Member 2's "Temperature Tracker" analysis endpoint,
 * integrated to run against THIS project's shared database.
 *
 * It serves two modes, chosen by the ?mode= query parameter:
 *   - GET /temperature?mode=batch  -> one JSON response with summary statistics
 *     (final AVERAGE and MAXIMUM temperature over the whole active dataset).
 *   - GET /temperature?mode=stream -> a Server-Sent Events (SSE) stream that
 *     emits records one-by-one and reports the INCREMENTAL RUNNING AVERAGE.
 *
 * INTEGRATION NOTES (why this differs from the original draft):
 *   - Uses DBConnection.getConnection() instead of hard-coded DB_URL/USER/PASSWORD,
 *     so it always matches the real database (weatherdb / WeatherApp123). One
 *     source of truth, no "works on my machine" drift.
 *   - Column is temperature_c (the actual column name in weather_data).
 *   - Every query keeps the mandatory WHERE is_active = 1 filter (soft-delete).
 *   - try-with-resources closes Connection/Statement/ResultSet automatically.
 */
// Two URL mappings: "/temperature" (clean, matches this app's style) and
// "/TemperatureServlet" (matches your teammate's original frontend calls).
@WebServlet({"/temperature", "/TemperatureServlet"})
public class TemperatureServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // How many records the live stream emits, and the pause between them.
    // (Streaming all 96k rows at 500ms each would take ~13 hours, so we cap it.)
    private static final int STREAM_LIMIT = 100;
    private static final long STREAM_DELAY_MS = 500;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String mode = request.getParameter("mode");
        if ("batch".equals(mode)) {
            handleBatchAnalysis(response);
        } else if ("stream".equals(mode)) {
            handleLiveStream(response);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid mode parameter (expected 'batch' or 'stream').");
        }
    }

    /**
     * BATCH MODE — compute Complete Summary Statistics in one SQL query and
     * return them as a single JSON object. AVG() and MAX() are aggregate
     * functions, so MySQL does the maths over all active rows in one pass.
     */
    private void handleBatchAnalysis(HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Aggregates ignore soft-deleted rows via WHERE is_active = 1.
        String sql = "SELECT AVG(temperature_c) AS final_avg, "
                   + "MAX(temperature_c) AS max_temp, "
                   + "COUNT(*) AS total "
                   + "FROM weather_data WHERE is_active = 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                double avg = rs.getDouble("final_avg");
                double max = rs.getDouble("max_temp");
                long total = rs.getLong("total");
                // Build the JSON by hand (no external JSON library needed).
                out.print("{\"average\": " + avg
                        + ", \"max\": " + max
                        + ", \"count\": " + total + "}");
            } else {
                out.print("{\"average\": 0, \"max\": 0, \"count\": 0}");
            }
        } catch (SQLException e) {
            // Proper HTTP 500 + a JSON error body (so the dashboard JS can react).
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Database error occurred.\"}");
        }
    }

    /**
     * REAL-TIME MODE (SSE) — stream records one at a time and report the
     * INCREMENTAL RUNNING AVERAGE after each one.
     *
     * Server-Sent Events: we set Content-Type "text/event-stream" and write
     * messages in the form "data: <payload>\n\n", flushing after each so the
     * browser's EventSource receives them live. The running average is just
     * sum/count maintained as we read each row.
     *
     * We LIMIT the stream to STREAM_LIMIT rows so the demo finishes quickly.
     * (Because of the LIMIT, the final running average covers those N rows, not
     * all 96k — that is the expected difference between the real-time view and
     * the batch summary.)
     */
    private void handleLiveStream(HttpServletResponse response) throws IOException {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        PrintWriter out = response.getWriter();

        // ORDER BY id ASC = stream in the same order the data was loaded.
        String sql = "SELECT temperature_c FROM weather_data "
                   + "WHERE is_active = 1 ORDER BY id ASC LIMIT " + STREAM_LIMIT;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            double sum = 0;
            int count = 0;

            while (rs.next()) {
                double temp = rs.getDouble("temperature_c");
                sum += temp;
                count++;
                double runningAvg = sum / count;   // incremental average

                // One SSE message per record.
                String jsonMsg = String.format(
                        "{\"current_temp\": %.2f, \"running_avg\": %.2f, \"count\": %d}",
                        temp, runningAvg, count);
                out.print("data: " + jsonMsg + "\n\n");
                out.flush();   // push immediately to the browser

                // If the client disconnected, stop streaming.
                if (out.checkError()) {
                    break;
                }

                Thread.sleep(STREAM_DELAY_MS);   // pace the stream for the demo
            }

            // Tell the client the stream is finished.
            out.print("event: done\ndata: Stream complete\n\n");
            out.flush();

        } catch (InterruptedException e) {
            // Good practice: restore the interrupt flag if the sleep was cut short.
            Thread.currentThread().interrupt();
        } catch (SQLException e) {
            out.print("event: error\ndata: Database error during stream\n\n");
            out.flush();
        }
    }
}
