package com.universe.android;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.universe.android.adapter.ParticipantAdapter;
import com.universe.android.manager.UserManager;
import com.universe.android.model.Participant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ActiveSessionActivity extends AppCompatActivity {
    private TextView timerText;
    private TextView sessionStatusText;
    private MaterialButton breakButton;
    private MaterialButton endButton;
    private ParticipantAdapter participantAdapter;
    private CountDownTimer timer;
    private boolean isOnBreak = false;

    private TextView settingsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_active_session);

        // Initialize views
        timerText = findViewById(R.id.timerText);
        sessionStatusText = findViewById(R.id.sessionStatusText);
        settingsText = findViewById(R.id.settingsText);
        breakButton = findViewById(R.id.breakButton);
        endButton = findViewById(R.id.endButton);
        RecyclerView participantsList = findViewById(R.id.participantsList);

        // Set up participants list
        participantAdapter = new ParticipantAdapter();
        participantsList.setLayoutManager(new LinearLayoutManager(this));
        participantsList.setAdapter(participantAdapter);

        // Get settings from intent
        loadSessionSettings();

        // Get session duration from intent (in minutes)
        int duration = getIntent().getIntExtra("duration", 60);
        startTimer(duration * 1000); // TODO: change it back to duration * 60 * 1000)

        // Set up buttons
        breakButton.setOnClickListener(v -> toggleBreak());
        endButton.setOnClickListener(v -> showEndSessionDialog());
    }

    private void loadSessionSettings() {
        Intent intent = getIntent();
        int duration = intent.getIntExtra("duration", 60);
        boolean usesBluetooth = intent.getBooleanExtra("bluetooth", false);
        boolean usesWifi = intent.getBooleanExtra("wifi", false);
        boolean usesLocation = intent.getBooleanExtra("location", false);
        boolean usesBreaks = intent.getBooleanExtra("breaks", false);
        int breakInterval = intent.getIntExtra("breakInterval", 45);

        // Build settings text
        StringBuilder settings = new StringBuilder();
        settings.append("Duration: ").append(duration).append(" minutes\n");
        settings.append("Features enabled:\n");
        if (usesBluetooth) settings.append("• Bluetooth\n");
        if (usesWifi) settings.append("• WiFi\n");
        if (usesLocation) settings.append("• Location\n");
        if (usesBreaks) settings.append("• Breaks (").append(breakInterval).append(" min intervals)\n");

        settingsText.setText(settings.toString());

        // Load participants
        ArrayList<Participant> participants = new ArrayList<>();
        // Add participants passed from WaitingRoom
        if (intent.hasExtra("participants")) {
            ArrayList<String> participantNames = intent.getStringArrayListExtra("participants");
            for (String name : participantNames) {
                participants.add(new Participant(name, true));
            }
        }
        participantAdapter.setParticipants(participants);
    }


    private void startTimer(long durationMillis) {
        timer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateTimerDisplay(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                sessionStatusText.setText("Session Complete!");
                showSessionCompleteDialog();
            }
        }.start();
    }

    private void updateTimerDisplay(long millisUntilFinished) {
        String time = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millisUntilFinished),
                TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60,
                TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60);
        timerText.setText(time);
    }

    private void toggleBreak() {
        isOnBreak = !isOnBreak;
        breakButton.setText(isOnBreak ? "End Break" : "Take Break");
        sessionStatusText.setText(isOnBreak ? "On Break" : "Session in progress");
        if (isOnBreak) {
            timer.cancel();
        } else {
            // Resume timer with remaining time
            timer.start();
            // TODO: Implement proper break handling
        }
    }

    private void showEndSessionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("End Session")
                .setMessage("Are you sure you want to end this session early?")
                .setPositiveButton("End Session", (dialog, which) -> {
                    timer.cancel();
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSessionCompleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Session Complete!")
                .setMessage("Congratulations! All participants earned 100 points!")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Get non-anonymous usernames
                    List<String> participants = participantAdapter.getParticipants()
                            .stream()
                            .map(Participant::getName)
                            .collect(Collectors.toList());

                    // Award points using UserManager
                    UserManager.getInstance()
                            .awardSessionPoints(participants, 100)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("Session", "Successfully awarded points to all participants");
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Session", "Error awarding points: " + e.getMessage());
                                Toast.makeText(this,
                                        "Error awarding points. Please contact support.",
                                        Toast.LENGTH_LONG).show();
                                finish();
                            });
                })
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
    }



}