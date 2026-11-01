package com.vasilis.gymtracker.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Date helpers.
 * All dates are stored as "milliseconds since 1 Jan 1970" (a long).
 */
public final class DateUtils {

    // Milliseconds in one day: 24 hours × 60 minutes × 60 seconds × 1000.
    public static final long DAY_MS = 24L * 60 * 60 * 1000;

    // Private constructor: this class only has static methods.
    private DateUtils() {
    }

    /**
     * Returns 12:00 noon of the same day.
     * Any two moments of the same day give exactly the same number (a "day key").
     * Noon instead of midnight protects against daylight-saving shifts
     * moving the date to the previous day.
     */
    public static long toDayKey(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /** The day key of today. */
    public static long todayKey() {
        return toDayKey(System.currentTimeMillis());
    }

    /**
     * Today's day of the week, with 0 = Monday ... 6 = Sunday.
     * Java counts SUNDAY = 1, MONDAY = 2, ..., SATURDAY = 7,
     * so (javaDay + 5) % 7 converts it: Monday 2 → 0, Sunday 1 → 6.
     */
    public static int todayIndex() {
        int javaDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return (javaDay + 5) % 7;
    }

    /** e.g. 29/10/2026 */
    public static String formatDate(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(millis));
    }

    /** e.g. 29/10 (used on the chart axis) */
    public static String formatShort(long millis) {
        return new SimpleDateFormat("dd/MM", Locale.getDefault()).format(new Date(millis));
    }

    /**
     * MaterialDatePicker returns "midnight UTC" of the selected day.
     * We convert it to noon of the same calendar date in the local time zone.
     */
    public static long fromPickerSelection(long utcMillis) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(utcMillis);
        Calendar local = Calendar.getInstance();
        local.clear();
        local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH),
                utc.get(Calendar.DAY_OF_MONTH), 12, 0, 0);
        return local.getTimeInMillis();
    }

    /** The opposite conversion: local date → "midnight UTC" for MaterialDatePicker. */
    public static long toPickerSelection(long localMillis) {
        Calendar local = Calendar.getInstance();
        local.setTimeInMillis(localMillis);
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH),
                local.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        return utc.getTimeInMillis();
    }
}
