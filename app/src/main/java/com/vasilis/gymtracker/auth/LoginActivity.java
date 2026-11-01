package com.vasilis.gymtracker.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.vasilis.gymtracker.MainActivity;
import com.vasilis.gymtracker.R;
import com.vasilis.gymtracker.util.LanguageHelper;

/**
 * The first screen of the app: sign in with email and password.
 * From here the user can also go to sign-up, reset the password or change the language.
 */
public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private SessionManager session;

    private TextInputLayout layoutEmail;
    private TextInputLayout layoutPassword;
    private TextInputEditText inputEmail;
    private TextInputEditText inputPassword;
    private MaterialCheckBox checkRemember;
    private MaterialButton buttonLogin;
    private LinearProgressIndicator progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        // Firebase emails (e.g. password reset) are sent in the app's current language.
        auth.useAppLanguage();
        session = new SessionManager(this);

        // ---- "Keep me signed in" ----
        // If a user is already signed in:
        //   - with "Keep me signed in" ticked → go straight to the main screen
        //   - without it → sign them out and show the login form
        if (auth.getCurrentUser() != null) {
            if (session.isRememberMe()) {
                openMain();
                return;
            }
            auth.signOut();
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        // Keep the content clear of the status bar, navigation bar and keyboard.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        layoutEmail = findViewById(R.id.layout_email);
        layoutPassword = findViewById(R.id.layout_password);
        inputEmail = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);
        checkRemember = findViewById(R.id.check_remember);
        buttonLogin = findViewById(R.id.button_login);
        progress = findViewById(R.id.progress_login);

        buttonLogin.setOnClickListener(v -> attemptLogin());
        findViewById(R.id.button_forgot).setOnClickListener(v -> showForgotPasswordDialog());
        findViewById(R.id.button_go_signup).setOnClickListener(v ->
                startActivity(new Intent(this, SignupActivity.class)));
        findViewById(R.id.button_language).setOnClickListener(v ->
                LanguageHelper.showLanguageDialog(this));
    }

    // ------------------------------------------------------------------
    // Sign in
    // ------------------------------------------------------------------

    private void attemptLogin() {
        layoutEmail.setError(null);
        layoutPassword.setError(null);

        String email = textOf(inputEmail).trim();
        String password = textOf(inputPassword);

        boolean valid = true;
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError(getString(R.string.auth_error_invalid_email));
            valid = false;
        }
        if (password.isEmpty()) {
            layoutPassword.setError(getString(R.string.error_required));
            valid = false;
        }
        if (!valid) {
            return;
        }

        setLoading(true);
        // Asynchronous call: the answer arrives in addOnCompleteListener.
        // Passing "this" means the listener is dropped if the screen closes.
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        session.setRememberMe(checkRemember.isChecked());
                        openMain();
                    } else {
                        showMessage(AuthErrors.message(this, task.getException()));
                    }
                });
    }

    // ------------------------------------------------------------------
    // Forgot password
    // ------------------------------------------------------------------

    private void showForgotPasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null, false);
        TextInputLayout layoutResetEmail = dialogView.findViewById(R.id.layout_reset_email);
        TextInputEditText inputResetEmail = dialogView.findViewById(R.id.input_reset_email);
        // Pre-fill with the email already typed in the login form, if any.
        inputResetEmail.setText(textOf(inputEmail).trim());

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.forgot_password_title)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.send, null) // our own listener is set below
                .create();

        // We set the click listener after the dialog is shown, so that
        // the dialog stays open when the email is invalid.
        dialog.setOnShowListener(d -> {
            Button sendButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            sendButton.setOnClickListener(v -> {
                layoutResetEmail.setError(null);
                String email = textOf(inputResetEmail).trim();
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    layoutResetEmail.setError(getString(R.string.auth_error_invalid_email));
                    return;
                }
                sendButton.setEnabled(false);
                // Firebase sends an email with a link to set a new password.
                auth.sendPasswordResetEmail(email).addOnCompleteListener(this, task -> {
                    sendButton.setEnabled(true);
                    if (task.isSuccessful()) {
                        dialog.dismiss();
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.reset_sent_title)
                                .setMessage(getString(R.string.reset_sent_message, email))
                                .setPositiveButton(R.string.ok, null)
                                .show();
                    } else {
                        layoutResetEmail.setError(AuthErrors.message(this, task.getException()));
                    }
                });
            });
        });

        dialog.show();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void openMain() {
        Intent intent = new Intent(this, MainActivity.class);
        // CLEAR_TASK: the login screen is removed, so Back does not return to it.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.INVISIBLE);
        buttonLogin.setEnabled(!loading);
    }

    private void showMessage(String message) {
        Snackbar.make(findViewById(R.id.login_root), message, Snackbar.LENGTH_LONG).show();
    }

    private static String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
