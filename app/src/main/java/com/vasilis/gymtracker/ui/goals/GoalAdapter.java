package com.vasilis.gymtracker.ui.goals;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.vasilis.gymtracker.R;
import com.vasilis.gymtracker.model.Goal;
import com.vasilis.gymtracker.util.CardioTypes;
import com.vasilis.gymtracker.util.DateUtils;
import com.vasilis.gymtracker.util.Formatters;

import java.util.ArrayList;
import java.util.List;

/**
 * Goal cards with a progress bar.
 */
public class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.GoalViewHolder> {

    public interface OnGoalActionListener {
        void onDeleteGoal(Goal goal);
    }

    private final List<GoalProgress> items = new ArrayList<>();
    private final OnGoalActionListener listener;

    public GoalAdapter(OnGoalActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<GoalProgress> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GoalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_goal, parent, false);
        return new GoalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GoalViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /** e.g. "Bench press: 70 kg" or "Running: 5 km in 25:00". */
    static String titleFor(Context ctx, Goal goal) {
        if (goal.isStrengthGoal()) {
            return ctx.getString(R.string.goal_title_strength,
                    goal.getTarget(), Formatters.number(goal.getTargetWeightKg()));
        }
        String type = CardioTypes.label(ctx, goal.getTarget());
        if (goal.hasTimeTarget()) {
            return ctx.getString(R.string.goal_title_cardio_time, type,
                    Formatters.number(goal.getTargetDistanceKm()),
                    Formatters.duration(goal.getTargetMinutes()));
        }
        return ctx.getString(R.string.goal_title_cardio_distance, type,
                Formatters.number(goal.getTargetDistanceKm()));
    }

    static class GoalViewHolder extends RecyclerView.ViewHolder {

        private final TextView textTitle;
        private final TextView textDeadline;
        private final LinearProgressIndicator progressBar;
        private final TextView textCurrent;
        private final TextView textStatus;
        private final MaterialButton buttonDelete;

        GoalViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_goal_title);
            textDeadline = itemView.findViewById(R.id.text_goal_deadline);
            progressBar = itemView.findViewById(R.id.progress_goal);
            textCurrent = itemView.findViewById(R.id.text_goal_current);
            textStatus = itemView.findViewById(R.id.text_goal_status);
            buttonDelete = itemView.findViewById(R.id.button_delete);
        }

        void bind(GoalProgress p, OnGoalActionListener listener) {
            Context ctx = itemView.getContext();
            Goal goal = p.goal;

            textTitle.setText(titleFor(ctx, goal));
            textDeadline.setText(ctx.getString(R.string.goal_deadline, DateUtils.formatDate(goal.getDeadline())));
            textCurrent.setText(currentFor(ctx, p));

            // The progress bar takes values 0–100.
            progressBar.setProgressCompat((int) Math.round(p.fraction * 100), false);

            int colorRes;
            switch (p.status) {
                case ACHIEVED:
                    textStatus.setText(R.string.goal_status_achieved);
                    colorRes = R.color.status_success;
                    break;
                case EXPIRED:
                    textStatus.setText(R.string.goal_status_expired);
                    colorRes = R.color.status_error;
                    break;
                default:
                    if (p.daysLeft <= 0) {
                        textStatus.setText(R.string.goal_status_today);
                    } else if (p.daysLeft == 1) {
                        textStatus.setText(R.string.goal_status_one_day);
                    } else {
                        textStatus.setText(ctx.getString(R.string.goal_status_days_left, p.daysLeft));
                    }
                    colorRes = R.color.status_active;
                    break;
            }
            int color = ContextCompat.getColor(ctx, colorRes);
            textStatus.setTextColor(color);
            progressBar.setIndicatorColor(color);

            buttonDelete.setOnClickListener(v -> listener.onDeleteGoal(goal));
        }

        /** Your current performance compared to the goal. */
        private static String currentFor(Context ctx, GoalProgress p) {
            if (!p.hasData) {
                return ctx.getString(R.string.goal_no_data);
            }
            if (p.goal.isStrengthGoal()) {
                return ctx.getString(R.string.goal_current_strength, Formatters.number(p.bestWeight));
            }
            if (p.goal.hasTimeTarget() && p.bestSession != null) {
                return ctx.getString(R.string.goal_current_cardio_time,
                        Formatters.number(p.bestSession.getDistanceKm()),
                        Formatters.duration(p.bestSession.getDurationMinutes()),
                        Formatters.pace(p.bestSession.getPaceMinPerKm()));
            }
            return ctx.getString(R.string.goal_current_cardio_distance, Formatters.number(p.maxDistance));
        }
    }
}
