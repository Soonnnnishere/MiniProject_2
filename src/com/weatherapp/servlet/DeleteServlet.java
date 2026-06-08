package com.weatherapp.servlet;

import java.io.IOException;
import java.sql.SQLException;

import com.weatherapp.dao.WeatherDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * DeleteServlet — performs the SOFT delete.
 *
 * IMPORTANT (viva): this servlet NEVER runs a SQL DELETE. It calls
 * dao.softDelete(id), which does UPDATE ... SET is_active = 0. The row stays in
 * the database; it is just hidden from every read. This satisfies the project's
 * mandatory soft-delete rule and keeps data recoverable.
 */
@WebServlet("/delete")
public class DeleteServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final WeatherDAO dao = new WeatherDAO();

    /** Allow GET to reuse the same logic (handy for a simple link). */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Validate the id before touching the DB.
        long id;
        String raw = request.getParameter("id");
        if (raw == null || raw.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing record id.");
            return;
        }
        try {
            id = Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid record id.");
            return;
        }

        try {
            dao.softDelete(id);   // UPDATE is_active = 0
            // Redirect back to the list with a confirmation message.
            response.sendRedirect(request.getContextPath() + "/browse?msg=Record+soft-deleted");
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not delete record: " + e.getMessage());
        }
    }
}
