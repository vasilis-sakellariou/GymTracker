package com.vasilis.gymtracker.ui.goals;

import com.vasilis.gymtracker.model.CardioSession;
import com.vasilis.gymtracker.model.Goal;
import com.vasilis.gymtracker.model.StrengthLog;
import com.vasilis.gymtracker.util.DateUtils;
import com.vasilis.gymtracker.util.Formatters;

import java.util.List;

/**
 * How close a goal is, based on the user's logged data.
 *
 * Rules:
 *   - Strength: achieved if, between the day the goal was created and the deadline,
 *     there is an entry with weight >= the target.
 *   - Cardio: achieved if, in the same period, there is a workout of the same type
 *     with distance >= the target and (if there is a time target) a pace equal to or
 *     faster than the goal's pace. E.g. goal 5 km in 25:00 (5:00 /km):
 *     6 km in 29:00 (4:50 /km) counts as a success.
 */
public class GoalProgress {

    // The order here is also the display order: active goals first.
    public enum Status { ACTIVE, ACHIEVED, EXPIRED }

    public final Goal goal;
    public Status status;
    public double fraction;           // 0.0 to 1.0, for the progress bar
    public long daysLeft;
    public boolean hasData;           // is there at least one related entry?
    public double bestWeight;         // strength: heaviest weight lifted
    public double maxDistance;        // cardio: longest distance
    public CardioSession bestSession; // cardio: fastest workout that was long enough

    private GoalProgress(Goal goal) {
        this.goal = goal;
    }

    public static GoalProgress evaluate(Goal goal, List<StrengthLog> logs,
                                        List<CardioSession> sessions, long now) {
        GoalProgress p = new GoalProgress(goal);
        long startDay = DateUtils.toDayKey(goal.getCreatedAt());
        long endDay = DateUtils.toDayKey(goal.getDeadline());
        String key = Formatters.normalizeKey(goal.getTarget());
        boolean achieved = false;

        if (goal.isStrengthGoal()) {
            for (StrengthLog log : logs) {
                if (!key.equals(log.getNameKey())) {
                    continue; // a different exercise
                }
                p.hasData = true;
                p.bestWeight = Math.max(p.bestWeight, log.getWeightKg());
                long day = DateUtils.toDayKey(log.getDate());
                boolean inPeriod = day >= startDay && day <= endDay;
                if (inPeriod && log.getWeightKg() >= goal.getTargetWeightKg()) {
                    achieved = true;
                }
            }
            p.fraction = goal.getTargetWeightKg() > 0 ? p.bestWeight / goal.getTargetWeightKg() : 0;
        } else {
            for (CardioSession s : sessions) {
                if (!key.equals(Formatters.normalizeKey(s.getType()))) {
                    continue; // a different cardio type
                }
                p.hasData = true;
                p.maxDistance = Math.max(p.maxDistance, s.getDistanceKm());

                boolean longEnough = s.getDistanceKm() >= goal.getTargetDistanceKm();
                if (longEnough && (p.bestSession == null
                        || s.getPaceMinPerKm() < p.bestSession.getPaceMinPerKm())) {
                    p.bestSession = s;
                }
                // 1e-9: a tiny margin for rounding errors in decimals.
                boolean fastEnough = !goal.hasTimeTarget()
                        || s.getPaceMinPerKm() <= goal.getTargetPace() + 1e-9;
                long day = DateUtils.toDayKey(s.getDate());
                boolean inPeriod = day >= startDay && day <= endDay;
                if (inPeriod && longEnough && fastEnough) {
                    achieved = true;
                }
            }
            if (goal.hasTimeTarget() && p.bestSession != null) {
                // How close your best pace is to the goal's pace.
                p.fraction = goal.getTargetPace() / p.bestSession.getPaceMinPerKm();
            } else {
                // How close your longest distance is to the goal's distance.
                p.fraction = goal.getTargetDistanceKm() > 0
                        ? p.maxDistance / goal.getTargetDistanceKm() : 0;
            }
        }

        // Keep the fraction between 0 and 1.
        p.fraction = Math.max(0, Math.min(1, p.fraction));
        if (achieved) {
            p.fraction = 1;
        }

        long today = DateUtils.toDayKey(now);
        p.daysLeft = Math.round((endDay - today) / (double) DateUtils.DAY_MS);
        if (achieved) {
            p.status = Status.ACHIEVED;
        } else if (today > endDay) {
            p.status = Status.EXPIRED;
        } else {
            p.status = Status.ACTIVE;
        }
        return p;
    }
}
