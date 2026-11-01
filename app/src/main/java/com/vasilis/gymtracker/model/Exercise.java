package com.vasilis.gymtracker.model;

import com.google.firebase.firestore.DocumentId;

/**
 * A strength exercise in the weekly program (e.g. "Bench press" on Monday).
 *
 * Firestore converts this object to a document and back automatically.
 * For that it needs:
 *   1) an empty public constructor
 *   2) public getters and setters for every field
 */
public class Exercise {

    // @DocumentId: Firestore fills in the document id here when reading.
    // When writing, this field is ignored (the id is the document's "name").
    @DocumentId
    private String id;
    private int dayIndex;        // 0 = Monday ... 6 = Sunday
    private String name;
    private int sets;
    private int reps;
    private double weightKg;
    private String note;         // optional
    private long createdAt;      // keeps exercises in the order they were added

    // Empty constructor required by Firestore.
    public Exercise() {
    }

    public Exercise(int dayIndex, String name, int sets, int reps, double weightKg, String note) {
        this.dayIndex = dayIndex;
        this.name = name;
        this.sets = sets;
        this.reps = reps;
        this.weightKg = weightKg;
        this.note = note;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getDayIndex() {
        return dayIndex;
    }

    public void setDayIndex(int dayIndex) {
        this.dayIndex = dayIndex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSets() {
        return sets;
    }

    public void setSets(int sets) {
        this.sets = sets;
    }

    public int getReps() {
        return reps;
    }

    public void setReps(int reps) {
        this.reps = reps;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(double weightKg) {
        this.weightKg = weightKg;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
