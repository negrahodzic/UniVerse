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

public class LoginActivity extends AppCompatActivity {
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Initialize views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        MaterialButton forgotPasswordButton = findViewById(R.id.forgotPasswordButton);

        // Set up toolbar
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set up login button
        loginButton.setOnClickListener(v -> loginUser());

        // Set up forgot password button
        forgotPasswordButton.setOnClickListener(v -> showForgotPasswordDialog());

        // Pre-fill email if provided
        String prefillEmail = getIntent().getStringExtra("email");
        if (prefillEmail != null) {
            emailInput.setText(prefillEmail);
            passwordInput.requestFocus(); // Focus on password field
        }
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        // Reset errors
        emailLayout.setError(null);
        passwordLayout.setError(null);

        // Validate inputs
        if (email.isEmpty()) {
            emailLayout.setError("Email is required");
            return;
        }

        if (password.isEmpty()) {
            passwordLayout.setError("Password is required");
            return;
        }

        // Disable login button to prevent multiple clicks
        loginButton.setEnabled(false);

        // Show loading state
        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");


        // Attempt login
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    // Reset button state
                    loginButton.setEnabled(true);
                    loginButton.setText("Login");

                    if (task.isSuccessful()) {
                        // Check email verification
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            // Navigate to dashboard
                            Intent intent = new Intent(this, DashboardActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else if (user != null) {
                            // Email not verified
                            Toast.makeText(this, "Please verify your email", Toast.LENGTH_SHORT).show();

                            user.sendEmailVerification();
                            loginButton.setEnabled(true);
                        }
                    } else {
                        // Login failed
                        Toast.makeText(this, "Authentication failed: " +
                                        (task.getException() != null ? task.getException().getLocalizedMessage() : "Unknown error"),
                                Toast.LENGTH_SHORT).show();
                        loginButton.setEnabled(true);
                    }
                });
    }

    private void showForgotPasswordDialog() {
        String email = emailInput.getText().toString().trim();
        if (email.isEmpty()) {
            emailLayout.setError("Enter your email first");
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Password reset email sent to " + email,
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this,
                                "Failed to send reset email: " +
                                        task.getException().getLocalizedMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already logged in and verified
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}