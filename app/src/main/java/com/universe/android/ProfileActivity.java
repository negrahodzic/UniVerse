package com.universe.android;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.universe.android.manager.UserManager;

public class ProfileActivity extends AppCompatActivity {
    private TextView usernameText;
    private TextView organisationText;
    private TextView totalPointsText;
    private TextView currentLevelText;
    private TextView globalRankText;
    private TextView totalSessionsText;
    private TextView totalHoursText;
    private TextView avgSessionLengthText;
    private TextView eventsAttendedText;
    private TextView pointsSpentText;

    private MaterialButton logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        // Initialize views
        initializeViews();

        // Load user data
        loadUserData();

        // Setup bottom navigation
        setupBottomNavigation();
    }

    private void initializeViews() {
        usernameText = findViewById(R.id.usernameText);
        organisationText = findViewById(R.id.organisationText);
        totalPointsText = findViewById(R.id.totalPointsText);
        currentLevelText = findViewById(R.id.currentLevelText);
        globalRankText = findViewById(R.id.globalRankText);
        totalSessionsText = findViewById(R.id.totalSessionsText);
        totalHoursText = findViewById(R.id.totalHoursText);
        avgSessionLengthText = findViewById(R.id.avgSessionLengthText);
        eventsAttendedText = findViewById(R.id.eventsAttendedText);
        pointsSpentText = findViewById(R.id.pointsSpentText);
        logoutButton = findViewById(R.id.logoutButton);

        logoutButton.setOnClickListener(v -> logout());
    }

    private void loadUserData() {
        UserManager.getInstance().getCurrentUserData()
                .addOnSuccessListener(user -> {
                    if (user != null) {
                        // Set basic info
                        usernameText.setText(user.getUsername());
                        organisationText.setText(user.getOrganisationId());

                        // Set stats
                        totalPointsText.setText(String.valueOf(user.getPoints()));
                        currentLevelText.setText(String.valueOf(calculateLevel(user.getPoints())));
                        globalRankText.setText("#-"); // TODO: Implement rank calculation

                        // Study stats
                        long totalHours = user.getTotalStudyTime() / 60; // Convert minutes to hours
                        totalHoursText.setText(String.valueOf(totalHours));

                        // Set other stats to 0 for now
                        totalSessionsText.setText("0");
                        avgSessionLengthText.setText("0h");
                        eventsAttendedText.setText("0");
                        pointsSpentText.setText("0");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                });
    }

    private int calculateLevel(int points) {
        return Math.max(1, points / 1000 + 1);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_dashboard) {
                startActivity(new Intent(this, DashboardActivity.class));
                return true;
            } else if (itemId == R.id.navigation_study) {
                startActivity(new Intent(this, StudySessionActivity.class));
                return true;
            } else if (itemId == R.id.navigation_events) {
                startActivity(new Intent(this, EventsActivity.class));
                return true;
            } else if (itemId == R.id.navigation_profile) {
                return true;
            }
            return false;
        });
    }

    private void logout() {
        UserManager.getInstance().signOut();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


}