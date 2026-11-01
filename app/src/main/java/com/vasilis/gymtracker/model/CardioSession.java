package com.vasilis.gymtracker.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

/**
 * One cardio workout, e.g. "Running, 5 km in 25:30, on 29/10".
 */
public class CardioSession {

    @DocumentId
    private String id;
    private String type;              // language-independent key, e.g. "running" (see CardioTypes)
    private double distanceKm;
    private double durationMinutes;   // 25.5 = 25 minutes 30 seconds
    private long date;                // day key (DateUtils.toDayKey)
    private String note;

    public CardioSession() {
    }

    /**
     * Pace in minutes per kilometre (e.g. 5.1 = 5:06 /km).
     * @Exclude: this is a calculated value, so it is not written to the database.
     */
    @Exclude
    public double getPaceMinPerKm() {
        return distanceKm > 0 ? durationMinutes / distanceKm : 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public double getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(double durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
