<%-- ===================================================================
     index.jsp — Dataset Management panel (Member 1), inside the unified
     sidebar shell. BrowseServlet forwards here with the records/paging
     attributes; this page only displays them (the View in MVC).
=================================================================== --%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.*, com.weatherapp.model.WeatherRecord" %>
<%!
    // esc() — HTML-escape values to prevent XSS before printing them.
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
    // Tells the shared sidebar which nav item to highlight.
    String activePage = "dataset";

    // Data placed on the request by BrowseServlet (null-guarded for direct load).
    @SuppressWarnings("unchecked")
    List<WeatherRecord> records = (List<WeatherRecord>) request.getAttribute("records");
    String search = (String) request.getAttribute("search");
    if (search == null) search = "";
    Integer pageNum = (Integer) request.getAttribute("page");   // "page" is reserved in JSP
    if (pageNum == null) pageNum = 1;
    Integer totalPages = (Integer) request.getAttribute("totalPages");
    if (totalPages == null) totalPages = 1;
    Integer totalCount = (Integer) request.getAttribute("totalCount");
    if (totalCount == null) totalCount = 0;
    Integer datasetSize = (Integer) request.getAttribute("datasetSize");   // whole-dataset size
    if (datasetSize == null) datasetSize = 0;
    String msg = (String) request.getAttribute("msg");

    String encSearch = java.net.URLEncoder.encode(search, "UTF-8");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dataset Management &mdash; Weather Analytics</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<div class="app">
    <%@ include file="sidebar.jsp" %>
    <main class="main">

        <header class="page-header">
            <h1>Dataset Management</h1>
            <p class="subtitle">Upload, browse, search, edit and soft-delete weather records.</p>
            <%-- Remark: current size of the whole active dataset (live count). --%>
            <p class="dataset-remark">&#128202; Current dataset: <strong><%= String.format("%,d", datasetSize) %></strong> active records</p>
        </header>

        <% if (msg != null && !msg.isEmpty()) { %>
            <div class="banner"><%= esc(msg) %></div>
        <% } %>

        <%-- UPLOAD + CLEAR --%>
        <section class="panel">
            <h2>Upload CSV</h2>
            <div style="display:flex; gap:14px; align-items:center; flex-wrap:wrap;">
                <form action="upload" method="post" enctype="multipart/form-data" class="inline-form">
                    <input type="file" name="csvFile" accept=".csv" required>
                    <button type="submit" class="btn btn-primary">Upload CSV</button>
                </form>
                <%-- Clear Dataset: hard reset (TRUNCATE). Destructive -> confirm first. --%>
                <form action="reset" method="post"
                      onsubmit="return confirm('This will PERMANENTLY remove ALL records and reset the dataset to empty. Continue?');">
                    <button type="submit" class="btn btn-danger">Clear Dataset</button>
                </form>
            </div>
            <p class="hint">Expected file: <code>weatherHistory.csv</code> (header row is skipped automatically).
               <strong>Clear Dataset</strong> empties the table so re-uploading does not create duplicates.</p>
        </section>

        <%-- SEARCH --%>
        <section class="panel">
            <h2>Search</h2>
            <form action="browse" method="get" class="inline-form">
                <input type="text" name="search" value="<%= esc(search) %>"
                       placeholder="Search summary, date or daily summary...">
                <button type="submit" class="btn">Search</button>
                <a href="browse" class="btn btn-ghost">Reset</a>
            </form>
        </section>

        <%-- RECORDS --%>
        <section class="panel">
            <h2>Records</h2>
            <% if (records == null || records.isEmpty()) { %>
                <p class="empty">No records &mdash; upload the CSV.</p>
            <% } else { %>
            <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>ID</th><th>Date</th><th>Summary</th><th>Precip</th>
                        <th>Temp(C)</th><th>Apparent(C)</th><th>Humidity</th>
                        <th>Wind(km/h)</th><th>Pressure</th><th>Daily Summary</th><th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                <% for (WeatherRecord r : records) { %>
                    <tr>
                        <td><%= r.getId() %></td>
                        <td><%= esc(r.getFormattedDate()) %></td>
                        <td><%= esc(r.getSummary()) %></td>
                        <td><%= r.getPrecipType() == null ? "<span class=\"null\">null</span>" : esc(r.getPrecipType()) %></td>
                        <td><%= r.getTemperatureC() %></td>
                        <td><%= r.getApparentTemperatureC() %></td>
                        <td><%= r.getHumidity() %></td>
                        <td><%= r.getWindSpeedKmh() %></td>
                        <td><%= r.getPressureMb() %></td>
                        <td class="daily"><%= esc(r.getDailySummary()) %></td>
                        <td class="actions">
                            <a class="btn btn-small" href="edit?id=<%= r.getId() %>">Edit</a>
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

    </main>
</div>
</body>
</html>
