# Mini Project 2 — Member 1: Dataset Management Module — Design

**Date:** 2026-06-07
**Author:** Member 1
**Module:** Dataset Management (CRUD) & Preprocessing — the DB foundation for the Weather App.

## Context

- **Project:** Web-Based Weather Application (Temperature focus), Vertical Slice architecture.
- **Dataset:** `weatherHistory.csv` — 96,453 rows, 12 columns.
- **Stack:** Eclipse Dynamic Web Project · Java 23 · Tomcat 10.0.2 (`jakarta.servlet.*`) · MySQL 9.3 via JDBC (Connector/J).
- **My scope (Member 1):** CSV upload, JDBC batch insert, mandatory soft-delete, browse/search/paginate UI with edit + delete buttons. Also owns the shared `weather_data` schema that Members 2 & 3 read from.

## Decisions

- **Insert strategy:** Batched `PreparedStatement` (`addBatch`/`executeBatch`, chunks of 1000) with `rewriteBatchedStatements=true`. Fast (~seconds) and showcases the JDBC `PreparedStatement` skill the rubric requires.
- **Upload mechanism:** Web upload — multipart POST from a browser file picker to `UploadServlet`.
- **Soft-delete:** `UPDATE ... SET is_active = 0` — never SQL `DELETE`.
- **Date handling:** stored as `VARCHAR` (analysis is temperature-focused, not time-series).
- **`"null"` precip values:** parsed to SQL `NULL`.

## Database Schema (`weatherdb.weather_data`)

| Column | Type | Source |
|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT PK` | generated |
| `formatted_date` | `VARCHAR(40)` | Formatted Date |
| `summary` | `VARCHAR(100)` | Summary |
| `precip_type` | `VARCHAR(20)` NULL | Precip Type |
| `temperature_c` | `DOUBLE` | Temperature (C) |
| `apparent_temperature_c` | `DOUBLE` | Apparent Temperature (C) |
| `humidity` | `DOUBLE` | Humidity |
| `wind_speed_kmh` | `DOUBLE` | Wind Speed (km/h) |
| `wind_bearing_deg` | `INT` | Wind Bearing (degrees) |
| `visibility_km` | `DOUBLE` | Visibility (km) |
| `loud_cover` | `DOUBLE` | Loud Cover (always 0) |
| `pressure_mb` | `DOUBLE` | Pressure (millibars) |
| `daily_summary` | `VARCHAR(255)` | Daily Summary |
| `is_active` | `TINYINT DEFAULT 1` | **mandatory soft-delete flag** |
| `created_at` | `TIMESTAMP DEFAULT CURRENT_TIMESTAMP` | audit |
| `updated_at` | `TIMESTAMP` ON UPDATE | audit |

Index on `is_active` (all reads filter `WHERE is_active = 1`).

## Components

```
MiniProject2/
├── src/com/weatherapp/
│   ├── db/DBConnection.java       # JDBC connection helper
│   ├── model/WeatherRecord.java   # POJO
│   ├── dao/WeatherDAO.java        # batch insert, search+paginate, update, soft-delete
│   └── servlet/
│       ├── UploadServlet.java     # multipart CSV → batch insert
│       ├── BrowseServlet.java     # search + pagination (WHERE is_active=1)
│       ├── EditServlet.java       # update a record
│       └── DeleteServlet.java     # soft-delete (UPDATE is_active=0)
├── WebContent/
│   ├── index.jsp                  # upload + browse table panel
│   ├── edit.jsp                   # edit a record
│   ├── css/style.css
│   └── WEB-INF/
│       ├── web.xml                # <error-page> 400/404/500 mappings
│       └── lib/mysql-connector-j.jar
└── sql/schema.sql
```

## Data Flow

1. **Upload:** `index.jsp` file picker → multipart POST `UploadServlet` → parse CSV, batch every 1000, commit → redirect with row-count message.
2. **Browse:** `index.jsp` GET → `BrowseServlet` (`?search=&page=`) → paginated table (`LIMIT/OFFSET`, `WHERE is_active=1`) with Edit / Delete buttons.
3. **Edit:** Edit button → `edit.jsp` → POST `EditServlet` → `UPDATE`.
4. **Delete (soft):** Delete button → `DeleteServlet` → `UPDATE is_active=0` → redirect.

## Error Handling

- Servlets catch `SQLException` / parse errors → set HTTP 400 (bad input) or 500 (server) → forward to friendly error JSP.
- `web.xml` maps 404/500 to custom error pages (shared-group rule: never show a raw stack trace).
- Frontend shows user-friendly messages on failure.

## Out of Scope (other members)

- Temperature analysis SQL / SSE dashboard (Member 2).
- Export to CSV/JSON, `ExportLog` table (Member 3).
- Deployment over real IP (shared) — code is unaffected.
