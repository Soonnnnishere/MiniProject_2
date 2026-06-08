package com.weatherapp.servlet;

import java.io.IOException;
import java.sql.SQLException;

import com.weatherapp.dao.WeatherDAO;
import com.weatherapp.model.WeatherRecord;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * EditServlet — handles the "edit a record" feature using ONE servlet for two
 * jobs (a common pattern):
 *   - doGet  -> show the pre-filled edit form (the user clicked "Edit").
 *   - doPost -> apply the submitted changes (the user clicked "Save").
 *
 * This separation matches HTTP semantics: GET reads (show form), POST writes
 * (perform the update).
 */
@WebServlet("/edit")
public class EditServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final WeatherDAO dao = new WeatherDAO();

    /** GET /edit?id=N -> load record N and show it in edit.jsp. */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long id;
        try {
            id = parseId(request.getParameter("id"));
        } catch (NumberFormatException e) {
            // Missing/garbage id -> HTTP 400 Bad Request.
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing record id.");
            return;
        }

        try {
            WeatherRecord record = dao.findById(id);
            if (record == null) {
                // No such row -> HTTP 404 Not Found.
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Record " + id + " was not found.");
                return;
            }
            // Put the record on the request and forward to the form view.
            request.setAttribute("record", record);
            request.getRequestDispatcher("/edit.jsp").forward(request, response);
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not load record: " + e.getMessage());
        }
    }

    /** POST /edit -> read the form fields, build a record, UPDATE it. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        WeatherRecord r = new WeatherRecord();
        try {
            // Read every form field (name attributes in edit.jsp must match).
            r.setId(parseId(request.getParameter("id")));
            r.setSummary(request.getParameter("summary"));

            // Blank or "null" precip box -> store SQL NULL.
            String precip = request.getParameter("precip_type");
            if (precip == null || precip.trim().isEmpty() || precip.trim().equalsIgnoreCase("null")) {
                r.setPrecipType(null);
            } else {
                r.setPrecipType(precip.trim());
            }

            r.setTemperatureC(parseDouble(request.getParameter("temperature_c")));
            r.setApparentTemperatureC(parseDouble(request.getParameter("apparent_temperature_c")));
            r.setHumidity(parseDouble(request.getParameter("humidity")));
            r.setWindSpeedKmh(parseDouble(request.getParameter("wind_speed_kmh")));
            r.setWindBearingDeg(parseInt(request.getParameter("wind_bearing_deg")));
            r.setVisibilityKm(parseDouble(request.getParameter("visibility_km")));
            r.setPressureMb(parseDouble(request.getParameter("pressure_mb")));
            r.setDailySummary(request.getParameter("daily_summary"));
        } catch (NumberFormatException e) {
            // A non-numeric value in a number field -> HTTP 400.
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "One or more numeric fields were invalid.");
            return;
        }

        try {
            dao.update(r);
            // Post-Redirect-Get: redirect back to the list after saving.
            response.sendRedirect(request.getContextPath() + "/browse?msg=Updated");
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not update record: " + e.getMessage());
        }
    }

    // --- small parsing helpers that throw NumberFormatException on bad input ---

    private static long parseId(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new NumberFormatException("missing id");
        }
        return Long.parseLong(raw.trim());
    }

    private static double parseDouble(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new NumberFormatException("missing number");
        }
        return Double.parseDouble(raw.trim());
    }

    private static int parseInt(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new NumberFormatException("missing number");
        }
        // Accept "251" or "251.0" defensively.
        return (int) Double.parseDouble(raw.trim());
    }
}
