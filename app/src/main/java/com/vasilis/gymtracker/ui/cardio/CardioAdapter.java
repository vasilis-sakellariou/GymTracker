package com.vasilis.gymtracker.ui.cardio;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.vasilis.gymtracker.R;
import com.vasilis.gymtracker.model.CardioSession;
import com.vasilis.gymtracker.util.CardioTypes;
import com.vasilis.gymtracker.util.DateUtils;
import com.vasilis.gymtracker.util.Formatters;

import java.util.ArrayList;
import java.util.List;

/**
 * Cards for the cardio workouts.
 */
public class CardioAdapter extends RecyclerView.Adapter<CardioAdapter.CardioViewHolder> {

    public interface OnCardioActionListener {
        void onEditCardio(CardioSession session);

        void onDeleteCardio(CardioSession session);
    }

    private final List<CardioSession> items = new ArrayList<>();
    private final OnCardioActionListener listener;

    public CardioAdapter(OnCardioActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<CardioSession> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CardioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cardio, parent, false);
        return new CardioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardioViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CardioViewHolder extends RecyclerView.ViewHolder {

        private final TextView textType;
        private final TextView textDate;
        private final TextView textDetails;
        private final TextView textNote;
        private final MaterialButton buttonEdit;
        private final MaterialButton buttonDelete;

        CardioViewHolder(@NonNull View itemView) {
            super(itemView);
            textType = itemView.findViewById(R.id.text_cardio_type);
            textDate = itemView.findViewById(R.id.text_cardio_date);
            textDetails = itemView.findViewById(R.id.text_cardio_details);
            textNote = itemView.findViewById(R.id.text_cardio_note);
            buttonEdit = itemView.findViewById(R.id.button_edit);
            buttonDelete = itemView.findViewById(R.id.button_delete);
        }

        void bind(CardioSession session, OnCardioActionListener listener) {
            Context ctx = itemView.getContext();
            // The type is stored as a key ("running"); show it in the current language.
            textType.setText(CardioTypes.label(ctx, session.getType()));
            textDate.setText(DateUtils.formatDate(session.getDate()));
            // e.g. "5 km · 25:30 · 5:06 /km"
            textDetails.setText(ctx.getString(R.string.cardio_details,
                    Formatters.number(session.getDistanceKm()),
                    Formatters.duration(session.getDurationMinutes()),
                    Formatters.pace(session.getPaceMinPerKm())));

            String note = session.getNote();
            if (note == null || note.trim().isEmpty()) {
                textNote.setVisibility(View.GONE);
            } else {
                textNote.setVisibility(View.VISIBLE);
                textNote.setText(note);
            }

            itemView.setOnClickListener(v -> listener.onEditCardio(session));
            buttonEdit.setOnClickListener(v -> listener.onEditCardio(session));
            buttonDelete.setOnClickListener(v -> listener.onDeleteCardio(session));
        }
    }
}
