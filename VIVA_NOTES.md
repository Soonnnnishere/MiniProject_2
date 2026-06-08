# Weather Analytics — Viva / Technical Q&A Notes

Quick-reference answers for likely lecturer questions. Pair this with the
in-code comments. Covers Member 1 (Dataset Management), the integrated Analysis
module, the Clear-Dataset feature, and the unified GUI.

## 1. Architecture

**Q: Explain your architecture.**
MVC with a DAO layer:
- **Model** = `WeatherRecord` (a JavaBean for one row) + the MySQL table.
- **View** = JSP pages (`index.jsp`, `edit.jsp`, `dashboard.jsp`, `export.jsp`,
  shared `sidebar.jsp`) — pure presentation, no SQL.
- **Controller** = Servlets (`Upload`, `Browse`, `Edit`, `Delete`, `Reset`,
  `Temperature`) — handle the HTTP request, call the DAO, choose the response.
- **DAO** = `WeatherDAO` — the ONLY class that contains SQL. Keeps DB code in one
  place (separation of concerns).

**Q: Request flow for browsing?**
Browser → `GET /browse` → `BrowseServlet` reads params → calls `dao.search()` →
puts results on the request (`setAttribute`) → forwards to `index.jsp` → JSP
renders the HTML table.

## 2. JDBC

**Q: What is JDBC?** Java Database Connectivity — the standard Java API to run SQL.
**Q: What is the driver / where is it?** `mysql-connector-j-9.2.0.jar` in
`WEB-INF/lib`. Loaded via `Class.forName("com.mysql.cj.jdbc.Driver")` (auto-registers
on JDBC 4.0+). `DriverManager.getConnection(url,user,pass)` opens a connection.
**Q: Connection URL options?** `rewriteBatchedStatements=true` (fast batching),
`useSSL=false`, `allowPublicKeyRetrieval=true`, `serverTimezone=UTC`.

**Q: Statement vs PreparedStatement? Why PreparedStatement?**
`PreparedStatement` uses `?` placeholders and binds values with `setString/setInt…`.
- **Prevents SQL injection** (values are sent separately, never concatenated).
- **Faster** for repeated execution (the SQL is pre-parsed) and enables batching.

**Q: How do you avoid leaking connections?**
`try-with-resources` — `Connection`, `PreparedStatement`, `ResultSet` are all
`AutoCloseable`, so they close automatically even on exception.

## 3. The 96k-row upload (batch insert)

**Q: How do you insert 96,000 rows fast?**
JDBC **batching**: for each row `ps.addBatch()`, then `ps.executeBatch()` every
1000 rows. With `rewriteBatchedStatements=true` the driver packs the batch into a
single multi-row INSERT — one network round-trip instead of thousands. ~3 seconds
total.
**Q: Why flush every 1000 instead of all at once?** Keeps memory bounded.
**Q: How is the file received?** `@MultipartConfig` + `request.getPart("csvFile")`,
read line by line with a `BufferedReader` (streamed, not loaded whole).
**Q: Malformed rows?** `parseLine` returns `null`; we count and skip them so one
bad row doesn't abort the whole import.
**Q: NULL handling?** "Precip Type" can be the text "null" → stored as SQL `NULL`
via `ps.setNull(i, Types.VARCHAR)`.

## 4. Soft delete (mandatory feature)

**Q: What is soft delete and how did you implement it?**
Instead of `DELETE FROM ...`, we run `UPDATE weather_data SET is_active = 0 WHERE id = ?`.
The row stays in the table but is flagged inactive. **Every read filters
`WHERE is_active = 1`**, so deleted rows disappear from the app but data is
recoverable and the analysis module can ignore them cleanly.
**Q: Why not real DELETE?** Data recovery, audit trail, and it's a project rule.

## 5. Browse / search / pagination

**Q: How does search work?** SQL `LIKE '%text%'` on summary, date, daily_summary.
Blank box → pattern `%` (matches all).
**Q: How does pagination work?** `LIMIT ? OFFSET ?`. `OFFSET = (page-1)*pageSize`,
`pageSize = 20`. Total pages = `ceil(totalCount / pageSize)` from a `COUNT(*)` query.
**Q: What is the "Current dataset: N records" remark?** A separate `COUNT(*) WHERE
is_active=1` (ignoring the search filter) that `BrowseServlet` puts on the request
as `datasetSize`, so the panel always shows the true total dataset size.

## 6. Clear Dataset (hard reset) — vs Soft Delete

**Q: What does the "Clear Dataset" button do?**
`ResetServlet` (`POST /reset`) calls `dao.clearAll()` → `TRUNCATE TABLE weather_data`.
This **empties the whole table and resets the auto-increment id to 1**. It is an
administrative "start fresh" action used before re-importing the CSV (so a
re-upload doesn't create duplicates). The UI asks for confirmation first.
**Q: Isn't the rule "never use DELETE"?** That rule is the per-record **soft-delete**
(hide one row with `is_active=0`). Clear-Dataset is a deliberately different,
table-level reset; `TRUNCATE` is the correct tool to truly empty the dataset.
Two distinct, intentional behaviours.

**Q: If I edit or soft-delete a record, then re-upload the same CSV, does my change
stay / does the data come back?**
Your change stays on its row (every CRUD op is keyed on the primary `id`), **but
re-uploading also re-inserts the file's original data as brand-new rows** — the upload
is a blind bulk `INSERT` with no link to existing rows. So:
- **Soft-deleted** row stays hidden (`is_active=0`); a fresh **active** duplicate is
  added → the data "reappears" as a *new* row (the delete was not undone — a new copy
  was created).
- **Edited** row keeps your edited values; a fresh duplicate with the **original** CSV
  values is added alongside it.
Either way you get **duplicates**, and the file's original values come back.
**Mental model:** your edits/deletes live in the *database*, but the *CSV file* still
holds the original data — re-uploading pours it back in as new rows; it does **not**
sync or merge. **Fix:** click **Clear Dataset** (`TRUNCATE`) before re-importing for a
clean single copy.
One-liner: *"CRUD ops are keyed on the primary key so they persist, but re-import is an
unconditional bulk insert of the file's original rows, so it neither respects nor undoes
prior edits/soft-deletes — it just adds duplicates; we Clear (TRUNCATE) first to
re-import cleanly."*

## 7. Analysis module (Batch & Real-Time)

**Q: How does Batch analysis work?** `GET /TemperatureServlet?mode=batch`. One SQL
query `SELECT AVG(temperature_c), MAX(temperature_c), COUNT(*) ... WHERE is_active=1`
computes everything **server-side** and returns it in a **single JSON response**;
the page renders the complete result at once. (Matches the rule: full calc on the
server, one HTTP response, no piece-by-piece loading.)
**Q: How does Real-Time work?** `GET ...?mode=stream` opens a **Server-Sent Events**
connection (`Content-Type: text/event-stream`). The servlet reads records one-by-one,
maintains an **incremental running average** (`sum/count`), and writes each as
`data: {json}\n\n`, flushing after each, with a 500 ms pace. The browser's
`EventSource` updates the numbers + Chart.js graph live, **no page refresh**.
**Q: SSE vs polling?** Both are allowed by the spec. SSE = one long-lived server→client
stream (what we used). Polling = the client re-requests every 500 ms. SSE is more
efficient (no repeated request overhead).
**Q: Why LIMIT the stream?** Streaming all 96k rows at 500 ms each ≈ 13 hours, so the
demo streams the first 100 records (the running average covers those 100).

## 8. Servlets & error handling

**Q: What is a servlet?** A Java class (`extends HttpServlet`) that handles HTTP
requests; `doGet` for reads, `doPost` for writes. Mapped with `@WebServlet("/path")`.
**Q: GET vs POST here?** Browse/search/analysis = GET (read). Upload/edit-save/delete/
clear = POST (write). After a POST we **redirect** to a GET (`/browse`) — the
Post-Redirect-Get pattern stops refresh from re-submitting.
**Q: Global error handling?** Servlets call `response.sendError(400/404/500,...)`.
`web.xml` `<error-page>` maps those codes to friendly JSPs in `/error/`, so the
user never sees a raw stack trace. 400 = bad input, 404 = not found, 500 = server/DB error.
**Q: XSS protection in JSP?** The `esc()` helper HTML-escapes every value before
printing it, so data can't inject `<script>`.

## 9. Unified GUI

**Q: How is the GUI "unified"?** A single **sidebar-navigation** shell with one master
CSS theme (`style.css`) shared by every panel: Dataset Management, Analysis Control +
Results Display, and Export. Clicking a sidebar link switches the panel; the look stays
consistent.
**Q: How does the shared sidebar work?** `sidebar.jsp` is pulled into each page with a
**static include** `<%@ include file="sidebar.jsp" %>`. Each page sets a `String
activePage` variable before the include; because a static include merges the source,
the sidebar reads that variable to highlight the current item.
**Q: Batch vs Real-Time display rules met?** Batch = button-triggered, single response,
rendered all at once. Real-Time = button-triggered SSE, incremental per-record updates,
no page flash/refresh. ✅

**Q: Does switching panels lose the live stream / analysis state?**
We use a **hybrid** layout:
- **Analysis and Export share ONE page (`analytics.jsp`) as in-page JS tabs.** Switching
  between them is a JavaScript show/hide — **no page reload** — so the SSE stream, the
  running-average numbers, and the Chart.js graph **keep running** across the switch.
  (Implementation: `showTab()` toggles `display`; it never closes the `EventSource`.)
- **Dataset Management is a separate server-rendered page.** Navigating to it *is* a full
  reload (which is fine — its records simply re-query from the DB, so nothing is lost).
**Why hybrid?** A full single-page app would also have to rewrite the Dataset CRUD
(upload/browse/search/paginate/edit/delete) as AJAX — high effort/risk. The hybrid keeps
the verified server-side Dataset module untouched while solving the real pain point
(the stream dropping when moving from Analysis to Export).
One-liner: *"Analysis and Export are JS tabs on one page so the SSE stream survives the
switch; Dataset stays a separate JSP page since its state lives in the DB and re-queries.
We deliberately didn't go full single-page to avoid rewriting the working Dataset CRUD as
AJAX."*

## 10. Environment trivia

- **Tomcat 10 → `jakarta.servlet.*`** (Jakarta EE 9). Tomcat 9 used `javax.servlet.*`.
  Same code, different package namespace.
- **`@WebServlet` vs web.xml:** the annotation replaces `<servlet-mapping>`; we only
  use web.xml for welcome-file and error pages.
- **Schema:** one `weather_data` table, surrogate `id` PK, `is_active` flag (indexed),
  `created_at`/`updated_at` audit timestamps.

## 11. Honest "limitations" answers (good to volunteer)

- The dataset has no natural unique key, so re-uploading without first clicking
  **Clear Dataset** would insert duplicates (we added Clear-Dataset to handle this).
- One fresh connection per call; a production app would use a connection pool.
- `formatted_date` stored as text (the analysis is temperature-focused, not time-series).
- The real-time stream is capped (LIMIT 100) for demo speed, so its running average
  covers the streamed subset, not all 96k (that's the batch view).

## 12. Deployment over a real network (no-localhost rule)

**Q: How do you deploy/demo over a real IP (no `localhost`)?**
Tomcat runs on the demo machine and listens on port 8080 on ALL network interfaces — its
`<Connector port="8080">` has no bind address, so it binds `0.0.0.0` (just like a
`new ServerSocket(8080)` with no address specified). So the app is automatically reachable
at the machine's LAN IP, e.g. **`http://192.168.1.12:8080/MiniProject2/`**. Find the IP
with `ipconfig getifaddr en0` and use that URL instead of `localhost`. The server machine
and the client device must be on the **same Wi-Fi / subnet**.

**Q: Do you change `DBConnection.java` for the real-IP demo?**
**NO.** It stays `jdbc:mysql://localhost:3306/weatherdb`, because the Servlet and MySQL run
on the **same machine** — the servlet connects to MySQL locally. The "no localhost" rule is
about the **browser URL** (how a client reaches Tomcat over the network), NOT the internal
DB connection. (You'd only change it if MySQL were on a different host.)

**Q: Does the frontend need changing?**
No. The JS uses **relative URLs** (`fetch("TemperatureServlet?...")`, `new EventSource("...")`),
which resolve against whatever host loaded the page — so they work over any IP automatically.

**Q: How does this relate to Mini Project 1?**
Same TCP/IP networking: a client connects to the server's LAN IP on a port, both on the
same subnet. The only difference is the server is **Tomcat (HTTP, port 8080)** and the
client is a **browser**, instead of a custom `ServerSocket` and socket client. We configure
no IP in code — Tomcat already listens on every interface; we just read the Mac's IP.

**Deployment checklist:** (1) Tomcat running; (2) it auto-listens on `*:8080`; (3) read the
Mac's IP (don't set it); (4) client on the same Wi-Fi/subnet; (5) firewall allows 8080.
**Gotcha:** campus/public Wi-Fi often blocks device-to-device traffic (AP isolation) — use a
phone hotspot/own router if needed; and the IP can change (DHCP), so re-check on demo day.
