package com.vasilis.gymtracker;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.vasilis.gymtracker.data.FirestoreRepository;
import com.vasilis.gymtracker.model.CardioSession;
import com.vasilis.gymtracker.model.Goal;
import com.vasilis.gymtracker.model.StrengthLog;
import com.vasilis.gymtracker.util.CardioTypes;
import com.vasilis.gymtracker.util.DateUtils;
import com.vasilis.gymtracker.util.Formatters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Progress screen: pick a strength exercise or a cardio type from the Spinner
 * and see a line chart over time.
 *
 *   Strength:          kg per day
 *   Cardio distance:   km per day
 *   Cardio pace:       min/km per day (lower = faster)
 *
 * If there is a goal for the selection, it is drawn as a dashed line.
 */
public class ProgressActivity extends AppCompatActivity {

    private static final int KIND_STRENGTH = 0;
    private static final int KIND_CARDIO_DISTANCE = 1;
    private static final int KIND_CARDIO_PACE = 2;

    /** One option of the Spinner. */
    private static class ChartOption {
        final String label;  // what the user sees
        final int kind;      // one of KIND_*
        final String key;    // normalised exercise name or cardio type key

        ChartOption(String label, int kind, String key) {
            this.label = label;
            this.kind = kind;
            this.key = key;
        }

        // The Spinner's ArrayAdapter shows whatever toString() returns.
        @Override
        public String toString() {
            return label;
        }
    }

    private LineChart chart;
    private TextView summaryView;
    private int colorPrimary;
    private int colorOnSurface;
    private int colorGoal;

    private List<StrengthLog> strengthLogs = new ArrayList<>();
    private List<CardioSession> cardioSessions = new ArrayList<>();
    private List<Goal> goals = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_progress);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.progress_root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar_progress);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        chart = findViewById(R.id.line_chart);
        summaryView = findViewById(R.id.text_summary);
        CircularProgressIndicator loading = findViewById(R.id.progress_loading);

        colorPrimary = MaterialColors.getColor(chart, androidx.appcompat.R.attr.colorPrimary);
        colorOnSurface = MaterialColors.getColor(chart, com.google.android.material.R.attr.colorOnSurface);
        colorGoal = ContextCompat.getColor(this, R.color.goal_line);

        setupChart();

        // Load all three at the same time and wait until all of them have arrived.
        FirestoreRepository repo = FirestoreRepository.getInstance();
        Task<List<StrengthLog>> logsTask = repo.fetchStrengthLogs();
        Task<List<CardioSession>> cardioTask = repo.fetchCardioSessions();
        Task<List<Goal>> goalsTask = repo.fetchGoals();

        Tasks.whenAllSuccess(logsTask, cardioTask, goalsTask)
                .addOnSuccessListener(this, results -> {
                    loading.setVisibility(View.GONE);
                    strengthLogs = logsTask.getResult();
                    cardioSessions = cardioTask.getResult();
                    goals = goalsTask.getResult();
                    setupSpinner();
                })
                .addOnFailureListener(this, e -> {
                    loading.setVisibility(View.GONE);
                    showEmpty(getString(R.string.progress_load_error));
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void showEmpty(String message) {
        findViewById(R.id.progress_content).setVisibility(View.GONE);
        TextView emptyView = findViewById(R.id.text_progress_empty);
        emptyView.setText(message);
        emptyView.setVisibility(View.VISIBLE);
    }

    // ------------------------------------------------------------------
    // Spinner
    // ------------------------------------------------------------------

    private void setupSpinner() {
        // TreeMap: alphabetical order and no duplicates (key = normalised name).
        Map<String, String> exercises = new TreeMap<>();
        for (StrengthLog log : strengthLogs) {
            if (log.getNameKey() != null && !exercises.containsKey(log.getNameKey())) {
                exercises.put(log.getNameKey(), log.getExerciseName());
            }
        }
        Map<String, String> cardioTypes = new TreeMap<>();
        for (CardioSession s : cardioSessions) {
            String key = Formatters.normalizeKey(s.getType());
            if (!cardioTypes.containsKey(key)) {
                // Cardio types are stored as keys ("running"); show them in the current language.
                cardioTypes.put(key, CardioTypes.label(this, s.getType()));
            }
        }

        List<ChartOption> options = new ArrayList<>();
        for (Map.Entry<String, String> e : exercises.entrySet()) {
            options.add(new ChartOption(getString(R.string.option_strength, e.getValue()), KIND_STRENGTH, e.getKey()));
        }
        for (Map.Entry<String, String> e : cardioTypes.entrySet()) {
            options.add(new ChartOption(getString(R.string.option_cardio_distance, e.getValue()), KIND_CARDIO_DISTANCE, e.getKey()));
            options.add(new ChartOption(getString(R.string.option_cardio_pace, e.getValue()), KIND_CARDIO_PACE, e.getKey()));
        }

        if (options.isEmpty()) {
            showEmpty(getString(R.string.no_data_for_chart));
            return;
        }

        Spinner spinner = findViewById(R.id.spinner_exercises);
        ArrayAdapter<ChartOption> spinnerAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                showChartFor(options.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    // ------------------------------------------------------------------
    // Chart
    // ------------------------------------------------------------------

    /** Settings shared by all options. */
    private void setupChart() {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.setNoDataText(getString(R.string.chart_no_data));
        chart.setNoDataTextColor(colorOnSurface);
        chart.setExtraOffsets(8f, 16f, 16f, 8f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f); // one-day step
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(colorOnSurface);
        // The X axis holds "days since 1/1/1970". Turn them back into dates.
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return DateUtils.formatShort((long) (value * DateUtils.DAY_MS));
            }
        });

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(colorOnSurface);
        leftAxis.setDrawLimitLinesBehindData(true);
    }

    private void showChartFor(ChartOption option) {
        // 1) One point per day (TreeMap: sorted by date).
        Map<Long, Double> byDay = new TreeMap<>();
        if (option.kind == KIND_STRENGTH) {
            for (StrengthLog log : strengthLogs) {
                if (option.key.equals(log.getNameKey())) {
                    mergeDay(byDay, log.getDate(), log.getWeightKg(), true);
                }
            }
        } else {
            for (CardioSession s : cardioSessions) {
                if (!option.key.equals(Formatters.normalizeKey(s.getType()))) {
                    continue;
                }
                if (option.kind == KIND_CARDIO_DISTANCE) {
                    mergeDay(byDay, s.getDate(), s.getDistanceKm(), true);       // the day's longest distance
                } else {
                    mergeDay(byDay, s.getDate(), s.getPaceMinPerKm(), false);    // the day's fastest pace
                }
            }
        }

        if (byDay.isEmpty()) {
            chart.clear();
            summaryView.setText("");
            return;
        }

        // 2) Chart points (x = day, y = value).
        List<Entry> entries = new ArrayList<>();
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double last = 0;
        for (Map.Entry<Long, Double> e : byDay.entrySet()) {
            float x = (float) (e.getKey() / (double) DateUtils.DAY_MS);
            double y = e.getValue();
            entries.add(new Entry(x, (float) y));
            min = Math.min(min, y);
            max = Math.max(max, y);
            last = y;
        }

        // 3) Goal (if any) as a dashed line.
        Double target = findGoalTarget(option);
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        if (target != null) {
            LimitLine goalLine = new LimitLine(target.floatValue(),
                    getString(R.string.chart_goal_label, formatValue(option.kind, target)));
            goalLine.setLineColor(colorGoal);
            goalLine.setLineWidth(1.5f);
            goalLine.enableDashedLine(14f, 8f, 0f);
            goalLine.setTextColor(colorOnSurface);
            goalLine.setTextSize(11f);
            leftAxis.addLimitLine(goalLine);
            min = Math.min(min, target);
            max = Math.max(max, target);
        }

        // 4) Y axis limits with some space above and below, so the goal line is visible too.
        double range = max - min;
        if (range == 0) {
            range = Math.max(1, Math.abs(max) * 0.1);
        }
        double low = min - range * 0.15;
        if (option.kind != KIND_CARDIO_PACE) {
            low = Math.max(0, low);
        }
        leftAxis.setAxisMinimum((float) low);
        leftAxis.setAxisMaximum((float) (max + range * 0.15));
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return formatValue(option.kind, value);
            }
        });

        // 5) With a single point, widen the X axis a little so it is not stuck to the edges.
        XAxis xAxis = chart.getXAxis();
        if (entries.size() == 1) {
            float x = entries.get(0).getX();
            xAxis.setAxisMinimum(x - 1);
            xAxis.setAxisMaximum(x + 1);
        } else {
            xAxis.resetAxisMinimum();
            xAxis.resetAxisMaximum();
        }

        // 6) Line styling.
        LineDataSet dataSet = new LineDataSet(entries, option.label);
        dataSet.setColor(colorPrimary);
        dataSet.setCircleColor(colorPrimary);
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4.5f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextColor(colorOnSurface);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(option.kind != KIND_CARDIO_PACE);
        dataSet.setFillColor(colorPrimary);
        dataSet.setFillAlpha(40);
        dataSet.setHighLightColor(colorPrimary);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return option.kind == KIND_CARDIO_PACE ? Formatters.duration(value) : Formatters.number(value);
            }
        });

        chart.setData(new LineData(dataSet));
        chart.animateX(600);
        chart.invalidate();

        // 7) Summary below the Spinner.
        int count = entries.size();
        if (option.kind == KIND_STRENGTH) {
            summaryView.setText(getString(R.string.summary_strength,
                    Formatters.number(last), Formatters.number(max), count));
        } else if (option.kind == KIND_CARDIO_DISTANCE) {
            summaryView.setText(getString(R.string.summary_distance,
                    Formatters.number(last), Formatters.number(maxOf(byDay)), count));
        } else {
            summaryView.setText(getString(R.string.summary_pace,
                    Formatters.pace(last), Formatters.pace(minOf(byDay)), count));
        }
    }

    /** Keeps one value per day: the largest (keepMax) or the smallest. */
    private static void mergeDay(Map<Long, Double> byDay, long date, double value, boolean keepMax) {
        long day = DateUtils.toDayKey(date);
        Double current = byDay.get(day);
        if (current == null || (keepMax ? value > current : value < current)) {
            byDay.put(day, value);
        }
    }

    private static double maxOf(Map<Long, Double> byDay) {
        double max = -Double.MAX_VALUE;
        for (double v : byDay.values()) {
            max = Math.max(max, v);
        }
        return max;
    }

    private static double minOf(Map<Long, Double> byDay) {
        double min = Double.MAX_VALUE;
        for (double v : byDay.values()) {
            min = Math.min(min, v);
        }
        return min;
    }

    /** The most recent goal matching the option, or null. */
    private Double findGoalTarget(ChartOption option) {
        Goal latest = null;
        for (Goal g : goals) {
            if (!option.key.equals(Formatters.normalizeKey(g.getTarget()))) {
                continue;
            }
            boolean matches;
            if (option.kind == KIND_STRENGTH) {
                matches = g.isStrengthGoal();
            } else if (option.kind == KIND_CARDIO_DISTANCE) {
                matches = !g.isStrengthGoal();
            } else {
                matches = !g.isStrengthGoal() && g.hasTimeTarget();
            }
            if (matches && (latest == null || g.getCreatedAt() > latest.getCreatedAt())) {
                latest = g;
            }
        }
        if (latest == null) {
            return null;
        }
        if (option.kind == KIND_STRENGTH) {
            return latest.getTargetWeightKg();
        }
        if (option.kind == KIND_CARDIO_DISTANCE) {
            return latest.getTargetDistanceKm();
        }
        return latest.getTargetPace();
    }

    private String formatValue(int kind, double value) {
        switch (kind) {
            case KIND_STRENGTH:
                return Formatters.number(value) + " kg";
            case KIND_CARDIO_DISTANCE:
                return Formatters.number(value) + " km";
            default:
                return Formatters.duration(value);
        }
    }
}
