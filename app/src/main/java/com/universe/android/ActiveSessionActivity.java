package com.universe.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.universe.android.adapter.ParticipantAdapter;
import com.universe.android.manager.UserManager;
import com.universe.android.model.Participant;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ActiveSessionActivity extends AppCompatActivity {
    private static final String TAG = "ActiveSessionActivity";

    private TextView timerText;
    private TextView sessionStatusText;
    private MaterialButton breakButton;
    private MaterialButton endButton;
    private ParticipantAdapter participantAdapter;
    private CountDownTimer timer;
    private boolean isOnBreak = false;

    private TextView settingsText;

    // Session data
    private String sessionId;
    private int duration;
    private int points;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_active_session);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

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

        // Start the timer
        startTimer(duration * 1000); // Convert seconds to milliseconds

        // Set up buttons
        breakButton.setOnClickListener(v -> toggleBreak());
        endButton.setOnClickListener(v -> showEndSessionDialog());
    }

    private void loadSessionSettings() {
        Intent intent = getIntent();
        sessionId = intent.getStringExtra("sessionId");
        duration = intent.getIntExtra("duration", 3); // Default to 3 seconds for testing
        points = intent.getIntExtra("points", 20);   // Default to 20 points


        String durationText = (duration < 60) ?
                duration + " seconds" : // For testing use seconds
                (duration / 60) + " minutes"; // For production use minutes

        settingsText.setText("Duration: " + durationText + "\n" +
                "Points reward: " + points + " points");

        // Load participants
        ArrayList<Participant> participants = new ArrayList<>();

        // Check for the participant data
        if (intent.hasExtra("participantData")) {
            ArrayList<HashMap<String, String>> participantData =
                    (ArrayList<HashMap<String, String>>) intent.getSerializableExtra("participantData");

            for (HashMap<String, String> data : participantData) {
                Participant p = new Participant(data.get("name"), true);
                p.setUserId(data.get("userId"));
                p.setActualUsername(data.get("actualUsername"));
                participants.add(p);
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

                // Vibrate device to provide feedback
                vibrate();

                // Mark session as completed in Firestore
                completeStudySession();

                // Show completion dialog
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
        }
    }

    private void showEndSessionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("End Session")
                .setMessage("Are you sure you want to end this session early? No points will be awarded.")
                .setPositiveButton("End Session", (dialog, which) -> {
                    timer.cancel();

                    // Mark session as incomplete in Firestore
                    cancelStudySession();

                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void vibrate() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                // Create a vibration pattern for success feedback
                // Vibrate for 500ms, pause for 100ms, vibrate for 500ms
                long[] pattern = {0, 500, 100, 500};

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
                } else {
                    // Deprecated in API 26
                    vibrator.vibrate(pattern, -1);
                }
            }
        } catch (SecurityException e) {
            // Permission not granted, just log it and continue without vibration
            Log.e(TAG, "Vibration permission not granted", e);
        }
    }

    private void showSessionCompleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Session Complete!")
                .setMessage("Congratulations! All participants earned " + points + " points!")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Get all participants' actual usernames
                    List<String> participants = participantAdapter.getParticipants()
                            .stream()
                            .map(participant -> {
                                // Use the stored actual username if available, otherwise try to extract from name
                                if (participant.getActualUsername() != null && !participant.getActualUsername().isEmpty()) {
                                    return participant.getActualUsername();
                                } else {
                                    // Fallback to extract from display name if needed
                                    String name = participant.getName();
                                    return name.replace(" (Host)", "").trim();
                                }
                            })
                            .collect(Collectors.toList());

                    // Award points using UserManager
                    UserManager.getInstance()
                            .awardSessionPoints(participants, points)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Successfully awarded points to all participants");
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error awarding points: " + e.getMessage());
                                Toast.makeText(this,
                                        "Error awarding points. Please contact support.",
                                        Toast.LENGTH_LONG).show();
                                finish();
                            });
                })
                .setCancelable(false)
                .show();
    }

    private void completeStudySession() {
        if (sessionId == null) return;

        db.collection("sessions").document(sessionId)
                .update(
                        "completed", true,
                        "endTime", new Date()
                )
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update session status: " + e.getMessage());
                });
    }

    private void cancelStudySession() {
        if (sessionId == null) return;

        db.collection("sessions").document(sessionId)
                .update(
                        "completed", false,
                        "endTime", new Date()
                )
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update session status: " + e.getMessage());
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
    }
}