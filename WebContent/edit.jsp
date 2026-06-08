<%-- ===================================================================
     edit.jsp — the VIEW for editing one record.
     EditServlet.doGet loads a WeatherRecord, puts it on the request as
     "record", and forwards here. This page renders a form pre-filled with that
     record's values; submitting it POSTs back to /edit, where doPost saves it.
=================================================================== --%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.weatherapp.model.WeatherRecord" %>
<%!
    // Same HTML-escaping helper as index.jsp (prevents XSS when echoing values).
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
    // Read the record the servlet forwarded. If it is missing (e.g. page opened
    // directly), fail safely with a 404 instead of a NullPointerException.
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
    <title>Edit Record #<%= r.getId() %> &mdash; Weather Dataset</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<div class="container">
    <header class="page-header">
        <h1>Edit Record #<%= r.getId() %></h1>
        <p class="subtitle">Date: <%= esc(r.getFormattedDate()) %></p>
    </header>

    <section class="panel">
        <%-- POST to /edit. The hidden "id" tells the servlet which row to UPDATE.
             Each input's value is pre-filled from the record's getter. The
             name attributes must match request.getParameter(...) in EditServlet. --%>
        <form action="edit" method="post" class="edit-form">
            <input type="hidden" name="id" value="<%= r.getId() %>">

            <div class="field">
                <label for="summary">Summary</label>
                <input type="text" id="summary" name="summary" value="<%= esc(r.getSummary()) %>">
            </div>

            <div class="field">
                <label for="precip_type">Precip Type</label>
                <%-- null precip -> empty box; leaving it blank stores SQL NULL --%>
                <input type="text" id="precip_type" name="precip_type"
                       value="<%= r.getPrecipType() == null ? "" : esc(r.getPrecipType()) %>"
                       placeholder="leave blank for null">
            </div>

            <%-- type="number" step="any" allows decimal values like 9.47 --%>
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
</div>
</body>
</html>
