package com.universe.android;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.universe.android.model.User;

public class AccountSetupActivity extends AppCompatActivity {
    private TextInputLayout usernameLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmPasswordLayout;
    private TextInputEditText usernameInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private MaterialButton completeButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account_setup);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        usernameLayout = findViewById(R.id.usernameLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        completeButton = findViewById(R.id.completeButton);

        // Set up toolbar
        setSupportActionBar(toolbar);

        // Set up complete button
        completeButton.setOnClickListener(v -> validateAndComplete());
    }

    private void validateAndComplete() {
        // Reset errors
        usernameLayout.setError(null);
        passwordLayout.setError(null);
        confirmPasswordLayout.setError(null);

        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        // Validate inputs
        if (username.isEmpty()) {
            usernameLayout.setError("Username is required");
            return;
        }

        if (password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError("Passwords don't match");
            return;
        }

        // Disable button while processing
        completeButton.setEnabled(false);

        // Get current user
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "No authenticated user found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Update password
        user.updatePassword(password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateUserProfile(user.getUid(), username);
                    } else {
                        handleError("Failed to update password");
                        completeButton.setEnabled(true);
                    }
                });
    }

    private void updateUserProfile(String uid, String username) {
        // Create new user object
        User newUser = new User(
                uid,
                auth.getCurrentUser().getEmail(),
                username,
                getSharedPreferences("universe", MODE_PRIVATE).getString("selected_org_id", "")
        );

        db.collection("users").document(uid)
                .set(newUser)
                .addOnSuccessListener(aVoid -> {
                    // Move to dashboard
                    Intent intent = new Intent(this, DashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    handleError("Failed to create profile: " + e.getMessage());
                    completeButton.setEnabled(true);
                });
    }


    private void handleError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in
        if (auth.getCurrentUser() == null) {
            // No user signed in, return to start
            finish();
        }
    }
}