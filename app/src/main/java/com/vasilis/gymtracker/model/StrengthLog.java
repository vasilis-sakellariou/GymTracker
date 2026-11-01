package com.vasilis.gymtracker.model;

import com.google.firebase.firestore.DocumentId;
import com.vasilis.gymtracker.util.Formatters;

/**
 * One weight entry: "on day X I lifted Y kg in exercise Z".
 * The progress chart and the goal checks are built from these entries.
 *
 * They are stored separately from the exercises, so deleting an exercise
 * from the program does not delete its history.
 */
public class StrengthLog {

    @DocumentId
    private String id;
    private String exerciseId;
    private String exerciseName;   // as the user typed it, e.g. "Bench press"
    private String nameKey;        // normalised, e.g. "bench press", used for comparisons
    private long date;             // day key (DateUtils.toDayKey)
    private double weightKg;

    public StrengthLog() {
    }

    public StrengthLog(String exerciseId, String exerciseName, long date, double weightKg) {
        this.exerciseId = exerciseId;
        this.exerciseName = exerciseName.trim();
        this.nameKey = Formatters.normalizeKey(exerciseName);
        this.date = date;
        this.weightKg = weightKg;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(String exerciseId) {
        this.exerciseId = exerciseId;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public String getNameKey() {
        return nameKey;
    }

    public void setNameKey(String nameKey) {
        this.nameKey = nameKey;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(double weightKg) {
        this.weightKg = weightKg;
    }
}
