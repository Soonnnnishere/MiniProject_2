<%-- ===================================================================
     dashboard.jsp — Analysis Control + Results Display panel (Member 2),
     restyled into the unified sidebar shell with the shared master theme.
     Backend: TemperatureServlet (?mode=batch | ?mode=stream).
     All analysis logic (Chart.js, batch fetch, SSE running average) preserved.
=================================================================== --%>
<%@ page contentType="text/html;charset=UTF-8" %>
<% String activePage = "analysis"; %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Analysis &mdash; Weather Analytics</title>
    <link rel="stylesheet" href="css/style.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body>
<div class="app">
    <%@ include file="sidebar.jsp" %>
    <main class="main">

        <header class="page-header">
            <h1>Temperature Analysis</h1>
            <p class="subtitle">Batch summary statistics and a real-time incremental running average.</p>
        </header>

        <%-- BATCH: one button, one HTTP request, full result rendered at once. --%>
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

        <%-- REAL-TIME: SSE stream; numbers + chart update incrementally. --%>
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

    </main>
</div>

<script>
    let eventSource;
    let myChart;

    // BATCH: single request; render the complete result on receipt.
    function runBatch() {
        document.getElementById("batchAvg").innerText = "...";
        document.getElementById("batchMax").innerText = "...";
        fetch("TemperatureServlet?mode=batch")
            .then(response => {
                if (!response.ok) throw new Error("Server Error");
                return response.json();
            })
            .then(data => {
                document.getElementById("batchAvg").innerText = data.average.toFixed(2) + " °C";
                document.getElementById("batchMax").innerText = data.max.toFixed(2) + " °C";
            })
            .catch(error => alert("Error fetching batch data. Is the server running and data uploaded?"));
    }

    // Build an empty live line chart (incoming temp + running average).
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

    // REAL-TIME: open an SSE connection; update numbers + chart per record.
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
            // Sliding window: keep the last 20 points on screen.
            if (myChart.data.labels.length > 20) {
                myChart.data.labels.shift();
                myChart.data.datasets[0].data.shift();
                myChart.data.datasets[1].data.shift();
            }
            myChart.update();
        };

        // Fires when the server closes the stream (after the LIMIT) or on error.
        eventSource.onerror = function() {
            console.log("Stream ended.");
            eventSource.close();
        };
    }

    function stopStream() {
        if (eventSource) {
            eventSource.close();
            document.getElementById("currentTemp").innerText = "Stopped";
        }
    }
</script>
</body>
</html>
