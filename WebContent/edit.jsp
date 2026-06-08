<%-- ===================================================================
     edit.jsp — edit form for one record (part of Dataset Management),
     inside the unified sidebar shell. EditServlet.doGet forwards here with
     the "record"; submitting POSTs back to /edit (doPost) to UPDATE it.
=================================================================== --%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.weatherapp.model.WeatherRecord" %>
<%!
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
    String activePage = "dataset";
    WeatherRecord r = (WeatherRecord) request.getAttribute("record");
    if (r == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Record not found.");
        return;
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Edit Record #<%= r.getId() %> &mdash; Weather Analytics</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<div class="app">
    <%@ include file="sidebar.jsp" %>
    <main class="main">

        <header class="page-header">
            <h1>Edit Record #<%= r.getId() %></h1>
            <p class="subtitle">Date: <%= esc(r.getFormattedDate()) %></p>
        </header>

        <section class="panel">
            <form action="edit" method="post" class="edit-form">
                <input type="hidden" name="id" value="<%= r.getId() %>">

                <div class="field">
                    <label for="summary">Summary</label>
                    <input type="text" id="summary" name="summary" value="<%= esc(r.getSummary()) %>">
                </div>

                <div class="field">
                    <label for="precip_type">Precip Type</label>
                    <input type="text" id="precip_type" name="precip_type"
                           value="<%= r.getPrecipType() == null ? "" : esc(r.getPrecipType()) %>"
                           placeholder="leave blank for null">
                </div>

                <div class="field">
                    <label for="temperature_c">Temperature (C)</label>
                    <input type="number" step="any" id="temperature_c" name="temperature_c"
                           value="<%= r.getTemperatureC() %>" required>
                </div>

                <div class="field">
                    <label for="apparent_temperature_c">Apparent Temperature (C)</label>
                    <input type="number" step="any" id="apparent_temperature_c" name="apparent_temperature_c"
                           value="<%= r.getApparentTemperatureC() %>" required>
                </div>

                <div class="field">
                    <label for="humidity">Humidity</label>
                    <input type="number" step="any" id="humidity" name="humidity"
                           value="<%= r.getHumidity() %>" required>
                </div>

                <div class="field">
                    <label for="wind_speed_kmh">Wind Speed (km/h)</label>
                    <input type="number" step="any" id="wind_speed_kmh" name="wind_speed_kmh"
                           value="<%= r.getWindSpeedKmh() %>" required>
                </div>

                <div class="field">
                    <label for="wind_bearing_deg">Wind Bearing (degrees)</label>
                    <input type="number" step="any" id="wind_bearing_deg" name="wind_bearing_deg"
                           value="<%= r.getWindBearingDeg() %>" required>
                </div>

                <div class="field">
                    <label for="visibility_km">Visibility (km)</label>
                    <input type="number" step="any" id="visibility_km" name="visibility_km"
                           value="<%= r.getVisibilityKm() %>" required>
                </div>

                <div class="field">
                    <label for="pressure_mb">Pressure (millibars)</label>
                    <input type="number" step="any" id="pressure_mb" name="pressure_mb"
                           value="<%= r.getPressureMb() %>" required>
                </div>

                <div class="field field-wide">
                    <label for="daily_summary">Daily Summary</label>
                    <input type="text" id="daily_summary" name="daily_summary"
                           value="<%= esc(r.getDailySummary()) %>">
                </div>

                <div class="form-actions">
                    <button type="submit" class="btn btn-primary">Save</button>
                    <a href="browse" class="btn btn-ghost">Cancel</a>
                </div>
            </form>
        </section>

    </main>
</div>
</body>
</html>
