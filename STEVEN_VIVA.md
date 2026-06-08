# Member 2 — Data Analyst — Viva / Technical Q&A Notes

**Module:** Temperature analysis — Batch + Real-Time ("Temperature Tracker").
**My files:** `TemperatureServlet.java` (backend) + the **Analysis** tab markup & JS
in `analytics.jsp` (fenced with `MEMBER 2 — ANALYSIS TAB [START]…[END]`).

## 1. What my module does
Two analysis modes over the temperature data, both filtering `WHERE is_active = 1`
(so soft-deleted rows are ignored):
- **Batch** — summary statistics (final Average + Maximum) in one shot.
- **Real-Time** — streams records one-by-one and shows an **incremental running average**.

## 2. Batch mode
**Q: How does Batch work?**
`GET /TemperatureServlet?mode=batch`. A single SQL aggregate query
`SELECT AVG(temperature_c), MAX(temperature_c), COUNT(*) FROM weather_data WHERE is_active=1`
does the whole calculation **on the server** and returns it in a **single JSON
response**. The page renders the complete result at once.
**Q: Why is that the rule?** The spec requires the full calculation server-side, returned
in one HTTP response — the UI must NOT load it piece-by-piece.

## 3. Real-Time mode (SSE)
**Q: How does Real-Time work?**
`?mode=stream` opens a **Server-Sent Events (SSE)** connection
(`Content-Type: text/event-stream`). The servlet reads records in order, maintains a
**running average = sum/count**, and writes each as `data: {json}\n\n`, calling
`flush()` after each so the browser receives it live, paced with `Thread.sleep(500)`.
The browser's `EventSource` updates the numbers + Chart.js graph **with no page refresh**.
**Q: SSE vs polling?** Both are allowed. SSE = one long-lived server→client stream (what I
used). Polling = the client re-requests every 500 ms. SSE avoids repeated-request overhead.
**Q: Why limit the stream to 100 records?** Streaming all ~96k at 500 ms each ≈ 13 hours,
so the demo streams the first 100; the running average covers that sample.
**Q: What is an "incremental running average"?** After each new reading I recompute
`sum/count` — so the average updates one record at a time, which is exactly what the live
dashboard shows changing on screen.

## 4. Frontend (JavaScript)
- `runBatch()` — `fetch("...mode=batch")` → renders the Average + Maximum cards.
- `startStream()` — `new EventSource("...mode=stream")`; `onmessage` updates the cards and
  pushes points into a **Chart.js** line chart (sliding 20-point window).
- `stopStream()` — closes the `EventSource`.

## 5. Integration facts (good to know)
- Column is **`temperature_c`** — the real column in the shared `weather_data` schema.
- DB access uses the shared **`DBConnection`** helper (one source of truth).
- The Analysis panel shares one page with Export (`analytics.jsp`) as JS tabs so the live
  stream survives switching tabs — but my analysis code is clearly fenced as Member 2.

## 6. Likely trap questions
- *"Is batch streamed?"* No — batch is a single response; only real-time streams.
- *"Does the page refresh during the live run?"* No — SSE updates the DOM in place.
- *"Why doesn't the real-time average equal the batch average?"* Real-time covers the
  first 100 records (the stream limit); batch covers the whole active dataset.
