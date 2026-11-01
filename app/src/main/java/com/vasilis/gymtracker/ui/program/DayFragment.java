package com.vasilis.gymtracker.ui.program;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
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

import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.ListenerRegistration;
import com.vasilis.gymtracker.R;
import com.vasilis.gymtracker.data.FirestoreRepository;
import com.vasilis.gymtracker.model.Exercise;
import com.vasilis.gymtracker.util.Formatters;
import com.vasilis.gymtracker.util.UiUtils;

/**
 * The screen of one day: day note, list of exercises and an "Add exercise" button.
 */
public class DayFragment extends Fragment implements ExerciseAdapter.OnExerciseActionListener {

    private static final String ARG_DAY_INDEX = "day_index";
    private static final String TAG = "DayFragment";

    private int dayIndex;
    private FirestoreRepository repository;
    private ExerciseAdapter adapter;
    private TextView emptyView;
    private TextInputEditText noteInput;
    private ListenerRegistration exercisesListener;

    // The note as it is stored. null = not loaded yet, so we must NOT save
    // (otherwise we could overwrite the stored note with an empty one).
    private String savedNote = null;

    /** The day is passed in a Bundle, so it survives if Android recreates the fragment. */
    public static DayFragment newInstance(int dayIndex) {
        DayFragment fragment = new DayFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_DAY_INDEX, dayIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dayIndex = requireArguments().getInt(ARG_DAY_INDEX);
        repository = FirestoreRepository.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_day, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ---- Day note ----
        noteInput = view.findViewById(R.id.input_day_note);
        repository.loadDayNote(dayIndex, note -> {
            if (noteInput == null) {
                return; // the screen closed before the answer arrived
            }
            savedNote = note;
            noteInput.setText(note);
        });
        // Save when the cursor leaves the field.
        noteInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                saveNoteIfChanged();
            }
        });

        // ---- Exercise list ----
        RecyclerView recyclerView = view.findViewById(R.id.recycler_exercises);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ExerciseAdapter(this);
        recyclerView.setAdapter(adapter);
        emptyView = view.findViewById(R.id.text_empty);

        ExtendedFloatingActionButton fab = view.findViewById(R.id.fab_add_exercise);
        fab.setOnClickListener(v -> showExerciseDialog(null));
        // The button shrinks to a plain "+" while scrolling down.
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 0) {
                    fab.shrink();
                } else if (dy < 0) {
                    fab.extend();
                }
            }
        });

        // Live listener: every change (add, edit, delete) updates the list automatically.
        exercisesListener = repository.listenExercises(dayIndex, exercises -> {
            adapter.setItems(exercises);
            emptyView.setVisibility(exercises.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        // Save the note when the user leaves the page.
        saveNoteIfChanged();
    }

    @Override
    public void onDestroyView() {
        // Stop listening, otherwise Firestore would keep updating a screen that no longer exists.
        if (exercisesListener != null) {
            exercisesListener.remove();
            exercisesListener = null;
        }
        noteInput = null;
        super.onDestroyView();
    }

    private void saveNoteIfChanged() {
        if (noteInput == null || savedNote == null) {
            return;
        }
        String current = noteInput.getText() == null ? "" : noteInput.getText().toString();
        if (!current.equals(savedNote)) {
            savedNote = current;
            repository.saveDayNote(dayIndex, current)
                    .addOnFailureListener(e -> Log.w(TAG, "Could not save the day note", e));
        }
    }

    // ------------------------------------------------------------------
    // Card actions
    // ------------------------------------------------------------------

    @Override
    public void onEditExercise(Exercise exercise) {
        showExerciseDialog(exercise);
    }

    @Override
    public void onDeleteExercise(Exercise exercise) {
        Context appContext = requireContext().getApplicationContext();
        // Ask for confirmation before deleting.
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_exercise)
                .setMessage(getString(R.string.delete_exercise_message, exercise.getName()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) ->
                        repository.deleteExercise(exercise.getId())
                                .addOnFailureListener(e -> UiUtils.showSaveError(appContext, e)))
                .show();
    }

    // ------------------------------------------------------------------
    // Add / edit dialog
    // ------------------------------------------------------------------

    /** @param existing null for a new exercise, otherwise the exercise being edited. */
    private void showExerciseDialog(@Nullable Exercise existing) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_exercise, null, false);

        TextInputLayout layoutName = dialogView.findViewById(R.id.layout_name);
        TextInputLayout layoutSets = dialogView.findViewById(R.id.layout_sets);
        TextInputLayout layoutReps = dialogView.findViewById(R.id.layout_reps);
        TextInputLayout layoutWeight = dialogView.findViewById(R.id.layout_weight);
        TextInputEditText inputName = dialogView.findViewById(R.id.input_name);
        TextInputEditText inputSets = dialogView.findViewById(R.id.input_sets);
        TextInputEditText inputReps = dialogView.findViewById(R.id.input_reps);
        TextInputEditText inputWeight = dialogView.findViewById(R.id.input_weight);
        TextInputEditText inputNote = dialogView.findViewById(R.id.input_note);

        // When editing, fill the fields with the current values.
        if (existing != null) {
            inputName.setText(existing.getName());
            inputSets.setText(String.valueOf(existing.getSets()));
            inputReps.setText(String.valueOf(existing.getReps()));
            inputWeight.setText(Formatters.number(existing.getWeightKg()));
            inputNote.setText(existing.getNote());
        }

        Context appContext = requireContext().getApplicationContext();

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(existing == null ? R.string.add_exercise : R.string.edit_exercise)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                // null: our own listener is set below, so the dialog stays open on invalid input.
                .setPositiveButton(R.string.save, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // Clear old error messages.
            layoutName.setError(null);
            layoutSets.setError(null);
            layoutReps.setError(null);
            layoutWeight.setError(null);

            String name = textOf(inputName).trim();
            Integer sets = Formatters.parseInt(textOf(inputSets));
            Integer reps = Formatters.parseInt(textOf(inputReps));
            Double weight = Formatters.parseDecimal(textOf(inputWeight));
            String note = textOf(inputNote).trim();

            // Validate everything, so all errors are shown at once.
            boolean valid = true;
            if (name.isEmpty()) {
                layoutName.setError(getString(R.string.error_required));
                valid = false;
            }
            if (sets == null || sets <= 0) {
                layoutSets.setError(getString(R.string.error_invalid_number));
                valid = false;
            }
            if (reps == null || reps <= 0) {
                layoutReps.setError(getString(R.string.error_invalid_number));
                valid = false;
            }
            if (weight == null || weight < 0) {
                layoutWeight.setError(getString(R.string.error_invalid_number));
                valid = false;
            }
            if (!valid) {
                return; // the dialog stays open
            }

            Task<Void> task;
            if (existing == null) {
                task = repository.addExercise(new Exercise(dayIndex, name, sets, reps, weight, note));
            } else {
                existing.setName(name);
                existing.setSets(sets);
                existing.setReps(reps);
                existing.setWeightKg(weight);
                existing.setNote(note);
                task = repository.updateExercise(existing);
            }
            // We don't wait for the server: Firestore updates the list immediately
            // (and syncs when there is internet). We only show a message if it fails.
            task.addOnFailureListener(e -> UiUtils.showSaveError(appContext, e));
            dialog.dismiss();
        }));

        dialog.show();
    }

    /** Returns the text of a field, or "" if empty (never null). */
    private static String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
