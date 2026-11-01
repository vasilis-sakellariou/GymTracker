package com.vasilis.gymtracker.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.vasilis.gymtracker.MainActivity;
import com.vasilis.gymtracker.R;
import com.vasilis.gymtracker.data.FirestoreRepository;

/**
 * Sign-up for a new user: name, email, password and password confirmation.
 */
public class SignupActivity extends AppCompatActivity {

    // Firebase requires at least 6 characters.
    private static final int MIN_PASSWORD_LENGTH = 6;

    private FirebaseAuth auth;

    private TextInputLayout layoutName;
    private TextInputLayout layoutEmail;
    private TextInputLayout layoutPassword;
    private TextInputLayout layoutConfirm;
    private TextInputEditText inputName;
    private TextInputEditText inputEmail;
    private TextInputEditText inputPassword;
    private TextInputEditText inputConfirm;
    private MaterialButton buttonSignup;
    private LinearProgressIndicator progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signup_root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar_signup);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // "back" arrow
        }

        auth = FirebaseAuth.getInstance();
        auth.useAppLanguage();

        layoutName = findViewById(R.id.layout_name);
        layoutEmail = findViewById(R.id.layout_email);
        layoutPassword = findViewById(R.id.layout_password);
        layoutConfirm = findViewById(R.id.layout_confirm);
        inputName = findViewById(R.id.input_name);
        inputEmail = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);
        inputConfirm = findViewById(R.id.input_confirm);
        buttonSignup = findViewById(R.id.button_signup);
        progress = findViewById(R.id.progress_signup);

        buttonSignup.setOnClickListener(v -> attemptSignup());
        // "Already have an account?" → back to the login screen (which is right below this one).
        findViewById(R.id.button_go_login).setOnClickListener(v -> finish());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void attemptSignup() {
        layoutName.setError(null);
        layoutEmail.setError(null);
        layoutPassword.setError(null);
        layoutConfirm.setError(null);

        String name = textOf(inputName).trim();
        String email = textOf(inputEmail).trim();
        String password = textOf(inputPassword);
        String confirm = textOf(inputConfirm);

        // All checks run together, so the user sees every error at once.
        boolean valid = true;
        if (name.isEmpty()) {
            layoutName.setError(getString(R.string.error_required));
            valid = false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError(getString(R.string.auth_error_invalid_email));
            valid = false;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            layoutPassword.setError(getString(R.string.auth_error_weak_password));
            valid = false;
        }
        if (!password.equals(confirm)) {
            layoutConfirm.setError(getString(R.string.error_passwords_mismatch));
            valid = false;
        }
        if (!valid) {
            return;
        }

        setLoading(true);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        setLoading(false);
                        showMessage(AuthErrors.message(this, task.getException()));
                        return;
                    }
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        setLoading(false);
                        return;
                    }
                    // Save the profile in the database (no need to wait for it).
                    FirestoreRepository.getInstance().createUserProfile(name, email);

                    // Set the display name on the account, then open the app.
                    UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build();
                    user.updateProfile(profile).addOnCompleteListener(this, t -> {
                        setLoading(false);
                        // After signing up, the user stays signed in.
                        new SessionManager(this).setRememberMe(true);
                        openMain();
                    });
                });
    }

    private void openMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.INVISIBLE);
        buttonSignup.setEnabled(!loading);
    }

    private void showMessage(String message) {
        Snackbar.make(findViewById(R.id.signup_root), message, Snackbar.LENGTH_LONG).show();
    }

    private static String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
