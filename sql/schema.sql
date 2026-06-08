-- ============================================================================
-- schema.sql — creates the database + table for the Weather App.
-- Run ONCE to set up the database:   mysql -u root -p < schema.sql
--
-- DESIGN NOTES (viva):
--   * One denormalised table holds the whole weather dataset. The project is
--     temperature-focused but Dataset Management stores ALL columns so any
--     module (analysis, reporting) can use the data it needs.
--   * is_active is the MANDATORY soft-delete flag (1 = visible, 0 = deleted).
--   * created_at / updated_at give a basic audit trail.
-- ============================================================================

-- Create the database only if it does not already exist, then switch to it.
CREATE DATABASE IF NOT EXISTS weatherdb;
USE weatherdb;

-- Start clean each time the script is run (drops the old table first).
-- WARNING: this also wipes existing data — only run on a fresh setup.
DROP TABLE IF EXISTS weather_data;

CREATE TABLE weather_data (
  -- Surrogate primary key: auto-incremented unique id for every row.
  id                     BIGINT AUTO_INCREMENT PRIMARY KEY,

  -- The 12 columns from weatherHistory.csv:
  formatted_date         VARCHAR(40),        -- kept as text (has a timezone offset)
  summary                VARCHAR(100),
  precip_type            VARCHAR(20) NULL,    -- NULL allowed (CSV sometimes has none)
  temperature_c          DOUBLE,              -- the figure Member 2 analyses
  apparent_temperature_c DOUBLE,
  humidity               DOUBLE,
  wind_speed_kmh         DOUBLE,
  wind_bearing_deg       INT,
  visibility_km          DOUBLE,
  loud_cover             DOUBLE,              -- always 0 in this dataset
  pressure_mb            DOUBLE,
  daily_summary          VARCHAR(255),

  -- MANDATORY soft-delete flag. Defaults to 1 so new rows are active. The app
  -- never DELETEs; it sets this to 0 and every query filters WHERE is_active = 1.
  is_active              TINYINT NOT NULL DEFAULT 1,

  -- Audit timestamps (set automatically by MySQL).
  created_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  -- Index on is_active: every read filters by it, so indexing speeds them up.
  INDEX idx_is_active (is_active)
);

-- ----------------------------------------------------------------------------
-- export_log (Member 3 / Reporting) — audit trail of every report download.
-- IF NOT EXISTS + no DROP, so it survives re-running this script.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS export_log (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  report_type VARCHAR(100),                              -- what was exported
  format      VARCHAR(10),                               -- "CSV" or "JSON"
  exported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP        -- when (precise timestamp)
);
