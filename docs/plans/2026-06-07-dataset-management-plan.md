# Dataset Management Module — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build Member 1's Dataset Management module — CSV upload, JDBC batch insert, soft-delete, and a browse/search/paginate UI — as an Eclipse Dynamic Web Project on Tomcat 10 + MySQL.

**Architecture:** Classic servlet MVC. JSP views → servlets (controllers) → `WeatherDAO` (data access) → MySQL via JDBC. Soft-delete via `is_active` flag; all reads filter `WHERE is_active = 1`.

**Tech Stack:** Java 23, Tomcat 10 (`jakarta.servlet.*`), MySQL 9.3, JDBC Connector/J, JSP/HTML/CSS.

**Environment notes:** Not a git repo (skip commit steps). No JUnit harness — verification is manual via MySQL CLI + browser. Project is imported into Eclipse and run via the Servers view.

---

## Task 0: Prerequisites (one-time setup)

**Step 1: Download MySQL Connector/J**
Get `mysql-connector-j-9.x.jar` and place it at `WebContent/WEB-INF/lib/`. (Claude will fetch this during execution.)

**Step 2: Create the database**
Run `sql/schema.sql` against MySQL:
```bash
mysql -u root -p < sql/schema.sql
```
Verify:
```sql
USE weatherdb; SHOW TABLES; DESCRIBE weather_data;
```
Expected: `weather_data` table with all columns incl. `is_active`.

**Step 3: Set DB credentials** in `DBConnection.java` (URL, user, password).

---

## Task 1: SQL Schema

**Files:** Create `sql/schema.sql`

Create `weatherdb` + `weather_data` table per the design doc (15 data columns + `is_active`, `created_at`, `updated_at`, index on `is_active`).

**Verify:** `DESCRIBE weather_data;` shows `is_active TINYINT DEFAULT 1`.

---

## Task 2: DBConnection helper

**Files:** Create `src/com/weatherapp/db/DBConnection.java`

Static `getConnection()` returning a JDBC `Connection` using URL with `rewriteBatchedStatements=true&useSSL=false&allowPublicKeyRetrieval=true`.

**Verify:** Build the project in Eclipse — no compile errors, driver class resolves.

---

## Task 3: WeatherRecord POJO

**Files:** Create `src/com/weatherapp/model/WeatherRecord.java`

Fields for all columns + `id`, `isActive`. Getters/setters.

**Verify:** Compiles.

---

## Task 4: WeatherDAO — batch insert

**Files:** Create `src/com/weatherapp/dao/WeatherDAO.java`

`int batchInsert(List<WeatherRecord>)` using `PreparedStatement.addBatch()`/`executeBatch()` in chunks of 1000, `setNull` for null precip type.

**Verify (after Task 6):** Upload CSV, then `SELECT COUNT(*) FROM weather_data;` → ~96,453.

---

## Task 5: CSV parser util

**Files:** Add `parseLine` logic (in DAO or a small `CsvUtil`).

Split on commas (dataset has no embedded commas in numeric fields; daily_summary is last and quote-free), map `"null"` → null, parse doubles/ints defensively (skip/clean malformed rows, count skipped).

**Verify:** Upload runs without throwing; skipped-row count logged.

---

## Task 6: UploadServlet

**Files:** Create `src/com/weatherapp/servlet/UploadServlet.java`

`@WebServlet("/upload")` + `@MultipartConfig`. Read uploaded `Part`, stream lines, build records, call `batchInsert`, redirect to `index.jsp?msg=Inserted+N+rows`. Catch errors → HTTP 500 + error page.

**Verify:** Pick `weatherHistory.csv` in browser, submit, see success count; `SELECT COUNT(*)` matches.

---

## Task 7: BrowseServlet (search + pagination)

**Files:** Create `src/com/weatherapp/servlet/BrowseServlet.java` + DAO method `search(String q, int page, int size)` and `count(String q)`.

`@WebServlet("/browse")`. Query params `search`, `page` (size=20). SQL: `WHERE is_active=1 AND (summary LIKE ? OR formatted_date LIKE ?) ORDER BY id LIMIT ? OFFSET ?`. Set records + paging info as request attributes, forward to `index.jsp`.

**Verify:** Browser shows 20 rows, page 2 shows next 20, search "Rain" filters.

---

## Task 8: index.jsp (upload + browse panel)

**Files:** Create `WebContent/index.jsp`, `WebContent/css/style.css`

Upload form (multipart), search box, results table with Edit/Delete buttons, pagination controls. Show `msg` param.

**Verify:** Page renders styled; all controls visible.

---

## Task 9: EditServlet + edit.jsp

**Files:** Create `src/com/weatherapp/servlet/EditServlet.java`, `WebContent/edit.jsp` + DAO `findById(id)` and `update(record)`.

GET → load record into `edit.jsp` form; POST → `UPDATE ... WHERE id=?` → redirect to browse.

**Verify:** Edit a record's temperature, confirm change in DB and table.

---

## Task 10: DeleteServlet (soft-delete)

**Files:** Create `src/com/weatherapp/servlet/DeleteServlet.java` + DAO `softDelete(id)` = `UPDATE weather_data SET is_active=0 WHERE id=?`.

**Verify:** Click Delete → row disappears from browse; `SELECT is_active FROM weather_data WHERE id=?` → 0 (row still exists).

---

## Task 11: web.xml + error pages

**Files:** Create `WebContent/WEB-INF/web.xml`, `WebContent/error/400.jsp`, `404.jsp`, `500.jsp`

Map `<error-page>` for 400/404/500 to friendly JSPs (shared-group rule).

**Verify:** Hit a bad URL → custom 404 page, not a stack trace.

---

## Task 12: End-to-end check

Deploy on Tomcat, run full flow: upload → browse → search → paginate → edit → soft-delete. Confirm no raw stack traces. Note: final demo must use real IP, not `localhost`.
