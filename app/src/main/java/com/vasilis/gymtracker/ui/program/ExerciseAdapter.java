package com.vasilis.gymtracker.ui.program;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.vasilis.gymtracker.R;
import com.vasilis.gymtracker.model.Exercise;
import com.vasilis.gymtracker.util.Formatters;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns every Exercise into a card in the list.
 * RecyclerView "recycles" cards while scrolling, so it stays fast even with many items.
 */
public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder> {

    /** The adapter tells the fragment what was tapped; the fragment decides what to do. */
    public interface OnExerciseActionListener {
        void onEditExercise(Exercise exercise);

        void onDeleteExercise(Exercise exercise);
    }

    private final List<Exercise> items = new ArrayList<>();
    private final OnExerciseActionListener listener;

    public ExerciseAdapter(OnExerciseActionListener listener) {
        this.listener = listener;
    }

    /** Replaces the list and redraws it. */
    public void setItems(List<Exercise> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    /** Creates a new (empty) card from res/layout/item_exercise.xml. */
    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise, parent, false);
        return new ExerciseViewHolder(view);
    }

    /** Fills a card with the data of the exercise at "position". */
    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /** Holds the views of one card, so we don't call findViewById on every scroll. */
    static class ExerciseViewHolder extends RecyclerView.ViewHolder {

        private final TextView textName;
        private final TextView textDetails;
        private final TextView textNote;
        private final MaterialButton buttonEdit;
        private final MaterialButton buttonDelete;

        ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_exercise_name);
            textDetails = itemView.findViewById(R.id.text_exercise_details);
            textNote = itemView.findViewById(R.id.text_exercise_note);
            buttonEdit = itemView.findViewById(R.id.button_edit);
            buttonDelete = itemView.findViewById(R.id.button_delete);
        }

        void bind(Exercise exercise, OnExerciseActionListener listener) {
            textName.setText(exercise.getName());
            // e.g. "4 sets × 8 reps · 62.5 kg"
            textDetails.setText(itemView.getContext().getString(R.string.exercise_details,
                    exercise.getSets(), exercise.getReps(), Formatters.number(exercise.getWeightKg())));

            // The note is only shown when there is one.
            String note = exercise.getNote();
            if (note == null || note.trim().isEmpty()) {
                textNote.setVisibility(View.GONE);
            } else {
                textNote.setVisibility(View.VISIBLE);
                textNote.setText(note);
            }

            // Tapping the card or the pencil opens the edit dialog.
            itemView.setOnClickListener(v -> listener.onEditExercise(exercise));
            buttonEdit.setOnClickListener(v -> listener.onEditExercise(exercise));
            buttonDelete.setOnClickListener(v -> listener.onDeleteExercise(exercise));
        }
    }
}
