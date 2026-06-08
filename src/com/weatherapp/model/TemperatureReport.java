package com.weatherapp.model;

/**
 * TemperatureReport — the assembled analysis results for the Export module.
 *
 * It gathers BOTH halves of Member 2's analysis (the PDF: "assemble results from
 * both batch and real-time runs"):
 *   - BATCH run    -> average, maximum (Member 2's batch stats) + count as context.
 *   - REAL-TIME run -> running-average milestones: the running average after the
 *     first 25 / 50 / 75 / 100 records (the same first-100 sample the live SSE
 *     stream uses).
 *
 * This single object is what ExportServlet turns into CSV or JSON.
 */
public class TemperatureReport {

    // --- batch summary statistics ---
    private double average;
    private double maximum;
    private long count;   // context: how many active records were analysed

    // --- real-time running-average milestones (parallel arrays) ---
    // e.g. milestoneCounts = {25,50,75,100}, milestoneAverages = running avg at each.
    private int[] milestoneCounts = new int[0];
    private double[] milestoneAverages = new double[0];

    public double getAverage() { return average; }
    public void setAverage(double average) { this.average = average; }

    public double getMaximum() { return maximum; }
    public void setMaximum(double maximum) { this.maximum = maximum; }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }

    public int[] getMilestoneCounts() { return milestoneCounts; }
    public void setMilestoneCounts(int[] milestoneCounts) { this.milestoneCounts = milestoneCounts; }

    public double[] getMilestoneAverages() { return milestoneAverages; }
    public void setMilestoneAverages(double[] milestoneAverages) { this.milestoneAverages = milestoneAverages; }
}
