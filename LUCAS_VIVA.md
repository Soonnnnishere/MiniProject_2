# Member 3 — Reporting Specialist — Viva / Technical Q&A Notes

**Module:** Report generation & export (CSV/JSON) + audit log.
**My files:** `ExportServlet.java`, `ExportDAO.java`, `TemperatureReport.java`,
the **`export_log`** table, + the **Export** tab markup & JS in `analytics.jsp`
(fenced with `MEMBER 3 — EXPORT TAB [START]…[END]`).

## 1. What my module does
Assembles the temperature analysis results into a report, lets the user **download it as
CSV or JSON**, shows an HTML preview first, and **logs every download** to the database.

## 2. Data assembly
**Q: Where do the report numbers come from?**
`WeatherDAO.getTemperatureReport()` gathers results from **both** analysis runs:
- **Batch** → Average, Maximum, Count (one aggregate query).
- **Real-Time** → running-average **milestones** (running avg after 25/50/75/100 of the
  first-100 sample — the same sample the live SSE stream uses).
So the report reflects the spec's "results from **both batch and real-time runs**."

## 3. Dual-format generation
**Q: Two formats?** `ExportServlet` turns one `TemperatureReport` object into either CSV
(sectioned text) or JSON (built by hand), chosen by `?format=csv|json`.

## 4. HTTP file streaming (the key part)
**Q: How do you force the browser to download a file?**
Set **`Content-Disposition: attachment; filename="temperature_report.csv"`** and write the
file bytes straight to `response.getWriter()` (the HTTP response output stream). Without
that header the browser would just *display* the text; with it, the browser *saves a file*.
I also set the right `Content-Type` (`text/csv` / `application/json`).

## 5. Export log (audit trail)
**Q: What is `export_log`?** A brand-new MySQL table I created: `id, report_type, format,
exported_at`. **Every** time a download happens, `ExportDAO.logExport()` does a JDBC
`INSERT` recording **what** was exported, the **format**, and a **timestamp**
(`DEFAULT CURRENT_TIMESTAMP`).
**Q: How do you prove it works live?** The page's **Export Log viewer** fetches
`?action=log` and **auto-refreshes after each download** — so a new audit row appears on
screen the moment you click Download. (The download fires via a hidden `<iframe>`, then JS
calls `loadLog()` to re-query.)

## 6. Frontend UI requirements (all present)
- **Export control buttons** — Download CSV / Download JSON.
- **In-browser analysis display** — summary cards (Average / Maximum / Records).
- **HTML report preview** — `?action=preview` returns the report as JSON (no download
  header); JS renders it as a table so the lecturer sees exactly what the file contains
  **before** downloading.
- **Live export log viewer** — the auto-refreshing audit table.
- **Frontend JS:** `loadPreview()`, `loadLog()`, `downloadFile()`.

## 7. Likely trap questions
- *"What makes it download vs display?"* The `Content-Disposition: attachment` header.
- *"Where is the file written?"* Directly to the HTTP response stream — not saved on the
  server first.
- *"Is the log written for downloads only?"* Yes — preview/log views don't log; only the
  actual CSV/JSON downloads insert an `export_log` row.
- *"Why does the milestone table stop at 100?"* It mirrors the real-time stream's 100-record
  limit; the batch "Records Analysed" covers the whole dataset.
