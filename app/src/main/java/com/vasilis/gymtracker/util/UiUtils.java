package com.vasilis.gymtracker.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.vasilis.gymtracker.R;

/**
 * Small UI helpers.
 */
public final class UiUtils {

    private static final String TAG = "GymTracker";

    private UiUtils() {
    }

    /**
     * Shows a message when a database write fails.
     * Uses the application context because Firebase may answer
     * after the screen that started the write has already closed.
     */
    public static void showSaveError(Context context, Exception e) {
        Log.w(TAG, "Save failed", e);
        Toast.makeText(context.getApplicationContext(), R.string.error_save_failed, Toast.LENGTH_LONG).show();
    }
}
