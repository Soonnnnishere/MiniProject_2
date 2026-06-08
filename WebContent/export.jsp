<%-- ===================================================================
     export.jsp — Export panel placeholder (Member 3 / Reporting Specialist).
     Kept in the unified shell so all four required spaces exist and feel
     like one app. Member 3 fills in the preview + CSV/JSON download here.
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
            <p class="subtitle">Preview and download the analysis results as CSV or JSON.</p>
        </header>

        <section class="panel">
            <h2>Export Control Panel</h2>
            <p class="placeholder">This panel will be implemented by Member 3 (Reporting Specialist).</p>
            <p class="hint">Planned: preview the temperature analysis results, then download as
                <code>.csv</code> or <code>.json</code>, with an <code>ExportLog</code> record of every download.</p>
            <div style="margin-top:14px;">
                <button class="btn" disabled>Download CSV</button>
                <button class="btn" disabled>Download JSON</button>
            </div>
        </section>

    </main>
</div>
</body>
</html>
