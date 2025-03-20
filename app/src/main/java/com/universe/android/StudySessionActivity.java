package com.universe.android;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.journeyapps.barcodescanner.ScanOptions;
import com.universe.android.util.QrCodeUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class StudySessionActivity extends AppCompatActivity implements QrCodeUtil.QrScanCallback {
    private CircularProgressIndicator timeSelector;
    private TextView timeSelectionText;
    private TextView pointsInfoText;
    private LinearLayout time3Button;
    private LinearLayout time6Button;
    private LinearLayout time9Button;
    private LinearLayout time12Button;
    private MaterialButton createSessionButton;
    private MaterialButton joinSessionButton;

    // Time presets for testing in seconds
    private static final int TIME_3_SEC = 3;
    private static final int TIME_6_SEC = 6;
    private static final int TIME_9_SEC = 9;
    private static final int TIME_12_SEC = 12;

    // Points for each time preset
    private static final int POINTS_3_SEC = 20;
    private static final int POINTS_6_SEC = 40;
    private static final int POINTS_9_SEC = 80;
    private static final int POINTS_12_SEC = 100;

    // Currently selected duration (in seconds for testing)
    private int selectedDuration = TIME_3_SEC;
    private int selectedPoints = POINTS_3_SEC;

    // QR code scanner
    private ActivityResultLauncher<ScanOptions> qrScanLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_study_session);

        // Initialize views
        initializeViews();

        // Set up listeners
        setupListeners();

        // Set up bottom navigation
        setupBottomNavigation();

        // Initialize with default selection
        updateDurationSelection(TIME_3_SEC, POINTS_3_SEC);

        // Setup QR code scanner
        qrScanLauncher = QrCodeUtil.setupScanner(this, this);
    }

    private void initializeViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        timeSelector = findViewById(R.id.timeSelector);
        timeSelectionText = findViewById(R.id.timeSelectionText);
        pointsInfoText = findViewById(R.id.pointsInfoText);
        time3Button = findViewById(R.id.time3Button);
        time6Button = findViewById(R.id.time6Button);
        time9Button = findViewById(R.id.time9Button);
        time12Button = findViewById(R.id.time12Button);
        createSessionButton = findViewById(R.id.createSessionButton);
        joinSessionButton = findViewById(R.id.joinSessionButton);

        // Set up toolbar
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        // Time preset button listeners
        time3Button.setOnClickListener(v -> updateDurationSelection(TIME_3_SEC, POINTS_3_SEC));
        time6Button.setOnClickListener(v -> updateDurationSelection(TIME_6_SEC, POINTS_6_SEC));
        time9Button.setOnClickListener(v -> updateDurationSelection(TIME_9_SEC, POINTS_9_SEC));
        time12Button.setOnClickListener(v -> updateDurationSelection(TIME_12_SEC, POINTS_12_SEC));

        // Create session button listener
        createSessionButton.setOnClickListener(v -> createStudySession());

        // Join session button listener
        joinSessionButton.setOnClickListener(v -> startQrScanner());
    }

    private void updateDurationSelection(int durationSeconds, int points) {
        // Update the selected values
        selectedDuration = durationSeconds;
        selectedPoints = points;

        // Update the circular progress based on maximum time (assuming 12 seconds is max)
        int progress = (durationSeconds * 100) / TIME_12_SEC;
        timeSelector.setProgress(progress);

        // Update text displays
        timeSelectionText.setText(durationSeconds + "s"); // For testing we show seconds
        pointsInfoText.setText("You will earn " + points + " points for completing this session.");
    }

    private void createStudySession() {
        // Launch waiting room with selected settings
        Intent intent = new Intent(this, WaitingRoomActivity.class);
        intent.putExtra("duration", selectedDuration);
        intent.putExtra("points", selectedPoints);
        startActivity(intent);
        finish();
    }

    private void startQrScanner() {
        QrCodeUtil.startScanner(qrScanLauncher, "Scan session QR code to join");
    }

    @Override
    public void onScanSuccess(String result) {
        try {
            Log.d("StudySessionActivity", "QR Scan Result: " + result);
            JSONObject data = QrCodeUtil.parseQrCodeJson(result);

            if (data != null && "session".equals(data.optString("type"))) {
                String scannedSessionId = data.getString("sessionId");
                String hostId = data.getString("hostId");
                String hostUsername = data.getString("hostUsername");

                Log.d("StudySessionActivity", "Parsed session ID: " + scannedSessionId + ", hostId: " + hostId);

                // Launch JoinSessionActivity passing scanned info
                Intent intent = new Intent(this, JoinSessionActivity.class);
                intent.putExtra("sessionId", scannedSessionId);  // Make sure we're using the exact session ID from the QR code
                intent.putExtra("hostId", hostId);
                intent.putExtra("hostUsername", hostUsername);
                startActivity(intent);
            } else {
                showToast("Invalid session QR code format");
                Log.e("StudySessionActivity", "Invalid QR format: " + data);
            }
        } catch (JSONException e) {
            showToast("Error reading QR code: " + e.getMessage());
            Log.e("StudySessionActivity", "JSON parse error: " + e.getMessage(), e);
        } catch (Exception e) {
            showToast("Unexpected error processing QR code");
            Log.e("StudySessionActivity", "Unexpected error: " + e.getMessage(), e);
        }
    }

    @Override
    public void onScanCancelled() {
        // User cancelled scan
    }

    private void showToast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.navigation_study);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_dashboard) {
                Intent intent = new Intent(this, DashboardActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.navigation_study) {
                return true;
            } else if (itemId == R.id.navigation_events) {
                Intent intent = new Intent(this, EventsActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.navigation_leaderboard) {
                Intent intent = new Intent(this, LeaderboardActivity.class);
                startActivity(intent);
                return true;
            }
            else if (itemId == R.id.navigation_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }
}