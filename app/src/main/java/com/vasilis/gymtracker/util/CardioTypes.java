package com.vasilis.gymtracker.util;

import android.content.Context;

import com.vasilis.gymtracker.R;

/**
 * Converts between cardio type keys (stored in the database, e.g. "running")
 * and display names (shown on screen, e.g. "Running" or "Τρέξιμο").
 *
 * Storing the key instead of the display name means that switching the app
 * language does not split the same activity into two different types.
 */
public final class CardioTypes {

    private CardioTypes() {
    }

    /** The display names in the current language, in the same order as the keys. */
    public static String[] labels(Context context) {
        return context.getResources().getStringArray(R.array.cardio_types);
    }

    private static String[] keys(Context context) {
        return context.getResources().getStringArray(R.array.cardio_type_keys);
    }

    /** "running" → "Running" (or "Τρέξιμο"). Unknown values are shown as they are. */
    public static String label(Context context, String key) {
        String[] keys = keys(context);
        String[] labels = labels(context);
        for (int i = 0; i < keys.length && i < labels.length; i++) {
            if (keys[i].equals(key)) {
                return labels[i];
            }
        }
        return key == null ? "" : key;
    }

    /** "Running" (or "Τρέξιμο") → "running". Unknown values are stored as they are. */
    public static String keyForLabel(Context context, String label) {
        String[] keys = keys(context);
        String[] labels = labels(context);
        for (int i = 0; i < keys.length && i < labels.length; i++) {
            if (labels[i].equals(label)) {
                return keys[i];
            }
        }
        return label;
    }
}
