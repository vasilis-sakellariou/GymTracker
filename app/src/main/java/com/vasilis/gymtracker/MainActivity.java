package com.vasilis.gymtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.vasilis.gymtracker.auth.LoginActivity;
import com.vasilis.gymtracker.auth.SessionManager;
import com.vasilis.gymtracker.ui.cardio.CardioFragment;
import com.vasilis.gymtracker.ui.goals.GoalsFragment;
import com.vasilis.gymtracker.ui.program.ProgramFragment;
import com.vasilis.gymtracker.util.LanguageHelper;

/**
 * Main screen after sign-in.
 *   - Top: toolbar with the chart icon and a menu (Language, Sign out)
 *   - Middle: the content of the selected section
 *   - Bottom: navigation bar Program / Cardio / Goals
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Safety check: if for some reason nobody is signed in, go back to the login screen.
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            goToLogin();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // Padding at the top and sides for the status bar.
        // The BottomNavigationView handles the bottom inset by itself.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Show the user's name (or email) under the title.
        String displayName = user.getDisplayName();
        toolbar.setSubtitle(displayName != null && !displayName.isEmpty() ? displayName : user.getEmail());

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            showSection(item.getItemId());
            return true;
        });
        // Tapping the already selected tab does nothing.
        bottomNav.setOnItemReselectedListener(item -> {
        });

        // First launch: show the Program section.
        // (After a rotation or a language change Android restores the open fragment by itself.)
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_program);
        }
    }

    /** Replaces the fragment in the middle of the screen. */
    private void showSection(int itemId) {
        Fragment fragment;
        if (itemId == R.id.nav_cardio) {
            fragment = new CardioFragment();
        } else if (itemId == R.id.nav_goals) {
            fragment = new GoalsFragment();
        } else {
            fragment = new ProgramFragment();
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_progress) {
            startActivity(new Intent(this, ProgressActivity.class));
            return true;
        }
        if (id == R.id.action_language) {
            LanguageHelper.showLanguageDialog(this);
            return true;
        }
        if (id == R.id.action_logout) {
            confirmLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.logout, (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    new SessionManager(this).clear();
                    goToLogin();
                })
                .show();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
