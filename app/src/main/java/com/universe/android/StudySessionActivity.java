package com.universe.android;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

public class StudySessionActivity extends AppCompatActivity {
    private Slider durationSlider;
    private SwitchMaterial bluetoothSwitch;
    private SwitchMaterial wifiSwitch;
    private SwitchMaterial locationSwitch;
    private SwitchMaterial breaksSwitch;
    private TextInputEditText breakIntervalInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_study_session);

        // Initialize views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        durationSlider = findViewById(R.id.durationSlider);
        bluetoothSwitch = findViewById(R.id.bluetoothSwitch);
        wifiSwitch = findViewById(R.id.wifiSwitch);
        locationSwitch = findViewById(R.id.locationSwitch);
        breaksSwitch = findViewById(R.id.breaksSwitch);
        breakIntervalInput = findViewById(R.id.breakIntervalInput);
        MaterialButton createSessionButton = findViewById(R.id.createSessionButton);

        // Set up toolbar
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set up duration slider
        durationSlider.setLabelFormatter(value -> String.format("%.0f minutes", value));

        // Enable/disable break interval based on breaks switch
        breaksSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            breakIntervalInput.setEnabled(isChecked);
        });

        createSessionButton.setOnClickListener(v -> {
            if (validateSettings()) {
                createStudySession();
            }
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

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
            } else if (itemId == R.id.navigation_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    private boolean validateSettings() {
        if (breaksSwitch.isChecked()) {
            String breakInterval = breakIntervalInput.getText().toString();
            if (breakInterval.isEmpty() || Integer.parseInt(breakInterval) < 15) {
                breakIntervalInput.setError("Break interval must be at least 15 minutes");
                return false;
            }
        }
        return true;
    }

    private void createStudySession() {
        // Collect all settings
        int duration = (int) durationSlider.getValue();
        boolean usesBluetooth = bluetoothSwitch.isChecked();
        boolean usesWifi = wifiSwitch.isChecked();
        boolean usesLocation = locationSwitch.isChecked();
        boolean usesBreaks = breaksSwitch.isChecked();
        int breakInterval = breaksSwitch.isChecked() ? Integer.parseInt(breakIntervalInput.getText().toString()) : 0;

        // Launch waiting room
        Intent intent = new Intent(this, WaitingRoomActivity.class);
        intent.putExtra("duration", duration);
        intent.putExtra("bluetooth", usesBluetooth);
        intent.putExtra("wifi", usesWifi);
        intent.putExtra("location", usesLocation);
        intent.putExtra("breaks", usesBreaks);
        intent.putExtra("breakInterval", breakInterval);
        startActivity(intent);
        finish();
    }

    private String getEnabledFeatures() {
        StringBuilder features = new StringBuilder();
        if (bluetoothSwitch.isChecked()) features.append("Bluetooth, ");
        if (wifiSwitch.isChecked()) features.append("WiFi, ");
        if (locationSwitch.isChecked()) features.append("Location, ");
        if (breaksSwitch.isChecked()) features.append("Breaks, ");

        return features.length() > 0 ? features.substring(0, features.length() - 2) : "None";
    }
}
