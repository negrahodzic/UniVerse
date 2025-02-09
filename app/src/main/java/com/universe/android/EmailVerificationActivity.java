package com.universe.android;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.universe.android.manager.OrganisationManager;

import java.util.UUID;

public class EmailVerificationActivity extends AppCompatActivity {
    public static final String EXTRA_ORGANISATION_NAME = "organisation_name";
    public static final String EXTRA_ORGANISATION_ID = "organisation_id";

    private TextInputEditText emailInput;
    private MaterialButton registerButton;
    private MaterialButton loginButton;
    private String organisationId;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_email_verification);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Get organisation info from intent
        String organisationName = getIntent().getStringExtra(EXTRA_ORGANISATION_NAME);
        organisationId = getIntent().getStringExtra(EXTRA_ORGANISATION_ID);
        Log.d("EmailVerification", "Organisation ID: " + organisationId);

        // Set up views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        emailInput = findViewById(R.id.emailInput);
        registerButton = findViewById(R.id.registerButton);
        loginButton = findViewById(R.id.loginButton);

        // Set organisation name
        TextView organisationNameText = findViewById(R.id.organisationNameText);
        organisationNameText.setText(organisationName);

        // Set up toolbar
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set up register button
        registerButton.setOnClickListener(v -> validateAndSendVerification());

        // Set up login button
        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            if (!isValidEmail(email)) {
                emailInput.setError("Please enter a valid email address");
                return;
            }

            // Validate domain before proceeding to login
            OrganisationManager.getInstance()
                    .validateEmailDomain(organisationId, email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult()) {
                            // Domain is valid, proceed to login screen
                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.putExtra("email", email);
                            startActivity(intent);
                        } else {
                            emailInput.setError("Please use your university email address");
                        }
                    })
                    .addOnFailureListener(e -> {
                        handleError("Error checking email domain");
                    });
        });
    }

    private void validateAndSendVerification() {
        String email = emailInput.getText().toString().trim();

        if (!isValidEmail(email)) {
            emailInput.setError("Please enter a valid email address");
            return;
        }

        registerButton.setEnabled(false);
        loginButton.setEnabled(false);

        // First validate domain
        OrganisationManager.getInstance()
                .validateEmailDomain(organisationId, email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult()) {
                        // Domain is valid, create account with temporary password
                        String tempPassword = UUID.randomUUID().toString();

                        // Create Firebase account
                        auth.createUserWithEmailAndPassword(email, tempPassword)
                                .addOnCompleteListener(authTask -> {
                                    if (authTask.isSuccessful()) {
                                        // Send verification email
                                        auth.getCurrentUser().sendEmailVerification()
                                                .addOnCompleteListener(emailTask -> {
                                                    if (emailTask.isSuccessful()) {
                                                        // Move to pending screen
                                                        Intent intent = new Intent(this, VerificationPendingActivity.class);
                                                        intent.putExtra("email", email);
                                                        startActivity(intent);
                                                        finish();
                                                    } else {
                                                        handleError("Failed to send verification email");
                                                    }
                                                });
                                    } else {
                                        if (authTask.getException() instanceof FirebaseAuthUserCollisionException) {
                                            emailInput.setError("An account with this email already exists");
                                        } else {
                                            handleError("Failed to create account");
                                        }
                                    }
                                    registerButton.setEnabled(true);
                                    loginButton.setEnabled(true);
                                });
                    } else {
                        emailInput.setError("Please use your university email address");
                        registerButton.setEnabled(true);
                        loginButton.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    handleError("Error checking email domain");
                    registerButton.setEnabled(true);
                    loginButton.setEnabled(true);
                });
    }

    private void handleError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}