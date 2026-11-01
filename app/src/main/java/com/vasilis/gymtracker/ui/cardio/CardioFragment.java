package com.vasilis.gymtracker.ui.cardio;

import android.content.Context;
import android.os.Bundle;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.ListenerRegistration;
import com.vasilis.gymtracker.R;
import com.vasilis.gymtracker.data.FirestoreRepository;
import com.vasilis.gymtracker.model.CardioSession;
import com.vasilis.gymtracker.util.CardioTypes;
import com.vasilis.gymtracker.util.DateUtils;
import com.vasilis.gymtracker.util.Formatters;
import com.vasilis.gymtracker.util.UiUtils;

import java.util.List;

/**
 * "Cardio" section: summary of the last 7 days, list of workouts and an "Add cardio" button.
 */
public class CardioFragment extends Fragment implements CardioAdapter.OnCardioActionListener {

    private FirestoreRepository repository;
    private CardioAdapter adapter;
    private TextView emptyView;
    private TextView summaryView;
    private ListenerRegistration cardioListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = FirestoreRepository.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cardio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_cardio);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CardioAdapter(this);
        recyclerView.setAdapter(adapter);
        emptyView = view.findViewById(R.id.text_cardio_empty);
        summaryView = view.findViewById(R.id.text_cardio_summary);

        ExtendedFloatingActionButton fab = view.findViewById(R.id.fab_add_cardio);
        fab.setOnClickListener(v -> showCardioDialog(null));
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

        cardioListener = repository.listenCardio(sessions -> {
            adapter.setItems(sessions);
            emptyView.setVisibility(sessions.isEmpty() ? View.VISIBLE : View.GONE);
            updateSummary(sessions);
        });
    }

    @Override
    public void onDestroyView() {
        if (cardioListener != null) {
            cardioListener.remove();
            cardioListener = null;
        }
        super.onDestroyView();
    }

    /** "Last 7 days: 3 workouts · 15.2 km · 1:20:00" */
    private void updateSummary(List<CardioSession> sessions) {
        long fromDay = DateUtils.todayKey() - 6 * DateUtils.DAY_MS; // today + the 6 previous days
        int count = 0;
        double km = 0;
        double minutes = 0;
        for (CardioSession s : sessions) {
            if (DateUtils.toDayKey(s.getDate()) >= fromDay) {
                count++;
                km += s.getDistanceKm();
                minutes += s.getDurationMinutes();
            }
        }
        // getQuantityString picks "1 workout" or "3 workouts" (and the Greek equivalents).
        String workouts = getResources().getQuantityString(R.plurals.workouts_count, count, count);
        summaryView.setText(getString(R.string.cardio_week_summary,
                workouts, Formatters.number(Math.round(km * 10) / 10.0), Formatters.duration(minutes)));
    }

    // ------------------------------------------------------------------
    // Card actions
    // ------------------------------------------------------------------

    @Override
    public void onEditCardio(CardioSession session) {
        showCardioDialog(session);
    }

    @Override
    public void onDeleteCardio(CardioSession session) {
        Context appContext = requireContext().getApplicationContext();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_cardio)
                .setMessage(R.string.delete_cardio_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) ->
                        repository.deleteCardio(session.getId())
                                .addOnFailureListener(e -> UiUtils.showSaveError(appContext, e)))
                .show();
    }

    // ------------------------------------------------------------------
    // Add / edit dialog
    // ------------------------------------------------------------------

    private void showCardioDialog(@Nullable CardioSession existing) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_cardio, null, false);

        TextInputLayout layoutType = dialogView.findViewById(R.id.layout_cardio_type);
        TextInputLayout layoutDistance = dialogView.findViewById(R.id.layout_distance);
        TextInputLayout layoutDuration = dialogView.findViewById(R.id.layout_duration);
        MaterialAutoCompleteTextView inputType = dialogView.findViewById(R.id.input_cardio_type);
        TextInputEditText inputDistance = dialogView.findViewById(R.id.input_distance);
        TextInputEditText inputDuration = dialogView.findViewById(R.id.input_duration);
        TextInputEditText inputNote = dialogView.findViewById(R.id.input_cardio_note);
        MaterialButton buttonDate = dialogView.findViewById(R.id.button_cardio_date);

        // Dropdown with the cardio types in the current language (Running, Cycling, ...).
        String[] typeLabels = CardioTypes.labels(requireContext());
        inputType.setSimpleItems(typeLabels);

        // One-element array: lambdas can only use "effectively final" variables,
        // so we keep the date inside an array to be able to change it.
        final long[] selectedDate = {DateUtils.todayKey()};

        if (existing != null) {
            // false: don't open the dropdown while setting the text
            inputType.setText(CardioTypes.label(requireContext(), existing.getType()), false);
            inputDistance.setText(Formatters.number(existing.getDistanceKm()));
            inputDuration.setText(Formatters.duration(existing.getDurationMinutes()));
            inputNote.setText(existing.getNote());
            selectedDate[0] = existing.getDate();
        } else {
            inputType.setText(typeLabels[0], false);
        }
        buttonDate.setText(DateUtils.formatDate(selectedDate[0]));

        // Date picker (future dates are not allowed).
        buttonDate.setOnClickListener(v -> {
            CalendarConstraints constraints = new CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointBackward.now())
                    .build();
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.choose_date)
                    .setSelection(DateUtils.toPickerSelection(selectedDate[0]))
                    .setCalendarConstraints(constraints)
                    .build();
            picker.addOnPositiveButtonClickListener(selection -> {
                selectedDate[0] = DateUtils.fromPickerSelection(selection);
                buttonDate.setText(DateUtils.formatDate(selectedDate[0]));
            });
            picker.show(getChildFragmentManager(), "cardio_date");
        });

        Context appContext = requireContext().getApplicationContext();

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(existing == null ? R.string.add_cardio : R.string.edit_cardio)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            layoutType.setError(null);
            layoutDistance.setError(null);
            layoutDuration.setError(null);

            String typeLabel = inputType.getText() == null ? "" : inputType.getText().toString().trim();
            Double distance = Formatters.parseDecimal(textOf(inputDistance));
            Double duration = Formatters.parseDuration(textOf(inputDuration));
            String note = textOf(inputNote).trim();

            boolean valid = true;
            if (typeLabel.isEmpty()) {
                layoutType.setError(getString(R.string.error_required));
                valid = false;
            }
            if (distance == null || distance <= 0) {
                layoutDistance.setError(getString(R.string.error_invalid_number));
                valid = false;
            }
            if (duration == null || duration <= 0) {
                layoutDuration.setError(getString(R.string.error_invalid_duration));
                valid = false;
            }
            if (!valid) {
                return;
            }

            CardioSession session = existing != null ? existing : new CardioSession();
            // Store the language-independent key ("running"), not the label ("Running").
            session.setType(CardioTypes.keyForLabel(requireContext(), typeLabel));
            session.setDistanceKm(distance);
            session.setDurationMinutes(duration);
            session.setDate(selectedDate[0]);
            session.setNote(note);

            Task<Void> task = existing == null
                    ? repository.addCardio(session)
                    : repository.updateCardio(session);
            task.addOnFailureListener(e -> UiUtils.showSaveError(appContext, e));
            dialog.dismiss();
        }));

        dialog.show();
    }

    private static String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
