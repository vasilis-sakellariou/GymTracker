package com.vasilis.gymtracker.auth;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Remembers whether the user ticked "Keep me signed in".
 *
 * Firebase keeps the user signed in on its own.
 * If the user did NOT tick the option, LoginActivity signs them out
 * the next time the app starts.
 */
public class SessionManager {

    private static final String PREFS_NAME = "session_prefs";
    private static final String KEY_REMEMBER_ME = "remember_me";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setRememberMe(boolean remember) {
        prefs.edit().putBoolean(KEY_REMEMBER_ME, remember).apply();
    }

    public boolean isRememberMe() {
        return prefs.getBoolean(KEY_REMEMBER_ME, false);
    }

    /** Called on sign-out. */
    public void clear() {
        prefs.edit().clear().apply();
    }
}
