package com.weatherapp.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import com.weatherapp.dao.WeatherDAO;
import com.weatherapp.model.WeatherRecord;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * BrowseServlet — the Controller for the browse/search/paginate screen.
 *
 * FLOW (classic MVC with JSP):
 *   1. Browser GETs /browse?search=...&page=...
 *   2. This servlet (Controller) reads the params, asks the DAO (Model) for the
 *      matching page of data,
 *   3. puts the results on the request as "attributes",
 *   4. and FORWARDS to index.jsp (View), which renders the HTML table.
 *   The JSP never touches the database — that is the whole point of MVC.
 *
 * GET vs POST: browsing/searching is a READ, so it uses doGet (safe, bookmarkable,
 * shows params in the URL).
 */
@WebServlet("/browse")
public class BrowseServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // 20 rows per page — the pagination "page size".
    private static final int PAGE_SIZE = 20;

    private final WeatherDAO dao = new WeatherDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            // --- read query parameters from the URL ---
            String search = request.getParameter("search");
            if (search == null) {
                search = "";          // no search box value -> match everything
            }

            int page = parsePage(request.getParameter("page"));

            // --- ask the DAO (Model) for this page + the total count ---
            List<WeatherRecord> records = dao.search(search, page, PAGE_SIZE);
            int totalCount = dao.count(search);
            // ceil(totalCount / PAGE_SIZE) computed with integer maths.
            int totalPages = (totalCount + PAGE_SIZE - 1) / PAGE_SIZE;
            if (totalPages < 1) {
                totalPages = 1;
            }

            // Total active records in the WHOLE dataset (ignores the search filter)
            // -> shown as the "current dataset size" remark. Reuse totalCount when
            // there is no search to avoid an extra query.
            int datasetSize = search.isEmpty() ? totalCount : dao.count("");

            // --- hand the data to the JSP via request attributes ---
            // (setAttribute = "put this object where the JSP can read it")
            request.setAttribute("records", records);
            request.setAttribute("search", search);
            request.setAttribute("page", page);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("totalCount", totalCount);
            request.setAttribute("datasetSize", datasetSize);          // whole-dataset remark
            request.setAttribute("msg", request.getParameter("msg"));  // success banner text

            // forward = server-side hand-off to the JSP (URL stays /browse).
            request.getRequestDispatcher("/index.jsp").forward(request, response);

        } catch (SQLException e) {
            // DB error -> HTTP 500, which web.xml maps to error/500.jsp.
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not load records: " + e.getMessage());
        }
    }

    /** Parses the page param defensively: defaults to 1, never below 1. */
    private int parsePage(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 1;
        }
        try {
            int p = Integer.parseInt(raw.trim());
            return p < 1 ? 1 : p;
        } catch (NumberFormatException e) {
            return 1;   // garbage in the URL -> fall back to page 1
        }
    }
}
