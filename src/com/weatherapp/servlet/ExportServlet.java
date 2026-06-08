package com.weatherapp.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

import com.weatherapp.dao.ExportDAO;
import com.weatherapp.dao.WeatherDAO;
import com.weatherapp.model.TemperatureReport;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * ExportServlet (Member 3 / Reporting) — assembles Member 2's temperature
 * analysis into a report and serves it in several ways via ONE endpoint:
 *
 *   GET /export?action=preview   -> report as JSON (for the in-browser preview; NO download)
 *   GET /export?action=log       -> recent export_log rows as JSON (for the log viewer)
 *   GET /export?format=csv       -> streams a .csv file (Content-Disposition) + logs the export
 *   GET /export?format=json      -> streams a .json file (Content-Disposition) + logs the export
 *
 * "Force download" = setting the Content-Disposition: attachment header. Without
 * it the browser would just display the text; with it the browser saves a file.
 */
@WebServlet("/export")
public class ExportServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String REPORT_NAME = "Temperature Analysis Report";

    private final WeatherDAO weatherDao = new WeatherDAO();
    private final ExportDAO exportDao = new ExportDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String format = request.getParameter("format");
        String action = request.getParameter("action");
        try {
            if ("csv".equalsIgnoreCase(format)) {
                downloadCsv(response);
            } else if ("json".equalsIgnoreCase(format)) {
                downloadJson(response);
            } else if ("log".equals(action)) {
                writeLogJson(response);
            } else {
                writePreviewJson(response);   // default = preview
            }
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Export failed: " + e.getMessage());
        }
    }

    // ---- download modes (force a file download + write an audit row) ----

    private void downloadCsv(HttpServletResponse response) throws SQLException, IOException {
        TemperatureReport r = weatherDao.getTemperatureReport();
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        // This header is what makes the browser DOWNLOAD (save) the file.
        response.setHeader("Content-Disposition", "attachment; filename=\"temperature_report.csv\"");
        PrintWriter out = response.getWriter();   // write the file straight to the HTTP stream
        out.print(buildCsv(r));
        exportDao.logExport(REPORT_NAME, "CSV");  // audit: one row per download
    }

    private void downloadJson(HttpServletResponse response) throws SQLException, IOException {
        TemperatureReport r = weatherDao.getTemperatureReport();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"temperature_report.json\"");
        PrintWriter out = response.getWriter();
        out.print(buildReportJson(r));
        exportDao.logExport(REPORT_NAME, "JSON");
    }

    // ---- view modes (no download, no logging — just JSON for the page) ----

    private void writePreviewJson(HttpServletResponse response) throws SQLException, IOException {
        TemperatureReport r = weatherDao.getTemperatureReport();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().print(buildReportJson(r));   // note: NO Content-Disposition
    }

    private void writeLogJson(HttpServletResponse response) throws SQLException, IOException {
        List<String[]> rows = exportDao.getRecentExports(10);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().print(buildLogJson(rows));
    }

    // ---- formatting helpers ----

    private String now() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
    }

    private static double round2(double d) {
        return Math.round(d * 100.0) / 100.0;
    }

    /** Builds the CSV report text (sectioned: summary + milestones). */
    private String buildCsv(TemperatureReport r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Temperature Analysis Report\n");
        sb.append("Generated At,").append(now()).append("\n\n");
        sb.append("Summary Statistics\n");
        sb.append("Metric,Value\n");
        sb.append("Overall Average Temperature (C),").append(round2(r.getAverage())).append("\n");
        sb.append("Maximum Temperature (C),").append(round2(r.getMaximum())).append("\n");
        sb.append("Active Records Analysed,").append(r.getCount()).append("\n\n");
        sb.append("Real-Time Running Average Milestones\n");
        sb.append("Records Processed,Running Average (C)\n");
        int[] mc = r.getMilestoneCounts();
        double[] ma = r.getMilestoneAverages();
        for (int i = 0; i < mc.length; i++) {
            sb.append(mc[i]).append(",").append(round2(ma[i])).append("\n");
        }
        return sb.toString();
    }

    /** Builds the JSON report (also reused for the preview). */
    private String buildReportJson(TemperatureReport r) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"report\":\"").append(REPORT_NAME).append("\",");
        sb.append("\"generatedAt\":\"").append(now()).append("\",");
        sb.append("\"summary\":{");
        sb.append("\"overallAverage\":").append(round2(r.getAverage())).append(",");
        sb.append("\"maximum\":").append(round2(r.getMaximum())).append(",");
        sb.append("\"count\":").append(r.getCount());
        sb.append("},");
        sb.append("\"runningAverageMilestones\":[");
        int[] mc = r.getMilestoneCounts();
        double[] ma = r.getMilestoneAverages();
        for (int i = 0; i < mc.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"recordsProcessed\":").append(mc[i])
              .append(",\"runningAverage\":").append(round2(ma[i])).append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    /** Builds a JSON array of recent export_log rows for the live log viewer. */
    private String buildLogJson(List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"id\":").append(row[0])
              .append(",\"reportType\":\"").append(jsonEsc(row[1])).append("\"")
              .append(",\"format\":\"").append(jsonEsc(row[2])).append("\"")
              .append(",\"exportedAt\":\"").append(jsonEsc(row[3])).append("\"}");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String jsonEsc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
