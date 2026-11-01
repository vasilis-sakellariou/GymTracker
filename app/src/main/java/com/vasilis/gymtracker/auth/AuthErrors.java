package com.vasilis.gymtracker.auth;

import android.content.Context;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthException;
import com.vasilis.gymtracker.R;

/**
 * Turns Firebase errors (technical codes) into clear, translated messages.
 */
public final class AuthErrors {

    private AuthErrors() {
    }

    public static String message(Context context, Exception e) {
        if (e instanceof FirebaseNetworkException) {
            return context.getString(R.string.auth_error_network);
        }
        if (e instanceof FirebaseTooManyRequestsException) {
            return context.getString(R.string.auth_error_too_many);
        }
        if (e instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) e).getErrorCode();
            switch (code) {
                case "ERROR_INVALID_EMAIL":
                    return context.getString(R.string.auth_error_invalid_email);
                case "ERROR_WRONG_PASSWORD":
                case "ERROR_USER_NOT_FOUND":
                case "ERROR_INVALID_CREDENTIAL":
                    // Same message for all three, so we don't reveal which emails have accounts.
                    return context.getString(R.string.auth_error_wrong_credentials);
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    return context.getString(R.string.auth_error_email_in_use);
                case "ERROR_WEAK_PASSWORD":
                    return context.getString(R.string.auth_error_weak_password);
                case "ERROR_USER_DISABLED":
                    return context.getString(R.string.auth_error_disabled);
                case "ERROR_TOO_MANY_REQUESTS":
                    return context.getString(R.string.auth_error_too_many);
                default:
                    break;
            }
        }
        String details = e == null ? "" : e.getLocalizedMessage();
        return context.getString(R.string.auth_error_generic, details);
    }
}
