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
 * ResetServlet — handles the "Clear Dataset" button: wipes the whole table so
 * the dataset is empty (e.g. before re-importing the CSV).
 *
 * This is a HARD reset (DAO.clearAll() -> TRUNCATE), deliberately different from
 * the per-record soft-delete. It is destructive, so it is exposed only via POST
 * (the button) and the UI asks for confirmation first.
 */
@WebServlet("/reset")
public class ResetServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final WeatherDAO dao = new WeatherDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            dao.clearAll();   // TRUNCATE TABLE weather_data
            // Post-Redirect-Get back to the (now empty) list with a message.
            response.sendRedirect(request.getContextPath() + "/browse?msg=Dataset+cleared");
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not clear dataset: " + e.getMessage());
        }
    }
}
