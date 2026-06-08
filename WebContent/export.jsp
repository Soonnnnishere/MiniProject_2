<%-- ===================================================================
     export.jsp — Export Control Panel (Member 3 / Reporting), in the unified
     sidebar shell. Backend = ExportServlet (/export).
       - In-browser analysis display: summary cards (avg/max/min/count)
       - HTML report preview: shows exactly what the file will contain
       - Download CSV / JSON buttons (force a file download via Content-Disposition)
       - Live Export Log viewer: refreshes after each download to prove the
         export_log audit row was inserted.
=================================================================== --%>
<%@ page contentType="text/html;charset=UTF-8" %>
<% String activePage = "export"; %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Export &mdash; Weather Analytics</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<div class="app">
    <%@ include file="sidebar.jsp" %>
    <main class="main">

        <header class="page-header">
            <h1>Export</h1>
            <p class="subtitle">Assemble the temperature analysis and download it as CSV or JSON.</p>
        </header>

        <%-- IN-BROWSER ANALYSIS DISPLAY: summary cards --%>
        <section class="panel">
            <h2>Temperature Analysis Summary</h2>
            <div class="metric-row">
                <div class="metric accent">
                    <span class="metric-label">Overall Average</span>
                    <span class="metric-value" id="sAvg">&mdash;</span>
                </div>
                <div class="metric warning">
                    <span class="metric-label">Maximum</span>
                    <span class="metric-value" id="sMax">&mdash;</span>
                </div>
                <div class="metric success">
                    <span class="metric-label">Records Analysed</span>
                    <span class="metric-value" id="sCount">&mdash;</span>
                </div>
            </div>
        </section>

        <%-- EXPORT CONTROL + HTML PREVIEW --%>
        <section class="panel">
            <h2>Export Control Panel</h2>
            <p class="hint">Preview the report below, then download. Every download is recorded in the export log.</p>
            <div style="margin-bottom:16px; display:flex; gap:10px; flex-wrap:wrap;">
                <button class="btn btn-primary" onclick="downloadFile('csv')">&#8681; Download CSV</button>
                <button class="btn btn-primary" onclick="downloadFile('json')">&#8681; Download JSON</button>
                <button class="btn btn-ghost" onclick="loadPreview()">Refresh Preview</button>
            </div>
            <h3 style="margin:0 0 8px; font-size:1rem;">HTML Report Preview</h3>
            <div id="previewBox" class="preview-box">Loading preview&hellip;</div>
        </section>

        <%-- LIVE EXPORT LOG VIEWER --%>
        <section class="panel">
            <h2>Export Log</h2>
            <p class="hint">Audit trail from the <code>export_log</code> table &mdash; refreshes automatically after each download.</p>
            <div class="table-wrap">
                <table>
                    <thead>
                        <tr><th>ID</th><th>Report</th><th>Format</th><th>Exported At</th></tr>
                    </thead>
                    <tbody id="logBody">
                        <tr><td colspan="4" class="empty">No exports yet.</td></tr>
                    </tbody>
                </table>
            </div>
        </section>

    </main>
</div>

<%-- hidden frame used to trigger downloads without leaving the page --%>
<iframe id="dlframe" style="display:none;" title="download"></iframe>

<script>
    // Load the analysis summary + build the HTML preview of the exact report contents.
    function loadPreview() {
        fetch("export?action=preview")
            .then(r => r.json())
            .then(d => {
                document.getElementById("sAvg").innerText   = d.summary.overallAverage.toFixed(2) + " °C";
                document.getElementById("sMax").innerText   = d.summary.maximum.toFixed(2) + " °C";
                document.getElementById("sCount").innerText = d.summary.count.toLocaleString();

                let html = "<strong>" + d.report + "</strong>"
                         + "<div class='muted'>Generated At: " + d.generatedAt + "</div>"
                         + "<table class='preview-table'><tr><th>Metric</th><th>Value</th></tr>"
                         + "<tr><td>Overall Average Temperature (C)</td><td>" + d.summary.overallAverage.toFixed(2) + "</td></tr>"
                         + "<tr><td>Maximum Temperature (C)</td><td>" + d.summary.maximum.toFixed(2) + "</td></tr>"
                         + "<tr><td>Active Records Analysed</td><td>" + d.summary.count + "</td></tr>"
                         + "</table>"
                         + "<div class='muted' style='margin-top:12px;'>Real-Time Running Average Milestones</div>"
                         + "<table class='preview-table'><tr><th>Records Processed</th><th>Running Average (C)</th></tr>";
                d.runningAverageMilestones.forEach(m => {
                    html += "<tr><td>" + m.recordsProcessed + "</td><td>" + m.runningAverage.toFixed(2) + "</td></tr>";
                });
                html += "</table>";
                document.getElementById("previewBox").innerHTML = html;
            })
            .catch(() => {
                document.getElementById("previewBox").innerText = "Could not load preview. Upload the dataset first.";
            });
    }

    // Load the export_log audit table.
    function loadLog() {
        fetch("export?action=log")
            .then(r => r.json())
            .then(rows => {
                const body = document.getElementById("logBody");
                if (!rows.length) {
                    body.innerHTML = "<tr><td colspan='4' class='empty'>No exports yet.</td></tr>";
                    return;
                }
                body.innerHTML = rows.map(r =>
                    "<tr><td>" + r.id + "</td><td>" + r.reportType + "</td><td>" + r.format + "</td><td>" + r.exportedAt + "</td></tr>"
                ).join("");
            });
    }

    // Trigger a file download via the hidden iframe, then refresh the log to
    // prove a new audit row was inserted into MySQL.
    function downloadFile(fmt) {
        document.getElementById("dlframe").src = "export?format=" + fmt;
        setTimeout(loadLog, 1200);
    }

    loadPreview();
    loadLog();
</script>
</body>
</html>
