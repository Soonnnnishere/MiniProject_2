<%-- ===================================================================
     index.jsp — the VIEW (the "V" in MVC) for the Dataset Management panel.
     It renders: the upload form, the search box, and the paginated table
     with Edit / soft-Delete buttons.

     HOW JSP WORKS (viva): a JSP is compiled by Tomcat into a servlet the first
     time it is requested. It is HTML with embedded Java. The data it shows is
     put on the request by BrowseServlet (request.setAttribute), so this page
     contains NO database code — it only displays what the controller prepared.

     JSP syntax used below:
       <%@ ... %>  = directive (page settings / imports)
       <%! ... %>  = declaration (defines a method/field on the servlet)
       <%  ... %>  = scriptlet (Java statements that run during rendering)
       <%= ... %>  = expression (prints a value into the HTML)
     ('request' is an implicit object JSP gives us for free.)
=================================================================== --%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.*, com.weatherapp.model.WeatherRecord" %>
<%!
    // esc() — escapes HTML special characters to prevent XSS (cross-site
    // scripting). If a record's text contained "<script>", we must show it as
    // text, not run it. Always escape user/DB data before putting it in HTML.
    private String esc(Object o) {
        if (o == null) return "";
        String s = String.valueOf(o);
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
%>
<%
    // Pull the data the BrowseServlet placed on the request. Each getAttribute
    // is null-guarded so opening the page directly (before any servlet runs)
    // still renders a clean empty state instead of crashing.
    @SuppressWarnings("unchecked")
    List<WeatherRecord> records = (List<WeatherRecord>) request.getAttribute("records");
    String search = (String) request.getAttribute("search");
    if (search == null) search = "";
    // NOTE: this local is called pageNum, not "page", because "page" is a
    // RESERVED JSP implicit object — naming a variable "page" is a compile error.
    Integer pageNum = (Integer) request.getAttribute("page");
    if (pageNum == null) pageNum = 1;
    Integer totalPages = (Integer) request.getAttribute("totalPages");
    if (totalPages == null) totalPages = 1;
    Integer totalCount = (Integer) request.getAttribute("totalCount");
    if (totalCount == null) totalCount = 0;
    String msg = (String) request.getAttribute("msg");

    // URL-encode the search term so it is safe to put inside the pagination links.
    String encSearch = java.net.URLEncoder.encode(search, "UTF-8");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Weather Dataset Management</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<div class="container">
    <header class="page-header">
        <h1>Weather Dataset Management</h1>
        <p class="subtitle">Upload, browse, search, edit and soft-delete weather records.</p>
    </header>

    <%-- Success/status banner (e.g. "Inserted 96453 rows") if one was passed --%>
    <% if (msg != null && !msg.isEmpty()) { %>
        <div class="banner"><%= esc(msg) %></div>
    <% } %>

    <%-- UPLOAD: multipart form posts the chosen file to the /upload servlet.
         enctype="multipart/form-data" is REQUIRED for file uploads. --%>
    <section class="panel">
        <h2>Upload CSV</h2>
        <form action="upload" method="post" enctype="multipart/form-data" class="inline-form">
            <input type="file" name="csvFile" accept=".csv" required>
            <button type="submit" class="btn btn-primary">Upload CSV</button>
        </form>
        <p class="hint">Expected file: <code>weatherHistory.csv</code> (header row is skipped automatically).</p>
    </section>

    <%-- SEARCH: a GET form to /browse; the typed text becomes ?search=... --%>
    <section class="panel">
        <h2>Search</h2>
        <form action="browse" method="get" class="inline-form">
            <input type="text" name="search" value="<%= esc(search) %>"
                   placeholder="Search summary, date or daily summary...">
            <button type="submit" class="btn">Search</button>
            <a href="browse" class="btn btn-ghost">Reset</a>
        </form>
    </section>

    <%-- RECORDS TABLE --%>
    <section class="panel">
        <h2>Records</h2>
        <% if (records == null || records.isEmpty()) { %>
            <p class="empty">No records &mdash; upload the CSV.</p>
        <% } else { %>
        <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Date</th>
                    <th>Summary</th>
                    <th>Precip</th>
                    <th>Temp(C)</th>
                    <th>Apparent(C)</th>
                    <th>Humidity</th>
                    <th>Wind(km/h)</th>
                    <th>Pressure</th>
                    <th>Daily Summary</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
            <%-- Loop over the page of records and print one <tr> per record. --%>
            <% for (WeatherRecord r : records) { %>
                <tr>
                    <td><%= r.getId() %></td>
                    <td><%= esc(r.getFormattedDate()) %></td>
                    <td><%= esc(r.getSummary()) %></td>
                    <%-- NULL precip is shown as a greyed "null" label --%>
                    <td><%= r.getPrecipType() == null ? "<span class=\"null\">null</span>" : esc(r.getPrecipType()) %></td>
                    <td><%= r.getTemperatureC() %></td>
                    <td><%= r.getApparentTemperatureC() %></td>
                    <td><%= r.getHumidity() %></td>
                    <td><%= r.getWindSpeedKmh() %></td>
                    <td><%= r.getPressureMb() %></td>
                    <td class="daily"><%= esc(r.getDailySummary()) %></td>
                    <td class="actions">
                        <%-- Edit = a link to GET /edit?id=N (shows the form) --%>
                        <a class="btn btn-small" href="edit?id=<%= r.getId() %>">Edit</a>
                        <%-- Delete = a tiny POST form to /delete with the row id.
                             onsubmit confirm() asks the user before soft-deleting. --%>
                        <form action="delete" method="post" class="inline"
                              onsubmit="return confirm('Soft-delete this record?');">
                            <input type="hidden" name="id" value="<%= r.getId() %>">
                            <button type="submit" class="btn btn-small btn-danger">Delete</button>
                        </form>
                    </td>
                </tr>
            <% } %>
            </tbody>
        </table>
        </div>

        <%-- PAGINATION: Prev/Next links carry the search term and the new page
             number. Disabled (grey) when already on the first/last page. --%>
        <nav class="pagination">
            <% if (pageNum > 1) { %>
                <a class="btn btn-small" href="browse?search=<%= encSearch %>&page=<%= (pageNum - 1) %>">&laquo; Prev</a>
            <% } else { %>
                <span class="btn btn-small btn-disabled">&laquo; Prev</span>
            <% } %>

            <span class="page-info">Page <%= pageNum %> of <%= totalPages %> (<%= totalCount %> records)</span>

            <% if (pageNum < totalPages) { %>
                <a class="btn btn-small" href="browse?search=<%= encSearch %>&page=<%= (pageNum + 1) %>">Next &raquo;</a>
            <% } else { %>
                <span class="btn btn-small btn-disabled">Next &raquo;</span>
            <% } %>
        </nav>
        <% } %>
    </section>
</div>
</body>
</html>
