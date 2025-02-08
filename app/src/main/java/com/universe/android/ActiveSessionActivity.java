package com.universe.android;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.universe.android.adapter.ParticipantAdapter;
import com.universe.android.model.Participant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ActiveSessionActivity extends AppCompatActivity {
    private TextView timerText;
    private TextView sessionStatusText;
    private MaterialButton breakButton;
    private MaterialButton endButton;
    private ParticipantAdapter participantAdapter;
    private CountDownTimer timer;
    private boolean isOnBreak = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_active_session);

        // Initialize views
        timerText = findViewById(R.id.timerText);
        sessionStatusText = findViewById(R.id.sessionStatusText);
        breakButton = findViewById(R.id.breakButton);
        endButton = findViewById(R.id.endButton);
        RecyclerView participantsList = findViewById(R.id.participantsList);

        // Set up participants list
        participantAdapter = new ParticipantAdapter();
        participantsList.setLayoutManager(new LinearLayoutManager(this));
        participantsList.setAdapter(participantAdapter);

        // Load sample participants
        loadSampleParticipants();

        // Get session duration from intent (in minutes)
        int duration = getIntent().getIntExtra("duration", 60);
        startTimer(duration * 60 * 1000); // Convert to milliseconds

        // Set up buttons
        breakButton.setOnClickListener(v -> toggleBreak());
        endButton.setOnClickListener(v -> showEndSessionDialog());
    }

    private void loadSampleParticipants() {
        List<Participant> participants = new ArrayList<>();
        participants.add(new Participant("You", true));
        participants.add(new Participant("John", true));
        participants.add(new Participant("Alice", false));
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
                .setMessage("Congratulations! You've earned 100 points!")
                .setPositiveButton("OK", (dialog, which) -> finish())
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