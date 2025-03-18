package com.universe.android;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.universe.android.adapter.ParticipantAdapter;
import com.universe.android.model.Participant;
import com.universe.android.model.StudySession;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SessionDetailsActivity extends AppCompatActivity {
    private static final String TAG = "SessionDetailsActivity";

    // UI Components
    private MaterialToolbar toolbar;
    private TextView sessionDateText;
    private TextView sessionDurationText;
    private TextView sessionStatusText;
    private TextView pointsAwardedText;
    private TextView participantCountText;
    private RecyclerView participantsRecyclerView;
    private ProgressBar loadingProgressBar;
    private View contentLayout;

    // Data
    private String sessionId;
    private FirebaseFirestore db;
    private ParticipantAdapter participantAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_details);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Get session ID from intent
        sessionId = getIntent().getStringExtra("session_id");
        if (sessionId == null) {
            Toast.makeText(this, "Error: Session ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI components
        initializeViews();

        // Load session data
        loadSessionData();
    }

    private void initializeViews() {
        // Find views
        toolbar = findViewById(R.id.toolbar);
        sessionDateText = findViewById(R.id.sessionDateText);
        sessionDurationText = findViewById(R.id.sessionDurationText);
        sessionStatusText = findViewById(R.id.sessionStatusText);
        pointsAwardedText = findViewById(R.id.pointsAwardedText);
        participantCountText = findViewById(R.id.participantCountText);
        participantsRecyclerView = findViewById(R.id.participantsRecyclerView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        contentLayout = findViewById(R.id.contentLayout);

        // Set up toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Session Details");
        }

        // Set up recycler view
        participantAdapter = new ParticipantAdapter();
        participantsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        participantsRecyclerView.setAdapter(participantAdapter);

        // Show loading state
        loadingProgressBar.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.GONE);
    }

    private void loadSessionData() {
        DocumentReference sessionRef = db.collection("sessions").document(sessionId);

        sessionRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                StudySession session = documentSnapshot.toObject(StudySession.class);
                if (session != null) {
                    displaySessionData(session);
                    loadParticipants(session);
                } else {
                    showError("Failed to parse session data");
                }
            } else {
                showError("Session not found");
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading session data", e);
            showError("Error loading session: " + e.getMessage());
        });
    }

    private void displaySessionData(StudySession session) {
        // Format date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy • h:mm a", Locale.US);
        String formattedDate = session.getStartTime() != null ?
                dateFormat.format(session.getStartTime()) : "Unknown date";
        sessionDateText.setText(formattedDate);

        // Format duration
        String durationText;
        if (session.getDurationSeconds() < 60) {
            durationText = session.getDurationSeconds() + " seconds"; // For testing
        } else if (session.getDurationSeconds() < 3600) {
            durationText = (session.getDurationSeconds() / 60) + " minutes";
        } else {
            int hours = session.getDurationSeconds() / 3600;
            int minutes = (session.getDurationSeconds() % 3600) / 60;
            durationText = hours + "h " + (minutes > 0 ? minutes + "m" : "");
        }
        sessionDurationText.setText(durationText);

        // Set status
        if (session.isCompleted()) {
            sessionStatusText.setText("Completed");
            sessionStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            sessionStatusText.setText("Incomplete");
            sessionStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        }

        // Set points
        pointsAwardedText.setText(session.getPointsAwarded() + " points");

        // Update UI state
        loadingProgressBar.setVisibility(View.GONE);
        contentLayout.setVisibility(View.VISIBLE);
    }

    private void loadParticipants(StudySession session) {
        if (session.getParticipants() == null || session.getParticipants().isEmpty()) {
            participantCountText.setText("No participants");
            return;
        }

        participantCountText.setText(session.getParticipants().size() + " participants");

        List<Participant> participants = new ArrayList<>();
        for (Map<String, String> participantMap : session.getParticipants()) {
            String username = participantMap.get("username");
            String userId = participantMap.get("userId");

            if (username != null) {
                Participant participant = new Participant(username, true);
                participant.setUserId(userId);
                participant.setActualUsername(username);
                participants.add(participant);
            }
        }

        participantAdapter.setParticipants(participants);
    }

    private void showError(String message) {
        loadingProgressBar.setVisibility(View.GONE);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}