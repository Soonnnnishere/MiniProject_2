package com.weatherapp.model;

/**
 * WeatherRecord — a "Plain Old Java Object" (POJO) / JavaBean that represents
 * ONE row of the weather_data table. It is the "M" (Model) in our MVC design.
 *
 * WHY have this class? (viva)
 *   It lets us carry a database row around the app as a normal Java object
 *   instead of loose variables. The DAO converts a JDBC ResultSet row into a
 *   WeatherRecord; the servlet puts it on the request; the JSP reads it back.
 *
 * JavaBean conventions (why getters/setters and a no-arg constructor):
 *   - private fields + public getX()/setX() = encapsulation (callers cannot
 *     touch the fields directly; access goes through methods).
 *   - a public no-argument constructor + getters/setters is the standard
 *     "JavaBean" shape that JSP/EL and frameworks expect (e.g. JSP could call
 *     ${record.summary}, which maps to getSummary()).
 *
 * Each field maps to one column. Note isActive mirrors the soft-delete flag
 * (1 = visible, 0 = soft-deleted).
 */
public class WeatherRecord {

    // --- fields: one per weather_data column ---
    private long id;                    // primary key (auto-increment in DB)
    private String formattedDate;       // original date string from the CSV
    private String summary;             // e.g. "Partly Cloudy"
    private String precipType;          // "rain"/"snow"/null  (NULL allowed)
    private double temperatureC;        // the figure Member 2 analyses
    private double apparentTemperatureC;
    private double humidity;
    private double windSpeedKmh;
    private int windBearingDeg;
    private double visibilityKm;
    private double loudCover;           // always 0 in this dataset (kept for fidelity)
    private double pressureMb;
    private String dailySummary;
    private int isActive;               // soft-delete flag: 1 = active, 0 = deleted

    /** No-arg constructor — required by the JavaBean convention. */
    public WeatherRecord() {
    }

    // --- getters / setters (encapsulation) ---
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFormattedDate() { return formattedDate; }
    public void setFormattedDate(String formattedDate) { this.formattedDate = formattedDate; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getPrecipType() { return precipType; }
    public void setPrecipType(String precipType) { this.precipType = precipType; }

    public double getTemperatureC() { return temperatureC; }
    public void setTemperatureC(double temperatureC) { this.temperatureC = temperatureC; }

    public double getApparentTemperatureC() { return apparentTemperatureC; }
    public void setApparentTemperatureC(double apparentTemperatureC) { this.apparentTemperatureC = apparentTemperatureC; }

    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }

    public double getWindSpeedKmh() { return windSpeedKmh; }
    public void setWindSpeedKmh(double windSpeedKmh) { this.windSpeedKmh = windSpeedKmh; }

    public int getWindBearingDeg() { return windBearingDeg; }
    public void setWindBearingDeg(int windBearingDeg) { this.windBearingDeg = windBearingDeg; }

    public double getVisibilityKm() { return visibilityKm; }
    public void setVisibilityKm(double visibilityKm) { this.visibilityKm = visibilityKm; }

    public double getLoudCover() { return loudCover; }
    public void setLoudCover(double loudCover) { this.loudCover = loudCover; }

    public double getPressureMb() { return pressureMb; }
    public void setPressureMb(double pressureMb) { this.pressureMb = pressureMb; }

    public String getDailySummary() { return dailySummary; }
    public void setDailySummary(String dailySummary) { this.dailySummary = dailySummary; }

    public int getIsActive() { return isActive; }
    public void setIsActive(int isActive) { this.isActive = isActive; }
}
