package com.universe.android;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.appbar.MaterialToolbar;

public class EmailVerificationActivity extends AppCompatActivity {
    public static final String EXTRA_ORGANISATION_NAME = "organisation_name";
    private TextInputEditText emailInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_email_verification);

        // Get organisation name from intent
        String organisationName = getIntent().getStringExtra(EXTRA_ORGANISATION_NAME);

        // Set up views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        emailInput = findViewById(R.id.emailInput);
        MaterialButton verifyButton = findViewById(R.id.verifyButton);

        // Set organisation name
        TextView organisationNameText = findViewById(R.id.organisationNameText);
        organisationNameText.setText(organisationName);

        // Set up toolbar
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set up verify button
        verifyButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            if (isValidEmail(email)) {
                Intent intent = new Intent(this, DashboardActivity.class);
                startActivity(intent);
                finish();
            } else {
                emailInput.setError("Please enter a valid email address");
            }
        });
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}