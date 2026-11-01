package com.vasilis.gymtracker.ui.goals;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.ListenerRegistration;
import com.vasilis.gymtracker.R;
import com.vasilis.gymtracker.data.FirestoreRepository;
import com.vasilis.gymtracker.model.CardioSession;
import com.vasilis.gymtracker.model.Goal;
import com.vasilis.gymtracker.model.StrengthLog;
import com.vasilis.gymtracker.util.CardioTypes;
import com.vasilis.gymtracker.util.DateUtils;
import com.vasilis.gymtracker.util.Formatters;
import com.vasilis.gymtracker.util.UiUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * "Goals" section: list of goals with their progress and a "New goal" button.
 *
 * It listens to THREE collections (goals, weight logs, cardio). Whenever any of them
 * changes, progress is recalculated. So if you log a new run in Cardio,
 * the related goal updates automatically.
 */
public class GoalsFragment extends Fragment implements GoalAdapter.OnGoalActionListener {

    private static final int DEFAULT_WEEKS = 2;
    private static final int MAX_WEEKS = 104; // 2 years

    private FirestoreRepository repository;
    private GoalAdapter adapter;
    private TextView emptyView;

    private final List<ListenerRegistration> listeners = new ArrayList<>();
    private List<Goal> goals = new ArrayList<>();
    private List<StrengthLog> strengthLogs = new ArrayList<>();
    private List<CardioSession> cardioSessions = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = FirestoreRepository.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_goals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_goals);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new GoalAdapter(this);
        recyclerView.setAdapter(adapter);
        emptyView = view.findViewById(R.id.text_goals_empty);

        ExtendedFloatingActionButton fab = view.findViewById(R.id.fab_add_goal);
        fab.setOnClickListener(v -> showGoalDialog());

        listeners.add(repository.listenGoals(data -> {
            goals = data;
            refresh();
        }));
        listeners.add(repository.listenStrengthLogs(data -> {
            strengthLogs = data;
            refresh();
        }));
        listeners.add(repository.listenCardio(data -> {
            cardioSessions = data;
            refresh();
        }));
    }

    @Override
    public void onDestroyView() {
        for (ListenerRegistration l : listeners) {
            l.remove();
        }
        listeners.clear();
        super.onDestroyView();
    }

    /** Recalculates the progress of every goal and updates the list. */
    private void refresh() {
        long now = System.currentTimeMillis();
        List<GoalProgress> items = new ArrayList<>();
        for (Goal goal : goals) {
            items.add(GoalProgress.evaluate(goal, strengthLogs, cardioSessions, now));
        }
        // Active first, then achieved, then expired.
        // Inside each group the deadline order from the database is kept, because sort() is stable.
        items.sort((a, b) -> Integer.compare(a.status.ordinal(), b.status.ordinal()));
        adapter.setItems(items);
        emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDeleteGoal(Goal goal) {
        Context appContext = requireContext().getApplicationContext();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_goal)
                .setMessage(getString(R.string.delete_goal_message,
                        GoalAdapter.titleFor(requireContext(), goal)))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) ->
                        repository.deleteGoal(goal.getId())
                                .addOnFailureListener(e -> UiUtils.showSaveError(appContext, e)))
                .show();
    }

    // ------------------------------------------------------------------
    // New goal dialog
    // ------------------------------------------------------------------

    private void showGoalDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_goal, null, false);

        MaterialButtonToggleGroup toggleKind = dialogView.findViewById(R.id.toggle_goal_kind);
        View sectionStrength = dialogView.findViewById(R.id.section_strength);
        View sectionCardio = dialogView.findViewById(R.id.section_cardio);

        TextInputLayout layoutExercise = dialogView.findViewById(R.id.layout_goal_exercise);
        TextInputLayout layoutTargetWeight = dialogView.findViewById(R.id.layout_goal_weight);
        MaterialAutoCompleteTextView inputExercise = dialogView.findViewById(R.id.input_goal_exercise);
        TextInputEditText inputTargetWeight = dialogView.findViewById(R.id.input_goal_weight);

        TextInputLayout layoutCardioType = dialogView.findViewById(R.id.layout_goal_cardio_type);
        TextInputLayout layoutDistance = dialogView.findViewById(R.id.layout_goal_distance);
        TextInputLayout layoutTime = dialogView.findViewById(R.id.layout_goal_time);
        MaterialAutoCompleteTextView inputCardioType = dialogView.findViewById(R.id.input_goal_cardio_type);
        TextInputEditText inputDistance = dialogView.findViewById(R.id.input_goal_distance);
        TextInputEditText inputTime = dialogView.findViewById(R.id.input_goal_time);

        TextInputLayout layoutWeeks = dialogView.findViewById(R.id.layout_goal_weeks);
        TextInputEditText inputWeeks = dialogView.findViewById(R.id.input_goal_weeks);

        // Suggestions from the exercises you have already logged (you can also type a new one).
        inputExercise.setSimpleItems(knownExerciseNames());

        String[] cardioLabels = CardioTypes.labels(requireContext());
        inputCardioType.setSimpleItems(cardioLabels);
        inputCardioType.setText(cardioLabels[0], false);

        // Strength / Cardio switch: show the matching fields.
        toggleKind.check(R.id.button_kind_strength);
        toggleKind.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            boolean strength = checkedId == R.id.button_kind_strength;
            sectionStrength.setVisibility(strength ? View.VISIBLE : View.GONE);
            sectionCardio.setVisibility(strength ? View.GONE : View.VISIBLE);
        });

        // Deadline in weeks, with the end date shown live under the field.
        inputWeeks.setText(String.valueOf(DEFAULT_WEEKS));
        updateDeadlineHelper(layoutWeeks, DEFAULT_WEEKS);
        inputWeeks.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                Integer weeks = Formatters.parseInt(s.toString());
                if (weeks != null && weeks > 0 && weeks <= MAX_WEEKS) {
                    updateDeadlineHelper(layoutWeeks, weeks);
                } else {
                    layoutWeeks.setHelperText(null);
                }
            }
        });

        Context appContext = requireContext().getApplicationContext();

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_goal)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            for (TextInputLayout l : new TextInputLayout[]{layoutExercise, layoutTargetWeight,
                    layoutCardioType, layoutDistance, layoutTime, layoutWeeks}) {
                l.setError(null);
            }

            boolean valid = true;
            Integer weeks = Formatters.parseInt(textOf(inputWeeks));
            if (weeks == null || weeks <= 0 || weeks > MAX_WEEKS) {
                layoutWeeks.setError(getString(R.string.error_weeks_range, MAX_WEEKS));
                valid = false;
            }

            long now = System.currentTimeMillis();
            Goal goal = new Goal();
            goal.setCreatedAt(now);

            if (toggleKind.getCheckedButtonId() == R.id.button_kind_strength) {
                String exercise = textOf(inputExercise).trim();
                Double weight = Formatters.parseDecimal(textOf(inputTargetWeight));
                if (exercise.isEmpty()) {
                    layoutExercise.setError(getString(R.string.error_required));
                    valid = false;
                }
                if (weight == null || weight <= 0) {
                    layoutTargetWeight.setError(getString(R.string.error_invalid_number));
                    valid = false;
                }
                if (valid) {
                    goal.setKind(Goal.KIND_STRENGTH);
                    goal.setTarget(exercise);
                    goal.setTargetWeightKg(weight);
                }
            } else {
                String typeLabel = textOf(inputCardioType).trim();
                Double distance = Formatters.parseDecimal(textOf(inputDistance));
                String timeText = textOf(inputTime).trim();
                Double minutes = timeText.isEmpty() ? Double.valueOf(0) : Formatters.parseDuration(timeText);
                if (typeLabel.isEmpty()) {
                    layoutCardioType.setError(getString(R.string.error_required));
                    valid = false;
                }
                if (distance == null || distance <= 0) {
                    layoutDistance.setError(getString(R.string.error_invalid_number));
                    valid = false;
                }
                // The time is optional, but if it is filled in it must be valid.
                if (minutes == null || minutes < 0 || (!timeText.isEmpty() && minutes == 0)) {
                    layoutTime.setError(getString(R.string.error_invalid_duration));
                    valid = false;
                }
                if (valid) {
                    goal.setKind(Goal.KIND_CARDIO);
                    // Store the language-independent key, like cardio workouts do.
                    goal.setTarget(CardioTypes.keyForLabel(requireContext(), typeLabel));
                    goal.setTargetDistanceKm(distance);
                    goal.setTargetMinutes(minutes);
                }
            }
            if (!valid) {
                return;
            }

            goal.setDeadline(DateUtils.toDayKey(now + weeks * 7L * DateUtils.DAY_MS));
            repository.addGoal(goal).addOnFailureListener(e -> UiUtils.showSaveError(appContext, e));
            dialog.dismiss();
        }));

        dialog.show();
    }

    private void updateDeadlineHelper(TextInputLayout layoutWeeks, int weeks) {
        long deadline = System.currentTimeMillis() + weeks * 7L * DateUtils.DAY_MS;
        layoutWeeks.setHelperText(getString(R.string.goal_deadline_helper, DateUtils.formatDate(deadline)));
    }

    /** Names of all exercises you have logged, without duplicates, in alphabetical order. */
    private String[] knownExerciseNames() {
        Map<String, String> names = new TreeMap<>();
        for (StrengthLog log : strengthLogs) {
            if (log.getNameKey() != null && !names.containsKey(log.getNameKey())) {
                names.put(log.getNameKey(), log.getExerciseName());
            }
        }
        return names.values().toArray(new String[0]);
    }

    private static String textOf(TextView input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
