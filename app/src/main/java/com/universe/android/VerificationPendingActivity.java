package com.universe.android;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class VerificationPendingActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private String email;
    private MaterialButton checkButton;
    private MaterialButton resendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verification_pending);

        auth = FirebaseAuth.getInstance();
        email = getIntent().getStringExtra("email");

        // Set up views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        TextView emailText = findViewById(R.id.emailText);
        checkButton = findViewById(R.id.checkButton);
        resendButton = findViewById(R.id.resendButton);

        // Set up toolbar
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Display email
        emailText.setText(email);

        // Set up buttons
        checkButton.setOnClickListener(v -> checkVerificationStatus());
        resendButton.setOnClickListener(v -> resendVerificationEmail());
    }

    private void checkVerificationStatus() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // Reload user to get fresh verification status
            user.reload().addOnCompleteListener(task -> {
                if (user.isEmailVerified()) {
                    // Email verified, move to account setup
                    Intent intent = new Intent(this, AccountSetupActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Email not verified yet", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void resendVerificationEmail() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            resendButton.setEnabled(false);
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Verification email sent", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Failed to send verification email", Toast.LENGTH_SHORT).show();
                        }
                        resendButton.setEnabled(true);
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check verification status when returning to the app
        checkVerificationStatus();
    }
}