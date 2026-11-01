package com.vasilis.gymtracker.util;

import java.util.Locale;

/**
 * Formatting and parsing of numbers (kilograms, kilometres, time).
 */
public final class Formatters {

    private Formatters() {
    }

    /**
     * 60.0 → "60", 62.5 → "62.5".
     * Locale.US so the decimal separator is always a dot.
     */
    public static String number(double value) {
        if (value == Math.floor(value)) {
            return String.format(Locale.US, "%d", (long) value);
        }
        return String.format(Locale.US, "%.1f", value);
    }

    /** Parses a decimal written with a comma or a dot ("62,5" or "62.5"). Returns null if invalid. */
    public static Double parseDecimal(String text) {
        if (text == null) {
            return null;
        }
        String cleaned = text.trim().replace(',', '.');
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Parses an integer. Returns null if invalid. */
    public static Integer parseInt(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses a duration in minutes. Accepts:
     *   "25"      → 25 minutes
     *   "25,5"    → 25.5 minutes
     *   "25:30"   → 25 minutes 30 seconds = 25.5
     *   "1:05:00" → 1 hour 5 minutes = 65
     * Returns null if invalid.
     */
    public static Double parseDuration(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String t = text.trim().replace(',', '.');
        if (!t.contains(":")) {
            return parseDecimal(t);
        }
        String[] parts = t.split(":");
        try {
            if (parts.length == 2) {
                int minutes = Integer.parseInt(parts[0].trim());
                int seconds = Integer.parseInt(parts[1].trim());
                if (minutes < 0 || seconds < 0 || seconds >= 60) {
                    return null;
                }
                return minutes + seconds / 60.0;
            }
            if (parts.length == 3) {
                int hours = Integer.parseInt(parts[0].trim());
                int minutes = Integer.parseInt(parts[1].trim());
                int seconds = Integer.parseInt(parts[2].trim());
                if (hours < 0 || minutes < 0 || minutes >= 60 || seconds < 0 || seconds >= 60) {
                    return null;
                }
                return hours * 60 + minutes + seconds / 60.0;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }

    /** 25.5 minutes → "25:30", 65 minutes → "1:05:00". */
    public static String duration(double minutes) {
        long totalSeconds = Math.round(minutes * 60);
        long hours = totalSeconds / 3600;
        long mins = (totalSeconds % 3600) / 60;
        long secs = totalSeconds % 60;
        if (hours > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, mins, secs);
        }
        return String.format(Locale.US, "%d:%02d", mins, secs);
    }

    /** Pace: 5.1 min/km → "5:06 /km". */
    public static String pace(double minutesPerKm) {
        return duration(minutesPerKm) + " /km";
    }

    /**
     * "  Bench Press " and "bench press" should count as the same exercise,
     * so we trim spaces and lower-case everything before comparing.
     */
    public static String normalizeKey(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
