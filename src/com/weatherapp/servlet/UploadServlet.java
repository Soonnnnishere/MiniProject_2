package com.weatherapp.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.weatherapp.dao.WeatherDAO;
import com.weatherapp.model.WeatherRecord;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

/**
 * UploadServlet — receives the uploaded weatherHistory.csv, parses every row,
 * and batch-inserts it into MySQL via the DAO. This is the "C" (Controller) in
 * MVC: it handles the request, calls the model/DAO, and sends a response.
 *
 * SERVLET BASICS (viva):
 *   - A servlet is a Java class that handles HTTP requests on the server.
 *   - It extends HttpServlet and overrides doGet/doPost.
 *   - @WebServlet("/upload") maps it to the URL .../MiniProject2/upload — this
 *     annotation replaces a <servlet-mapping> entry in web.xml.
 *   - The browser's <form method="post"> hits doPost().
 *
 * @MultipartConfig: required so the servlet can read a FILE upload
 *   (enctype="multipart/form-data"). Without it, request.getPart() fails.
 *   maxFileSize/maxRequestSize cap the upload size for safety.
 */
@WebServlet("/upload")
@MultipartConfig(maxFileSize = 50 * 1024 * 1024, maxRequestSize = 60 * 1024 * 1024)
public class UploadServlet extends HttpServlet {

    // serialVersionUID: HttpServlet is Serializable; this id is a Java
    // housekeeping field. Not functionally important here.
    private static final long serialVersionUID = 1L;

    private final WeatherDAO dao = new WeatherDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            // getPart("csvFile") grabs the uploaded file. "csvFile" must match
            // the <input type="file" name="csvFile"> in index.jsp.
            Part filePart = request.getPart("csvFile");
            if (filePart == null || filePart.getSize() == 0) {
                // No file chosen -> return HTTP 400 Bad Request (web.xml maps it
                // to a friendly page; the user never sees a raw error).
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "No CSV file was provided. Please choose a file and try again.");
                return;
            }

            List<WeatherRecord> records = new ArrayList<>();
            int skipped = 0;

            // Read the uploaded file as text, one line at a time. BufferedReader
            // is memory-friendly: we stream the file rather than loading it whole.
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(filePart.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                boolean firstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false;   // skip the CSV header row
                        continue;
                    }
                    if (line.trim().isEmpty()) {
                        continue;            // skip blank lines
                    }
                    WeatherRecord rec = parseLine(line);
                    if (rec == null) {
                        skipped++;           // malformed row -> count and skip
                    } else {
                        records.add(rec);
                    }
                }
            }

            // One DAO call inserts all parsed rows using JDBC batching.
            int inserted = dao.batchInsert(records);

            // POST-REDIRECT-GET pattern: after a successful POST we redirect to
            // /browse (a GET). This stops a browser refresh from re-submitting
            // the upload. The count is passed back as a URL-encoded message.
            String msg = "Inserted " + inserted + " rows (" + skipped + " skipped)";
            response.sendRedirect(request.getContextPath() + "/browse?msg="
                    + URLEncoder.encode(msg, StandardCharsets.UTF_8));

        } catch (Exception e) {
            // Any unexpected failure -> HTTP 500, never a raw stack trace to the user.
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Upload failed: " + e.getMessage());
        }
    }

    /**
     * Parses ONE CSV data line into a WeatherRecord.
     * The dataset has 12 comma-separated fields and no embedded commas, so a
     * simple split is safe. Returns null for a malformed line so the caller can
     * skip it instead of aborting the whole upload (robustness).
     */
    private WeatherRecord parseLine(String line) {
        try {
            // limit -1 keeps trailing empty fields if any.
            String[] f = line.split(",", -1);
            if (f.length < 12) {
                return null;   // not enough columns -> malformed
            }

            WeatherRecord r = new WeatherRecord();
            r.setFormattedDate(trim(f[0]));
            r.setSummary(trim(f[1]));

            // "Precip Type" is sometimes the literal text "null" -> store SQL NULL.
            String precip = trim(f[2]);
            if (precip == null || precip.isEmpty() || precip.equalsIgnoreCase("null")) {
                r.setPrecipType(null);
            } else {
                r.setPrecipType(precip);
            }

            r.setTemperatureC(Double.parseDouble(trim(f[3])));
            r.setApparentTemperatureC(Double.parseDouble(trim(f[4])));
            r.setHumidity(Double.parseDouble(trim(f[5])));
            r.setWindSpeedKmh(Double.parseDouble(trim(f[6])));
            // Wind bearing arrives like "251.0" -> parse as double then cast to int.
            r.setWindBearingDeg((int) Double.parseDouble(trim(f[7])));
            r.setVisibilityKm(Double.parseDouble(trim(f[8])));
            r.setLoudCover(Double.parseDouble(trim(f[9])));
            r.setPressureMb(Double.parseDouble(trim(f[10])));
            r.setDailySummary(trim(f[11]));

            return r;
        } catch (Exception e) {
            // Bad number or unexpected structure -> treat the row as skippable.
            return null;
        }
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
