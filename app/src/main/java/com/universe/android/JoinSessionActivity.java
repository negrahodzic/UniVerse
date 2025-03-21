package com.universe.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.ListenerRegistration;
import com.universe.android.model.StudySession;
import com.universe.android.model.User;
import com.universe.android.repository.SessionRepository;
import com.universe.android.repository.UserRepository;
import com.universe.android.util.ThemeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class JoinSessionActivity extends AppCompatActivity {

    private TextView sessionIdText;
    private TextView hostText;
    private TextView statusText;
    private ProgressBar progressBar;
    private MaterialButton joinButton;
    private MaterialButton cancelButton;

    private String sessionId;
    private String hostId;
    private String hostUsername;

    private UserRepository userRepository;
    private SessionRepository sessionRepository;

    private ListenerRegistration sessionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply organisation theme
        String orgId = ThemeManager.getCurrentOrg(this);
        if (!orgId.isEmpty()) {
            ThemeManager.applyOrganisationTheme(this, orgId);
        }

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_join_session);

        // Get data from intent
        sessionId = getIntent().getStringExtra("sessionId");
        hostId = getIntent().getStringExtra("hostId");
        hostUsername = getIntent().getStringExtra("hostUsername");

        if (sessionId == null || hostId == null) {
            Toast.makeText(this, "Invalid session information", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize repositories
        userRepository = UserRepository.getInstance();
        sessionRepository = SessionRepository.getInstance();

        // Initialize views
        initializeViews();

        // Verify session exists
        verifyAndLoadSession();
    }

    private void initializeViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        sessionIdText = findViewById(R.id.sessionIdText);
        hostText = findViewById(R.id.hostText);
        statusText = findViewById(R.id.statusText);
        progressBar = findViewById(R.id.progressBar);
        joinButton = findViewById(R.id.joinButton);
        cancelButton = findViewById(R.id.cancelButton);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Join Session");
        }

        // Add organisation logo to toolbar
        String orgId = ThemeManager.getCurrentOrg(this);
        if (!orgId.isEmpty()) {
            ThemeManager.addLogoToToolbar(this, toolbar, orgId);
        }

        joinButton.setOnClickListener(v -> joinSession());
        cancelButton.setOnClickListener(v -> finish());

        // Set initial text
        sessionIdText.setText("Session #" + sessionId);
        hostText.setText("Host: " + hostUsername);
        statusText.setText("Verifying session...");
    }

    private void verifyAndLoadSession() {
        progressBar.setVisibility(View.VISIBLE);
        joinButton.setEnabled(false);

        sessionRepository.getSessionById(sessionId).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);

            if (task.isSuccessful() && task.getResult() != null) {
                StudySession session = task.getResult();

                // Verify host matches
                if (!session.getHostId().equals(hostId)) {
                    statusText.setText("Error: Host information doesn't match");
                    return;
                }

                // Check if session has already started
                userRepository.getCurrentUserData().addOnSuccessListener(user -> {
                    if (user.getUid().equals(session.getHostId())) {
                        // User is the host
                        statusText.setText("You are the host. Start the session when ready.");
                        joinButton.setText("Start Session");
                        joinButton.setEnabled(true);
                    } else {
                        // User is a participant
                        statusText.setText("Waiting for host to start the session. " +
                                session.getParticipants().size() + " participants in room.");
                        joinButton.setText("Join");
                        joinButton.setEnabled(true);
                    }
                });

                // Add real-time listener to detect session start
                sessionListener = sessionRepository.listenToSessionUpdates(sessionId, updatedSession -> {
                    runOnUiThread(() -> {
                        if (updatedSession != null) {
                            // Update status with participant count
                            userRepository.getCurrentUserData().addOnSuccessListener(user -> {
                                if (!user.getUid().equals(updatedSession.getHostId())) {
                                    statusText.setText("Waiting for host to start. " +
                                            updatedSession.getParticipants().size() +
                                            " participants in room.");
                                }
                            });

                            // Check if session is started using isStarted() method
                            if (updatedSession.isStarted()) {
                                // Automatically start the session for participants
                                Intent intent = new Intent(this, ActiveSessionActivity.class);
                                intent.putExtra("sessionId", sessionId);
                                intent.putExtra("duration", session.getDurationSeconds());
                                intent.putExtra("points", session.getPointsAwarded());

                                ArrayList<HashMap<String, String>> participantData = new ArrayList<>();
                                for (Map<String, String> p : updatedSession.getParticipants()) {
                                    participantData.add(new HashMap<>(p));
                                }

                                intent.putExtra("participantData", participantData);
                                intent.putExtra("isParticipant", true);
                                startActivity(intent);
                                finish();
                            }
                        }
                    });
                });
            } else {
                statusText.setText("Session not found or has expired");
            }
        });
    }

    private void joinSession() {
        userRepository.getCurrentUserData().addOnSuccessListener(user -> {
            if (user.getUid().equals(hostId)) {
                // Host is starting the session
                startHostSession();
            } else {
                // Participant joining
                addParticipantToSession(user);
            }
        });
    }

    private void startHostSession() {
        // Start the active session activity as host
        Intent intent = new Intent(this, ActiveSessionActivity.class);
        intent.putExtra("sessionId", sessionId);
        intent.putExtra("duration", getIntent().getIntExtra("duration", 3));
        intent.putExtra("points", getIntent().getIntExtra("points", 20));

        // Get participants from the session
        sessionRepository.getSessionById(sessionId).addOnSuccessListener(session -> {
            ArrayList<HashMap<String, String>> participantData = new ArrayList<>();
            for (Map<String, String> p : session.getParticipants()) {
                participantData.add(new HashMap<>(p));
            }

            intent.putExtra("participantData", participantData);
            intent.putExtra("isParticipant", false); // Flag that we're the host
            startActivity(intent);
            finish();
        });
    }

    private void addParticipantToSession(User user) {
        Map<String, String> participantMap = new HashMap<>();
        participantMap.put("userId", user.getUid());
        participantMap.put("username", user.getUsername());
        participantMap.put("nfcId", user.getNfcId() != null ? user.getNfcId() : "");

        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Joining session...");

        sessionRepository.addParticipantToSession(sessionId, participantMap)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    statusText.setText("Successfully joined. Waiting for host to start the session.");
                    joinButton.setEnabled(false);

                    // After joining, update the UI to show all participants including host
                    sessionRepository.getSessionById(sessionId).addOnSuccessListener(updatedSession -> {
                        if (updatedSession != null) {
                            statusText.setText("Successfully joined with " +
                                    updatedSession.getParticipants().size() +
                                    " participants. Waiting for host to start.");
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    statusText.setText("Failed to join: " + e.getMessage());
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sessionListener != null) {
            sessionListener.remove();
        }
    }
}