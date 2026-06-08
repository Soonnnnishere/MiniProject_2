<%-- ===================================================================
     analytics.jsp — HYBRID page that holds BOTH the Analysis panel (Member 2)
     and the Export panel (Member 3) as in-page TABS switched by JavaScript.

     WHY: the sidebar uses full-page navigation, so going from a separate
     Analysis page to a separate Export page reloads the browser and kills the
     live SSE stream. Here, Analysis <-> Export is a JS show/hide on the SAME
     page (no reload), so the running-average stream + chart keep going.
     (The Dataset panel stays its own server-rendered page — unchanged.)
=================================================================== --%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%
    // Which tab to show first (from the sidebar link ?tab=...). Also drives the
    // sidebar highlight via activePage.
    String tab = request.getParameter("tab");
    if (tab == null || (!tab.equals("export") && !tab.equals("analysis"))) tab = "analysis";
    String activePage = tab.equals("export") ? "export" : "analysis";
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Analytics &mdash; Weather Analytics</title>
    <link rel="stylesheet" href="css/style.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body>
<div class="app">
    <%@ include file="sidebar.jsp" %>
    <main class="main">

        <header class="page-header">
            <h1>Analytics</h1>
            <p class="subtitle">Analysis and report export &mdash; switch tabs without losing the live stream.</p>
        </header>

        <%-- In-page tab bar: JS-switched, NO page reload (stream survives). --%>
        <div class="tabs">
            <button class="tab-btn <%= tab.equals("analysis") ? "active" : "" %>" id="tabbtn-analysis" onclick="showTab('analysis')">Analysis</button>
            <button class="tab-btn <%= tab.equals("export")   ? "active" : "" %>" id="tabbtn-export"   onclick="showTab('export')">Export</button>
        </div>

        <%-- ════════════════════════════════════════════════════════════════
             MEMBER 2 (Data Analyst) — ANALYSIS TAB  [START]
             Owner: Member 2.  Backend servlet: TemperatureServlet.java
               - ?mode=batch  -> AVG + MAX in one query, single JSON response
               - ?mode=stream -> SSE incremental running average (first 100 records)
             JS for this section: runBatch(), initChart(), startStream(), stopStream()
        ════════════════════════════════════════════════════════════════ --%>
        <div id="tab-analysis" class="tab-panel" style="<%= tab.equals("analysis") ? "" : "display:none;" %>">

            <section class="panel">
                <h2>Batch Analysis</h2>
                <p class="hint">Full calculation runs on the server and returns the complete result in a single HTTP response.</p>
                <button class="btn btn-primary" onclick="runBatch()">Run Batch Analysis</button>
                <div class="metric-row">
                    <div class="metric accent">
                        <span class="metric-label">Final Average</span>
                        <span class="metric-value" id="batchAvg">&mdash;</span>
                    </div>
                    <div class="metric accent">
                        <span class="metric-label">Maximum</span>
                        <span class="metric-value" id="batchMax">&mdash;</span>
                    </div>
                </div>
            </section>

            <section class="panel">
                <h2>Real-Time Stream (SSE)</h2>
                <p class="hint">Streams active records one-by-one; the running average updates live without refreshing the page.</p>
                <button class="btn btn-success" onclick="startStream()">Start Live Stream</button>
                <button class="btn btn-danger" onclick="stopStream()">Stop Stream</button>
                <div class="metric-row">
                    <div class="metric warning">
                        <span class="metric-label">Current Incoming Temp</span>
                        <span class="metric-value" id="currentTemp">&mdash;</span>
                    </div>
                    <div class="metric success">
                        <span class="metric-label">Live Running Average</span>
                        <span class="metric-value" id="runningAvg">&mdash;</span>
                    </div>
                </div>
                <div class="chart-box">
                    <canvas id="liveChart" height="100"></canvas>
                </div>
            </section>

        </div>

        <%-- MEMBER 2 (Data Analyst) — ANALYSIS TAB  [END] --%>

        <%-- ════════════════════════════════════════════════════════════════
             MEMBER 3 (Reporting Specialist) — EXPORT TAB  [START]
             Owner: Member 3.  Backend: ExportServlet.java + ExportDAO.java
               - ?action=preview -> assembled report JSON (cards + HTML preview)
               - ?format=csv|json -> streamed download (Content-Disposition) + export_log insert
               - ?action=log -> recent export_log rows (live audit viewer)
             JS for this section: loadPreview(), loadLog(), downloadFile()
        ════════════════════════════════════════════════════════════════ --%>
        <div id="tab-export" class="tab-panel" style="<%= tab.equals("export") ? "" : "display:none;" %>">

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

        </div>
        <%-- MEMBER 3 (Reporting Specialist) — EXPORT TAB  [END] --%>

    </main>
</div>

<iframe id="dlframe" style="display:none;" title="download"></iframe>

<script>
    const INITIAL_TAB = "<%= tab %>";
    let exportLoaded = false;

    // ---- TAB SWITCHING (no reload -> the SSE stream keeps running) ----
    function showTab(name) {
        document.getElementById('tab-analysis').style.display = (name === 'analysis') ? 'block' : 'none';
        document.getElementById('tab-export').style.display   = (name === 'export')   ? 'block' : 'none';
        document.getElementById('tabbtn-analysis').classList.toggle('active', name === 'analysis');
        document.getElementById('tabbtn-export').classList.toggle('active', name === 'export');
        // keep the sidebar highlight in sync
        const na = document.getElementById('nav-analysis'), ne = document.getElementById('nav-export');
        if (na) na.classList.toggle('active', name === 'analysis');
        if (ne) ne.classList.toggle('active', name === 'export');

        if (name === 'export') {
            loadPreview();           // refresh export data each time it's shown
            loadLog();
        }
        // chart can render at 0px if it was hidden; resize when re-shown
        if (name === 'analysis' && myChart) { myChart.resize(); myChart.update(); }
    }

    // ════ MEMBER 2 (Data Analyst) — analysis JS [START] ════
    //     runBatch() / initChart() / startStream() / stopStream()
    let eventSource;
    let myChart;

    function runBatch() {
        document.getElementById("batchAvg").innerText = "...";
        document.getElementById("batchMax").innerText = "...";
        fetch("TemperatureServlet?mode=batch")
            .then(response => { if (!response.ok) throw new Error("Server Error"); return response.json(); })
            .then(data => {
                document.getElementById("batchAvg").innerText = data.average.toFixed(2) + " °C";
                document.getElementById("batchMax").innerText = data.max.toFixed(2) + " °C";
            })
            .catch(() => alert("Error fetching batch data. Is the server running and data uploaded?"));
    }

    function initChart() {
        const ctx = document.getElementById('liveChart').getContext('2d');
        if (myChart) myChart.destroy();
        myChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: [],
                datasets: [
                    { label: 'Incoming Temp (°C)', borderColor: '#ea580c',
                      backgroundColor: 'rgba(234,88,12,0.1)', data: [], borderWidth: 2, fill: true, tension: 0.3 },
                    { label: 'Running Average (°C)', borderColor: '#16a34a',
                      borderDash: [5, 5], data: [], borderWidth: 2, fill: false, tension: 0.3 }
                ]
            },
            options: { responsive: true, animation: false }
        });
    }

    function startStream() {
        if (eventSource) eventSource.close();
        document.getElementById("currentTemp").innerText = "Connecting...";
        document.getElementById("runningAvg").innerText = "Connecting...";
        initChart();
        let timeTracker = 0;
        eventSource = new EventSource("TemperatureServlet?mode=stream");
        eventSource.onmessage = function(event) {
            const data = JSON.parse(event.data);
            document.getElementById("currentTemp").innerText = data.current_temp.toFixed(2) + " °C";
            document.getElementById("runningAvg").innerText = data.running_avg.toFixed(2) + " °C";
            timeTracker += 0.5;
            myChart.data.labels.push(timeTracker + "s");
            myChart.data.datasets[0].data.push(data.current_temp);
            myChart.data.datasets[1].data.push(data.running_avg);
            if (myChart.data.labels.length > 20) {
                myChart.data.labels.shift();
                myChart.data.datasets[0].data.shift();
                myChart.data.datasets[1].data.shift();
            }
            myChart.update();
        };
        eventSource.onerror = function() { console.log("Stream ended."); eventSource.close(); };
    }

    function stopStream() {
        if (eventSource) { eventSource.close(); document.getElementById("currentTemp").innerText = "Stopped"; }
    }

    // ════ MEMBER 2 — analysis JS [END] ════

    // ════ MEMBER 3 (Reporting Specialist) — export JS [START] ════
    //     loadPreview() / loadLog() / downloadFile()
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
            .catch(() => { document.getElementById("previewBox").innerText = "Could not load preview. Upload the dataset first."; });
    }

    function loadLog() {
        fetch("export?action=log")
            .then(r => r.json())
            .then(rows => {
                const body = document.getElementById("logBody");
                if (!rows.length) { body.innerHTML = "<tr><td colspan='4' class='empty'>No exports yet.</td></tr>"; return; }
                body.innerHTML = rows.map(r =>
                    "<tr><td>" + r.id + "</td><td>" + r.reportType + "</td><td>" + r.format + "</td><td>" + r.exportedAt + "</td></tr>"
                ).join("");
            });
    }

    function downloadFile(fmt) {
        document.getElementById("dlframe").src = "export?format=" + fmt;
        setTimeout(loadLog, 1200);
    }
    // ════ MEMBER 3 — export JS [END] ════

    // ---- init (Member 1 integration): sidebar Analysis/Export links switch tabs
    //      (no reload) while on this page; show the initial tab + its data.
    ['analysis', 'export'].forEach(function (t) {
        const link = document.getElementById('nav-' + t);
        if (link) link.addEventListener('click', function (e) { e.preventDefault(); showTab(t); });
    });
    if (INITIAL_TAB === 'export') { loadPreview(); loadLog(); }
</script>
</body>
</html>
