package com.vasilis.gymtracker.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

/**
 * A goal with a deadline. Two kinds:
 *   - Strength: "Bench press 70 kg within 2 weeks"
 *   - Cardio:   "Running 5 km in 25 minutes within 4 weeks"
 *               (the time is optional: "Running 10 km")
 */
public class Goal {

    public static final String KIND_STRENGTH = "STRENGTH";
    public static final String KIND_CARDIO = "CARDIO";

    @DocumentId
    private String id;
    private String kind;              // KIND_STRENGTH or KIND_CARDIO
    private String target;            // exercise name, or cardio type key (e.g. "running")
    private double targetWeightKg;    // strength only
    private double targetDistanceKm;  // cardio only
    private double targetMinutes;     // cardio only, 0 = no time target
    private long createdAt;
    private long deadline;            // day key of the last day

    public Goal() {
    }

    @Exclude
    public boolean isStrengthGoal() {
        return KIND_STRENGTH.equals(kind);
    }

    @Exclude
    public boolean hasTimeTarget() {
        return targetMinutes > 0;
    }

    /** The required pace (min/km), e.g. 25 minutes / 5 km = 5:00 /km. */
    @Exclude
    public double getTargetPace() {
        return targetDistanceKm > 0 ? targetMinutes / targetDistanceKm : 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public double getTargetWeightKg() {
        return targetWeightKg;
    }

    public void setTargetWeightKg(double targetWeightKg) {
        this.targetWeightKg = targetWeightKg;
    }

    public double getTargetDistanceKm() {
        return targetDistanceKm;
    }

    public void setTargetDistanceKm(double targetDistanceKm) {
        this.targetDistanceKm = targetDistanceKm;
    }

    public double getTargetMinutes() {
        return targetMinutes;
    }

    public void setTargetMinutes(double targetMinutes) {
        this.targetMinutes = targetMinutes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }
}
